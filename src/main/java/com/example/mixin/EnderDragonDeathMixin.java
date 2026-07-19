package com.example.mixin;

import com.example.ChaosMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EnderDragonDeathMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void chaos$dragonKillerDies(DamageSource source, CallbackInfo ci) {
        if (!ChaosMod.config.enderDragonKillEnabled) return;
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.getWorld().isClient()) return;
        if (self instanceof EnderDragonEntity) {
            if (source != null && source.getAttacker() instanceof PlayerEntity player) {
                player.kill();
            }
        }
    }
}
