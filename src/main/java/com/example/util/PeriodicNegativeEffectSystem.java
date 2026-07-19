package com.example.util;

import com.example.ChaosMod;
import com.example.config.LanguageManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** 每名玩家独立循环的随机负面状态效果。 */
public final class PeriodicNegativeEffectSystem {
    private static final int EFFECT_DURATION_TICKS = 200;
    private static final Map<UUID, Long> NEXT_TRIGGER_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CONFIGURED_INTERVAL_TICKS = new ConcurrentHashMap<>();
    private static MinecraftServer stateServer;

    private PeriodicNegativeEffectSystem() {}

    public static void tickPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (stateServer != server) {
            stateServer = server;
            NEXT_TRIGGER_TICKS.clear();
            CONFIGURED_INTERVAL_TICKS.clear();
        }

        UUID playerId = player.getUuid();
        if (!ChaosMod.config.periodicNegativeEffectEnabled
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()
                || player.isDisconnected()) {
            NEXT_TRIGGER_TICKS.remove(playerId);
            CONFIGURED_INTERVAL_TICKS.remove(playerId);
            return;
        }

        long currentTick = server.getTicks();
        long intervalTicks = ChaosMod.config.periodicNegativeEffectIntervalSeconds * 20L;
        Long configuredIntervalTicks = CONFIGURED_INTERVAL_TICKS.put(playerId, intervalTicks);
        if (configuredIntervalTicks == null || configuredIntervalTicks != intervalTicks) {
            NEXT_TRIGGER_TICKS.put(playerId, currentTick + intervalTicks);
            return;
        }
        Long nextTriggerTick = NEXT_TRIGGER_TICKS.putIfAbsent(playerId, currentTick + intervalTicks);
        if (nextTriggerTick == null || currentTick < nextTriggerTick) {
            return;
        }

        List<RegistryEntry.Reference<StatusEffect>> harmfulEffects = Registries.STATUS_EFFECT.streamEntries()
            .filter(entry -> entry.value().getCategory() == StatusEffectCategory.HARMFUL)
            .toList();
        if (!harmfulEffects.isEmpty()) {
            RegistryEntry<StatusEffect> effect = harmfulEffects.get(
                ThreadLocalRandom.current().nextInt(harmfulEffects.size())
            );
            int duration = effect.value().isInstant() ? 1 : EFFECT_DURATION_TICKS;
            player.addStatusEffect(new StatusEffectInstance(effect, duration, 0));
            player.sendMessage(
                Text.literal(String.format(
                    LanguageManager.getMessage("periodic_negative_effect"),
                    effect.value().getName().getString()
                )).formatted(Formatting.DARK_RED),
                true
            );
        }

        NEXT_TRIGGER_TICKS.put(playerId, currentTick + intervalTicks);
    }

    public static void cleanupPlayer(UUID playerId) {
        NEXT_TRIGGER_TICKS.remove(playerId);
        CONFIGURED_INTERVAL_TICKS.remove(playerId);
    }
}
