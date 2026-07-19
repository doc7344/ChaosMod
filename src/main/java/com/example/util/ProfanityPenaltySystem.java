package com.example.util;

import com.example.ChaosMod;
import com.example.config.LanguageManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 第56项：祸从口出（服务端权威）。
 * 客户端通过 Shriek 前置把语音识别出的文字发到服务端，
 * 这里对照脏话词库结算扣血：每命中1个词扣1颗心，单句封顶5颗心。
 */
public final class ProfanityPenaltySystem {
    /** 每命中一个脏话扣 1 颗心 */
    private static final float DAMAGE_PER_HIT = 2.0F;
    /** 单句封顶 5 颗心 */
    private static final float MAX_DAMAGE_PER_SENTENCE = 10.0F;
    /** 词库文件名，放在 config 目录下，服主可以自己加词删词 */
    private static final String WORD_FILE_NAME = "chaosmod_profanity_words.txt";

    /** 已加载的词库，按长度降序排好，保证长词优先匹配（"操你妈"优先于"你妈"） */
    private static volatile List<String> words = List.of();
    private static volatile boolean loaded = false;

    private ProfanityPenaltySystem() {}

    /**
     * 内置默认词库。首次启动时写入 config 文件，之后以文件内容为准。
     * 只放常见中文脏话及语音识别常见的同音变体；服主可随时编辑文件调整。
     */
    private static final List<String> DEFAULT_WORDS = List.of(
        // 问候家人系列
        "操你妈", "草你妈", "艹你妈", "曹尼玛", "操尼玛", "草泥马", "日你妈", "干你妈",
        "你妈死了", "你马死了", "滚你妈", "去你妈的", "你妈的", "他妈的", "她妈的", "特么的",
        "妈了个逼", "马勒戈壁", "妈勒个逼",
        // 单口语气系列
        "卧槽", "我操", "我草", "我艹", "握草", "沃草", "卧艹", "我靠", "握操",
        "妈的", "妈蛋", "尼玛", "泥马", "麻痹", "妈逼", "马币",
        // 骂人系列
        "傻逼", "煞笔", "傻比", "沙比", "啥比", "傻叉", "傻吊", "傻屌",
        "二逼", "狗逼", "装逼", "牛逼", "撒比",
        "贱人", "贱货", "婊子", "狗娘养的", "王八蛋", "混蛋", "混账",
        "狗东西", "狗玩意", "废物", "垃圾玩意", "屌毛", "鸡巴", "几把", "吉巴",
        // 音译系列
        "法克", "谢特", "达米", "biao子"
    );

    /** 服务器启动时调用：加载词库；文件不存在则先写入默认词库 */
    public static synchronized void loadWordList() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(WORD_FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                List<String> lines = new ArrayList<>();
                lines.add("# ChaosMod 祸从口出脏话词库");
                lines.add("# 一行一个词，以#开头的行是注释，空行忽略");
                lines.add("# 修改后重启服务器生效");
                lines.addAll(DEFAULT_WORDS);
                Files.write(file, lines, StandardCharsets.UTF_8);
            }
            Set<String> parsed = new LinkedHashSet<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String word = normalize(line);
                if (word.isEmpty() || line.trim().startsWith("#")) continue;
                parsed.add(word);
            }
            List<String> sorted = new ArrayList<>(parsed);
            // 长词在前，避免"操你妈"被"你妈"抢先拆成两次命中
            sorted.sort(Comparator.comparingInt(String::length).reversed());
            words = List.copyOf(sorted);
            loaded = true;
            System.out.println("[ChaosMod] 祸从口出词库已加载，共 " + words.size() + " 个词");
        } catch (IOException e) {
            System.out.println("[ChaosMod] 祸从口出词库加载失败: " + e.getMessage());
            // 文件读不了就退回内置词库，保证功能可用
            List<String> fallback = new ArrayList<>();
            for (String word : DEFAULT_WORDS) fallback.add(normalize(word));
            fallback.sort(Comparator.comparingInt(String::length).reversed());
            words = List.copyOf(fallback);
            loaded = true;
        }
    }

    /** 去掉所有空白并转小写。Vosk 中文识别结果是"你 好 世 界"这种带空格的形式，必须先归一化 */
    private static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 处理一句语音识别文本（必须在服务器主线程调用）。
     * 命中脏话则扣血并给 ActionBar 提示；没命中什么都不做。
     */
    public static void handleSpeech(ServerPlayerEntity player, String rawText) {
        if (!ChaosMod.config.profanityPenaltyEnabled) return;
        if (player == null || !player.isAlive()) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (player.isDisconnected() || player.isRemoved()) return;

        if (!loaded) loadWordList();

        String normalized = normalize(rawText);
        if (normalized.isEmpty()) return;

        int hits = countHits(normalized);
        if (hits <= 0) return;

        float damage = Math.min(MAX_DAMAGE_PER_SENTENCE, hits * DAMAGE_PER_HIT);
        // 直伤隔离：不进背锅人/随机转移/延迟伤害等链路，骂人的自己承担
        DamageRouting.applyDirectDamage(
            player, player.getServerWorld().getDamageSources().generic(), damage
        );
        player.sendMessage(
            Text.literal(String.format(
                LanguageManager.getMessage("profanity_penalty_result"), hits, damage / 2.0F
            )).formatted(Formatting.DARK_RED, Formatting.BOLD),
            true
        );
    }

    /**
     * 统计一句话里命中的脏话总数。同一个词重复出现重复计数；
     * 命中的片段用占位符抹掉，避免"操你妈"再被"你妈"重复命中。
     */
    private static int countHits(String normalized) {
        int hits = 0;
        StringBuilder text = new StringBuilder(normalized);
        for (String word : words) {
            int idx = 0;
            while ((idx = text.indexOf(word, idx)) != -1) {
                hits++;
                for (int i = idx; i < idx + word.length(); i++) {
                    text.setCharAt(i, '\u0000');
                }
                idx += word.length();
            }
        }
        return hits;
    }
}
