package com.example.compat;

import com.example.util.ProfanityPenaltySystem;
import com.pryzmm.api.ShriekApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Shriek 前置的服务端接入（可选依赖隔离类）。
 * 只有在 FabricLoader.isModLoaded("shriek") 为 true 时才允许加载本类，
 * 否则 JVM 找不到 com.pryzmm 的类会直接报错。
 */
public final class ShriekServerIntegration {

    private ShriekServerIntegration() {}

    /** 注册"玩家说话"服务端监听：拿到识别文本后交给祸从口出系统结算 */
    public static void register() {
        ShriekApi.registerServerPlayerSpeechListener(event -> {
            ServerPlayerEntity player = event.getPlayer();
            String text = event.getText();
            if (player == null || text == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;
            // 扣血必须在服务器主线程执行；execute 在主线程时会立即运行
            server.execute(() -> ProfanityPenaltySystem.handleSpeech(player, text));
        });
        System.out.println("[ChaosMod] 已接入 Shriek 语音识别（服务端）");
    }
}
