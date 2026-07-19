package com.example.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Config defaults: ALL switches OFF by default. */
public class ChaosModConfig {
    public static final List<String> CONFIG_KEYS = List.of(
        "allHostileEnabled", "mobIgniteEnabled", "mobSlownessEnabled",
        "mobBlindnessEnabled", "mobThornsEnabled", "foodPoisonEnabled",
        "enderDragonBucketEnabled", "enderDragonKillEnabled", "playerDamageShareEnabled",
        "sharedHealthEnabled", "sharedDamageSplitEnabled", "randomDamageEnabled",
        "shieldNerfEnabled", "lowHealthNoHealEnabled", "waterToLavaEnabled",
        "endKeepOverrideEnabled", "reverseDamageEnabled", "sunburnEnabled",
        "healReverseEnabled", "fallTrapEnabled", "acrophobiaEnabled",
        "blockRevengeEnabled", "containerCurseEnabled", "inventoryCurseEnabled",
        "craftingTrapEnabled", "playerHealOnAttackEnabled", "positionSwapEnabled",
        "craftingBombEnabled", "waterDamageEnabled", "randomDamageAmountEnabled",
        "delayedDamageEnabled", "keyDisableEnabled", "randomEffectsEnabled",
        "damageScapegoatEnabled", "painSpreadEnabled", "panicMagnetEnabled",
        "pickupDrainEnabled", "vertigoScapegoatEnabled", "windowViolentShakeEnabled",
        "desktopPrankInvasionEnabled", "randomKeyPressEnabled", "touchHellEnabled",
        "movementTaxEnabled", "controlSeizurePlusEnabled", "jumpTaxEnabled",
        "forcedTetherEnabled", "hpAveragingEnabled", "multiplayerRouletteEnabled",
        "timedPositionSwapEnabled", "forcedSprintEnabled", "periodicNegativeEffectEnabled",
        "weaponSlipEnabled", "magmaBetrayalEnabled", "timeReboundEnabled", "burdenCollapseEnabled",
        "profanityPenaltyEnabled"
    );

    public static final List<String> INTERVAL_KEYS = List.of(
        "damageScapegoatIntervalSeconds", "vertigoScapegoatIntervalSeconds",
        "randomKeyPressIntervalSeconds", "forcedTetherIntervalSeconds",
        "hpAveragingIntervalSeconds", "multiplayerRouletteIntervalSeconds",
        "timedPositionSwapIntervalSeconds", "forcedSprintIntervalSeconds",
        "periodicNegativeEffectIntervalSeconds", "magmaBetrayalIntervalSeconds",
        "timeReboundIntervalSeconds", "burdenCollapseIntervalSeconds"
    );

    public static final List<String> PERCENTAGE_KEYS = List.of(
        "weaponSlipChancePercent"
    );

    public static final int MIN_INTERVAL_SECONDS = 5;
    public static final int MAX_INTERVAL_SECONDS = 600;
    public static final int MIN_PERCENTAGE = 1;
    public static final int MAX_PERCENTAGE = 100;

    // === Feature switches (all default false) ===
    public boolean allHostileEnabled = false;
    public boolean mobIgniteEnabled = false;
    public boolean mobSlownessEnabled = false;
    public boolean mobBlindnessEnabled = false;
    public boolean mobThornsEnabled = false;
    public boolean foodPoisonEnabled = false;
    public boolean enderDragonBucketEnabled = false;
    public boolean enderDragonKillEnabled = false;
    public boolean playerDamageShareEnabled = false;   // 贴身平摊
    public boolean sharedHealthEnabled = false;        // 全服共享同一生命、饥饿、饱和度和消耗度
    public boolean sharedDamageSplitEnabled = false;   // 全服平摊
    public boolean randomDamageEnabled = false;        // 随机转移
    public boolean shieldNerfEnabled = false;
    public boolean lowHealthNoHealEnabled = false;
    public boolean waterToLavaEnabled = false;         // 玩家放水50%变岩浆
    public boolean endKeepOverrideEnabled = false;     // 末地强制掉落，其他维度保留物品
    public boolean reverseDamageEnabled = false;       // 反向伤害：不受伤扣血，受伤停止扣血
    public boolean sunburnEnabled = false;             // 阳光灼伤：晴天白天阳光下自燃
    public boolean healReverseEnabled = false;         // 治疗反转：回血时50%概率变扣血
    public boolean fallTrapEnabled = false;            // 跌落陷阱：平地跳跃落地20%概率扣0.5♥
    public boolean acrophobiaEnabled = false;          // 恐高症：Y>80越高伤害越大，最高2♥
    public boolean blockRevengeEnabled = false;        // 方块反噬：破坏方块10%概率被反伤
    public boolean containerCurseEnabled = false;      // 容器诅咒：开箱子/熔炉25%概率扣1♥
    public boolean inventoryCurseEnabled = false;      // 物品栏诅咒：切换物品槽12%概率扣0.5♥
    public boolean craftingTrapEnabled = false;        // 合成陷阱：合成物品10%概率扣1♥
    
