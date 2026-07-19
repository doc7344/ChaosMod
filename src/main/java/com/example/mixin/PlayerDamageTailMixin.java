package com.example.mixin;

import com.example.ChaosMod;
import com.example.util.ChaosEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerDamageTailMixin {
    private static final ThreadLocal<Boolean> CHAOS_SHIELD_REENTRY = ThreadLocal.withInitial(() -> false);
    @Inject(method = "damage", at = @At("TAIL"))
    private void chaos$onDamageEnd(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity p = (PlayerEntity)(Object)this;
        if (p.getWorld().isClient()) return;
        if (com.example.util.DamageRouting.isDirectDamage()) return;
        
        // 只有在实际受到伤害时才触发（返回值为true）
        if (cir.getReturnValue()) {
            // 按键失灵：累计受伤次数
            ChaosEffects.handleKeyDisable(p);
            
            // 受伤随机增益：随机添加或移除状态效果
            ChaosEffects.handleRandomEffects(p);
            
            // 痛觉扩散：标记为带电状态
            com.example.util.PainSpreadSystem.markElectrified(p);
            
            // === 新增的混沌效果 ===
            // 惊惧磁铁：标记受伤玩家进入磁化状态
            ChaosEffects.markPanicMagnetized(p, source);
            
            // 眩晕背锅侠：处理伤害后果
            ChaosEffects.handleVertigoScapegoatDamage(p, source, amount);
        }
        
        // 盾牌削弱（原有功能）
        if (!ChaosMod.config.shieldNerfEnabled) return;
        if (amount <= 0) return;
        if (p.isBlocking() && p.blockedByShield(source)) {
            float extra = amount * 0.20f;
            try {
                CHAOS_SHIELD_REENTRY.set(true);
                // generic 属于 bypasses_armor，且该标签被 bypasses_shield 引用，保证20%真正穿盾。
                com.example.util.DamageRouting.applyDirectDamage(p, p.getDamageSources().generic(), extra);
            } finally {
                CHAOS_SHIELD_REENTRY.set(false);
            }
        }
    }
}
