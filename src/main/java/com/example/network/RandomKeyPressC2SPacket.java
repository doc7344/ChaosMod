package com.example.network;

import com.example.ChaosMod;
import com.example.config.LanguageManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 随机按键癫痫 - C2S数据包
 */
public record RandomKeyPressC2SPacket() implements CustomPayload {
    public static final CustomPayload.Id<RandomKeyPressC2SPacket> ID = new CustomPayload.Id<>(Identifier.of("chaosmod", "random_key_press"));
    public static final PacketCodec<PacketByteBuf, RandomKeyPressC2SPacket> CODEC = PacketCodec.of(
        (value, buf) -> {}, // 写入
        buf -> new RandomKeyPressC2SPacket() // 读取
    );
    
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new ConcurrentHashMap<>();
    
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * 注册服务端接收器
     */
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            // 在服务端主线程执行
            context.server().execute(() -> {
                handleRandomKeyPress(player);
            });
        });
    }
    
    /**
     * 处理电击中毒效果触发
     */
    private static void handleRandomKeyPress(ServerPlayerEntity player) {
        // 检查效果是否启用
        if (!ChaosMod.config.randomKeyPressEnabled) {
            return;
        }
        
        // 排除创造/旁观模式
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        long currentTick = player.getServer().getTicks();
        long triggerCooldownTicks = ChaosMod.config.randomKeyPressIntervalSeconds * 20L;
        Long lastTrigger = LAST_TRIGGER_TICK.get(player.getUuid());
        if (lastTrigger != null && currentTick >= lastTrigger
                && currentTick - lastTrigger < triggerCooldownTicks) {
            return;
        }
        LAST_TRIGGER_TICK.put(player.getUuid(), currentTick);
        
        // 扣除血量 1.0F = 0.5♥
        player.damage(player.getDamageSources().generic(), 1.0F);
        
        // 给予中毒2效果10秒
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.POISON, 
            200, // 10秒 = 200 ticks
            1    // 中毒2 (等级从0开始，所以1=中毒2)
        ));
        
        // 使用多语言系统发送带颜色的聊天栏消息
        String message = LanguageManager.getMessage("electric_poison_damage");
        player.sendMessage(Text.literal(message).formatted(net.minecraft.util.Formatting.DARK_PURPLE, net.minecraft.util.Formatting.BOLD), false);
        
    }
    
    /**
     * 客户端发送数据包到服务端
     */
    public static void sendToServer() {
        // 这个方法将在客户端代码中调用
    }

    public static void cleanupPlayer(UUID playerId) {
        LAST_TRIGGER_TICK.remove(playerId);
    }
}
