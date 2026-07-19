package com.example.mixin;

import com.example.ChaosMod;
import com.example.util.ChaosEffects;
import com.example.util.FourthWallPunishmentSystem;
import net.minecraft.entity.Entity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerRespawnMixin {
    
    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void chaos$onRespawnPlayer(ServerPlayerEntity player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        ServerPlayerEntity respawnedPlayer = cir.getReturnValue();
        
        // 原有逻辑：混沌效果重置
        ChaosEffects.resetOnRespawn(respawnedPlayer);
        com.example.util.SharedVitalitySystem.onPlayerRespawn(respawnedPlayer);
        
        // === v1.6.0 新增：第四面墙复活惩戒 ===
        // 检查是否为死亡复活（而非换维刷新）
        if (removalReason == Entity.RemovalReason.KILLED) {
            // 检查窗口抖动效果是否开启
            if (ChaosMod.config.windowViolentShakeEnabled) {
                // 复活惩戒：给玩家中毒2效果持续10秒
                FourthWallPunishmentSystem.applyRespawnPunishment(respawnedPlayer);
            }
            
            // 重置桌面文件生成状态（复活后可以重新生成）
            if (ChaosMod.config.desktopPrankInvasionEnabled) {
                com.example.util.DesktopFileRespawnResetSystem.resetPlayerGenerationState(respawnedPlayer.getUuid());
                com.example.util.DesktopFileManager.resetPlayerOnRespawn(respawnedPlayer.getUuid());
            }
        }
    }
}
