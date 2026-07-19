package com.example.util;

import com.example.ChaosMod;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Extend routing: if original victim is burning (on fire), split/transfer recipients
 * also get a short on-fire effect so the visuals are consistent with the source of damage.
 * (Still no knockback for environmental sources.)
 */
public final class DamageRouting {
    private DamageRouting(){}

    private static final Map<Entity, Integer> CONTACT_CD = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> DIRECT_DAMAGE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> RANDOM_TRANSFER_HANDLED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<PlayerEntity, Integer> NO_HEAL = new WeakHashMap<>();
    private static final Map<PlayerEntity, Boolean> BELOW_THRESHOLD = new WeakHashMap<>();
    
    // === Reverse Damage System ===
    private static final Map<PlayerEntity, Long> LAST_DAMAGE_TIME = new WeakHashMap<>();
    private static final Map<PlayerEntity, Integer> REVERSE_DAMAGE_COUNTER = new WeakHashMap<>();
    private static final int REVERSE_DAMAGE_INTERVAL = 12; // ticks, same as hand attack speed
    private static final int DAMAGE_IMMUNITY_DURATION = 40; // 2 seconds in ticks
    
    // === Fall Trap System (Legacy - now using precise mixin methods) ===
    
    // === Sunburn System ===
    private static final Map<PlayerEntity, Long> LAST_SUNBURN_DAMAGE_TIME = new WeakHashMap<>();
    private static final int SUNBURN_COOLDOWN_TICKS = 20; // 1 second cooldown for helmet damage

    public static boolean contactOnCooldown(Entity e) {
        Integer v = CONTACT_CD.get(e);
        if (v == null) return false;
        if (v <= 0) { CONTACT_CD.remove(e); return false; }
        CONTACT_CD.put(e, v - 1);
        return true;
    }
    public static void armContactCooldown(Entity e, int ticks) { CONTACT_CD.put(e, ticks); }

    public static boolean shouldBlockRouting(PlayerEntity p) {
        return p.hasStatusEffect(StatusEffects.POISON) || p.hasStatusEffect(StatusEffects.WITHER);
    }

