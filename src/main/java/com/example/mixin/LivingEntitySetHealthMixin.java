package com.example.mixin;

import com.example.util.SharedVitalitySystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySetHealthMixin {
    @Inject(method = "setHealth", at = @At("TAIL"))
    private void chaos$mirrorSharedHealth(float health, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            SharedVitalitySystem.onHealthChanged(player);
        }
    }
}
