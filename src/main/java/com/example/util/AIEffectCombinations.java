package com.example.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI随机效果组合 - 为玩家量身定做的20种邪恶套餐
 */
public class AIEffectCombinations {
    
    public static class EffectCombination {
        public final String name;
        public final String description;
        public final String[] effects;
        public final String chatMessage;
        
        public EffectCombination(String name, String description, String[] effects, String chatMessage) {
            this.name = name;
            this.description = description;
            this.effects = effects;
            this.chatMessage = chatMessage;
        }
    }
    
    // 20种AI预设组合
    public static final List<EffectCombination> ALL_COMBINATIONS = new ArrayList<>();
    
    static {
        // 用户指定的4种组合
        ALL_COMBINATIONS.add(new EffectCombination(
            "[火焰缓慢] 缓慢烧烤套餐",
            "慢慢烧死你，逃都逃不掉！",
            new String[]{"mobIgniteEnabled", "mobSlownessEnabled", "enderDragonBucketEnabled", "shieldNerfEnabled"},
            "AI为你推荐：缓慢烧烤套餐 - 让你体验慢火炖煮的绝望！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[生命共享] 同命同饿组合",
            "团队共享同一生命与饥饿状态！",
            new String[]{"sharedHealthEnabled", "mobIgniteEnabled", "shieldNerfEnabled", "mobSlownessEnabled"},
            "AI为你推荐：同命同饿组合 - 一个人挨饿，全队都挨饿！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[环境杀手] 环境杀手模式",
            "环境都是你的敌人！",
            new String[]{"waterToLavaEnabled", "randomDamageEnabled", "shieldNerfEnabled"},
            "AI为你推荐：环境杀手模式 - 连水都要背叛你！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[传送陷阱] 传送陷阱大师",
            "受伤就传送，传送到死亡陷阱！",
            new String[]{"positionSwapEnabled", "waterDamageEnabled", "randomDamageEnabled", "shieldNerfEnabled"},
            "AI为你推荐：传送陷阱大师 - 每次传送都是一场赌博！"
        ));
        
        // 额外的16种组合
        ALL_COMBINATIONS.add(new EffectCombination(
            "[新手] 新手入门套餐",
            "温和的邪恶入门体验",
            new String[]{"foodPoisonEnabled", "shieldNerfEnabled", "lowHealthNoHealEnabled"},
            "AI为你推荐：新手入门套餐 - 温柔地开始你的痛苦之旅！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[恐高] 恐高症专家",
            "高度越高越绝望！",
            new String[]{"acrophobiaEnabled", "reverseDamageEnabled", "fallTrapEnabled", "shieldNerfEnabled"},
            "AI为你推荐：恐高症专家 - 让天空成为你的噩梦！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[阳光] 阳光死神",
            "白天户外就是死亡！",
            new String[]{"sunburnEnabled", "mobIgniteEnabled", "healReverseEnabled", "reverseDamageEnabled"},
            "AI为你推荐：阳光死神 - 让每个晴天都成为末日！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[操作恐惧] 操作恐惧症",
            "基本操作都变成陷阱！",
            new String[]{"inventoryCurseEnabled", "craftingTrapEnabled", "containerCurseEnabled", "blockRevengeEnabled"},
            "AI为你推荐：操作恐惧症 - 让每个动作都充满恐惧！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[治疗背叛] 治疗背叛者",
            "回血变扣血的绝望！",
            new String[]{"healReverseEnabled", "lowHealthNoHealEnabled", "reverseDamageEnabled", "mobThornsEnabled"},
            "AI为你推荐：治疗背叛者 - 连治疗都是陷阱！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[末地龙王] 末地龙王复仇",
            "击杀龙王的代价！",
            new String[]{"enderDragonKillEnabled", "enderDragonBucketEnabled", "endKeepOverrideEnabled", "sharedHealthEnabled"},
            "AI为你推荐：末地龙王复仇 - 胜利即是死亡！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[全敌对] 全敌对模式",
            "世界都是你的敌人！",
            new String[]{"allHostileEnabled", "mobThornsEnabled", "mobBlindnessEnabled", "shieldNerfEnabled"},
            "AI为你推荐：全敌对模式 - 整个世界都想杀死你！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[磁力惊惧] 磁力惊惧套餐", 
            "受伤后成为队友噩梦！",
            new String[]{"panicMagnetEnabled", "painSpreadEnabled", "mobIgniteEnabled", "shieldNerfEnabled"},
            "AI为你推荐：磁力惊惧套餐 - 受伤后变成队友的死亡磁铁！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[随机命运] 完全随机命运",
            "一切都靠运气！",
            new String[]{"randomDamageAmountEnabled", "randomDamageEnabled", "fallTrapEnabled", "foodPoisonEnabled"},
            "AI为你推荐：完全随机命运 - 生死全凭天意！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[水系绝杀] 水系绝杀",
            "水变成最危险的东西！",
            new String[]{"waterDamageEnabled", "waterToLavaEnabled", "positionSwapEnabled", "healReverseEnabled"},
            "AI为你推荐：水系绝杀 - 从此再也不敢碰水！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[合成恐怖] 合成恐怖",
            "合成变成生死游戏！",
            new String[]{"craftingBombEnabled", "craftingTrapEnabled", "inventoryCurseEnabled", "containerCurseEnabled"},
            "AI为你推荐：合成恐怖 - 每次合成都是拆弹游戏！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[末地龙王] 末地龙王复仇",
            "击杀龙王的代价！",
            new String[]{"enderDragonKillEnabled", "enderDragonBucketEnabled", "endKeepOverrideEnabled", "sharedHealthEnabled"},
            "AI为你推荐：末地龙王复仇 - 胜利即是死亡！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[全敌对] 全敌对模式",
            "世界都是你的敌人！",
            new String[]{"allHostileEnabled", "mobThornsEnabled", "mobBlindnessEnabled", "shieldNerfEnabled"},
            "AI为你推荐：全敌对模式 - 整个世界都想杀死你！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[磁力惊惧] 磁力惊惧套餐", 
            "受伤后成为队友噩梦！",
            new String[]{"panicMagnetEnabled", "painSpreadEnabled", "mobIgniteEnabled", "shieldNerfEnabled"},
            "AI为你推荐：磁力惊惧套餐 - 受伤后变成队友的死亡磁铁！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[随机命运] 完全随机命运",
            "一切都靠运气！",
            new String[]{"randomDamageAmountEnabled", "randomDamageEnabled", "fallTrapEnabled", "foodPoisonEnabled"},
            "AI为你推荐：完全随机命运 - 生死全凭天意！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[水系绝杀] 水系绝杀",
            "水变成最危险的东西！",
            new String[]{"waterDamageEnabled", "waterToLavaEnabled", "positionSwapEnabled", "healReverseEnabled"},
            "AI为你推荐：水系绝杀 - 从此再也不敢碰水！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[合成恐怖] 合成恐怖",
            "合成变成生死游戏！",
            new String[]{"craftingBombEnabled", "craftingTrapEnabled", "inventoryCurseEnabled", "containerCurseEnabled"},
            "AI为你推荐：合成恐怖 - 每次合成都是拆弹游戏！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[传送混沌] 传送混沌",
            "位置永远不安全！",
            new String[]{"positionSwapEnabled", "acrophobiaEnabled", "waterDamageEnabled", "sunburnEnabled"},
            "AI为你推荐：传送混沌 - 每次传送都可能是死亡陷阱！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[贪婪诅咒] 贪婪诅咒",
            "拾取物品都要付出血的代价！",
            new String[]{"pickupDrainEnabled", "inventoryCurseEnabled", "containerCurseEnabled", "craftingTrapEnabled"},
            "AI为你推荐：贪婪诅咒 - 每次贪心都会失血！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[团队背叛] 团队背叛",
            "朋友都是潜在敌人！",
            new String[]{"playerDamageShareEnabled", "sharedDamageSplitEnabled", "playerHealOnAttackEnabled", "randomDamageEnabled"},
            "AI为你推荐：团队背叛 - 朋友比敌人更危险！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[终极混沌] 终极混沌",
            "所有类型的痛苦集合！",
            new String[]{"randomDamageAmountEnabled", "allHostileEnabled", "sharedHealthEnabled", "acrophobiaEnabled", "craftingBombEnabled"},
            "AI为你推荐：终极混沌 - 真正的地狱难度挑战！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[爆炸专家] 爆炸专家",
            "到处都是爆炸！",
            new String[]{"craftingBombEnabled", "mobThornsEnabled", "blockRevengeEnabled", "enderDragonKillEnabled"},
            "AI为你推荐：爆炸专家 - 让世界充满爆炸！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[眩晕背锅] 眩晕背锅侠",
            "神秘的背锅者承受他人痛苦！",
            new String[]{"vertigoScapegoatEnabled", "mobBlindnessEnabled", "painSpreadEnabled", "randomDamageEnabled"},
            "AI为你推荐：眩晕背锅侠 - 让无辜的人承受你的痛苦！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[混沌嘉年华] 混沌嘉年华",
            "疯狂的效果组合！",
            new String[]{"foodPoisonEnabled", "waterToLavaEnabled", "fallTrapEnabled", "inventoryCurseEnabled", "containerCurseEnabled"},
            "AI为你推荐：混沌嘉年华 - 疯狂的痛苦盛宴！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[电击专用] 电击专用版",
            "为电击设备优化！",
            new String[]{"acrophobiaEnabled", "reverseDamageEnabled", "healReverseEnabled", "randomDamageAmountEnabled", "sunburnEnabled"},
            "AI为你推荐：电击专用版 - 持续稳定的电击体验！"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[新时代混沌] 新时代混沌",
            "最新的痛苦科技！",
            new String[]{"panicMagnetEnabled", "pickupDrainEnabled", "vertigoScapegoatEnabled", "delayedDamageEnabled", "keyDisableEnabled"},
            "AI为你推荐：新时代混沌 - 体验最新的痛苦科技！"
        ));
        
        // === v1.6.0 第四面墙突破组合 ===
        ALL_COMBINATIONS.add(new EffectCombination(
            "[第四面墙粉碎] 第四面墙粉碎",
            "💀游戏突破虚拟界限！",
            new String[]{"windowViolentShakeEnabled", "desktopPrankInvasionEnabled", "randomDamageAmountEnabled", "acrophobiaEnabled"},
            "AI为你推荐：第四面墙粉碎 - 窗口抖动+桌面入侵，游戏真正突破屏幕！💀⚡"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[电击终极版] 电击终极版",
            "为电击设备量身定制！",
            new String[]{"windowViolentShakeEnabled", "randomDamageAmountEnabled", "acrophobiaEnabled", "waterDamageEnabled", "sunburnEnabled"},
            "AI为你推荐：电击终极版 - 窗口抖动+持续扣血，电击设备永不停歇！⚡💀"
        ));
        
        ALL_COMBINATIONS.add(new EffectCombination(
            "[现实入侵套餐] 现实入侵套餐", 
            "虚拟世界入侵现实！",
            new String[]{"desktopPrankInvasionEnabled", "windowViolentShakeEnabled", "keyDisableEnabled", "painSpreadEnabled"},
            "AI为你推荐：现实入侵套餐 - 桌面文件+窗口控制+按键失灵，全方位现实攻击！💀"
        ));
    }
}