    public static void applyOnHitEffects(PlayerEntity player, LivingEntity attacker) {
        if (player.isCreative() || player.isSpectator()) return;
        if (ChaosMod.config.mobIgniteEnabled) player.setOnFireFor(3);
        if (ChaosMod.config.mobSlownessEnabled) {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 20*5, 1));
        }
        if (ChaosMod.config.mobBlindnessEnabled) {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.BLINDNESS, 20, 0));
        }
    }

    private static boolean shouldApplyKnockbackForSource(DamageSource source) {
        try {
            return source.getAttacker() instanceof LivingEntity;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Propagate environmental visuals: if victim is burning, light up recipient briefly as well. */
    private static void propagateOnFireIfNeeded(PlayerEntity victim, PlayerEntity recipient) {
        if (victim.isOnFire()) {
            // Keep it short to avoid grief, just for feedback/visuals
            recipient.setOnFireFor(2); // 2s overlay
        }
    }

    public static boolean routePlayerDamage(PlayerEntity victim, DamageSource source, float amount) {
        if (REENTRY.get()) return false;
        if (DIRECT_DAMAGE.get()) return false;
        if (victim.getWorld().isClient()) return false;
        if (!(victim instanceof ServerPlayerEntity serverVictim)) return false;
        if (victim.isCreative() || victim.isSpectator()) return false;

        // 随机转移必须覆盖中毒、凋零等持续伤害。其他旧路由模式仍保留原过滤规则。
        boolean randomTransferEnabled = ChaosMod.config.randomDamageEnabled;
        if (!randomTransferEnabled && shouldBlockRouting(victim)) return false;

        List<ServerPlayerEntity> participants = new ArrayList<>();
        for (ServerPlayerEntity p : serverVictim.getServer().getPlayerManager().getPlayerList()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (!randomTransferEnabled && shouldBlockRouting(p)) continue;
            participants.add(p);
        }

        boolean kbAllowed = shouldApplyKnockbackForSource(source);

        // Full split
        if (ChaosMod.config.sharedDamageSplitEnabled && !participants.isEmpty()) {
            int n = participants.size();
            float each = Math.max(0.0f, amount / n);
            try {
                REENTRY.set(Boolean.TRUE);
                for (ServerPlayerEntity p : participants) {
                    p.damage(source, each);
                    if (kbAllowed && p != victim && p.getWorld() == victim.getWorld()) routedKnockback(p, source, victim);
                    if (p != victim) propagateOnFireIfNeeded(victim, p);
                }
            } finally { REENTRY.set(Boolean.FALSE); }
            return true;
        }

        // Random transfer (may pick victim)
        if (randomTransferEnabled && !participants.isEmpty()) {
            ServerPlayerEntity pick = participants.get(ThreadLocalRandom.current().nextInt(participants.size()));
            try {
                RANDOM_TRANSFER_HANDLED.set(Boolean.TRUE);
                REENTRY.set(Boolean.TRUE);
                pick.damage(source, amount);
                if (kbAllowed && pick != victim && pick.getWorld() == victim.getWorld()) routedKnockback(pick, source, victim);
                // 只转移当前伤害事件，不复制原玩家的燃烧状态。否则会生成第二条独立火焰伤害链，
                // 表现为原玩家和目标玩家同时持续掉血。
            } finally { REENTRY.set(Boolean.FALSE); }
            return true;
        }

        // Nearby split
        if (ChaosMod.config.playerDamageShareEnabled) {
            final double R = 2.0;
            List<ServerPlayerEntity> group = new ArrayList<>();
            for (ServerPlayerEntity p : participants) {
                if (p == victim) continue;
                if (p.getWorld() != victim.getWorld()) continue;
                if (p.squaredDistanceTo(victim) <= R*R) group.add(p);
            }
            if (!group.isEmpty()) {
                group.add((ServerPlayerEntity) victim);
                int n = group.size();
                float each = Math.max(0.0f, amount / n);
                try {
                    REENTRY.set(Boolean.TRUE);
                    for (ServerPlayerEntity p : group) {
                        p.damage(source, each);
                        if (kbAllowed && p != victim) routedKnockback(p, source, victim);
                        if (p != victim) propagateOnFireIfNeeded(victim, p);
                    }
                } finally { REENTRY.set(Boolean.FALSE); }
                return true;
            }
        }

        return false;
    }

    /** 在一次自定义怪物攻击前清空标记，避免读取到其他伤害事件留下的状态。 */
    public static void beginRandomTransferProbe() {
        RANDOM_TRANSFER_HANDLED.set(Boolean.FALSE);
    }

    /** 返回本次攻击是否已被随机转移处理，并立即清空标记。 */
    public static boolean consumeRandomTransferHandled() {
        boolean handled = RANDOM_TRANSFER_HANDLED.get();
        RANDOM_TRANSFER_HANDLED.set(Boolean.FALSE);
        return handled;
    }

    private static void routedKnockback(PlayerEntity target, DamageSource source, PlayerEntity originalVictim) {
        Entity attacker = source.getAttacker();
        double dx, dz;
        float strength = 0.4f;
        double kbV = 0.0;

        if (attacker instanceof LivingEntity le) {
            dx = target.getX() - le.getX();
            dz = target.getZ() - le.getZ();
            try {
                double kbAttr = le.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
                strength = (float)(0.4f + Math.max(0.0, kbAttr));
            } catch (Throwable ignored) {}
            EntityType<?> t = le.getType();
            if (t == EntityType.IRON_GOLEM) kbV = 0.50;
            else if (t == EntityType.RAVAGER) kbV = 0.20;
            else if (t == EntityType.WARDEN) kbV = 0.10;
        } else {
            Entity ref = originalVictim != null ? originalVictim : target;
            dx = target.getX() - ref.getX();
            dz = target.getZ() - ref.getZ();
        }

        double len = Math.sqrt(dx*dx + dz*dz);
        if (len > 1.0E-4) {
            double nx = dx/len, nz = dz/len;
            target.takeKnockback(strength, -nx, -nz);
            if (kbV > 0) {
                target.addVelocity(0, Math.min(0.9, kbV), 0);
                target.velocityDirty = true;
            }
        }
    }

    public static boolean updateAndCheckCrossing(PlayerEntity p) {
        boolean prev = BELOW_THRESHOLD.getOrDefault(p, Boolean.FALSE);
        boolean now = p.getHealth() <= 2.0f;
        BELOW_THRESHOLD.put(p, now);
        return (!prev && now);
    }

    public static void tickNoHeal(PlayerEntity p) {
        if (!ChaosMod.config.lowHealthNoHealEnabled) {
            NO_HEAL.remove(p);
            BELOW_THRESHOLD.remove(p);
            return;
        }
        Integer left = NO_HEAL.get(p);
        if (left == null) return;
        left -= 1;
        if (left <= 0) {
            NO_HEAL.remove(p);
            ChaosMod.config.noHealActive = false;
            ChaosMod.config.noHealEndTime = 0L;
        } else {
            NO_HEAL.put(p, left);
        }
    }

    public static void maybeStartNoHeal(PlayerEntity p, boolean crossedDown) {
        if (!ChaosMod.config.lowHealthNoHealEnabled) return;
        if (!crossedDown) return;
        if (NO_HEAL.containsKey(p)) return;
        if (p.getRandom().nextFloat() < 0.5f) {
            NO_HEAL.put(p, 20 * 10);
            ChaosMod.config.noHealActive = true;
            ChaosMod.config.noHealEndTime = p.getWorld().getTime() + 20 * 10;
        }
    }

    public static boolean isNoHeal(PlayerEntity p) {
        if (!ChaosMod.config.lowHealthNoHealEnabled) return false;
        Integer left = NO_HEAL.get(p);
        return left != null && left > 0;
    }

    // === Reverse Damage System Methods ===
    
    /** Record when a player takes damage to reset the reverse damage timer */
    public static void recordPlayerDamage(PlayerEntity player) {
        if (!ChaosMod.config.reverseDamageEnabled) return;
        long currentTime = player.getWorld().getTime();
        LAST_DAMAGE_TIME.put(player, currentTime);
        REVERSE_DAMAGE_COUNTER.remove(player); // Reset counter when taking damage
    }
    
    /** Check if player should take reverse damage (not damaged recently) */
    public static boolean shouldApplyReverseDamage(PlayerEntity player) {
        if (!ChaosMod.config.reverseDamageEnabled) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        
        long currentTime = player.getWorld().getTime();
        Long lastDamageTime = LAST_DAMAGE_TIME.get(player);
        
        // If never damaged, start applying reverse damage immediately
        if (lastDamageTime == null) return true;
        
        // Check if immunity period (2 seconds) has passed
        return (currentTime - lastDamageTime) >= DAMAGE_IMMUNITY_DURATION;
    }
    
    /** Tick reverse damage system for a player */
    public static void tickReverseDamage(PlayerEntity player) {
        if (!ChaosMod.config.reverseDamageEnabled) {
            LAST_DAMAGE_TIME.remove(player);
            REVERSE_DAMAGE_COUNTER.remove(player);
            return;
        }
        if (!shouldApplyReverseDamage(player)) return;
        
        Integer counter = REVERSE_DAMAGE_COUNTER.getOrDefault(player, 0);
        counter++;
        
        if (counter >= REVERSE_DAMAGE_INTERVAL) {
            // Apply reverse damage (1 heart, same as hand punch)
            try {
                REENTRY.set(Boolean.TRUE);
                // Create a generic damage source for reverse damage
                DamageSource reverseSource = player.getDamageSources().generic();
                player.damage(reverseSource, 1.0f);
            } finally {
                REENTRY.set(Boolean.FALSE);
            }
            counter = 0; // Reset counter
        }
        
        REVERSE_DAMAGE_COUNTER.put(player, counter);
    }
    
    /** Check if incoming damage should be blocked by reverse damage system */
    public static boolean shouldBlockDamageForReverse(PlayerEntity player, DamageSource source) {
        if (!ChaosMod.config.reverseDamageEnabled) return false;
        if (REENTRY.get()) return false; // Don't block our own reverse damage
        
        // Record the damage and block the original damage
        recordPlayerDamage(player);
        return true; // Block the original damage
    }
    
    // === Sunburn System Methods ===
    
    /** Tick sunburn system for a player */
    public static void tickSunburn(PlayerEntity player) {
        if (!ChaosMod.config.sunburnEnabled) {
            LAST_SUNBURN_DAMAGE_TIME.remove(player);
            return;
        }
        if (player.isCreative() || player.isSpectator()) return;
        if (player.getWorld().isClient()) return;
        
        // Check if it's clear weather and daytime
        if (!player.getWorld().isRaining() && !player.getWorld().isThundering()) {
            long timeOfDay = player.getWorld().getTimeOfDay() % 24000;
            boolean isDaytime = timeOfDay >= 0 && timeOfDay < 12000;
            
            if (isDaytime) {
                // Check if player has direct sunlight (no block above)
                BlockPos playerPos = player.getBlockPos();
                
                // Check from player head position all the way up to sky for any blocking blocks
                boolean hasBlockAbove = false;
                for (int y = playerPos.getY() + 2; y <= player.getWorld().getHeight(); y++) {
                    BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
                    if (!player.getWorld().isAir(checkPos)) {
                        hasBlockAbove = true;
                        break;
                    }
                }
                
                if (!hasBlockAbove) {
                    // Check for helmet protection
                    ItemStack helmet = player.getInventory().getArmorStack(3); // Helmet slot
                    boolean hasHelmetProtection = false;
                    
                    if (!helmet.isEmpty()) {
                        // Only damage helmet every SUNBURN_COOLDOWN_TICKS to prevent instant destruction
                        long currentTime = player.getWorld().getTime();
                        Long lastDamageTime = LAST_SUNBURN_DAMAGE_TIME.get(player);
                        
                        if (lastDamageTime == null || currentTime - lastDamageTime >= SUNBURN_COOLDOWN_TICKS) {
                            LAST_SUNBURN_DAMAGE_TIME.put(player, currentTime);
                            
                            // Leather helmet still burns but damages helmet
                            if (helmet.getItem() == Items.LEATHER_HELMET) {
                                helmet.damage(1, player, net.minecraft.entity.EquipmentSlot.HEAD);
                            } else {
                                // Other helmets protect but consume durability
                                helmet.damage(1, player, net.minecraft.entity.EquipmentSlot.HEAD);
                                hasHelmetProtection = true;
                            }
                        } else if (helmet.getItem() != Items.LEATHER_HELMET) {
                            // Non-leather helmets still provide protection even when not damaging
                            hasHelmetProtection = true;
                        }
                    }
                    
                    // If no helmet protection, set player on fire
                    if (!hasHelmetProtection) {
                        player.setOnFireFor(3); // 3 seconds of fire
                    }
                }
            }
        }
    }

    public static boolean isDirectDamage() {
        return DIRECT_DAMAGE.get();
    }

    public static boolean applyDirectDamage(LivingEntity entity, DamageSource source, float amount) {
        boolean previous = DIRECT_DAMAGE.get();
        try {
            DIRECT_DAMAGE.set(Boolean.TRUE);
            return entity.damage(source, amount);
        } finally {
            DIRECT_DAMAGE.set(previous);
        }
    }

    public static void cleanupPlayer(PlayerEntity player) {
        NO_HEAL.remove(player);
        BELOW_THRESHOLD.remove(player);
        LAST_DAMAGE_TIME.remove(player);
        REVERSE_DAMAGE_COUNTER.remove(player);
        LAST_SUNBURN_DAMAGE_TIME.remove(player);
    }
    
    // === Fall Trap System Methods (Legacy) ===
    // Note: Fall trap system now uses precise mixin injection methods
    // All new effects (bed explosion, block revenge, container curse, etc.) 
    // are implemented using targeted mixin approaches for better accuracy
}
