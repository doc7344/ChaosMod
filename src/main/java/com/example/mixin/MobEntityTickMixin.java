package com.example.mixin;

import com.example.ChaosMod;
import com.example.util.DamageRouting;
import com.example.util.ThreatProfiles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityTickMixin {

    @Unique private int chaos$ticker;
    @Unique private int chaos$attackCd; // melee cooldown in ticks
    @Unique private boolean chaos$allHostileWasActive;
    @Unique private double chaos$nativeNavigationMultiplier = Double.NaN;
    @Unique private PlayerEntity chaos$forcedTarget;

    @Inject(method = "tick", at = @At("TAIL"))
    private void chaos$aggroTick(CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        if (self.getWorld().isClient()) return;
        if (!ChaosMod.config.allHostileEnabled) {
            if (chaos$allHostileWasActive) {
                chaos$releaseForcedAggro(self);
                chaos$nativeNavigationMultiplier = Double.NaN;
            } else {
                chaos$rememberNativeNavigationMultiplier(self);
            }
            chaos$allHostileWasActive = false;
            return;
        }
        if (!chaos$allHostileWasActive) {
            // Preserve the multiplier most recently supplied by this entity's own vanilla AI.
            // MoveControl multiplies it by GENERIC_MOVEMENT_SPEED, so villagers, animals,
            // golems and monsters keep their individual native movement speed.
            chaos$rememberNativeNavigationMultiplier(self);
            chaos$allHostileWasActive = true;
        }
        if (!chaos$isValidNavigationMultiplier(chaos$nativeNavigationMultiplier)) {
            // A newly spawned or completely idle mob may not have issued a vanilla movement
            // command yet. Keep observing instead of inventing a shared fallback speed.
            chaos$rememberNativeNavigationMultiplier(self);
            if (!chaos$isValidNavigationMultiplier(chaos$nativeNavigationMultiplier)) return;
        }
        ServerWorld sw = (ServerWorld) self.getWorld();

        chaos$ticker++;
        if (chaos$attackCd > 0) chaos$attackCd--;

        if ((chaos$ticker & 1) != 0) return; // every 2 ticks

        // Follow range = vanilla FOLLOW_RANGE attribute
        double range = 16.0;
        try { range = self.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE); } catch (Throwable ignored) {}
        if (range <= 0) range = 16.0;
        final double targetRange = range;

        PlayerEntity target = sw.getPlayers().stream()
            .filter(PlayerEntity::isAlive)
            .filter(p -> !p.isCreative() && !p.isSpectator())
            .filter(p -> p.squaredDistanceTo(self) <= targetRange * targetRange)
            .min(java.util.Comparator.comparingDouble(p -> p.squaredDistanceTo(self)))
            .orElse(null);
        if (target == null) {
            chaos$releaseForcedAggro(self);
            return;
        }

        // Let vanilla AI see the target for better aggression
        chaos$forcedTarget = target;
        try { self.setTarget(target); } catch (Throwable ignored) {}

        // Navigate
        try {
            EntityNavigation nav = self.getNavigation();
            if (nav != null) nav.startMovingTo(target, chaos$nativeNavigationMultiplier);
        } catch (Throwable ignored) {}

        // Melee range check (3D distance including vertical)
        double dx = target.getX() - self.getX();
        double dy = target.getY() - self.getY();
        double dz = target.getZ() - self.getZ();
        double distSq = dx*dx + dy*dy + dz*dz;  // 3D距离平方

        double reach = (double)(self.getWidth() * 2.0f + 0.5f);
        double reachSq = reach * reach;

        int cdTicks = chaos$getAttackCooldownTicks(self);

        if (distSq <= reachSq) {
            if (chaos$attackCd == 0) {
                boolean hit = false;
                DamageRouting.beginRandomTransferProbe();
                try {
                    hit = self.tryAttack(target); // vanilla-style attack: handles damage/anim/horizontal KB
                } catch (Throwable ignored) {}
                boolean randomTransferHandled = DamageRouting.consumeRandomTransferHandled();

                // 随机转移时，实际承伤者的击退已由 DamageRouting 处理；
                // 不能再把原目标击飞，造成两名玩家同时受到攻击反馈。
                if (!randomTransferHandled) {
                    chaos$applySpecialVerticalKnockup(self, target);
                }

                if (!hit && !randomTransferHandled) {
                    // Fallback contact hit + knockback if entity lacks real melee
                    chaos$contactHitAndKnock(self, target, sw);
                }
                chaos$attackCd = cdTicks; // per-entity cooldown
            }
        } else {
            // If nearly in reach but not intersecting, add a tiny push to avoid "stuck near target but not hitting"
            double gap = Math.sqrt(distSq) - reach;
            if (gap <= 0.5) {
                Vec3d h = new Vec3d(dx, 0, dz).normalize().multiply(0.045);
                self.addVelocity(h.x, 0, h.z);
                self.velocityDirty = true;
            }
        }

        // Contact damage + vanilla-like knockback, now respecting the same cooldown window
        double extra = ThreatProfiles.extraReach(self.getType());
        Box reachBox = self.getBoundingBox().expand(extra);
        if (reachBox.intersects(target.getBoundingBox())) {
            if (chaos$attackCd == 0 && !DamageRouting.contactOnCooldown(self)) {
                DamageRouting.armContactCooldown(self, cdTicks);
                float contact = ThreatProfiles.contactDamage(self.getType());
                target.damage(sw.getDamageSources().mobAttack(self), contact);

                // Horizontal knockback from attacker toward target
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 0.0001) {
                    double nx = dx/len, nz = dz/len;
                    float strength = 0.4f;
                    try {
                        double kbAttr = self.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
                        strength = (float)(0.4f + Math.max(0.0, kbAttr));
                    } catch (Throwable ignored) {}
                    target.takeKnockback(strength, -nx, -nz);
                }

                // Vertical special add for certain mobs
                chaos$applySpecialVerticalKnockup(self, target);

                chaos$attackCd = cdTicks;
            }
        }
    }

    @Unique
    private void chaos$rememberNativeNavigationMultiplier(MobEntity self) {
        try {
            double multiplier = self.getMoveControl().getSpeed();
            if (chaos$isValidNavigationMultiplier(multiplier)) {
                chaos$nativeNavigationMultiplier = multiplier;
                return;
            }
        } catch (Throwable ignored) { }

        // Some idle controls expose zero. Recover the same multiplier from the entity's
        // current vanilla movement speed and its own movement-speed attribute when possible.
        try {
            double attributeSpeed = self.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            double currentMovementSpeed = self.getMovementSpeed();
            if (attributeSpeed > 0.0D && currentMovementSpeed > 0.0D) {
                double multiplier = currentMovementSpeed / attributeSpeed;
                if (chaos$isValidNavigationMultiplier(multiplier)) {
                    chaos$nativeNavigationMultiplier = multiplier;
                }
            }
        } catch (Throwable ignored) { }
    }

    @Unique
    private static boolean chaos$isValidNavigationMultiplier(double multiplier) {
        return Double.isFinite(multiplier) && multiplier > 0.0D;
    }

    @Unique
    private void chaos$releaseForcedAggro(MobEntity self) {
        if (chaos$forcedTarget == null) return;
        try {
            if (self.getTarget() == chaos$forcedTarget) {
                self.setTarget(null);
            }
        } catch (Throwable ignored) { }
        try {
            EntityNavigation navigation = self.getNavigation();
            if (navigation != null) navigation.stop();
        } catch (Throwable ignored) { }
        chaos$forcedTarget = null;
    }

    @Unique
    private static int chaos$getAttackCooldownTicks(MobEntity self) {
        // Conservative per-entity cooldowns (approximate vanilla feels)
        EntityType<?> t = self.getType();
        if (t == EntityType.IRON_GOLEM) return 20;   // 1.0s
        if (t == EntityType.RAVAGER)   return 40;   // 2.0s (slower, heavy)
        if (t == EntityType.WARDEN)    return 30;   // 1.5s
        return 20;                                  // default 1.0s
    }

    @Unique
    private static void chaos$applySpecialVerticalKnockup(MobEntity self, PlayerEntity target) {
        double kbV = 0.0;
        EntityType<?> t = self.getType();
        if (t == EntityType.IRON_GOLEM) kbV = 0.50;
        else if (t == EntityType.RAVAGER) kbV = 0.20;
        else if (t == EntityType.WARDEN) kbV = 0.10;
        if (kbV > 0) {
            target.addVelocity(0, Math.min(0.9, kbV), 0);
            target.velocityDirty = true;
        }
    }

    @Unique
    private static void chaos$contactHitAndKnock(MobEntity self, PlayerEntity target, ServerWorld sw) {
        // Deal a contact hit and apply vanilla-like knockback + special vertical
        float contact = ThreatProfiles.contactDamage(self.getType());
        target.damage(sw.getDamageSources().mobAttack(self), contact);

        // 水平方向的击退（不包括垂直）
        double dx = target.getX() - self.getX();
        double dz = target.getZ() - self.getZ();
        double len = Math.sqrt(dx*dx + dz*dz);
        if (len > 0.0001) {
            double nx = dx/len, nz = dz/len;
            float strength = 0.4f;
            try {
                double kbAttr = self.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
                strength = (float)(0.4f + Math.max(0.0, kbAttr));
            } catch (Throwable ignored) {}
            target.takeKnockback(strength, -nx, -nz);
        }
        chaos$applySpecialVerticalKnockup(self, target);
    }
}
