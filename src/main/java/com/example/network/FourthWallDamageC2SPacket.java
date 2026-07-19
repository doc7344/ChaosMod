package com.example.network;

import com.example.ChaosMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C2S packet for applying fourth wall damage from client effects
 * 客户端向服务端发送第四面墙效果造成的额外伤害
 */
public record FourthWallDamageC2SPacket(float damageAmount, String damageType) implements CustomPayload {
    public static final CustomPayload.Id<FourthWallDamageC2SPacket> ID = 
        new CustomPayload.Id<>(Identifier.of("chaosmod", "fourth_wall_damage"));
    
    public static final PacketCodec<RegistryByteBuf, FourthWallDamageC2SPacket> CODEC = 
        PacketCodec.tuple(
            PacketCodec.of(
                (value, buf) -> buf.writeFloat(value),
                (buf) -> buf.readFloat()
            ), FourthWallDamageC2SPacket::damageAmount,
            PacketCodec.of(
                (value, buf) -> buf.writeString(value),
                (buf) -> buf.readString()
            ), FourthWallDamageC2SPacket::damageType,
            FourthWallDamageC2SPacket::new
        );

    private record PendingDamage(String damageType, float damageAmount, long expiresAtTick) {}

    private static final Map<UUID, PendingDamage> PENDING_DAMAGE = new ConcurrentHashMap<>();
    private static final long AUTHORIZATION_LIFETIME_TICKS = 200L;

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Register packet handler on server side
     */
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (packet, context) -> {
            ServerPlayerEntity player = context.player();
            
            // 在服务端主线程中处理
            context.server().execute(() -> {
                PendingDamage pending = PENDING_DAMAGE.get(player.getUuid());
                long currentTick = context.server().getTicks();
                if (pending == null) return;
                if (currentTick > pending.expiresAtTick()) {
                    PENDING_DAMAGE.remove(player.getUuid(), pending);
                    return;
                }
                if (!pending.damageType().equals(packet.damageType())) return;
                if (Float.compare(pending.damageAmount(), packet.damageAmount()) != 0) return;

                boolean validDamage = switch (packet.damageType()) {
                    case "window_shake" -> ChaosMod.config.windowViolentShakeEnabled;
                    case "desktop_file" -> ChaosMod.config.desktopPrankInvasionEnabled;
                    default -> false;
                };
                if (!validDamage) return;
                if (!PENDING_DAMAGE.remove(player.getUuid(), pending)) return;
                
                // 应用第四面墙伤害
                player.damage(player.getServerWorld().getDamageSources().magic(), pending.damageAmount());
            });
        });
    }

    /**
     * 服务端在发送对应的 S2C 行为前授权一次回执伤害。没有授权或重复回执都会被拒绝。
     */
    public static void authorize(ServerPlayerEntity player, String damageType, float damageAmount) {
        if (player == null || player.getServer() == null) return;
        if (damageAmount <= 0.0F || damageAmount > 10.0F) return;
        if (!"desktop_file".equals(damageType) && !"window_shake".equals(damageType)) return;

        PENDING_DAMAGE.put(player.getUuid(), new PendingDamage(
            damageType, damageAmount, player.getServer().getTicks() + AUTHORIZATION_LIFETIME_TICKS
        ));
    }

    public static void cleanupPlayer(UUID playerId) {
        PENDING_DAMAGE.remove(playerId);
    }
}
