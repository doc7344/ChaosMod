package com.example.mixin;

import com.example.ChaosMod;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityTickMixin {
    
    @Unique
    private int chaos$craftingTimer = 0;
    @Unique private boolean chaos$jumpStartedOnGround = false;
    @Unique private boolean chaos$wasOnGround = true;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void chaos$serverPlayerTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        chaos$tickFallTrap(player);
        
        // === 恐高症效果 ===
        if (ChaosMod.config.acrophobiaEnabled) {
            chaos$tickAcrophobia(player);
        }
        
        // === 水中溺死效果 ===
        if (ChaosMod.config.waterDamageEnabled) {
            chaos$tickWaterDamage(player);
        }
        
        // === 合成炸弹效果 ===
        if (ChaosMod.config.craftingBombEnabled) {
            chaos$tickCraftingBomb(player);
        }
        
        // === 新增的混沌效果 ===
        // 惊惧磁铁：处理磁化状态的tick逻辑
        com.example.util.ChaosEffects.tickPanicMagnet(player);
        
        // 痛觉扩散：处理带电状态的tick逻辑（原有）
        com.example.util.PainSpreadSystem.tickElectrified(player);
        
        // === v1.7.0 电击地狱级效果 ===
        // 移动税：处理玩家移动累计
        com.example.util.ChaosEffects.handleMovementTax(player);
        
        // 控制癫痫Plus：每5秒扣血处理
        com.example.util.ChaosEffects.tickControlSeizurePlus(player);

        // 第51项：每名玩家独立循环的随机负面效果
        com.example.util.PeriodicNegativeEffectSystem.tickPlayer(player);
    }

    @Unique
    private void chaos$tickFallTrap(ServerPlayerEntity player) {
        boolean onGround = player.isOnGround();
        if (!ChaosMod.config.fallTrapEnabled || player.isCreative() || player.isSpectator()) {
            chaos$jumpStartedOnGround = false;
            chaos$wasOnGround = onGround;
            return;
        }
        if (chaos$wasOnGround && !onGround && player.getVelocity().y > 0.0) {
            chaos$jumpStartedOnGround = true;
        } else if (!chaos$wasOnGround && onGround) {
            if (chaos$jumpStartedOnGround && player.fallDistance < 1.0F && player.getRandom().nextFloat() < 0.20F) {
                player.damage(player.getDamageSources().generic(), 1.0F);
            }
            chaos$jumpStartedOnGround = false;
        }
        chaos$wasOnGround = onGround;
    }
    
    private void chaos$tickAcrophobia(ServerPlayerEntity player) {
        // 检查玩家模式
        if (player.isCreative() || player.isSpectator()) return;
        
        // 检查高度 (Y > 80为危险高度)
        int height = player.getBlockY();
        if (height <= 80) return;
        
        // 每2秒触发一次 (40 ticks = 2 seconds)
        if (player.age % 40 != 0) return;
        
        // 计算基于高度的伤害：越高越痛苦
        float damage = calculateHeightDamage(height);
        
        // 造成恐高症伤害
        player.damage(player.getServerWorld().getDamageSources().magic(), damage);
    }
    
    /**
     * 计算基于高度的恐高症伤害
     * Y=80: 安全线，无伤害
     * Y=81-120: 渐进式恐惧，每10层增加约0.5♥
     * Y=120+: 极度恐高，固定2♥伤害
     */
    private float calculateHeightDamage(int height) {
        if (height <= 80) {
            return 0.0F; // 安全高度
        } else if (height <= 120) {
            // 渐进式恐惧：40层高度内从0增长到2♥
            // 公式：(height - 80) * 0.1F = 每层0.05♥，每10层0.5♥
            return (height - 80) * 0.1F;
        } else {
            // 极度恐高：超过120层固定最大伤害2♥
            return 4.0F; // 4.0F = 2♥
        }
    }
    
    /**
     * 水中溺死效果：触碰水时按原版攻击间隔造成0.5♥伤害
     */
    private void chaos$tickWaterDamage(ServerPlayerEntity player) {
        // 检查玩家模式
        if (player.isCreative() || player.isSpectator()) return;
        
        // 检查玩家是否在水中
        boolean inWater = player.isSubmergedIn(FluidTags.WATER) || player.isTouchingWater();
        if (!inWater) return;
        
        // 检查无敌帧（hurtTime==0表示可以受伤）
        if (player.hurtTime != 0) return;
        
        // 造成0.5♥伤害（1.0F = 0.5♥）
        player.damage(player.getServerWorld().getDamageSources().drown(), 1.0F);
    }
    
    /**
     * 合成炸弹效果：打开工作台超过5秒爆炸
     */
    private void chaos$tickCraftingBomb(ServerPlayerEntity player) {
        // 检查玩家模式
        if (player.isCreative() || player.isSpectator()) {
            chaos$craftingTimer = 0;
            return;
        }
        
        // 检查当前屏幕是否是工作台
        if (player.currentScreenHandler instanceof CraftingScreenHandler) {
            chaos$craftingTimer++;
            
            // 100 ticks = 5秒
            if (chaos$craftingTimer >= 100) {
                // 在玩家位置创建真实爆炸（按Yarn 1.21正确API）
                player.getServerWorld().createExplosion(
                    null, // Entity 爆炸源
                    null, // DamageSource 
                    null, // ExplosionBehavior
                    player.getX(), player.getY(), player.getZ(), // 爆炸位置
                    3.0F, // 爆炸强度
                    true, // createFire
                    World.ExplosionSourceType.BLOCK, // ExplosionSourceType
                    ParticleTypes.EXPLOSION, // ParticleEffect
                    ParticleTypes.EXPLOSION_EMITTER, // ParticleEffect  
                    SoundEvents.ENTITY_GENERIC_EXPLODE // RegistryEntry<SoundEvent>
                );
                
                // 强制关闭界面
                player.closeHandledScreen();
                
                // 重置计时器
                chaos$craftingTimer = 0;
            }
        } else {
            // 界面变化或关闭时重置计时器
            chaos$craftingTimer = 0;
        }
    }
}
