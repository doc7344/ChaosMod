package com.example.util;

import com.example.ChaosMod;
import com.example.mixin.HungerManagerAccessor;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端权威的共享生命与饥饿状态。
 *
 * 生命变化通过 LivingEntity#setHealth 的真实入口即时镜像；饥饿系统每个服务端
 * tick 只由一名有效玩家执行一次原版 HungerManager#update，避免自然回血、饥饿
 * 扣血按在线人数重复结算。其他玩家在本 tick 产生的进食与消耗差值会汇总到同一
 * 份食物、饱和度和消耗度状态。
 */
public final class SharedVitalitySystem {
    private static final ThreadLocal<Boolean> APPLYING_SHARED_STATE =
        ThreadLocal.withInitial(() -> false);

    private static MinecraftServer stateServer;
    private static boolean active;
    private static boolean awaitingRespawnReset;
    private static float sharedHealth;
    private static int sharedFoodLevel;
    private static float sharedSaturation;
    private static float sharedExhaustion;
    private static int sharedFoodTickTimer;
    private static int sharedPrevFoodLevel;
    private static UUID hungerLeader;
    private static final Map<UUID, HungerSnapshot> START_HUNGER = new HashMap<>();
    private static final Map<UUID, Integer> PARTICIPANT_ENTITY_IDS = new HashMap<>();

    private SharedVitalitySystem() {}

    public static void startServerTick(MinecraftServer server) {
        ensureServer(server);
        if (!ChaosMod.config.sharedHealthEnabled) {
            clearRuntimeState();
            return;
        }

        List<ServerPlayerEntity> players = livingParticipants(server);
        if (players.isEmpty()) {
            hungerLeader = null;
            START_HUNGER.clear();
            return;
        }

        if (!active || awaitingRespawnReset) {
            initializeFrom(players);
        } else {
            registerCurrentEntities(players);
        }

        sharedHealth = Math.min(sharedHealth, commonHealthCap(players));
        applyHealthToLivingPlayers(players, null);
        applyHungerToPlayers(players);

        ServerPlayerEntity currentLeader = hungerLeader == null
            ? null : server.getPlayerManager().getPlayer(hungerLeader);
        if (!isLivingParticipant(currentLeader)) {
            hungerLeader = players.stream()
                .min(Comparator.comparing(player -> player.getUuid().toString()))
                .map(ServerPlayerEntity::getUuid)
                .orElse(null);
        }

        START_HUNGER.clear();
        for (ServerPlayerEntity player : players) {
            START_HUNGER.put(player.getUuid(), HungerSnapshot.capture(player));
        }
    }

    public static void endServerTick(MinecraftServer server) {
        if (server != stateServer || !ChaosMod.config.sharedHealthEnabled
                || !active || awaitingRespawnReset) {
            return;
        }

        List<ServerPlayerEntity> players = livingParticipants(server);
        if (players.isEmpty()) return;

        ServerPlayerEntity leader = hungerLeader == null
            ? null : server.getPlayerManager().getPlayer(hungerLeader);
        if (!isLivingParticipant(leader)) {
            leader = players.get(0);
            hungerLeader = leader.getUuid();
        }

        HungerManager leaderHunger = leader.getHungerManager();
        int nextFood = leaderHunger.getFoodLevel();
        float nextSaturation = leaderHunger.getSaturationLevel();
        float nextExhaustion = leaderHunger.getExhaustion();

        for (ServerPlayerEntity player : players) {
            if (player == leader) continue;
            HungerSnapshot before = START_HUNGER.get(player.getUuid());
            if (before == null) continue;

            HungerManager current = player.getHungerManager();
            nextFood += current.getFoodLevel() - before.foodLevel();
            nextSaturation += current.getSaturationLevel() - before.saturation();
            nextExhaustion += current.getExhaustion() - before.exhaustion();
        }

        sharedFoodLevel = clampFood(nextFood);
        sharedSaturation = clampSaturation(nextSaturation, sharedFoodLevel);
        sharedExhaustion = Math.max(0.0F, nextExhaustion);
        sharedFoodTickTimer = ((HungerManagerAccessor)(Object)leaderHunger).chaos$getFoodTickTimer();
        sharedPrevFoodLevel = leaderHunger.getPrevFoodLevel();
        applyHungerToPlayers(players);
    }

