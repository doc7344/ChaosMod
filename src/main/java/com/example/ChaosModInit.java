package com.example;

import com.example.config.ChaosModConfig;
import com.example.network.ConfigToggleC2SPacket;
// Removed screen handler factory - using simplified GUI approach
// Removed Fabric Permissions API - using standard OP level check
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
// Removed unused text imports
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChaosModInit implements ModInitializer {
    public static final ChaosModConfig config = ChaosMod.config;
    
    // Removed unused permission exception

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("allHostileEnabled", "所有生物敌对");
        LABELS.put("mobIgniteEnabled", "被怪命中点燃");
        LABELS.put("mobSlownessEnabled", "被怪命中缓慢II");
        LABELS.put("mobBlindnessEnabled", "被怪命中失明");
        LABELS.put("mobThornsEnabled", "反伤=50%");
        LABELS.put("foodPoisonEnabled", "吃食物概率中毒");
        LABELS.put("enderDragonBucketEnabled", "被龙打→水桶变牛奶");
        LABELS.put("enderDragonKillEnabled", "击杀末影龙者自杀");
        LABELS.put("playerDamageShareEnabled", "贴身平摊伤害");
        LABELS.put("sharedHealthEnabled", "共享生命与饥饿");
        LABELS.put("sharedDamageSplitEnabled", "全服平摊伤害");
        LABELS.put("randomDamageEnabled", "随机转移伤害");
        LABELS.put("shieldNerfEnabled", "盾牌仅吸收80%");
        LABELS.put("lowHealthNoHealEnabled", "≤1♥禁回血(10s)");
        LABELS.put("waterToLavaEnabled", "放水50%变岩浆(仅玩家)");
        LABELS.put("endKeepOverrideEnabled", "末地死亡掉落/其他维度保留物品");
        LABELS.put("reverseDamageEnabled", "反向伤害：不受伤扣血");
        LABELS.put("sunburnEnabled", "晴天白天阳光下自燃");
        LABELS.put("healReverseEnabled", "回血时50%概率变扣血");
        LABELS.put("fallTrapEnabled", "平地跳跃落地20%概率扣0.5♥");
        LABELS.put("acrophobiaEnabled", "恐高症：Y>80越高伤害越大(最高2♥)");
        LABELS.put("blockRevengeEnabled", "破坏方块10%概率被反伤");
        LABELS.put("containerCurseEnabled", "开箱子/熔炉25%概率扣1♥");
        LABELS.put("inventoryCurseEnabled", "切换物品槽12%概率扣0.5♥");
        LABELS.put("craftingTrapEnabled", "合成物品10%概率扣1♥");
        LABELS.put("playerHealOnAttackEnabled", "攻击玩家回血");
        LABELS.put("positionSwapEnabled", "位置互换");
        LABELS.put("craftingBombEnabled", "合成炸弹");
        LABELS.put("waterDamageEnabled", "水中溺死");
        LABELS.put("randomDamageAmountEnabled", "随机伤害值");
        LABELS.put("delayedDamageEnabled", "延迟受伤");
        LABELS.put("keyDisableEnabled", "按键失灵");
        LABELS.put("randomEffectsEnabled", "受伤随机增益");
        LABELS.put("damageScapegoatEnabled", "伤害背锅人");
        LABELS.put("painSpreadEnabled", "痛觉扩散");
        LABELS.put("panicMagnetEnabled", "惊惧磁铁");
        LABELS.put("pickupDrainEnabled", "贪婪吸血");
        LABELS.put("vertigoScapegoatEnabled", "眩晕背锅侠");
        LABELS.put("windowViolentShakeEnabled", "窗口暴力抖动");
        LABELS.put("desktopPrankInvasionEnabled", "桌面恶作剧入侵(会记录IP地址)");
        LABELS.put("randomKeyPressEnabled", "电击中毒癫痫");
        LABELS.put("touchHellEnabled", "触控地狱");
        LABELS.put("movementTaxEnabled", "移动税");
        LABELS.put("controlSeizurePlusEnabled", "控制癫痫Plus");
        LABELS.put("jumpTaxEnabled", "跳跃税");
        LABELS.put("forcedTetherEnabled", "强制捆绑");
        LABELS.put("hpAveragingEnabled", "血量平均");
        LABELS.put("multiplayerRouletteEnabled", "死亡轮盘(多人版)");
        LABELS.put("timedPositionSwapEnabled", "定时位置互换");
        LABELS.put("forcedSprintEnabled", "强制奔跑");
        LABELS.put("periodicNegativeEffectEnabled", "周期随机负面效果");
        LABELS.put("weaponSlipEnabled", "武器脱手");
        LABELS.put("magmaBetrayalEnabled", "地面背叛");
        LABELS.put("timeReboundEnabled", "时间回弹");
        LABELS.put("burdenCollapseEnabled", "负重崩塌");
    }

    @Override
    public void onInitialize() {
        
        // v1.3.0: Initialize language system
        com.example.config.LanguageManager.loadLanguageFromConfig();
        
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(
            com.example.network.ConfigToggleC2SPacket.ID,
            com.example.network.ConfigToggleC2SPacket.CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.example.network.ConfigSyncPacket.ID,
            com.example.network.ConfigSyncPacket.CODEC
        );

        // Register network packet receiver with proper permission checks
        ConfigToggleC2SPacket.registerServerReceiver();
        
        // Register S2C key disable packet payload type
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.example.network.KeyDisableS2CPacket.ID, 
            com.example.network.KeyDisableS2CPacket.CODEC
        );
        
        // === v1.6.0 注册第四面墙效果网络包 ===
        // Register C2S fourth wall damage packet
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(
            com.example.network.FourthWallDamageC2SPacket.ID,
            com.example.network.FourthWallDamageC2SPacket.CODEC
        );
        
        // Register S2C desktop file generation packet
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.example.network.DesktopFileGenerateS2CPacket.ID,
            com.example.network.DesktopFileGenerateS2CPacket.CODEC
        );
        
        // Register S2C desktop file content packet (new multi-language support)
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.example.network.DesktopFileContentS2CPacket.ID,
            com.example.network.DesktopFileContentS2CPacket.CODEC
        );
        
        // === v1.7.0 注册客户端IP探测网络包 ===
        // Register C2S player IP report packet (client reports probed IP)
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(
            com.example.network.PlayerIPReportC2SPacket.ID,
            com.example.network.PlayerIPReportC2SPacket.CODEC
        );
        
        // Register S2C IP probe request packet (server requests client to probe)
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.example.network.RequestIPProbeS2CPacket.ID,
            com.example.network.RequestIPProbeS2CPacket.CODEC
        );
        
        // 控制癫痫Plus现在使用现有的KeyDisableS2CPacket系统，无需新网络包
        
        // Register fourth wall damage packet receiver
        com.example.network.FourthWallDamageC2SPacket.registerServerReceiver();
        
        // Register player IP report packet receiver
        com.example.network.PlayerIPReportC2SPacket.registerServerReceiver();
        
        // Register key disable event handlers using Fabric events
        com.example.util.KeyDisableEventHandler.registerEvents();
        
        // Register RandomKeyPress C2S packet payload type
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(
            com.example.network.RandomKeyPressC2SPacket.ID,
            com.example.network.RandomKeyPressC2SPacket.CODEC
        );
        
        // Register RandomKeyPress C2S packet receiver
        com.example.network.RandomKeyPressC2SPacket.registerServerReceiver();
        
        // Initialize scapegoat system on server start
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            com.example.util.ScapegoatSystem.loadScapegoatFromPersistentState(server);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(
            com.example.util.AdditionalChaosEffects::shutdown
        );
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(
            com.example.util.SharedVitalitySystem::shutdown
        );
        
        // 注册背锅人选择定时器 - 使用ServerTickEvents.START_SERVER_TICK
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> {
            com.example.util.SharedVitalitySystem.startServerTick(server);
            com.example.util.ScapegoatSystem.tickScapegoat(server);
            
            // v1.8.0: 注册多人互坑效果tick处理
            com.example.util.MultiplayerChaosEffects.tickForcedTether(server);
            com.example.util.MultiplayerChaosEffects.tickHPAveraging(server);
            com.example.util.MultiplayerChaosEffects.tickMultiplayerRoulette(server);
            com.example.util.MultiplayerChaosEffects.tickTimedPositionSwap(server);
            com.example.util.MultiplayerChaosEffects.tickForcedSprint(server);
            com.example.util.AdditionalChaosEffects.tick(server);
            
            // 定期清理IP缓存（每1000 ticks = 50秒清理一次）
            if (server.getTicks() % 1000 == 0) {
                com.example.util.PublicIpProvider.cleanupExpiredCache();
                com.example.util.ClientIPCache.cleanupExpiredCache();
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
            com.example.util.SharedVitalitySystem::endServerTick
        );
        
        // 注册玩家JOIN/DISCONNECT事件监听器
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // JOIN即时重算
            com.example.util.ScapegoatSystem.onPlayerJoin(handler.getPlayer(), server);
            com.example.util.SharedVitalitySystem.onPlayerJoin(handler.getPlayer());
            
            // v1.7.0: 玩家加入时请求客户端探测IP
            com.example.network.RequestIPProbeS2CPacket.send(handler.getPlayer());
            com.example.network.ConfigSyncPacket.send(handler.getPlayer());
        });
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // DISCONNECT即时重算
            com.example.util.ScapegoatSystem.onPlayerDisconnect(handler.getPlayer(), server);
            
            // v1.8.0: 清理多人互坑效果数据
            com.example.util.MultiplayerChaosEffects.cleanupPlayerData(handler.getPlayer());
            com.example.network.RandomKeyPressC2SPacket.cleanupPlayer(handler.getPlayer().getUuid());
            com.example.util.PeriodicNegativeEffectSystem.cleanupPlayer(handler.getPlayer().getUuid());
            com.example.util.AdditionalChaosEffects.cleanupPlayer(handler.getPlayer().getUuid());
            com.example.util.ChaosEffects.cleanupPlayerData(handler.getPlayer());
            com.example.util.DamageRouting.cleanupPlayer(handler.getPlayer());
            com.example.util.SharedVitalitySystem.onPlayerDisconnect(handler.getPlayer().getUuid());
            
            // v1.7.0: 清理客户端IP缓存
            com.example.util.ClientIPCache.cleanupPlayer(handler.getPlayer().getUuid());
        });
        
        // Commands with Admin Permission Check (keeping only toggle command)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("chaos")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.literal("toggle")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                for (String k : LABELS.keySet()) builder.suggest(k);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String key = StringArgumentType.getString(ctx, "key");
                                toggle(ctx.getSource(), key);
                                return 1;
                            })))
            );
        });

        // Player water -> lava 50% (only player-placed water)
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!com.example.ChaosMod.config.waterToLavaEnabled) return ActionResult.PASS;
            if (player == null || player.isSpectator()) return ActionResult.PASS;
            ItemStack inHand = player.getStackInHand(hand);
            if (!inHand.isOf(Items.WATER_BUCKET)) return ActionResult.PASS;
            if (world.isClient()) return ActionResult.PASS;
            if (player.getRandom().nextFloat() < 0.5f) {
                player.setStackInHand(hand, new ItemStack(Items.LAVA_BUCKET));
            }
            return ActionResult.PASS;
        });
    }

    /**
     * 原来的菜单功能现在已经完全集成到 GUI 中：
     * 
     * 1. 权限检查：使用 Fabric Permissions API 配合 ExtendedScreenHandlerFactory
     * 2. 配置切换：通过 ConfigToggleC2SPacket 进行 C2S 通信
     * 3. 用户界面：使用 ChaosModConfigScreen 提供图形化界面
     * 4. 实时反馈：服务端验证权限后发送确认消息
     * 5. 管理员广播：配置变更会通知其他在线管理员
     * 
     * 所有原有功能都已保留并增强：
     * - 25 个配置项的完整列表和中文标签
     * - 权限验证（现在更加严格和安全）
     * - 即时切换反馈
     * - 点击式操作界面
     * - 全部启用/禁用快捷操作
     */

    private static void toggle(ServerCommandSource src, String key) {
        // 使用相同的权限检查逻辑
        try {
            ServerPlayerEntity player = src.getPlayer();
            boolean hasPermission = player.hasPermissionLevel(4); // Standard admin check
            
            if (!hasPermission) {
                send(src, Text.literal(com.example.config.LanguageManager.getMessage("config_permission_denied"))
                    .formatted(Formatting.RED, Formatting.BOLD));
                return;
            }
            
            boolean cur = com.example.ChaosMod.config.get(key);
            com.example.network.ConfigToggleC2SPacket.updateConfig(key, !cur, player);
            
        } catch (Exception e) {
            send(src, Text.literal(com.example.config.LanguageManager.getMessage("cannot_get_player"))
                .formatted(Formatting.RED));
        }
    }

    // Removed head and line methods - no longer needed without showMenu

    private static void send(ServerCommandSource src, Text text) {
        ServerPlayerEntity p = null;
        try { p = src.getPlayer(); } catch (Throwable ignored) { }
        if (p != null) p.sendMessage(text);
        else { try { src.sendFeedback(() -> text, false); } catch (Throwable ignored) { } }
    }
}