    // === 新增的5个邪恶效果 ===
    public boolean playerHealOnAttackEnabled = false;   // 攻击玩家回血：攻击其他玩家时自己回复1♥血
    public boolean positionSwapEnabled = false;         // 位置互换：受伤时与随机队友交换位置  
    public boolean craftingBombEnabled = false;         // 合成炸弹：打开工作台超过5秒直接爆炸
    public boolean waterDamageEnabled = false;          // 水中溺死：触碰水时持续造成0.5♥伤害
    public boolean randomDamageAmountEnabled = false;   // 随机伤害值：任何伤害都变成0.5♥-10♥随机值
    
    // === v1.5.0 新增的混沌效果 ===
    public boolean delayedDamageEnabled = false;        // 延迟受伤：被打中不会马上掉血，系统随机拖延0-5秒
    public boolean keyDisableEnabled = false;           // 按键失灵：受伤累积10次随机禁用一个常用键，死亡恢复
    public boolean randomEffectsEnabled = false;        // 受伤随机增益：每次挨打随机关上或开一个状态效果
    public boolean damageScapegoatEnabled = false;      // 伤害背锅人：每隔5分钟选出背锅侠承受所有伤害
    public boolean painSpreadEnabled = false;           // 痛觉扩散：被打后5秒内"带电"，靠近会被雷劈
    
    // === v1.5.5 新增的混沌效果 (总计38种) ===
    public boolean panicMagnetEnabled = false;          // 惊惧磁铁：受伤后10秒磁化期，每2秒拽队友到身边并扣血
    public boolean pickupDrainEnabled = false;          // 贪婪吸血：拾取物品时立刻扣0.5♥血量
    public boolean vertigoScapegoatEnabled = false;     // 眩晕背锅侠：随机选择背锅侠承受他人受伤后果
    
    // === v1.6.0 第四面墙突破效果：40种效果！ ===
    public boolean windowViolentShakeEnabled = false;   // 窗口暴力抖动：死亡时窗口超级抖动+复活惩戒，打破虚实界限
    public boolean desktopPrankInvasionEnabled = false; // 桌面恶作剧入侵：生命值低时在桌面生成求救文件并记录IP地址+额外扣血
    
    // === v1.7.0 电击地狱级效果：45种终极邪恶效果！ ===
    public boolean randomKeyPressEnabled = false;       // 随机乱按键：随机按键+扣血+电击提醒
    public boolean touchHellEnabled = false;            // 触控地狱：右键点击方块50%概率传送到地表岩浆池
    public boolean movementTaxEnabled = false;          // 移动税：每移动10格扣0.5♥血
    public boolean controlSeizurePlusEnabled = false;   // 控制癫痫Plus：死亡时WASD随机互换60秒+每5秒扣0.5♥血
    public boolean jumpTaxEnabled = false;              // 跳跃税：每次跳跃必定扣0.5♥血

