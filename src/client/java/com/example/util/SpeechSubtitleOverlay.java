package com.example.util;

import com.example.ChaosMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * 第56项：祸从口出的客户端字幕层。
 * 把自己麦克风识别出的话显示在屏幕左上角：
 * 每条5秒后消失，最多同时3行，超出时最旧的一行被顶掉，不会刷屏。
 */
public final class SpeechSubtitleOverlay {
    /** 单条字幕存活时间：5秒 */
    private static final long LIFETIME_MILLIS = 5000L;
    /** 最多同时显示的行数 */
    private static final int MAX_LINES = 3;
    /** 单行最大字符数，识别出超长句子时截断防止跑出屏幕 */
    private static final int MAX_CHARS = 40;

    private static final int MARGIN_X = 4;
    private static final int MARGIN_Y = 4;
    private static final int LINE_HEIGHT = 11;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /** 字幕队列，队头是最旧的一条。只在客户端主线程读写 */
    private static final Deque<Subtitle> SUBTITLES = new ArrayDeque<>();

    private SpeechSubtitleOverlay() {}

    /** 新增一条字幕（客户端主线程调用）。功能关闭时不收字幕 */
    public static void addSubtitle(String text) {
        if (!ChaosMod.config.profanityPenaltyEnabled) return;
        if (text == null) return;
        // Vosk 中文结果是"你 好 世 界"带空格的形式，去掉空格显示更自然
        String cleaned = text.replaceAll("\\s+", "");
        if (cleaned.isEmpty()) return;
        if (cleaned.length() > MAX_CHARS) {
            cleaned = cleaned.substring(0, MAX_CHARS) + "…";
        }
        SUBTITLES.addLast(new Subtitle(cleaned, System.currentTimeMillis() + LIFETIME_MILLIS));
        while (SUBTITLES.size() > MAX_LINES) {
            SUBTITLES.pollFirst();
        }
    }

    /** 每帧由 HudRenderCallback 调用，画在屏幕左上角 */
    public static void render(DrawContext context) {
        if (SUBTITLES.isEmpty()) return;
        // 功能被关闭时清掉残留字幕
        if (!ChaosMod.config.profanityPenaltyEnabled) {
            SUBTITLES.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Subtitle> iterator = SUBTITLES.iterator();
        while (iterator.hasNext()) {
            if (now >= iterator.next().expireAtMillis()) {
                iterator.remove();
            }
        }
        if (SUBTITLES.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int y = MARGIN_Y;
        for (Subtitle subtitle : SUBTITLES) {
            context.drawTextWithShadow(client.textRenderer, subtitle.text(), MARGIN_X, y, TEXT_COLOR);
            y += LINE_HEIGHT;
        }
    }

    /** 断线/换服时清空，避免把上一局的字幕带过去 */
    public static void clear() {
        SUBTITLES.clear();
    }

    private record Subtitle(String text, long expireAtMillis) {}
}
