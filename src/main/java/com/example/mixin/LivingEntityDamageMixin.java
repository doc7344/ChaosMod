
package com.example.mixin;

import com.example.ChaosMod;
import com.example.util.DamageRouting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    
    @Unique
    private static final ThreadLocal<Boolean> RANDOM_DAMAGE_REENTRY = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final Map<ServerPlayerEntity, Long> POSITION_SWAP_COOLDOWN = new WeakHashMap<>();
    @Unique
    private static final long POSITION_SWAP_COOLDOWN_TICKS = 2L;
    
    @Unique
    private static final float[] allDamageValues = {
        1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F,  // 0.5♥到5♥
        11.0F, 12.0F, 13.0F, 14.0F, 15.0F, 16.0F, 17.0F, 18.0F, 19.0F, 20.0F  // 5.5♥到10♥
    };
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void chaos$randomizeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity.getWorld().isClient()) return;
        if (DamageRouting.isDirectDamage()) return;

        // 随机伤害值效果：完全替换原版伤害值
        if (ChaosMod.config.randomDamageAmountEnabled) {
            // 防止递归
            if (RANDOM_DAMAGE_REENTRY.get()) return;
            
            float randomDamage = allDamageValues[ThreadLocalRandom.current().nextInt(allDamageValues.length)];
            
            // 取消原版伤害处理
            cir.cancel();
            
            // 置入递归抑制后直接调用damage并setReturnValue(true)
            try {
                RANDOM_DAMAGE_REENTRY.set(true);
                cir.setReturnValue(entity.damage(source, randomDamage));
            } finally {
                RANDOM_DAMAGE_REENTRY.set(false);
            }
        }
    }
    
    @Inject(method = "damage", at = @At("TAIL"))
    private void chaos$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity)(Object)this;
        if (victim.getWorld().isClient()) return;
        if (DamageRouting.isDirectDamage()) return;
        Entity attacker = source.getAttacker();

        if (attacker instanceof ServerPlayerEntity attackerPlayer) {
            com.example.util.AdditionalChaosEffects.handleWeaponSlip(
                attackerPlayer, victim, source, cir.getReturnValue()
            );
        }

        // Buffs when mob hits player + EnderDragon bucket conversion
        if (cir.getReturnValue() && victim instanceof PlayerEntity player && attacker instanceof MobEntity mob) {
            if (!player.isCreative() && !player.isSpectator()) {
                DamageRouting.applyOnHitEffects(player, mob);
                if (ChaosMod.config.enderDragonBucketEnabled && attacker instanceof EnderDragonEntity) {
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack st = player.getInventory().getStack(i);
                        if (!st.isEmpty() && st.getItem() == Items.WATER_BUCKET) {
                            player.getInventory().setStack(i, new ItemStack(Items.MILK_BUCKET));
                        }
                    }
                }
            }
        }

        // Reflect 50% when player damages a mob
        if (ChaosMod.config.mobThornsEnabled && victim instanceof MobEntity mob && attacker instanceof PlayerEntity player) {
            if (!player.isCreative() && !player.isSpectator() && amount > 0.0f) {
                player.damage(player.getDamageSources().thorns(victim), amount * 0.5f);
            }
        }

        // === 攻击玩家回血：攻击其他玩家时自己回复血量 ===
        if (ChaosMod.config.playerHealOnAttackEnabled && cir.getReturnValue() && 
            victim instanceof ServerPlayerEntity victimPlayer && 
            attacker instanceof ServerPlayerEntity attackerPlayer) {
            
            // 防止递归：添加标记避免治疗触发新的伤害事件
            if (!attackerPlayer.isCreative() && !attackerPlayer.isSpectator() && 
                !victimPlayer.isCreative() && !victimPlayer.isSpectator()) {
                
                // 恢复攻击者2.0F血量（1♥）
                attackerPlayer.heal(2.0F);
            }
        }

        // === 位置互换：受伤时与随机队友交换位置 ===
        if (ChaosMod.config.positionSwapEnabled && cir.getReturnValue() && 
            victim instanceof ServerPlayerEntity victimPlayer) {
            
            long currentTick = victimPlayer.getServer().getTicks();
            if (chaos$isPositionSwapEligible(victimPlayer, currentTick)) {
                java.util.List<ServerPlayerEntity> validTargets = victimPlayer.getServer().getPlayerManager()
                    .getPlayerList().stream()
                    .filter(player -> player != victimPlayer)
                    .filter(player -> chaos$isPositionSwapEligible(player, currentTick))
                    .toList();
                
                if (!validTargets.isEmpty()) {
                    // 随机选择一个目标
                    ServerPlayerEntity target = validTargets.get(victimPlayer.getRandom().nextInt(validTargets.size()));
                    chaos$swapPlayerPositions(victimPlayer, target, currentTick);
                }
            }
        }

        // 背刺回血效果已移除，保持30个效果
    }

    @Unique
    private static boolean chaos$isPositionSwapEligible(ServerPlayerEntity player, long currentTick) {
        if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) return false;
        if (player.isDisconnected() || player.isRemoved() || player.isInTeleportationState()) return false;
        Long cooldownUntil = POSITION_SWAP_COOLDOWN.get(player);
        return cooldownUntil == null || currentTick >= cooldownUntil;
    }

    @Unique
    private static void chaos$swapPlayerPositions(ServerPlayerEntity first, ServerPlayerEntity second, long currentTick) {
        ServerWorld firstWorld = first.getServerWorld();
        ServerWorld secondWorld = second.getServerWorld();
        Vec3d firstPos = first.getPos();
        Vec3d secondPos = second.getPos();
        Vec3d firstVelocity = first.getVelocity();
        Vec3d secondVelocity = second.getVelocity();
        float firstYaw = first.getYaw();
        float firstPitch = first.getPitch();
        float secondYaw = second.getYaw();
        float secondPitch = second.getPitch();

        TeleportTarget firstDestination = new TeleportTarget(
            secondWorld, secondPos, firstVelocity, secondYaw, secondPitch, TeleportTarget.NO_OP
        );
        TeleportTarget secondDestination = new TeleportTarget(
            firstWorld, firstPos, secondVelocity, firstYaw, firstPitch, TeleportTarget.NO_OP
        );

        boolean firstMoved = first.teleportTo(firstDestination) != null
            && chaos$isAtSwapDestination(first, secondWorld, secondPos);
        boolean secondMoved = second.teleportTo(secondDestination) != null
            && chaos$isAtSwapDestination(second, firstWorld, firstPos);
        if (firstMoved && secondMoved) {
            long cooldownUntil = currentTick + POSITION_SWAP_COOLDOWN_TICKS;
            POSITION_SWAP_COOLDOWN.put(first, cooldownUntil);
            POSITION_SWAP_COOLDOWN.put(second, cooldownUntil);
            return;
        }

        System.err.println("[ChaosMod][PositionSwap] 交换失败，正在回滚: first="
            + first.getName().getString() + " moved=" + firstMoved
            + ", second=" + second.getName().getString() + " moved=" + secondMoved);

        // 任一方失败时将已经移动的一方恢复，避免只交换一个玩家。
        if (firstMoved) {
            first.teleportTo(new TeleportTarget(
                firstWorld, firstPos, firstVelocity, firstYaw, firstPitch, TeleportTarget.NO_OP
            ));
        }
        if (secondMoved) {
            second.teleportTo(new TeleportTarget(
                secondWorld, secondPos, secondVelocity, secondYaw, secondPitch, TeleportTarget.NO_OP
            ));
        }
    }

    @Unique
    private static boolean chaos$isAtSwapDestination(ServerPlayerEntity player, ServerWorld world, Vec3d pos) {
        return player.getServerWorld() == world && player.getPos().squaredDistanceTo(pos) < 0.01D;
    }
}