    // === v1.8.0 多人互坑及新增效果：共55种效果 ===
    public boolean forcedTetherEnabled = false;         // 强制捆绑：两人距离>15格持续扣血
    public boolean hpAveragingEnabled = false;          // 血量平均：随机两人血量强制平均
    public boolean multiplayerRouletteEnabled = false;  // 死亡轮盘（多人版）：替换单人版，随机抽奖惩罚
    public boolean timedPositionSwapEnabled = false;    // 定时位置互换：定时触发而非受伤触发
    public boolean forcedSprintEnabled = false;         // 强制奔跑：必须持续移动否则扣血
    public boolean periodicNegativeEffectEnabled = false; // 周期负面效果：每隔一段时间获得随机有害状态效果
    public boolean weaponSlipEnabled = false;             // 武器脱手：近战命中敌对生物时概率丢出主手武器
    public boolean magmaBetrayalEnabled = false;           // 地面背叛：定时把随机玩家脚下方块临时变为岩浆块
    public boolean timeReboundEnabled = false;             // 时间回弹：5秒后回到原位并固定受到2颗心伤害
    public boolean burdenCollapseEnabled = false;          // 负重崩塌：提示5秒后按背包占用槽位结算伤害

    // === v1.9.0 语音识别效果：共56种效果（需要客户端安装 Shriek 前置） ===
    public boolean profanityPenaltyEnabled = false;        // 祸从口出：麦克风说脏话扣血，越多扣越多

    // === 可同步的触发间隔（秒）；默认值保持原硬编码行为 ===
    public int damageScapegoatIntervalSeconds = 300;
    public int vertigoScapegoatIntervalSeconds = 300;
    public int randomKeyPressIntervalSeconds = 120;
    public int forcedTetherIntervalSeconds = 120;
    public int hpAveragingIntervalSeconds = 60;
    public int multiplayerRouletteIntervalSeconds = 90;
    public int timedPositionSwapIntervalSeconds = 60;
    public int forcedSprintIntervalSeconds = 90;
    public int periodicNegativeEffectIntervalSeconds = 60;
    public int magmaBetrayalIntervalSeconds = 90;
    public int timeReboundIntervalSeconds = 90;
    public int burdenCollapseIntervalSeconds = 90;
    public int weaponSlipChancePercent = 20;

    // v1.3.0: Language setting
    public String language = "zh_cn";                  // 默认中文，可选: "en_us", "zh_cn"

    // === Legacy-visible flags ===
    public boolean noHealActive = false;
    public long noHealEndTime = 0L;

    public void markDirty() { /* no-op */ }

    public static boolean isValidKey(String key) {
        return CONFIG_KEYS.contains(key);
    }

    public static boolean isValidIntervalKey(String key) {
        return INTERVAL_KEYS.contains(key);
    }

    public static int clampIntervalSeconds(int value) {
        return Math.max(MIN_INTERVAL_SECONDS, Math.min(MAX_INTERVAL_SECONDS, value));
    }

    public static boolean isValidPercentageKey(String key) {
        return PERCENTAGE_KEYS.contains(key);
    }

    public static int clampPercentage(int value) {
        return Math.max(MIN_PERCENTAGE, Math.min(MAX_PERCENTAGE, value));
    }

