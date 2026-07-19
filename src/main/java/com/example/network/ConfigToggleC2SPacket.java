package com.example.network;

import com.example.ChaosMod;
import com.example.config.ChaosModConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigToggleC2SPacket implements CustomPayload {
    public static final CustomPayload.Id<ConfigToggleC2SPacket> ID =
        new CustomPayload.Id<>(Identifier.of(ChaosMod.MOD_ID, "config_toggle"));
    public static final PacketCodec<RegistryByteBuf, ConfigToggleC2SPacket> CODEC = PacketCodec.of(
        (packet, buf) -> {
            buf.writeVarInt(packet.changes.size());
            packet.changes.forEach((key, value) -> {
                buf.writeString(key, 64);
                buf.writeBoolean(value);
            });
            buf.writeVarInt(packet.intervalChanges.size());
            packet.intervalChanges.forEach((key, value) -> {
                buf.writeString(key, 64);
                buf.writeVarInt(value);
            });
            buf.writeVarInt(packet.percentageChanges.size());
            packet.percentageChanges.forEach((key, value) -> {
                buf.writeString(key, 64);
                buf.writeVarInt(value);
            });
        },
        buf -> {
            int size = buf.readVarInt();
            if (size < 0 || size > ChaosModConfig.CONFIG_KEYS.size()) {
                throw new IllegalArgumentException("Invalid config update size: " + size);
            }
            Map<String, Boolean> changes = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                changes.put(buf.readString(64), buf.readBoolean());
            }
            int intervalSize = buf.readVarInt();
            if (intervalSize < 0 || intervalSize > ChaosModConfig.INTERVAL_KEYS.size()) {
                throw new IllegalArgumentException("Invalid interval update size: " + intervalSize);
            }
            Map<String, Integer> intervalChanges = new LinkedHashMap<>();
            for (int i = 0; i < intervalSize; i++) {
                intervalChanges.put(buf.readString(64), buf.readVarInt());
            }
            int percentageSize = buf.readVarInt();
            if (percentageSize < 0 || percentageSize > ChaosModConfig.PERCENTAGE_KEYS.size()) {
                throw new IllegalArgumentException("Invalid percentage update size: " + percentageSize);
            }
            Map<String, Integer> percentageChanges = new LinkedHashMap<>();
            for (int i = 0; i < percentageSize; i++) {
                percentageChanges.put(buf.readString(64), buf.readVarInt());
            }
            return new ConfigToggleC2SPacket(changes, intervalChanges, percentageChanges);
        }
    );

    private final String key;
    private final boolean value;
    private final Map<String, Boolean> changes;
    private final Map<String, Integer> intervalChanges;
    private final Map<String, Integer> percentageChanges;
    
    public ConfigToggleC2SPacket(String key, boolean value) {
        this.key = key;
        this.value = value;
        this.changes = Map.of(key, value);
        this.intervalChanges = Map.of();
        this.percentageChanges = Map.of();
    }

    public ConfigToggleC2SPacket(Map<String, Boolean> changes) {
        this.changes = Map.copyOf(changes);
        Map.Entry<String, Boolean> first = this.changes.entrySet().stream().findFirst().orElse(null);
        this.key = first == null ? "" : first.getKey();
        this.value = first != null && first.getValue();
        this.intervalChanges = Map.of();
        this.percentageChanges = Map.of();
    }

    public ConfigToggleC2SPacket(Map<String, Boolean> changes, Map<String, Integer> intervalChanges) {
        this(changes, intervalChanges, Map.of());
    }

    public ConfigToggleC2SPacket(
            Map<String, Boolean> changes,
            Map<String, Integer> intervalChanges,
            Map<String, Integer> percentageChanges
    ) {
        this.changes = Map.copyOf(changes);
        this.intervalChanges = Map.copyOf(intervalChanges);
        this.percentageChanges = Map.copyOf(percentageChanges);
        Map.Entry<String, Boolean> first = this.changes.entrySet().stream().findFirst().orElse(null);
        this.key = first == null ? "" : first.getKey();
        this.value = first != null && first.getValue();
    }

    public static ConfigToggleC2SPacket interval(String key, int seconds) {
        return new ConfigToggleC2SPacket(Map.of(), Map.of(key, seconds));
    }

    public static ConfigToggleC2SPacket percentage(String key, int percentage) {
        return new ConfigToggleC2SPacket(Map.of(), Map.of(), Map.of(key, percentage));
    }
    
    public static ConfigToggleC2SPacket create(String key, boolean value) {
        return new ConfigToggleC2SPacket(key, value);
    }
    
    // 简化版本：直接处理配置更新，不使用复杂的网络包系统
    public static void updateConfig(String key, boolean value, ServerPlayerEntity player) {
        updateConfig(Map.of(key, value), Map.of(), Map.of(), player);
    }

    private static void updateConfig(
            Map<String, Boolean> changes,
            Map<String, Integer> intervalChanges,
            Map<String, Integer> percentageChanges,
            ServerPlayerEntity player
    ) {
        if (changes.isEmpty() && intervalChanges.isEmpty() && percentageChanges.isEmpty()) {
            ConfigSyncPacket.send(player);
            return;
        }

        // 🔒 服务端权限复核：防止客户端绕过权限检查
        if (!ConfigSyncPacket.hasConfigPermission(player)) {
            player.sendMessage(Text.literal(com.example.config.LanguageManager.getMessage("config_permission_denied"))
                .formatted(Formatting.RED, Formatting.BOLD), false);
            ConfigSyncPacket.send(player);
            return;
        }

        // 验证配置键的有效性
        for (String key : changes.keySet()) {
            if (!isValidConfigKey(key)) {
                player.sendMessage(Text.literal(com.example.config.LanguageManager.getMessage("config_invalid_key") + ": " + key)
                    .formatted(Formatting.RED), false);
                ConfigSyncPacket.send(player);
                return;
            }
        }
        for (String key : intervalChanges.keySet()) {
            if (!ChaosModConfig.isValidIntervalKey(key)) {
                ConfigSyncPacket.send(player);
                return;
            }
        }
        for (String key : percentageChanges.keySet()) {
            if (!ChaosModConfig.isValidPercentageKey(key)) {
                ConfigSyncPacket.send(player);
                return;
            }
        }

        Map<String, Boolean> nextValues = ChaosMod.config.snapshot();
        nextValues.putAll(changes);
        int mutexEnabled = 0;
        for (String mutexKey : new String[]{
                "playerDamageShareEnabled", "sharedHealthEnabled",
                "sharedDamageSplitEnabled", "randomDamageEnabled"}) {
            if (Boolean.TRUE.equals(nextValues.get(mutexKey))) {
                mutexEnabled++;
            }
        }
        if (mutexEnabled > 1) {
            ConfigSyncPacket.send(player);
            return;
        }

        // 更新配置
        changes.forEach(ChaosMod.config::set);
        intervalChanges.forEach(ChaosMod.config::setIntervalSeconds);
        percentageChanges.forEach(ChaosMod.config::setPercentage);
        if (intervalChanges.containsKey("damageScapegoatIntervalSeconds")) {
            com.example.util.ScapegoatSystem.onIntervalChanged(player.getServer());
        }
        if (intervalChanges.containsKey("vertigoScapegoatIntervalSeconds")) {
            com.example.util.ChaosEffects.onVertigoIntervalChanged(player.getServer());
        }

        // 发送确认消息（支持多语言）
        if (changes.size() == 1) {
            Map.Entry<String, Boolean> change = changes.entrySet().iterator().next();
            String state = change.getValue() ? "✓ 启用" : "✗ 禁用";
            String stateEn = change.getValue() ? "✓ Enabled" : "✗ Disabled";
            String currentState = "zh_cn".equals(com.example.ChaosMod.config.getLanguage()) ? state : stateEn;
            player.sendMessage(Text.literal("[" + com.example.config.LanguageManager.getMessage("config_updated") + "] " + change.getKey() + " -> " + currentState)
                .formatted(Formatting.YELLOW), false);

            // 广播给其他管理员
            broadcastConfigChange(player.getServer(), player, change.getKey(), change.getValue());
        }

        ConfigSyncPacket.broadcast(player.getServer());
    }
    
    public static void registerServerReceiver() {
        // 简化版本：暂时不注册复杂的网络包
        // 在集成服务器中，客户端和服务端共享同一个配置实例
        ServerPlayNetworking.registerGlobalReceiver(ID, (packet, context) ->
            context.server().execute(() -> updateConfig(
                packet.changes, packet.intervalChanges, packet.percentageChanges, context.player()
            ))
        );
    }
    
    private static boolean isValidConfigKey(String key) {
        return ChaosModConfig.isValidKey(key);
    }
    
    private static void broadcastConfigChange(net.minecraft.server.MinecraftServer server,
                                            ServerPlayerEntity sender, String key, boolean value) {
        if (server == null) return;
        
        // 多语言状态显示
        String language = com.example.ChaosMod.config.getLanguage();
        String state, changedText;
        if ("en_us".equals(language)) {
            state = value ? "✓ Enabled" : "✗ Disabled";
            changedText = com.example.config.LanguageManager.getMessage("config_changed");
        } else {
            state = value ? "✓ 启用" : "✗ 禁用";
            changedText = com.example.config.LanguageManager.getMessage("config_changed");
        }
        
        // 完全多语言的管理员广播消息
        String broadcastTemplate = "en_us".equals(language) ? 
            "[ChaosMod] %s %s %s to %s" : 
            "[ChaosMod] %s %s %s 设置为 %s";
        
        Text message = Text.literal(String.format(broadcastTemplate, 
            sender.getName().getString(), changedText, key, state))
            .formatted(Formatting.GRAY);
        
        // 发送给所有在线管理员（除了发送者）
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (player.hasPermissionLevel(4) && !player.equals(sender)) {
                player.sendMessage(message, false);
            }
        });
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public Map<String, Boolean> getChanges() {
        return changes;
    }

    public Map<String, Integer> getIntervalChanges() {
        return intervalChanges;
    }

    public Map<String, Integer> getPercentageChanges() {
        return percentageChanges;
    }

    public String getKey() {
        return key;
    }
    
    public boolean getValue() {
        return value;
    }
}
