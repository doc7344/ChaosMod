package com.example.util;

import com.example.ChaosMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/**
 * 痛觉扩散系统 - 修复自己被雷劈问题
 */
public class PainSpreadSystem {
    
    // 带电玩家
    private static final Map<PlayerEntity, Long> ELECTRIFIED_PLAYERS = new WeakHashMap<>(); 
    // 雷击冷却表 - 防重复
    private static final Map<PlayerEntity, Set<PlayerEntity>> LIGHTNING_COOLDOWNS = new WeakHashMap<>();
    
    private static final int ELECTRIFIED_DURATION = 100; // 5秒 = 100 ticks
    private static final double SPREAD_RADIUS = 3.5; // 扩散半径
    private static final int LIGHTNING_COOLDOWN = 20; // 1秒防重复
    
    /**
     * 标记被打的玩家为"带电"
     */
    public static void markElectrified(LivingEntity entity) {
        if (!ChaosMod.config.painSpreadEnabled) return;
        if (entity.getWorld().isClient()) return;
        if (!(entity instanceof PlayerEntity player)) return;

        long currentTime = entity.getWorld().getTime();
        ELECTRIFIED_PLAYERS.put(player, currentTime + ELECTRIFIED_DURATION);
        
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("⚡ " + com.example.config.LanguageManager.getMessage("electrified"))
                .formatted(Formatting.YELLOW, Formatting.BOLD), true);
        }
    }

    /**
     * tick处理带电玩家 - 按照严格规范实现，确保不劈自己
     */
    public static void tickElectrified(PlayerEntity owner) {
        if (!ChaosMod.config.painSpreadEnabled) {
            ELECTRIFIED_PLAYERS.remove(owner);
            LIGHTNING_COOLDOWNS.remove(owner);
            return;
        }
        if (owner.getWorld().isClient()) return;
        if (!(owner instanceof ServerPlayerEntity serverOwner)) return;

        Long electrifiedUntil = ELECTRIFIED_PLAYERS.get(owner);
        if (electrifiedUntil == null) return;

        long currentTime = owner.getWorld().getTime();
        
        // 检查是否已经过期
        if (currentTime >= electrifiedUntil) {
            ELECTRIFIED_PLAYERS.remove(owner);
            LIGHTNING_COOLDOWNS.remove(owner);
            serverOwner.sendMessage(Text.literal("✅ " + com.example.config.LanguageManager.getMessage("electrified_ended"))
                .formatted(Formatting.GREEN), true);
            return;
        }

        // 每10 tick检查一次周围玩家
        if (currentTime % 10 != 0) return;

        ServerWorld world = serverOwner.getServerWorld();
        double r2 = SPREAD_RADIUS * SPREAD_RADIUS;
        
        // 使用world.getPlayers过滤器直接排除施加者本体，按照你的要求
        List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> 
            p != owner &&                                    // 确保p != owner（绝不击中自己）
            !p.getUuid().equals(owner.getUuid()) &&         // 双重保险：比较UUID确保绝不击中自己
            p.isAlive() &&                                   // 必须存活
            !p.isSpectator() &&                             // 不是观察者
            p.squaredDistanceTo(owner) <= r2                // 在范围内
        );

        Set<PlayerEntity> cooldownSet = LIGHTNING_COOLDOWNS.computeIfAbsent(owner, k -> new HashSet<>());
        
        for (ServerPlayerEntity other : nearbyPlayers) {
            // 检查冷却（同刻去重/冷却表防止多次触发）
            if (cooldownSet.contains(other)) continue;
            
            // 添加到冷却列表
            cooldownSet.add(other);
            
            // 仅在目标位置显示无伤害闪电，伤害定向施加给目标，避免范围伤害波及带电者本人。
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.refreshPositionAfterTeleport(other.getX(), other.getY(), other.getZ());
                lightning.setCosmetic(true);
                world.spawnEntity(lightning);
            }
            other.damage(world.getDamageSources().lightningBolt(), 5.0F);
            
            // 发送消息
            other.sendMessage(Text.literal("⚡ " + 
                String.format(com.example.config.LanguageManager.getMessage("struck_by_lightning"), 
                owner.getName().getString()))
                .formatted(Formatting.RED), true);
        }

        // 清理过期的冷却（每秒清理一次）
        if (currentTime % 20 == 0) {
            cooldownSet.clear();
        }
    }
    
    /**
     * 获取带电玩家列表（调试用）
     */
    public static Set<PlayerEntity> getElectrifiedPlayers() {
        return new HashSet<>(ELECTRIFIED_PLAYERS.keySet());
    }

    public static void cleanupPlayer(PlayerEntity player) {
        ELECTRIFIED_PLAYERS.remove(player);
        LIGHTNING_COOLDOWNS.remove(player);
        for (Set<PlayerEntity> cooldowns : LIGHTNING_COOLDOWNS.values()) cooldowns.remove(player);
    }
}
