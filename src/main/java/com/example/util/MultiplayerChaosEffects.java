package com.example.util;

import com.example.ChaosMod;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * v1.8.0 多人互坑效果系统
 * 实现5个新的多人互坑效果
 */
public final class MultiplayerChaosEffects {
    private MultiplayerChaosEffects() {}

    private static MinecraftServer stateServer;

    private static long intervalTicks(int seconds) {
        return seconds * 20L;
    }

    private static void ensureServerState(MinecraftServer server) {
        if (stateServer == server) {
            return;
        }

        stateServer = server;
        activeTethers.clear();
        activeSprints.clear();
        currentRoulette = null;
        currentSwap = null;

        long currentTime = server.getTicks();
        lastTetherTime = currentTime;
        lastHPAveragingTime = currentTime;
        lastRouletteTime = currentTime;
        lastSwapTime = currentTime;
        lastSprintTime = currentTime;
    }

    private static List<ServerPlayerEntity> eligiblePlayers(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList().stream()
            .filter(ServerPlayerEntity::isAlive)
            .filter(p -> !p.isCreative() && !p.isSpectator())
            .toList();
    }

    // ==================== 强制捆绑系统 ====================
    
    private static class TetherPair {
        final UUID player1UUID;
        final UUID player2UUID;
        final long endTime;
        
        TetherPair(ServerPlayerEntity p1, ServerPlayerEntity p2, long endTime) {
            this.player1UUID = p1.getUuid();
            this.player2UUID = p2.getUuid();
            this.endTime = endTime;
        }
        
        // 获取实时玩家实例（支持死亡复活）
        ServerPlayerEntity getPlayer1(MinecraftServer server) {
            return server.getPlayerManager().getPlayer(player1UUID);
        }
        
        ServerPlayerEntity getPlayer2(MinecraftServer server) {
            return server.getPlayerManager().getPlayer(player2UUID);
        }
    }
    
    private static final List<TetherPair> activeTethers = new ArrayList<>();
    private static long lastTetherTime = 0;
    private static final long TETHER_DURATION = 1800; // 90秒 = 1800 ticks
    
    /**
     * 强制捆绑：每120秒随机选2个玩家绑定90秒
     */
    public static void tickForcedTether(MinecraftServer server) {
        ensureServerState(server);
        if (!ChaosMod.config.forcedTetherEnabled) {
            activeTethers.clear();
            lastTetherTime = server.getTicks();
            return;
        }
        
        long currentTime = server.getTicks();
        
        // 每120秒触发一次新捆绑
        if (currentTime - lastTetherTime >= intervalTicks(ChaosMod.config.forcedTetherIntervalSeconds)) {
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (players.size() >= 2) {
                ServerPlayerEntity p1 = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                ServerPlayerEntity p2;
                do {
                    p2 = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                } while (p1 == p2);
                
                TetherPair pair = new TetherPair(p1, p2, currentTime + TETHER_DURATION);
                activeTethers.add(pair);
                
                p1.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("forced_tether_start"),
                    p2.getName().getString()
                )).formatted(Formatting.RED, Formatting.BOLD), false);
                
