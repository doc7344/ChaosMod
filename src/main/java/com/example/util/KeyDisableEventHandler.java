package com.example.util;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 使用Fabric事件监听死亡和复活的按键失灵处理器
 */
public class KeyDisableEventHandler {
    
    /**
     * 注册Fabric事件监听器
     */
    public static void registerEvents() {
        try {
            // 监听死亡事件
            ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
                if (entity instanceof PlayerEntity player) {
                    ChaosEffects.markPlayerDead(player);
                    
                    // 背锅人死亡时设置标记但不触发重roll
                    if (entity instanceof ServerPlayerEntity serverPlayer) {
                        ChaosEffects.handleControlSeizurePlus(serverPlayer);
                        ScapegoatSystem.onScapegoatDeath(serverPlayer);
                    }
                }
            });
            
            // 监听复活事件
            ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
                // 复活时重置按键状态
                ChaosEffects.resetOnRespawn(newPlayer);
                ChaosEffects.activateControlSeizurePlus(newPlayer);
                
                // 背锅人复活时清除死亡标记
                ScapegoatSystem.onScapegoatRespawn(newPlayer);
            });
            
        } catch (Exception e) {
            // 静默处理事件注册失败
        }
    }
}
