package com.example.mixin;

import com.example.util.SharedVitalitySystem;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class HungerManagerUpdateMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void chaos$runSharedHungerOnce(PlayerEntity player, CallbackInfo ci) {
        if (!SharedVitalitySystem.shouldRunHungerUpdate(player)) {
            ci.cancel();
        }
    }
}
