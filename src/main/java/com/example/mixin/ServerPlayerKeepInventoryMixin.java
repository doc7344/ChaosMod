package com.example.mixin;

import com.example.ChaosMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 按用户要求实现：
 *  - 末地：已在 PlayerEntityDropInventoryMixin 中强制掉落
 *  - 其他维度：强制保留物品
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerKeepInventoryMixin {

    /** 确保末地死亡后背包为空 */
    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void chaos$ensureEndDeathEmpty(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (!ChaosMod.config.endKeepOverrideEnabled) return;
        
        ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;
        
        if (oldPlayer.getWorld().getRegistryKey() == World.END) {
            // 末地死亡：确保新玩家背包为空（物品已通过 PlayerEntityDropInventoryMixin 掉落）
            self.getInventory().clear();
        } else if (!alive) {
            self.getInventory().clone(oldPlayer.getInventory());
        }
    }
}
