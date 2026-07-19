package com.example.network;

import com.example.ChaosMod;
import com.example.config.ChaosModConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

// Simplified version without networking for now - GUI will work in single player / locally
public record ConfigSyncPacket(
        Map<String, Boolean> values,
        Map<String, Integer> intervals,
        Map<String, Integer> percentages,
        boolean hasPermission
) implements CustomPayload {
    public static final CustomPayload.Id<ConfigSyncPacket> ID =
        new CustomPayload.Id<>(Identifier.of(ChaosMod.MOD_ID, "config_sync"));
    public static final PacketCodec<RegistryByteBuf, ConfigSyncPacket> CODEC = PacketCodec.of(
        (packet, buf) -> {
            buf.writeVarInt(packet.values.size());
            packet.values.forEach((key, value) -> {
                buf.writeString(key, 64);
                buf.writeBoolean(value);
            });
            buf.writeVarInt(packet.intervals.size());
            packet.intervals.forEach((key, value) -> {
                buf.writeString(key, 64);
                buf.writeVarInt(value);
            });
            buf.writeVarInt(packet.percentages.size());
            packet.percentages.forEach((key, value) -> {
                buf.writeString(key, 64);
                buf.writeVarInt(value);
            });
            buf.writeBoolean(packet.hasPermission);
        },
        buf -> {
            int size = buf.readVarInt();
            if (size < 0 || size > ChaosModConfig.CONFIG_KEYS.size()) {
                throw new IllegalArgumentException("Invalid config snapshot size: " + size);
            }
            Map<String, Boolean> values = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String key = buf.readString(64);
                if (!ChaosModConfig.isValidKey(key)) {
                    throw new IllegalArgumentException("Invalid config key: " + key);
                }
                values.put(key, buf.readBoolean());
            }
            int intervalSize = buf.readVarInt();
            if (intervalSize < 0 || intervalSize > ChaosModConfig.INTERVAL_KEYS.size()) {
                throw new IllegalArgumentException("Invalid interval snapshot size: " + intervalSize);
            }
            Map<String, Integer> intervals = new LinkedHashMap<>();
            for (int i = 0; i < intervalSize; i++) {
                String key = buf.readString(64);
                if (!ChaosModConfig.isValidIntervalKey(key)) {
                    throw new IllegalArgumentException("Invalid interval key: " + key);
                }
                intervals.put(key, ChaosModConfig.clampIntervalSeconds(buf.readVarInt()));
            }
            int percentageSize = buf.readVarInt();
            if (percentageSize < 0 || percentageSize > ChaosModConfig.PERCENTAGE_KEYS.size()) {
                throw new IllegalArgumentException("Invalid percentage snapshot size: " + percentageSize);
            }
            Map<String, Integer> percentages = new LinkedHashMap<>();
            for (int i = 0; i < percentageSize; i++) {
                String key = buf.readString(64);
                if (!ChaosModConfig.isValidPercentageKey(key)) {
                    throw new IllegalArgumentException("Invalid percentage key: " + key);
                }
                percentages.put(key, ChaosModConfig.clampPercentage(buf.readVarInt()));
            }
            return new ConfigSyncPacket(values, intervals, percentages, buf.readBoolean());
        }
    );

    public ConfigSyncPacket {
        values = Map.copyOf(values);
        intervals = Map.copyOf(intervals);
        percentages = Map.copyOf(percentages);
    }

    // Simplified version that directly updates config (works for integrated server)
    public static void send(ServerPlayerEntity player) {
        if (player != null && ServerPlayNetworking.canSend(player, ID)) {
            ServerPlayNetworking.send(player, new ConfigSyncPacket(
                ChaosMod.config.snapshot(), ChaosMod.config.intervalSnapshot(),
                ChaosMod.config.percentageSnapshot(), hasConfigPermission(player)
            ));
        }
    }

    public static void broadcast(MinecraftServer server) {
        if (server == null) {
            return;
        }
        server.getPlayerManager().getPlayerList().forEach(ConfigSyncPacket::send);
    }

    public static boolean hasConfigPermission(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        return player.hasPermissionLevel(4)
            || (server != null && server.isSingleplayer() && server.isHost(player.getGameProfile()));
    }

    // Placeholder for future full networking implementation
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
