package com.example.compat;

import com.example.util.SpeechSubtitleOverlay;
import com.pryzmm.api.ShriekApi;
import com.pryzmm.client.event.EventHandler;
import net.minecraft.client.MinecraftClient;

/**
 * Shriek 前置的客户端接入（可选依赖隔离类）。
 * 只有在 FabricLoader.isModLoaded("shriek") 为 true 时才允许加载本类。
 */
public final class ShriekClientIntegration {
    /** 中文识别模型，约42MB，首次启动由 Shriek 自动下载到 shriek/vosk 目录 */
    private static final String CHINESE_MODEL = "vosk-model-small-cn-0.22";

    private ShriekClientIntegration() {}

    public static void register() {
        // 自己说话 -> 左上角字幕。Shriek 在客户端主线程触发该事件，execute 只是保险
        ShriekApi.registerClientSpeechListener(event -> {
            String text = event.getText();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || text == null) return;
            client.execute(() -> SpeechSubtitleOverlay.addSubtitle(text));
        });

        // 模型下载是同步阻塞IO，放后台线程跑，不卡游戏启动画面
        Thread loader = new Thread(() -> {
            try {
                EventHandler.loadVoskModel(CHINESE_MODEL);
            } catch (Throwable t) {
                System.out.println("[ChaosMod] 中文语音模型加载失败: " + t.getMessage());
            }
        }, "ChaosMod-VoskModelLoader");
        loader.setDaemon(true);
        loader.start();
        System.out.println("[ChaosMod] 已接入 Shriek 语音识别（客户端），正在后台加载中文模型");
    }
}
