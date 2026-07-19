package com.example.mixin;

import com.example.ChaosMod;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 按用户要求实现：
 *  - 末地：不管 GameRules.KEEP_INVENTORY 值，强制执行 dropInventory() 原始逻辑
 *  - 其他维度：强制保留物品
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDropInventoryMixin {

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void chaos$endForceDropOthersVanilla(CallbackInfo ci) {
        if (!ChaosMod.config.endKeepOverrideEnabled) return;
        
        PlayerEntity self = (PlayerEntity)(Object)this;
        
        if (self.getWorld().getRegistryKey() == World.END) {
            // 末地：强制执行原始 dropInventory() 逻辑，不管 KEEP_INVENTORY 规则
            ci.cancel(); // 取消原版调用
            // 复制 LivingEntity.dropInventory() 的代码逻辑（强制掉落）
            chaos$forceLivingEntityDropInventory(self);
        } else {
            // 其他维度：阻止死亡掉落，由copyFrom强制复制背包。
            ci.cancel();
        }
    }

    /**
     * 复制 LivingEntity.dropInventory() 的逻辑，强制掉落物品
     */
    @Unique
    private static void chaos$forceLivingEntityDropInventory(PlayerEntity player) {
        var inventory = player.getInventory();
        
        // 掉落主背包物品（0-35）
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = player.dropItem(stack, true, false);
                if (itemEntity != null) {
                    itemEntity.setOwner(player.getUuid());
                }
                inventory.main.set(i, ItemStack.EMPTY);
            }
        }
        
        // 掉落盔甲栏物品
        for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = inventory.armor.get(i);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = player.dropItem(stack, true, false);
                if (itemEntity != null) {
                    itemEntity.setOwner(player.getUuid());
                }
                inventory.armor.set(i, ItemStack.EMPTY);
            }
        }
        
        // 掉落副手物品
        for (int i = 0; i < inventory.offHand.size(); i++) {
            ItemStack stack = inventory.offHand.get(i);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = player.dropItem(stack, true, false);
                if (itemEntity != null) {
                    itemEntity.setOwner(player.getUuid());
                }
                inventory.offHand.set(i, ItemStack.EMPTY);
            }
        }
        
        inventory.updateItems();
    }
}