    /** HungerManagerMixin 用此方法保证共同饥饿状态每 tick 只跑一次原版更新。 */
    public static boolean shouldRunHungerUpdate(PlayerEntity player) {
        if (!ChaosMod.config.sharedHealthEnabled || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }
        if (!active || !isLivingParticipant(serverPlayer) || hungerLeader == null) {
            return true;
        }
        if (!isCurrentParticipantEntity(serverPlayer)) {
            return false;
        }
        return hungerLeader.equals(serverPlayer.getUuid());
    }

    /** LivingEntity#setHealth 完成后调用，将真实的新生命值镜像给全服。 */
    public static void onHealthChanged(ServerPlayerEntity source) {
        if (!ChaosMod.config.sharedHealthEnabled || awaitingRespawnReset
                || APPLYING_SHARED_STATE.get()
                || !isIncluded(source) || !isCurrentParticipantEntity(source)) {
            return;
        }
        MinecraftServer server = source.getServer();
        if (server == null) return;
        ensureServer(server);

        List<ServerPlayerEntity> living = livingParticipants(server);
        if (!active) {
            if (living.isEmpty()) return;
            initializeFrom(living);
        }

        float cap = living.isEmpty() ? source.getMaxHealth() : commonHealthCap(living);
        sharedHealth = Math.max(0.0F, Math.min(source.getHealth(), cap));
        if (sharedHealth <= 0.0F || !source.isAlive()) {
            triggerSharedDeath(server);
            return;
        }
        applyHealthToLivingPlayers(living, null);
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (!ChaosMod.config.sharedHealthEnabled || !active || awaitingRespawnReset
                || !isLivingParticipant(player)) {
            return;
        }
        PARTICIPANT_ENTITY_IDS.put(player.getUuid(), player.getId());
        applyHealthToLivingPlayers(List.of(player), null);
        applyHungerToPlayers(List.of(player));
    }

    public static void onPlayerRespawn(ServerPlayerEntity player) {
        if (!ChaosMod.config.sharedHealthEnabled || !active || awaitingRespawnReset
                || !isLivingParticipant(player)) {
            return;
        }
        PARTICIPANT_ENTITY_IDS.put(player.getUuid(), player.getId());
        applyHealthToLivingPlayers(List.of(player), null);
        applyHungerToPlayers(List.of(player));
    }

    public static void onPlayerDisconnect(UUID playerId) {
        START_HUNGER.remove(playerId);
        PARTICIPANT_ENTITY_IDS.remove(playerId);
        if (playerId.equals(hungerLeader)) hungerLeader = null;
    }

    public static void shutdown(MinecraftServer server) {
        if (stateServer != server) return;
        clearRuntimeState();
        stateServer = null;
    }

    private static void ensureServer(MinecraftServer server) {
        if (stateServer == server) return;
        clearRuntimeState();
        stateServer = server;
    }

    private static void initializeFrom(List<ServerPlayerEntity> players) {
        sharedHealth = players.stream()
            .map(ServerPlayerEntity::getHealth)
            .min(Float::compare)
            .orElse(20.0F);
        sharedHealth = Math.min(sharedHealth, commonHealthCap(players));
        sharedFoodLevel = players.stream()
            .mapToInt(player -> player.getHungerManager().getFoodLevel())
            .min()
            .orElse(20);
        sharedSaturation = players.stream()
            .map(player -> player.getHungerManager().getSaturationLevel())
            .min(Float::compare)
            .orElse(5.0F);
        sharedSaturation = clampSaturation(sharedSaturation, sharedFoodLevel);
        sharedExhaustion = players.stream()
            .map(player -> player.getHungerManager().getExhaustion())
            .max(Float::compare)
            .orElse(0.0F);
        sharedFoodTickTimer = players.stream()
            .mapToInt(player -> ((HungerManagerAccessor)(Object)player.getHungerManager())
                .chaos$getFoodTickTimer())
            .max()
            .orElse(0);
        sharedPrevFoodLevel = sharedFoodLevel;
        active = true;
        awaitingRespawnReset = false;
        PARTICIPANT_ENTITY_IDS.clear();
        registerCurrentEntities(players);
        applyHealthToLivingPlayers(players, null);
        applyHungerToPlayers(players);
    }

