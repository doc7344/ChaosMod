package com.example.util;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Random Effect Combinations - English Version
 */
public class AIEffectCombinationsEN {
    
    // 20 AI Preset Combinations - English Version
    public static final List<AIEffectCombinations.EffectCombination> ALL_COMBINATIONS_EN = new ArrayList<>();
    
    static {
        // User specified 4 combinations
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Fire Slow] Slow BBQ Package",
            "Burn you slowly, can't escape!",
            new String[]{"mobIgniteEnabled", "mobSlownessEnabled", "enderDragonBucketEnabled", "shieldNerfEnabled"},
            "AI Recommends: Slow BBQ Package - Experience slow-cooked despair!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Life Share] Shared Vitality",
            "The team shares one health and hunger state!",
            new String[]{"sharedHealthEnabled", "mobIgniteEnabled", "shieldNerfEnabled", "mobSlownessEnabled"},
            "AI Recommends: Shared Vitality - One player starves, everyone starves!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Environment Killer] Environment Killer Mode",
            "Environment is your enemy!",
            new String[]{"waterToLavaEnabled", "randomDamageEnabled", "shieldNerfEnabled"},
            "AI Recommends: Environment Killer Mode - Even water betrays you!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Panic Magnet] Panic Magnet Terror",
            "Get hurt and become a teammate nightmare!",
            new String[]{"panicMagnetEnabled", "painSpreadEnabled", "mobIgniteEnabled", "shieldNerfEnabled"},
            "AI Recommends: Panic Magnet Terror - Turn into a death magnet for teammates!"
        ));
        
        // Additional 16 combinations
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Beginner] Beginner Entry Package",
            "Gentle evil entry experience",
            new String[]{"foodPoisonEnabled", "shieldNerfEnabled", "lowHealthNoHealEnabled"},
            "AI Recommends: Beginner Entry Package - Gently start your pain journey!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Acrophobia] Acrophobia Expert",
            "Higher means more despair!",
            new String[]{"acrophobiaEnabled", "reverseDamageEnabled", "fallTrapEnabled", "shieldNerfEnabled"},
            "AI Recommends: Acrophobia Expert - Make the sky your nightmare!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Sunlight] Sunlight Death God",
            "Outdoor daylight means death!",
            new String[]{"sunburnEnabled", "mobIgniteEnabled", "healReverseEnabled", "reverseDamageEnabled"},
            "AI Recommends: Sunlight Death God - Make every sunny day doomsday!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Operation Fear] Operation Phobia",
            "Basic operations become traps!",
            new String[]{"inventoryCurseEnabled", "craftingTrapEnabled", "containerCurseEnabled", "blockRevengeEnabled"},
            "AI Recommends: Operation Phobia - Make every action fearful!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Healing Betrayal] Healing Betrayer",
            "Healing becomes blood loss despair!",
            new String[]{"healReverseEnabled", "lowHealthNoHealEnabled", "reverseDamageEnabled", "mobThornsEnabled"},
            "AI Recommends: Healing Betrayer - Even healing is a trap!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Ender Dragon] Ender Dragon Revenge",
            "The price of killing the dragon!",
            new String[]{"enderDragonKillEnabled", "enderDragonBucketEnabled", "endKeepOverrideEnabled", "sharedHealthEnabled"},
            "AI Recommends: Ender Dragon Revenge - Victory means death!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[All Hostile] All Hostile Mode",
            "The world is your enemy!",
            new String[]{"allHostileEnabled", "mobThornsEnabled", "mobBlindnessEnabled", "shieldNerfEnabled"},
            "AI Recommends: All Hostile Mode - The entire world wants to kill you!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[PVP Encourage] PVP Encouragement Package",
            "Encourage players to attack each other!",
            new String[]{"playerHealOnAttackEnabled", "randomDamageEnabled", "mobThornsEnabled", "reverseDamageEnabled"},
            "AI Recommends: PVP Encouragement Package - Friends are meant to be attacked!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Random Fate] Complete Random Fate",
            "Everything depends on luck!",
            new String[]{"randomDamageAmountEnabled", "randomDamageEnabled", "fallTrapEnabled", "foodPoisonEnabled"},
            "AI Recommends: Complete Random Fate - Life depends entirely on luck!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Water Kill] Water System Kill",
            "Water becomes the most dangerous thing!",
            new String[]{"waterDamageEnabled", "waterToLavaEnabled", "positionSwapEnabled", "healReverseEnabled"},
            "AI Recommends: Water System Kill - Never dare touch water again!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Crafting Terror] Crafting Terror",
            "Crafting becomes life-and-death game!",
            new String[]{"craftingBombEnabled", "craftingTrapEnabled", "inventoryCurseEnabled", "containerCurseEnabled"},
            "AI Recommends: Crafting Terror - Every craft is a bomb defusal game!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Teleport Chaos] Teleport Chaos",
            "Position never safe!",
            new String[]{"positionSwapEnabled", "acrophobiaEnabled", "waterDamageEnabled", "sunburnEnabled"},
            "AI Recommends: Teleport Chaos - Every teleport might be a death trap!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Greed Curse] Greed Curse",
            "Pick up items at the cost of blood!",
            new String[]{"pickupDrainEnabled", "inventoryCurseEnabled", "containerCurseEnabled", "craftingTrapEnabled"},
            "AI Recommends: Greed Curse - Every act of greed costs blood!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Team Betrayal] Team Betrayal",
            "Friends are potential enemies!",
            new String[]{"playerDamageShareEnabled", "sharedDamageSplitEnabled", "playerHealOnAttackEnabled", "randomDamageEnabled"},
            "AI Recommends: Team Betrayal - Friends are more dangerous than enemies!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Ultimate Chaos] Ultimate Chaos",
            "Collection of all pain types!",
            new String[]{"randomDamageAmountEnabled", "allHostileEnabled", "sharedHealthEnabled", "acrophobiaEnabled", "craftingBombEnabled"},
            "AI Recommends: Ultimate Chaos - True hell difficulty challenge!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Explosion Expert] Explosion Expert",
            "Explosions everywhere!",
            new String[]{"craftingBombEnabled", "mobThornsEnabled", "blockRevengeEnabled", "enderDragonKillEnabled"},
            "AI Recommends: Explosion Expert - Fill the world with explosions!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Vertigo Scapegoat] Vertigo Scapegoat",
            "Mysterious scapegoat bears others' pain!",
            new String[]{"vertigoScapegoatEnabled", "mobBlindnessEnabled", "painSpreadEnabled", "randomDamageEnabled"},
            "AI Recommends: Vertigo Scapegoat - Let innocent bear your pain!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Chaos Carnival] Chaos Carnival",
            "Crazy effect combinations!",
            new String[]{"foodPoisonEnabled", "waterToLavaEnabled", "fallTrapEnabled", "inventoryCurseEnabled", "containerCurseEnabled"},
            "AI Recommends: Chaos Carnival - Crazy pain feast!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Shock Optimized] Shock Optimized Version",
            "Optimized for shock devices!",
            new String[]{"acrophobiaEnabled", "reverseDamageEnabled", "healReverseEnabled", "randomDamageAmountEnabled", "sunburnEnabled"},
            "AI Recommends: Shock Optimized Version - Continuous stable shock experience!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[New Age Chaos] New Age Chaos",
            "Latest pain technology!",
            new String[]{"panicMagnetEnabled", "pickupDrainEnabled", "vertigoScapegoatEnabled", "delayedDamageEnabled", "keyDisableEnabled"},
            "AI Recommends: New Age Chaos - Experience latest pain technology!"
        ));
        
        // === v1.6.0 Fourth Wall Breaking Combinations ===
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Fourth Wall Shatter] Fourth Wall Shatter",
            "Game breaks through virtual boundaries!",
            new String[]{"windowViolentShakeEnabled", "desktopPrankInvasionEnabled", "randomDamageAmountEnabled", "acrophobiaEnabled"},
            "AI Recommends: Fourth Wall Shatter - Window shake + desktop invasion, game truly breaks through screen!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Shock Ultimate] Shock Ultimate Version",
            "Tailored for shock devices!",
            new String[]{"windowViolentShakeEnabled", "randomDamageAmountEnabled", "acrophobiaEnabled", "waterDamageEnabled", "sunburnEnabled"},
            "AI Recommends: Shock Ultimate Version - Window shake + continuous damage, shock device never stops!"
        ));
        
        ALL_COMBINATIONS_EN.add(new AIEffectCombinations.EffectCombination(
            "[Reality Invasion] Reality Invasion Package", 
            "Virtual world invades reality!",
            new String[]{"desktopPrankInvasionEnabled", "windowViolentShakeEnabled", "keyDisableEnabled", "painSpreadEnabled"},
            "AI Recommends: Reality Invasion Package - Desktop files + window control + key malfunction, full reality attack!"
        ));
    }
}