                p2.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("forced_tether_start"),
                    p1.getName().getString()
                )).formatted(Formatting.RED, Formatting.BOLD), false);
            }
            lastTetherTime = currentTime;
        }
        
        // 处理所有活跃捆绑
        Iterator<TetherPair> iterator = activeTethers.iterator();
        while (iterator.hasNext()) {
            TetherPair pair = iterator.next();
            
            // 只检查时间，不检查死亡或离线（跨死亡保持捆绑）
            if (currentTime >= pair.endTime) {
                // 90秒时间到，解除捆绑
                ServerPlayerEntity p1 = pair.getPlayer1(server);
                ServerPlayerEntity p2 = pair.getPlayer2(server);
                
                if (p1 != null) {
                    p1.sendMessage(Text.literal(
                        com.example.config.LanguageManager.getMessage("forced_tether_end")
                    ).formatted(Formatting.GREEN), true);
                }
                if (p2 != null) {
                    p2.sendMessage(Text.literal(
                        com.example.config.LanguageManager.getMessage("forced_tether_end")
                    ).formatted(Formatting.GREEN), true);
                }
                
                iterator.remove();
                continue;
            }
            
            // 性能优化：只每5 ticks处理一次（而非每tick）
            if (currentTime % 5 != 0) {
                continue;
            }
            
            // 获取最新的玩家实例（支持死亡复活）
            ServerPlayerEntity player1 = pair.getPlayer1(server);
            ServerPlayerEntity player2 = pair.getPlayer2(server);
            
            // 如果双方都不在线，移除捆绑
            if (player1 == null && player2 == null) {
                iterator.remove();
                continue;
            }
            
            // 如果有一方不在线，跳过本次tick
            if (player1 == null || player2 == null) {
                continue;
            }
            
            // 不同维度没有可比较的欧氏距离，按“无法靠近”处理，但不跨维度绘制粒子。
            final double MAX_DISTANCE = 15.0;
            final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE; // 225.0
            
            // 获取玩家实时位置用于粒子和距离计算
            Vec3d pos1 = player1.getPos();
            Vec3d pos2 = player2.getPos();
            
            boolean sameWorld = player1.getWorld() == player2.getWorld();
            double distanceSquared = sameWorld
                ? player1.squaredDistanceTo(player2)
                : Double.POSITIVE_INFINITY;
            
            // 绘制粒子连线（基于UUID存储的实时位置）
            // 每5 ticks绘制一次，与主逻辑同步
            if (sameWorld) {
                try {
                    drawParticleLine(player1.getServerWorld(),
                        pos1.add(0, 1, 0),
                        pos2.add(0, 1, 0));
                } catch (Exception ignored) {}
            }
            
            // 距离判定和扣血（完全独立于粒子）
            if (distanceSquared > MAX_DISTANCE_SQUARED) {
                // 每40 ticks (2秒) 扣血一次
                if (currentTime % 40 == 0) {
                    // 双方100%必定扣血（不管死亡与否）
                    try {
                        player1.damage(player1.getDamageSources().magic(), 1.0F);
                        player1.sendMessage(Text.literal(
                            com.example.config.LanguageManager.getMessage("forced_tether_too_far")
                        ).formatted(Formatting.RED), true);
                    } catch (Exception ignored) {}
                    
                    try {
                        player2.damage(player2.getDamageSources().magic(), 1.0F);
                        player2.sendMessage(Text.literal(
                            com.example.config.LanguageManager.getMessage("forced_tether_too_far")
                        ).formatted(Formatting.RED), true);
                    } catch (Exception ignored) {}
                }
            }
            
            // 显示剩余时间（不检查死亡状态）
            if (currentTime % 20 == 0) {
                long remainingSeconds = (pair.endTime - currentTime) / 20;
                
                try {
                    player1.sendMessage(Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("forced_tether_remaining"),
                        remainingSeconds
                    )).formatted(Formatting.YELLOW), true);
                } catch (Exception ignored) {}
                
                try {
                    player2.sendMessage(Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("forced_tether_remaining"),
                        remainingSeconds
                    )).formatted(Formatting.YELLOW), true);
                } catch (Exception ignored) {}
            }
        }
    }
    
    /**
     * 绘制两点间的粒子连线（基于UUID获取的实时位置）
     */
    private static void drawParticleLine(ServerWorld world, Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        // 远距离玩家不能按每0.5格无限生成粒子，否则可在多人服制造海量粒子包。
        int particles = Math.max(1, Math.min(64, (int) Math.ceil(distance * 2)));
        
        for (int i = 0; i < particles; i++) {
            double t = i / (double) particles;
            Vec3d pos = start.lerp(end, t);
            
            // 红色粒子
            net.minecraft.particle.DustParticleEffect dust = new net.minecraft.particle.DustParticleEffect(
                new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.0F
            );
            
            world.spawnParticles(dust, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }
    
    // ==================== 血量平均系统 ====================
    
    private static long lastHPAveragingTime = 0;
    
    /**
     * 血量平均：每60秒随机两人血量强制平均
     */
    public static void tickHPAveraging(MinecraftServer server) {
        ensureServerState(server);
        if (!ChaosMod.config.hpAveragingEnabled) {
            lastHPAveragingTime = server.getTicks();
            return;
        }
        
        long currentTime = server.getTicks();
        
        if (currentTime - lastHPAveragingTime >= intervalTicks(ChaosMod.config.hpAveragingIntervalSeconds)) {
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (players.size() >= 2) {
                ServerPlayerEntity p1 = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                ServerPlayerEntity p2;
                do {
                    p2 = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                } while (p1 == p2);
                
                float hp1 = p1.getHealth();
                float hp2 = p2.getHealth();
                float avgHP = (hp1 + hp2) / 2.0F;
                
                // 限制不超过最大血量
                float finalHP1 = Math.min(avgHP, p1.getMaxHealth());
                float finalHP2 = Math.min(avgHP, p2.getMaxHealth());
                
                p1.setHealth(finalHP1);
                p2.setHealth(finalHP2);
                
                // 发送消息
                p1.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("hp_averaging_result"),
                    p2.getName().getString(),
                    hp1 / 2.0F,
                    finalHP1 / 2.0F
                )).formatted(Formatting.AQUA), false);
                
                p2.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("hp_averaging_result"),
                    p1.getName().getString(),
                    hp2 / 2.0F,
                    finalHP2 / 2.0F
                )).formatted(Formatting.AQUA), false);
                
                // 全服广播
                server.getPlayerManager().broadcast(
                    Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("hp_averaging_broadcast"),
                        p1.getName().getString(),
                        p2.getName().getString()
                    )).formatted(Formatting.GOLD),
                    false
                );
                
                // 粒子特效
                p1.getServerWorld().spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    p1.getX(), p1.getY() + 1, p1.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                p2.getServerWorld().spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    p2.getX(), p2.getY() + 1, p2.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }
            lastHPAveragingTime = currentTime;
        }
    }
    
    // ==================== 死亡轮盘多人版 ====================
    
    private static class RouletteData {
        final ServerPlayerEntity trigger;
        final long executeTime;
        int countdown;
        
        RouletteData(ServerPlayerEntity trigger, long executeTime) {
            this.trigger = trigger;
            this.executeTime = executeTime;
            this.countdown = 5;
        }
    }
    
    private static RouletteData currentRoulette = null;
    private static long lastRouletteTime = 0;
    
    /**
     * 死亡轮盘：每90秒随机一人触发轮盘抽奖
     */
    public static void tickMultiplayerRoulette(MinecraftServer server) {
        ensureServerState(server);
        if (!ChaosMod.config.multiplayerRouletteEnabled) {
            currentRoulette = null;
            lastRouletteTime = server.getTicks();
            return;
        }
        
        long currentTime = server.getTicks();
        
        // 触发新轮盘
        if (currentRoulette == null && currentTime - lastRouletteTime >= intervalTicks(ChaosMod.config.multiplayerRouletteIntervalSeconds)) {
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (!players.isEmpty()) {
                ServerPlayerEntity trigger = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                currentRoulette = new RouletteData(trigger, currentTime + 100); // 5秒后执行
                
                server.getPlayerManager().broadcast(
                    Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("roulette_triggered"),
                        trigger.getName().getString()
                    )).formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
                    false
                );
                
                // 粒子特效
                trigger.getServerWorld().spawnParticles(ParticleTypes.PORTAL,
                    trigger.getX(), trigger.getY() + 2, trigger.getZ(), 50, 0.5, 0.5, 0.5, 0.5);
            }
            lastRouletteTime = currentTime;
        }
        
        // 处理进行中的轮盘
        if (currentRoulette != null) {
            // 检查触发者是否还在线
            if (currentRoulette.trigger.isDisconnected() || currentRoulette.trigger.isRemoved()) {
                currentRoulette = null;
                return;
            }
            
            // 倒计时显示
            if (currentTime % 20 == 0 && currentRoulette.countdown > 0) {
                currentRoulette.trigger.sendMessage(
                    Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("roulette_countdown"),
                        currentRoulette.countdown
                    )).formatted(Formatting.RED, Formatting.BOLD),
                    true
                );
                currentRoulette.countdown--;
            }
            
            // 执行抽奖
            if (currentTime >= currentRoulette.executeTime) {
                executeRoulette(server, currentRoulette.trigger);
                currentRoulette = null;
            }
        }
    }
    
    /**
     * 执行轮盘抽奖
     */
    private static void executeRoulette(MinecraftServer server, ServerPlayerEntity trigger) {
        double roll = ThreadLocalRandom.current().nextDouble();
        
        if (roll < 0.60) {
            // 60% - 安全
            trigger.sendMessage(Text.literal(
                com.example.config.LanguageManager.getMessage("roulette_safe")
            ).formatted(Formatting.GREEN, Formatting.BOLD), false);
            
            server.getPlayerManager().broadcast(
                Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("roulette_safe_broadcast"),
                    trigger.getName().getString()
                )).formatted(Formatting.GREEN),
                false
            );
            
        } else if (roll < 0.85) {
            // 25% - 自己受伤
            trigger.damage(trigger.getDamageSources().magic(), 6.0F); // 3颗心
            
            trigger.sendMessage(Text.literal(
                com.example.config.LanguageManager.getMessage("roulette_self_damage")
            ).formatted(Formatting.RED, Formatting.BOLD), false);
            
            server.getPlayerManager().broadcast(
                Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("roulette_self_damage_broadcast"),
                    trigger.getName().getString()
                )).formatted(Formatting.RED),
                false
            );
            
        } else if (roll < 0.95) {
            // 10% - 随机他人受伤
            List<ServerPlayerEntity> others = eligiblePlayers(server).stream()
                .filter(p -> p != trigger)
                .toList();
            
            if (!others.isEmpty()) {
                ServerPlayerEntity victim = others.get(ThreadLocalRandom.current().nextInt(others.size()));
                victim.damage(victim.getDamageSources().magic(), 10.0F); // 5颗心
                
                trigger.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("roulette_others_damage_trigger"),
                    victim.getName().getString()
                )).formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                
                victim.sendMessage(Text.literal(
                    com.example.config.LanguageManager.getMessage("roulette_others_damage_victim")
                ).formatted(Formatting.RED, Formatting.BOLD), false);
                
                server.getPlayerManager().broadcast(
                    Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("roulette_others_damage_broadcast"),
                        trigger.getName().getString(),
                        victim.getName().getString()
                    )).formatted(Formatting.DARK_RED),
                    false
                );
                
                // 爆炸粒子
                victim.getServerWorld().spawnParticles(ParticleTypes.EXPLOSION,
                    victim.getX(), victim.getY() + 1, victim.getZ(), 10, 0.5, 0.5, 0.5, 0);
            }
            
        } else {
            // 5% - 全服受伤
            for (ServerPlayerEntity p : eligiblePlayers(server)) {
                p.damage(p.getDamageSources().magic(), 4.0F); // 2颗心
                p.sendMessage(Text.literal(
                    com.example.config.LanguageManager.getMessage("roulette_all_damage")
                ).formatted(Formatting.DARK_RED, Formatting.BOLD), true);
            }
            
            server.getPlayerManager().broadcast(
                Text.literal(
                    com.example.config.LanguageManager.getMessage("roulette_all_damage_broadcast")
                ).formatted(Formatting.DARK_RED, Formatting.BOLD),
                false
            );
        }
    }
    
    // ==================== 定时位置互换 ====================
    
    private static class SwapData {
        final ServerPlayerEntity player1;
        final ServerPlayerEntity player2;
        final long executeTime;
        int countdown;
        
        SwapData(ServerPlayerEntity p1, ServerPlayerEntity p2, long executeTime) {
            this.player1 = p1;
            this.player2 = p2;
            this.executeTime = executeTime;
            this.countdown = 5;
        }
    }
    
    private static SwapData currentSwap = null;
    private static long lastSwapTime = 0;
    
    /**
     * 定时位置互换：每60秒随机两人交换位置
     */
    public static void tickTimedPositionSwap(MinecraftServer server) {
        ensureServerState(server);
        if (!ChaosMod.config.timedPositionSwapEnabled) {
            currentSwap = null;
            lastSwapTime = server.getTicks();
            return;
        }
        
        long currentTime = server.getTicks();
        
        // 触发新交换
        if (currentSwap == null && currentTime - lastSwapTime >= intervalTicks(ChaosMod.config.timedPositionSwapIntervalSeconds)) {
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (players.size() >= 2) {
                ServerPlayerEntity p1 = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                ServerPlayerEntity p2;
                do {
                    p2 = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                } while (p1 == p2);
                
                currentSwap = new SwapData(p1, p2, currentTime + 100); // 5秒后执行
                
                p1.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("position_swap_warning"),
                    p2.getName().getString()
                )).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
                
                p2.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("position_swap_warning"),
                    p1.getName().getString()
                )).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
                
                // 粒子特效
                p1.getServerWorld().spawnParticles(ParticleTypes.PORTAL,
                    p1.getX(), p1.getY() + 1, p1.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
                p2.getServerWorld().spawnParticles(ParticleTypes.PORTAL,
                    p2.getX(), p2.getY() + 1, p2.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
            }
            lastSwapTime = currentTime;
        }
        
        // 处理进行中的交换
        if (currentSwap != null) {
            // 检查玩家是否还在线
            if (currentSwap.player1.isDisconnected() || currentSwap.player1.isRemoved() ||
                currentSwap.player2.isDisconnected() || currentSwap.player2.isRemoved()) {
                currentSwap = null;
                return;
            }
            
            // 倒计时显示
            if (currentTime % 20 == 0 && currentSwap.countdown > 0) {
                currentSwap.player1.sendMessage(
                    Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("position_swap_countdown"),
                        currentSwap.countdown
                    )).formatted(Formatting.YELLOW),
                    true
                );
                currentSwap.player2.sendMessage(
                    Text.literal(String.format(
                        com.example.config.LanguageManager.getMessage("position_swap_countdown"),
                        currentSwap.countdown
                    )).formatted(Formatting.YELLOW),
                    true
                );
                currentSwap.countdown--;
            }
            
            // 执行交换
            if (currentTime >= currentSwap.executeTime) {
                executePositionSwap(server, currentSwap.player1, currentSwap.player2);
                currentSwap = null;
            }
        }
    }
    
    /**
     * 执行位置交换
     */
    private static void executePositionSwap(MinecraftServer server, ServerPlayerEntity p1, ServerPlayerEntity p2) {
        // 保存状态
        Vec3d pos1 = p1.getPos();
        Vec3d pos2 = p2.getPos();
        float yaw1 = p1.getYaw();
        float pitch1 = p1.getPitch();
        float yaw2 = p2.getYaw();
        float pitch2 = p2.getPitch();
        Vec3d vel1 = p1.getVelocity();
        Vec3d vel2 = p2.getVelocity();
        
        ServerWorld world1 = p1.getServerWorld();
        ServerWorld world2 = p2.getServerWorld();
        
        // 交换位置（保持各自朝向）
        p1.teleport(world2, pos2.x, pos2.y, pos2.z, yaw1, pitch1);
        p2.teleport(world1, pos1.x, pos1.y, pos1.z, yaw2, pitch2);
        
        // 恢复速度
        p1.setVelocity(vel1);
        p2.setVelocity(vel2);
        
        // 发送消息
        p1.sendMessage(Text.literal(String.format(
            com.example.config.LanguageManager.getMessage("position_swap_done"),
            p2.getName().getString()
        )).formatted(Formatting.LIGHT_PURPLE), false);
        
        p2.sendMessage(Text.literal(String.format(
            com.example.config.LanguageManager.getMessage("position_swap_done"),
            p1.getName().getString()
        )).formatted(Formatting.LIGHT_PURPLE), false);
        
        // 全服广播
        server.getPlayerManager().broadcast(
            Text.literal(String.format(
                com.example.config.LanguageManager.getMessage("position_swap_broadcast"),
                p1.getName().getString(),
                p2.getName().getString()
            )).formatted(Formatting.GOLD),
            false
        );
        
        // 传送特效
        world2.spawnParticles(ParticleTypes.PORTAL,
            pos2.x, pos2.y + 1, pos2.z, 50, 0.5, 1, 0.5, 0.5);
        world1.spawnParticles(ParticleTypes.PORTAL,
            pos1.x, pos1.y + 1, pos1.z, 50, 0.5, 1, 0.5, 0.5);
    }
    
    // ==================== 强制奔跑系统 ====================
    
    private static class SprintData {
        final ServerPlayerEntity player;
        final long endTime;
        Vec3d lastPos;
        int stopCounter;
        boolean isDamaging;
        
        SprintData(ServerPlayerEntity player, long endTime) {
            this.player = player;
            this.endTime = endTime;
            this.lastPos = player.getPos();
            this.stopCounter = 0;
            this.isDamaging = false;
        }
    }
    
    private static final List<SprintData> activeSprints = new ArrayList<>();
    private static long lastSprintTime = 0;
    private static final long SPRINT_DURATION = 1200; // 60秒 = 1200 ticks
    private static final double MOVEMENT_THRESHOLD = 0.01; // 移动阈值
    private static final int STOP_THRESHOLD = 60; // 3秒 = 60 ticks
    
    /**
     * 强制奔跑：每90秒随机一人必须持续移动
     */
    public static void tickForcedSprint(MinecraftServer server) {
        ensureServerState(server);
        if (!ChaosMod.config.forcedSprintEnabled) {
            activeSprints.clear();
            lastSprintTime = server.getTicks();
            return;
        }
        
        long currentTime = server.getTicks();
        
        // 触发新强制奔跑
        if (currentTime - lastSprintTime >= intervalTicks(ChaosMod.config.forcedSprintIntervalSeconds)) {
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (!players.isEmpty()) {
                ServerPlayerEntity target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                SprintData data = new SprintData(target, currentTime + SPRINT_DURATION);
                activeSprints.add(data);
                
                target.sendMessage(Text.literal(
                    com.example.config.LanguageManager.getMessage("forced_sprint_start")
                ).formatted(Formatting.YELLOW, Formatting.BOLD), false);
            }
            lastSprintTime = currentTime;
        }
        
        // 处理所有活跃的强制奔跑
        Iterator<SprintData> iterator = activeSprints.iterator();
        while (iterator.hasNext()) {
            SprintData data = iterator.next();
            
            // 检查是否过期或玩家离线
            if (currentTime >= data.endTime ||
                data.player.isDisconnected() || data.player.isRemoved()) {
                
                if (!data.player.isDisconnected() && !data.player.isRemoved()) {
                    data.player.sendMessage(Text.literal(
                        com.example.config.LanguageManager.getMessage("forced_sprint_end")
                    ).formatted(Formatting.GREEN), true);
                }
                
                iterator.remove();
                continue;
            }
            
            // 检测移动
            Vec3d currentPos = data.player.getPos();
            double moved = currentPos.distanceTo(data.lastPos);
            
            if (moved < MOVEMENT_THRESHOLD) {
                // 静止中
                data.stopCounter++;
                
                if (data.stopCounter >= STOP_THRESHOLD) {
                    // 停止超过3秒，开始扣血
                    if (!data.isDamaging) {
                        data.isDamaging = true;
                        data.player.sendMessage(Text.literal(
                            com.example.config.LanguageManager.getMessage("forced_sprint_stop_warning")
                        ).formatted(Formatting.RED), true);
                    }
                    
                    // 每40 ticks (2秒) 扣血
                    if (currentTime % 40 == 0) {
                        data.player.damage(data.player.getDamageSources().magic(), 1.0F);
                        data.player.sendMessage(Text.literal(
                            com.example.config.LanguageManager.getMessage("forced_sprint_damage")
                        ).formatted(Formatting.RED), true);
                        
                        // 粒子特效
                        data.player.getServerWorld().spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                            data.player.getX(), data.player.getY() + 2, data.player.getZ(),
                            5, 0.3, 0.3, 0.3, 0);
                    }
                }
            } else {
                // 正在移动
                if (data.isDamaging) {
                    data.player.sendMessage(Text.literal(
                        com.example.config.LanguageManager.getMessage("forced_sprint_resume")
                    ).formatted(Formatting.GREEN), true);
                }
                data.stopCounter = 0;
                data.isDamaging = false;
            }
            
            data.lastPos = currentPos;
            
            // 显示剩余时间（每秒更新）
            if (currentTime % 20 == 0) {
                long remainingSeconds = (data.endTime - currentTime) / 20;
                data.player.sendMessage(Text.literal(String.format(
                    com.example.config.LanguageManager.getMessage("forced_sprint_remaining"),
                    remainingSeconds
                )).formatted(Formatting.YELLOW), true);
            }
        }
    }
    
    /**
     * 清理玩家数据（玩家真正离线时才清理）
     */
    public static void cleanupPlayerData(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();
        
        // 使用UUID比较，确保正确清理
        activeTethers.removeIf(pair -> 
            pair.player1UUID.equals(playerUUID) || pair.player2UUID.equals(playerUUID)
        );
        activeSprints.removeIf(data -> data.player.getUuid().equals(playerUUID));
        
        if (currentRoulette != null && currentRoulette.trigger.getUuid().equals(playerUUID)) {
            currentRoulette = null;
        }
        if (currentSwap != null && 
            (currentSwap.player1.getUuid().equals(playerUUID) || currentSwap.player2.getUuid().equals(playerUUID))) {
            currentSwap = null;
        }
    }
}