    public Map<String, Boolean> snapshot() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (String key : CONFIG_KEYS) {
            values.put(key, get(key));
        }
        return values;
    }

    public void applySnapshot(Map<String, Boolean> values) {
        for (String key : CONFIG_KEYS) {
            Boolean value = values.get(key);
            if (value != null) {
                set(key, value);
            }
        }
    }

    public Map<String, Integer> intervalSnapshot() {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String key : INTERVAL_KEYS) values.put(key, getIntervalSeconds(key));
        return values;
    }

    public static Map<String, Integer> defaultIntervalSnapshot() {
        return new ChaosModConfig().intervalSnapshot();
    }

    public void applyIntervalSnapshot(Map<String, Integer> values) {
        for (String key : INTERVAL_KEYS) {
            Integer value = values.get(key);
            if (value != null) setIntervalSeconds(key, value);
        }
    }

    public Map<String, Integer> percentageSnapshot() {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String key : PERCENTAGE_KEYS) values.put(key, getPercentage(key));
        return values;
    }

    public static Map<String, Integer> defaultPercentageSnapshot() {
        return new ChaosModConfig().percentageSnapshot();
    }

    public void applyPercentageSnapshot(Map<String, Integer> values) {
        for (String key : PERCENTAGE_KEYS) {
            Integer value = values.get(key);
            if (value != null) setPercentage(key, value);
        }
    }

    public int getPercentage(String key) {
        return switch (key) {
            case "weaponSlipChancePercent" -> weaponSlipChancePercent;
            default -> throw new IllegalArgumentException("Unknown percentage key: " + key);
        };
    }

    public void setPercentage(String key, int value) {
        int clamped = clampPercentage(value);
        switch (key) {
            case "weaponSlipChancePercent" -> weaponSlipChancePercent = clamped;
            default -> throw new IllegalArgumentException("Unknown percentage key: " + key);
        }
        markDirty();
    }

    public int getIntervalSeconds(String key) {
        return switch (key) {
            case "damageScapegoatIntervalSeconds" -> damageScapegoatIntervalSeconds;
            case "vertigoScapegoatIntervalSeconds" -> vertigoScapegoatIntervalSeconds;
            case "randomKeyPressIntervalSeconds" -> randomKeyPressIntervalSeconds;
            case "forcedTetherIntervalSeconds" -> forcedTetherIntervalSeconds;
            case "hpAveragingIntervalSeconds" -> hpAveragingIntervalSeconds;
            case "multiplayerRouletteIntervalSeconds" -> multiplayerRouletteIntervalSeconds;
            case "timedPositionSwapIntervalSeconds" -> timedPositionSwapIntervalSeconds;
            case "forcedSprintIntervalSeconds" -> forcedSprintIntervalSeconds;
            case "periodicNegativeEffectIntervalSeconds" -> periodicNegativeEffectIntervalSeconds;
            case "magmaBetrayalIntervalSeconds" -> magmaBetrayalIntervalSeconds;
            case "timeReboundIntervalSeconds" -> timeReboundIntervalSeconds;
            case "burdenCollapseIntervalSeconds" -> burdenCollapseIntervalSeconds;
            default -> throw new IllegalArgumentException("Unknown interval key: " + key);
        };
    }

    public void setIntervalSeconds(String key, int value) {
        int clamped = clampIntervalSeconds(value);
        switch (key) {
            case "damageScapegoatIntervalSeconds" -> damageScapegoatIntervalSeconds = clamped;
            case "vertigoScapegoatIntervalSeconds" -> vertigoScapegoatIntervalSeconds = clamped;
            case "randomKeyPressIntervalSeconds" -> randomKeyPressIntervalSeconds = clamped;
            case "forcedTetherIntervalSeconds" -> forcedTetherIntervalSeconds = clamped;
            case "hpAveragingIntervalSeconds" -> hpAveragingIntervalSeconds = clamped;
            case "multiplayerRouletteIntervalSeconds" -> multiplayerRouletteIntervalSeconds = clamped;
            case "timedPositionSwapIntervalSeconds" -> timedPositionSwapIntervalSeconds = clamped;
            case "forcedSprintIntervalSeconds" -> forcedSprintIntervalSeconds = clamped;
            case "periodicNegativeEffectIntervalSeconds" -> periodicNegativeEffectIntervalSeconds = clamped;
            case "magmaBetrayalIntervalSeconds" -> magmaBetrayalIntervalSeconds = clamped;
            case "timeReboundIntervalSeconds" -> timeReboundIntervalSeconds = clamped;
            case "burdenCollapseIntervalSeconds" -> burdenCollapseIntervalSeconds = clamped;
            default -> throw new IllegalArgumentException("Unknown interval key: " + key);
        }
        markDirty();
    }

    public boolean get(String key) {
        switch (key) {
            case "allHostileEnabled": return allHostileEnabled;
            case "mobIgniteEnabled": return mobIgniteEnabled;
            case "mobSlownessEnabled": return mobSlownessEnabled;
            case "mobBlindnessEnabled": return mobBlindnessEnabled;
            case "mobThornsEnabled": return mobThornsEnabled;
            case "foodPoisonEnabled": return foodPoisonEnabled;
            case "enderDragonBucketEnabled": return enderDragonBucketEnabled;
            case "enderDragonKillEnabled": return enderDragonKillEnabled;
            case "playerDamageShareEnabled": return playerDamageShareEnabled;
            case "sharedHealthEnabled": return sharedHealthEnabled;
            case "sharedDamageSplitEnabled": return sharedDamageSplitEnabled;
            case "randomDamageEnabled": return randomDamageEnabled;
            case "shieldNerfEnabled": return shieldNerfEnabled;
            case "lowHealthNoHealEnabled": return lowHealthNoHealEnabled;
            case "waterToLavaEnabled": return waterToLavaEnabled;
            case "endKeepOverrideEnabled": return endKeepOverrideEnabled;
            case "reverseDamageEnabled": return reverseDamageEnabled;
            case "sunburnEnabled": return sunburnEnabled;
            case "healReverseEnabled": return healReverseEnabled;
            case "fallTrapEnabled": return fallTrapEnabled;
            case "acrophobiaEnabled": return acrophobiaEnabled;
            case "blockRevengeEnabled": return blockRevengeEnabled;
            case "containerCurseEnabled": return containerCurseEnabled;
            case "inventoryCurseEnabled": return inventoryCurseEnabled;
            case "craftingTrapEnabled": return craftingTrapEnabled;
            case "playerHealOnAttackEnabled": return playerHealOnAttackEnabled;
            case "positionSwapEnabled": return positionSwapEnabled;
            case "craftingBombEnabled": return craftingBombEnabled;
            case "waterDamageEnabled": return waterDamageEnabled;
            case "randomDamageAmountEnabled": return randomDamageAmountEnabled;
            case "delayedDamageEnabled": return delayedDamageEnabled;
            case "keyDisableEnabled": return keyDisableEnabled;
            case "randomEffectsEnabled": return randomEffectsEnabled;
            case "damageScapegoatEnabled": return damageScapegoatEnabled;
            case "painSpreadEnabled": return painSpreadEnabled;
            case "panicMagnetEnabled": return panicMagnetEnabled;
            case "pickupDrainEnabled": return pickupDrainEnabled;
            case "vertigoScapegoatEnabled": return vertigoScapegoatEnabled;
            case "windowViolentShakeEnabled": return windowViolentShakeEnabled;
            case "desktopPrankInvasionEnabled": return desktopPrankInvasionEnabled;
            case "randomKeyPressEnabled": return randomKeyPressEnabled;
            case "touchHellEnabled": return touchHellEnabled;
            case "movementTaxEnabled": return movementTaxEnabled;
            case "controlSeizurePlusEnabled": return controlSeizurePlusEnabled;
            case "jumpTaxEnabled": return jumpTaxEnabled;
            case "forcedTetherEnabled": return forcedTetherEnabled;
            case "hpAveragingEnabled": return hpAveragingEnabled;
            case "multiplayerRouletteEnabled": return multiplayerRouletteEnabled;
            case "timedPositionSwapEnabled": return timedPositionSwapEnabled;
            case "forcedSprintEnabled": return forcedSprintEnabled;
            case "periodicNegativeEffectEnabled": return periodicNegativeEffectEnabled;
            case "weaponSlipEnabled": return weaponSlipEnabled;
            case "magmaBetrayalEnabled": return magmaBetrayalEnabled;
            case "timeReboundEnabled": return timeReboundEnabled;
            case "burdenCollapseEnabled": return burdenCollapseEnabled;
            case "profanityPenaltyEnabled": return profanityPenaltyEnabled;
            default: return false;
        }
    }
    public void set(String key, boolean value) {
        switch (key) {
            case "allHostileEnabled": allHostileEnabled = value; break;
            case "mobIgniteEnabled": mobIgniteEnabled = value; break;
            case "mobSlownessEnabled": mobSlownessEnabled = value; break;
            case "mobBlindnessEnabled": mobBlindnessEnabled = value; break;
            case "mobThornsEnabled": mobThornsEnabled = value; break;
            case "foodPoisonEnabled": foodPoisonEnabled = value; break;
            case "enderDragonBucketEnabled": enderDragonBucketEnabled = value; break;
            case "enderDragonKillEnabled": enderDragonKillEnabled = value; break;
            case "playerDamageShareEnabled": playerDamageShareEnabled = value; break;
            case "sharedHealthEnabled": sharedHealthEnabled = value; break;
            case "sharedDamageSplitEnabled": sharedDamageSplitEnabled = value; break;
            case "randomDamageEnabled": randomDamageEnabled = value; break;
            case "shieldNerfEnabled": shieldNerfEnabled = value; break;
            case "lowHealthNoHealEnabled": lowHealthNoHealEnabled = value; break;
            case "waterToLavaEnabled": waterToLavaEnabled = value; break;
            case "endKeepOverrideEnabled": endKeepOverrideEnabled = value; break;
            case "reverseDamageEnabled": reverseDamageEnabled = value; break;
            case "sunburnEnabled": sunburnEnabled = value; break;
            case "healReverseEnabled": healReverseEnabled = value; break;
            case "fallTrapEnabled": fallTrapEnabled = value; break;
            case "acrophobiaEnabled": acrophobiaEnabled = value; break;
            case "blockRevengeEnabled": blockRevengeEnabled = value; break;
            case "containerCurseEnabled": containerCurseEnabled = value; break;
            case "inventoryCurseEnabled": inventoryCurseEnabled = value; break;
            case "craftingTrapEnabled": craftingTrapEnabled = value; break;
            case "playerHealOnAttackEnabled": playerHealOnAttackEnabled = value; break;
            case "positionSwapEnabled": positionSwapEnabled = value; break;
            case "craftingBombEnabled": craftingBombEnabled = value; break;
            case "waterDamageEnabled": waterDamageEnabled = value; break;
            case "randomDamageAmountEnabled": randomDamageAmountEnabled = value; break;
            case "delayedDamageEnabled": delayedDamageEnabled = value; break;
            case "keyDisableEnabled": keyDisableEnabled = value; break;
            case "randomEffectsEnabled": randomEffectsEnabled = value; break;
            case "damageScapegoatEnabled": damageScapegoatEnabled = value; break;
            case "painSpreadEnabled": painSpreadEnabled = value; break;
            case "panicMagnetEnabled": panicMagnetEnabled = value; break;
            case "pickupDrainEnabled": pickupDrainEnabled = value; break;
            case "vertigoScapegoatEnabled": vertigoScapegoatEnabled = value; break;
            case "windowViolentShakeEnabled": windowViolentShakeEnabled = value; break;
            case "desktopPrankInvasionEnabled": desktopPrankInvasionEnabled = value; break;
            case "randomKeyPressEnabled": randomKeyPressEnabled = value; break;
            case "touchHellEnabled": touchHellEnabled = value; break;
            case "movementTaxEnabled": movementTaxEnabled = value; break;
            case "controlSeizurePlusEnabled": controlSeizurePlusEnabled = value; break;
            case "jumpTaxEnabled": jumpTaxEnabled = value; break;
            case "forcedTetherEnabled": forcedTetherEnabled = value; break;
            case "hpAveragingEnabled": hpAveragingEnabled = value; break;
            case "multiplayerRouletteEnabled": multiplayerRouletteEnabled = value; break;
            case "timedPositionSwapEnabled": timedPositionSwapEnabled = value; break;
            case "forcedSprintEnabled": forcedSprintEnabled = value; break;
            case "periodicNegativeEffectEnabled": periodicNegativeEffectEnabled = value; break;
            case "weaponSlipEnabled": weaponSlipEnabled = value; break;
            case "magmaBetrayalEnabled": magmaBetrayalEnabled = value; break;
            case "timeReboundEnabled": timeReboundEnabled = value; break;
            case "burdenCollapseEnabled": burdenCollapseEnabled = value; break;
            case "profanityPenaltyEnabled": profanityPenaltyEnabled = value; break;
            default: break;
        }
        markDirty();
    }
    
    // Language configuration methods
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
        markDirty();
    }
}