    private static void triggerSharedDeath(MinecraftServer server) {
        sharedHealth = 0.0F;
        awaitingRespawnReset = true;
        hungerLeader = null;
        START_HUNGER.clear();
        APPLYING_SHARED_STATE.set(true);
        try {
            for (ServerPlayerEntity player : includedPlayers(server)) {
                if (player.isAlive()) {
                    DamageRouting.applyDirectDamage(
                        player, player.getDamageSources().genericKill(), Float.MAX_VALUE
                    );
                }
            }
        } finally {
            APPLYING_SHARED_STATE.set(false);
        }
    }

    private static void applyHealthToLivingPlayers(
            List<ServerPlayerEntity> players, ServerPlayerEntity source
    ) {
        APPLYING_SHARED_STATE.set(true);
        try {
            for (ServerPlayerEntity player : players) {
                if (player == source) continue;
                float target = Math.min(sharedHealth, player.getMaxHealth());
                if (Math.abs(player.getHealth() - target) > 0.0001F) {
                    player.setHealth(target);
                }
            }
        } finally {
            APPLYING_SHARED_STATE.set(false);
        }
    }

    private static void applyHungerToPlayers(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            HungerManager hunger = player.getHungerManager();
            if (hunger.getFoodLevel() != sharedFoodLevel) {
                hunger.setFoodLevel(sharedFoodLevel);
            }
            if (Math.abs(hunger.getSaturationLevel() - sharedSaturation) > 0.0001F) {
                hunger.setSaturationLevel(sharedSaturation);
            }
            if (Math.abs(hunger.getExhaustion() - sharedExhaustion) > 0.0001F) {
                hunger.setExhaustion(sharedExhaustion);
            }
            HungerManagerAccessor accessor = (HungerManagerAccessor)(Object)hunger;
            if (accessor.chaos$getFoodTickTimer() != sharedFoodTickTimer) {
                accessor.chaos$setFoodTickTimer(sharedFoodTickTimer);
            }
            accessor.chaos$setPrevFoodLevel(sharedPrevFoodLevel);
        }
    }

    private static List<ServerPlayerEntity> livingParticipants(MinecraftServer server) {
        return includedPlayers(server).stream()
            .filter(ServerPlayerEntity::isAlive)
            .toList();
    }

    private static List<ServerPlayerEntity> includedPlayers(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList().stream()
            .filter(SharedVitalitySystem::isIncluded)
            .toList();
    }

    private static boolean isLivingParticipant(ServerPlayerEntity player) {
        return isIncluded(player) && player.isAlive();
    }

    private static boolean isIncluded(ServerPlayerEntity player) {
        return player != null && !player.isCreative() && !player.isSpectator()
            && !player.isDisconnected() && !player.isRemoved();
    }

    private static boolean isCurrentParticipantEntity(ServerPlayerEntity player) {
        Integer entityId = PARTICIPANT_ENTITY_IDS.get(player.getUuid());
        return entityId != null && entityId == player.getId();
    }

    private static void registerCurrentEntities(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            PARTICIPANT_ENTITY_IDS.put(player.getUuid(), player.getId());
        }
        PARTICIPANT_ENTITY_IDS.keySet().removeIf(uuid -> players.stream()
            .noneMatch(player -> player.getUuid().equals(uuid)));
    }

    private static float commonHealthCap(List<ServerPlayerEntity> players) {
        return players.stream()
            .map(ServerPlayerEntity::getMaxHealth)
            .min(Float::compare)
            .orElse(20.0F);
    }

    private static int clampFood(int value) {
        return Math.max(0, Math.min(20, value));
    }

    private static float clampSaturation(float value, int foodLevel) {
        return Math.max(0.0F, Math.min(value, (float)foodLevel));
    }

    private static void clearRuntimeState() {
        active = false;
        awaitingRespawnReset = false;
        sharedHealth = 0.0F;
        sharedFoodLevel = 0;
        sharedSaturation = 0.0F;
        sharedExhaustion = 0.0F;
        sharedFoodTickTimer = 0;
        sharedPrevFoodLevel = 0;
        hungerLeader = null;
        START_HUNGER.clear();
        PARTICIPANT_ENTITY_IDS.clear();
        APPLYING_SHARED_STATE.set(false);
    }

    private record HungerSnapshot(int foodLevel, float saturation, float exhaustion) {
        private static HungerSnapshot capture(ServerPlayerEntity player) {
            HungerManager hunger = player.getHungerManager();
            return new HungerSnapshot(
                hunger.getFoodLevel(), hunger.getSaturationLevel(), hunger.getExhaustion()
            );
        }
    }
}
