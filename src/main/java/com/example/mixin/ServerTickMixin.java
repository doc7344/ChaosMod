package com.example.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ServerTickMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void chaos$serverTick(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer)(Object)this;
        
        // 眩晕背锅侠系统（新增）
        com.example.util.ChaosEffects.tickVertigoScapegoat(server);
    }
}
