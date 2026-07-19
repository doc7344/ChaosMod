package com.example.gui;

import com.example.ChaosMod;
import com.example.config.LanguageManager;
import com.example.network.ConfigToggleC2SPacket;
import com.example.screen.ChaosModScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.example.util.AIEffectCombinations;
import com.example.util.AIEffectCombinationsEN;
// import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking; // Simplified
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ChaosModConfigScreen extends HandledScreen<ChaosModScreenHandler> {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24; // MC 标准间距
    
    private final List<ButtonWidget> configButtons = new ArrayList<>();
    private final List<IntervalSlider> intervalSliders = new ArrayList<>();
    private final List<PercentageSlider> percentageSliders = new ArrayList<>();
    private IntervalSlider draggedIntervalSlider;
    private PercentageSlider draggedPercentageSlider;
    private boolean hasPermission;
    private int currentPage = 0;
    private final int itemsPerPage = 6;
    private int totalPages;
    
    // AI随机效果组合管理
    private static final List<Integer> usedCombinations = new ArrayList<>();
    
    public ChaosModConfigScreen(ChaosModScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.empty()); // 传入空标题
        // 从 ScreenHandler 获取服务端同步的权限状态
        this.hasPermission = handler.hasPermission();
        this.backgroundWidth = 256; // MC 标准宽度
        this.backgroundHeight = 256; // MC 标准高度
    }
    
    public void applyServerSync(boolean hasPermission) {
        boolean permissionChanged = this.hasPermission != hasPermission;
        this.hasPermission = hasPermission;
        if (permissionChanged) {
            this.clearAndInit();
        } else {
            updateButtonTexts();
        }
    }

    @Override
    protected void init() {
        super.init();
        
        this.configButtons.clear();
        this.intervalSliders.clear();
        this.percentageSliders.clear();
        this.draggedIntervalSlider = null;
        this.draggedPercentageSlider = null;
        
        if (!hasPermission) {
            // 渲染"无管理员权限无法设置"的受限页面
            initRestrictedInterface();
        } else {
            // 渲染 MC 风格的主菜单布局
            initMainMenuStyle();
        }
    }
    
    private void initRestrictedInterface() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        
        // 权限不足提示 - MC 风格居中
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.permission_denied")).formatted(Formatting.RED),
            btn -> {}
        ).dimensions(centerX - 100, startY, 200, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.admin_required")).formatted(Formatting.YELLOW),
            btn -> {}
        ).dimensions(centerX - 100, startY + 30, 200, 20).build());
        
        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.back")),
            btn -> this.close()
        ).dimensions(centerX - 100, startY + 80, 200, 20).build());
    }
    
    private void initMainMenuStyle() {
        int centerX = this.width / 2;
        int startY = 50;
        
        // 语言切换按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.language") + ": " + LanguageManager.getCurrentLanguage().displayName),
            btn -> toggleLanguage()
        ).dimensions(centerX - 100, 20, 200, 20).build());
        
        // 获取所有配置项的键值对
        Map<String, String> currentLabels = getCurrentLabels();
        
        // 计算总页数
        totalPages = (currentLabels.size() + itemsPerPage - 1) / itemsPerPage;
        
        // 当前页的配置按钮；带间隔参数的项目在按钮下方显示滑块。
        List<Map.Entry<String, String>> entries = new ArrayList<>(currentLabels.entrySet());
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, entries.size());
        
        int yPos = startY;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, String> entry = entries.get(i);
            String key = entry.getKey();
            String label = entry.getValue();
            
            ButtonWidget button = ButtonWidget.builder(
                getButtonText(key, label),
                btn -> toggleConfig(key)
            )
            .dimensions(centerX - BUTTON_WIDTH / 2, yPos, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.of(Text.literal(getTooltipText(key)).formatted(Formatting.WHITE)))
            .build();
            
            this.addDrawableChild(button);
            this.configButtons.add(button);

            String intervalKey = getIntervalKey(key);
            if (intervalKey != null) {
                IntervalSlider slider = new IntervalSlider(
                    centerX - BUTTON_WIDTH / 2,
                    yPos + BUTTON_HEIGHT + 2,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    intervalKey
                );
                this.addDrawableChild(slider);
                this.intervalSliders.add(slider);
                yPos += 46;
            } else {
                String percentageKey = getPercentageKey(key);
                if (percentageKey != null) {
                    PercentageSlider slider = new PercentageSlider(
                        centerX - BUTTON_WIDTH / 2,
                        yPos + BUTTON_HEIGHT + 2,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        percentageKey
                    );
                    this.addDrawableChild(slider);
                    this.percentageSliders.add(slider);
                    yPos += 46;
                } else {
                    yPos += BUTTON_SPACING;
                }
            }
        }
        
        // 控制按钮 - MC 风格底部布局
        int bottomY = yPos + 8;
        
        // 分页按钮（如果需要的话）
        if (totalPages > 1) {
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(LanguageManager.getUI("gui.previous_page")),
                btn -> previousPage()
            ).dimensions(centerX - 150, bottomY, 80, 20).build());
            
            // 页码显示
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(String.format("%d/%d", currentPage + 1, totalPages)),
                btn -> {}
            ).dimensions(centerX - 30, bottomY, 60, 20).build());
            
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(LanguageManager.getUI("gui.next_page")),
                btn -> nextPage()
            ).dimensions(centerX + 70, bottomY, 80, 20).build());
            
            bottomY += 30;
        }
        
        // 批量操作按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.enable_all")),
            btn -> toggleAllConfigs(true)
        ).dimensions(centerX - 150, bottomY, 80, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.disable_all")),
            btn -> toggleAllConfigs(false)
        ).dimensions(centerX - 40, bottomY, 80, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("gui.close")),
            btn -> this.close()
        ).dimensions(centerX + 70, bottomY, 80, 20).build());
        
        // AI随机效果按钮
        bottomY += 30;
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(LanguageManager.getUI("ai.random.button")),
            btn -> selectRandomAIEffects()
        ).dimensions(centerX - 100, bottomY, 200, 20).build());
    }
    
    private Map<String, String> getCurrentLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        
        // 获取所有配置键
        String[] keys = {
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
            "damageScapegoatEnabled", "painSpreadEnabled",
            // v1.6.0 新增效果
            "panicMagnetEnabled", "pickupDrainEnabled", "vertigoScapegoatEnabled",
            // v1.6.0 第四面墙突破效果
            "windowViolentShakeEnabled", "desktopPrankInvasionEnabled",
            // v1.7.0 电击地狱级效果
            "randomKeyPressEnabled", "touchHellEnabled", "movementTaxEnabled",
            "controlSeizurePlusEnabled", "jumpTaxEnabled",
            // v1.8.0 多人互坑效果
            "forcedTetherEnabled", "hpAveragingEnabled", "multiplayerRouletteEnabled",
            "timedPositionSwapEnabled", "forcedSprintEnabled", "periodicNegativeEffectEnabled",
            // v1.8.0 第52-55项效果
            "weaponSlipEnabled", "magmaBetrayalEnabled",
            "timeReboundEnabled", "burdenCollapseEnabled",
            // v1.9.0 语音识别效果
            "profanityPenaltyEnabled"
        };
        
        for (String key : keys) {
            labels.put(key, LanguageManager.getLabel(key));
        }
        
        return labels;
    }
    
    private void toggleLanguage() {
        LanguageManager.Language current = LanguageManager.getCurrentLanguage();
        LanguageManager.Language newLang = current == LanguageManager.Language.ENGLISH ? 
            LanguageManager.Language.CHINESE : LanguageManager.Language.ENGLISH;
        
        LanguageManager.setLanguage(newLang);
        
        // 重新初始化界面以更新所有文本
        this.clearAndInit();
    }
    
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            this.clearAndInit();
        }
    }
    
    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            this.clearAndInit();
        }
    }
    
    private Text getButtonText(String key, String label) {
        boolean enabled = ChaosMod.config.get(key);
        String state = enabled ? LanguageManager.getUI("gui.enabled") : LanguageManager.getUI("gui.disabled");
        Formatting color = enabled ? Formatting.GREEN : Formatting.WHITE;
        
        return Text.literal(label + " [" + state + "]").formatted(color);
    }

    private String getIntervalKey(String configKey) {
        return switch (configKey) {
            case "damageScapegoatEnabled" -> "damageScapegoatIntervalSeconds";
            case "vertigoScapegoatEnabled" -> "vertigoScapegoatIntervalSeconds";
            case "randomKeyPressEnabled" -> "randomKeyPressIntervalSeconds";
            case "forcedTetherEnabled" -> "forcedTetherIntervalSeconds";
            case "hpAveragingEnabled" -> "hpAveragingIntervalSeconds";
            case "multiplayerRouletteEnabled" -> "multiplayerRouletteIntervalSeconds";
            case "timedPositionSwapEnabled" -> "timedPositionSwapIntervalSeconds";
            case "forcedSprintEnabled" -> "forcedSprintIntervalSeconds";
            case "periodicNegativeEffectEnabled" -> "periodicNegativeEffectIntervalSeconds";
            case "magmaBetrayalEnabled" -> "magmaBetrayalIntervalSeconds";
            case "timeReboundEnabled" -> "timeReboundIntervalSeconds";
            case "burdenCollapseEnabled" -> "burdenCollapseIntervalSeconds";
            default -> null;
        };
    }

    private String getPercentageKey(String configKey) {
        return switch (configKey) {
            case "weaponSlipEnabled" -> "weaponSlipChancePercent";
            default -> null;
        };
    }
    
    private void toggleConfig(String key) {
        boolean current = ChaosMod.config.get(key);
        boolean newValue = !current;
        
        // 检查互斥逻辑
        if (newValue && !checkMutualExclusion(key)) {
            return; // 如果互斥检查失败，不允许开启
        }
        
        ClientPlayNetworking.send(new ConfigToggleC2SPacket(key, newValue));
        
        // 更新按钮文本
        updateButtonTexts();
    }
    
    
    /**
     * 检查互斥逻辑：贴身平摊、共享生命、全服平摊、随机转移不能同时开启
     */
    private boolean checkMutualExclusion(String key) {
        // 定义互斥的效果组
        String[] mutexGroup = {
            "playerDamageShareEnabled",  // 贴身平摊伤害
            "sharedHealthEnabled",       // 全服共享生命与饥饿
            "sharedDamageSplitEnabled",  // 全服平摊伤害
            "randomDamageEnabled"        // 随机转移伤害
        };
        
        // 如果当前要开启的key不在互斥组中，允许开启
        boolean isInMutexGroup = false;
        for (String mutexKey : mutexGroup) {
            if (mutexKey.equals(key)) {
                isInMutexGroup = true;
                break;
            }
        }
        
        if (!isInMutexGroup) {
            return true; // 不在互斥组中，允许开启
        }
        
        // 检查是否有其他互斥效果已经开启
        for (String mutexKey : mutexGroup) {
            if (!mutexKey.equals(key) && ChaosMod.config.get(mutexKey)) {
                // 找到已开启的效果，显示提示信息
                String currentEffectName = getCurrentLabels().get(mutexKey);
                String targetEffectName = getCurrentLabels().get(key);
                
                if (this.client != null && this.client.player != null) {
                    // 多语言效果冲突警告
                    String language = com.example.config.LanguageManager.getCurrentLanguage().code;
                    String warningMsg;
                    if ("en_us".equals(language)) {
                        warningMsg = String.format("You have enabled '%s' and cannot enable '%s'", currentEffectName, targetEffectName);
                    } else {
                        warningMsg = String.format("当前你开启了「%s」无法开启「%s」", currentEffectName, targetEffectName);
                    }
                    
                    this.client.player.sendMessage(
                        Text.literal("[" + ("en_us".equals(language) ? "Warning" : "警告") + "] " + warningMsg)
                            .formatted(Formatting.RED, Formatting.BOLD),
                        false
                    );
                }
                
                return false; // 不允许开启
            }
        }
        
        return true; // 允许开启
    }
    
    private void toggleAllConfigs(boolean enable) {
        Map<String, Boolean> changes = new LinkedHashMap<>();
        boolean mutexSelected = false;
        for (String key : getCurrentLabels().keySet()) {
            boolean value = enable;
            if (enable && isMutualExclusionKey(key)) {
                value = !mutexSelected;
                mutexSelected = true;
            }
            changes.put(key, value);
        }
        ClientPlayNetworking.send(new ConfigToggleC2SPacket(changes));
        
        // 更新按钮文本
        updateButtonTexts();
    }
    
    private boolean isMutualExclusionKey(String key) {
        return "playerDamageShareEnabled".equals(key)
            || "sharedHealthEnabled".equals(key)
            || "sharedDamageSplitEnabled".equals(key)
            || "randomDamageEnabled".equals(key);
    }

    private void updateButtonTexts() {
        Map<String, String> currentLabels = getCurrentLabels();
        List<Map.Entry<String, String>> entries = new ArrayList<>(currentLabels.entrySet());
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, entries.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int buttonIndex = i - startIndex;
            if (buttonIndex < this.configButtons.size()) {
                Map.Entry<String, String> entry = entries.get(i);
                ButtonWidget button = this.configButtons.get(buttonIndex);
                button.setMessage(getButtonText(entry.getKey(), entry.getValue()));
            }
        }
        this.intervalSliders.forEach(IntervalSlider::syncFromConfig);
        this.percentageSliders.forEach(PercentageSlider::syncFromConfig);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (IntervalSlider slider : intervalSliders) {
                if (slider.active && slider.isMouseOver(mouseX, mouseY)) {
                    draggedIntervalSlider = slider;
                    draggedPercentageSlider = null;
                    slider.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
            for (PercentageSlider slider : percentageSliders) {
                if (slider.active && slider.isMouseOver(mouseX, mouseY)) {
                    draggedPercentageSlider = slider;
                    draggedIntervalSlider = null;
                    slider.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggedIntervalSlider != null) {
            draggedIntervalSlider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            return true;
        }
        if (button == 0 && draggedPercentageSlider != null) {
            draggedPercentageSlider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggedIntervalSlider != null) {
            IntervalSlider slider = draggedIntervalSlider;
            draggedIntervalSlider = null;
            slider.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        if (button == 0 && draggedPercentageSlider != null) {
            PercentageSlider slider = draggedPercentageSlider;
            draggedPercentageSlider = null;
            slider.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            int delta = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            for (IntervalSlider slider : intervalSliders) {
                if (slider.active && slider.isHovered()) {
                    return slider.adjustBySeconds(delta);
                }
            }
            for (PercentageSlider slider : percentageSliders) {
                if (slider.active && slider.isHovered()) {
                    return slider.adjustByPercentage(delta);
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private final class IntervalSlider extends SliderWidget {
        private final String intervalKey;
        private boolean syncing;
        private boolean mouseAdjusting;
        private int lastSubmittedSeconds;

        private IntervalSlider(int x, int y, int width, int height, String intervalKey) {
            super(x, y, width, height, Text.empty(), toSliderValue(ChaosMod.config.getIntervalSeconds(intervalKey)));
            this.intervalKey = intervalKey;
            this.lastSubmittedSeconds = ChaosMod.config.getIntervalSeconds(intervalKey);
            this.active = hasPermission;
            updateMessage();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            mouseAdjusting = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
            mouseAdjusting = true;
            super.onDrag(mouseX, mouseY, deltaX, deltaY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            if (mouseAdjusting) {
                mouseAdjusting = false;
                submitCurrentValue();
            }
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(LanguageManager.getFormattedUI("gui.interval", currentSeconds())));
        }

        @Override
        protected void applyValue() {
            updateMessage();
            // 鼠标拖动过程中只更新本地显示，避免每移动一个像素就触发服务端
            // 全量同步并把滑块拉回。鼠标松开时再统一提交一次。
            if (!syncing && !mouseAdjusting) {
                submitCurrentValue();
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                return adjustBySeconds(-1);
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                return adjustBySeconds(1);
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private boolean adjustBySeconds(int delta) {
            int current = currentSeconds();
            int next = com.example.config.ChaosModConfig.clampIntervalSeconds(current + delta);
            if (next == current) {
                return true;
            }

            syncing = true;
            value = toSliderValue(next);
            updateMessage();
            syncing = false;
            submitCurrentValue();
            return true;
        }

        private void submitCurrentValue() {
            int seconds = currentSeconds();
            if (!hasPermission || seconds == lastSubmittedSeconds
                    || !ClientPlayNetworking.canSend(ConfigToggleC2SPacket.ID)) {
                return;
            }
            lastSubmittedSeconds = seconds;
            ClientPlayNetworking.send(ConfigToggleC2SPacket.interval(intervalKey, seconds));
        }

        private int currentSeconds() {
            int range = com.example.config.ChaosModConfig.MAX_INTERVAL_SECONDS
                - com.example.config.ChaosModConfig.MIN_INTERVAL_SECONDS;
            return com.example.config.ChaosModConfig.clampIntervalSeconds(
                com.example.config.ChaosModConfig.MIN_INTERVAL_SECONDS + (int)Math.round(value * range)
            );
        }

        private void syncFromConfig() {
            // 其他配置同步不能打断当前的长按拖动；松手提交后再采用服务端回包。
            if (mouseAdjusting) {
                return;
            }
            int seconds = ChaosMod.config.getIntervalSeconds(intervalKey);
            syncing = true;
            value = toSliderValue(seconds);
            lastSubmittedSeconds = seconds;
            updateMessage();
            syncing = false;
        }

        private static double toSliderValue(int seconds) {
            int min = com.example.config.ChaosModConfig.MIN_INTERVAL_SECONDS;
            int max = com.example.config.ChaosModConfig.MAX_INTERVAL_SECONDS;
            return (com.example.config.ChaosModConfig.clampIntervalSeconds(seconds) - min) / (double)(max - min);
        }
    }

    private final class PercentageSlider extends SliderWidget {
        private final String percentageKey;
        private boolean syncing;
        private boolean mouseAdjusting;
        private int lastSubmittedPercentage;

        private PercentageSlider(int x, int y, int width, int height, String percentageKey) {
            super(x, y, width, height, Text.empty(), toSliderValue(ChaosMod.config.getPercentage(percentageKey)));
            this.percentageKey = percentageKey;
            this.lastSubmittedPercentage = ChaosMod.config.getPercentage(percentageKey);
            this.active = hasPermission;
            updateMessage();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            mouseAdjusting = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
            mouseAdjusting = true;
            super.onDrag(mouseX, mouseY, deltaX, deltaY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            if (mouseAdjusting) {
                mouseAdjusting = false;
                submitCurrentValue();
            }
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(LanguageManager.getFormattedUI("gui.probability", currentPercentage())));
        }

        @Override
        protected void applyValue() {
            updateMessage();
            if (!syncing && !mouseAdjusting) {
                submitCurrentValue();
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                return adjustByPercentage(-1);
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                return adjustByPercentage(1);
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private boolean adjustByPercentage(int delta) {
            int current = currentPercentage();
            int next = com.example.config.ChaosModConfig.clampPercentage(current + delta);
            if (next == current) {
                return true;
            }

            syncing = true;
            value = toSliderValue(next);
            updateMessage();
            syncing = false;
            submitCurrentValue();
            return true;
        }

        private void submitCurrentValue() {
            int percentage = currentPercentage();
            if (!hasPermission || percentage == lastSubmittedPercentage
                    || !ClientPlayNetworking.canSend(ConfigToggleC2SPacket.ID)) {
                return;
            }
            lastSubmittedPercentage = percentage;
            ClientPlayNetworking.send(ConfigToggleC2SPacket.percentage(percentageKey, percentage));
        }

        private int currentPercentage() {
            int range = com.example.config.ChaosModConfig.MAX_PERCENTAGE
                - com.example.config.ChaosModConfig.MIN_PERCENTAGE;
            return com.example.config.ChaosModConfig.clampPercentage(
                com.example.config.ChaosModConfig.MIN_PERCENTAGE + (int)Math.round(value * range)
            );
        }

        private void syncFromConfig() {
            if (mouseAdjusting) {
                return;
            }
            int percentage = ChaosMod.config.getPercentage(percentageKey);
            syncing = true;
            value = toSliderValue(percentage);
            lastSubmittedPercentage = percentage;
            updateMessage();
            syncing = false;
        }

        private static double toSliderValue(int percentage) {
            int min = com.example.config.ChaosModConfig.MIN_PERCENTAGE;
            int max = com.example.config.ChaosModConfig.MAX_PERCENTAGE;
            return (com.example.config.ChaosModConfig.clampPercentage(percentage) - min) / (double)(max - min);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 完全透明背景 - 不渲染任何背景，直接看到游戏世界
        
        // 去掉标题 - 不显示任何文字，避免重叠
        
        // 去掉页码信息 - 不显示任何文字，避免重叠
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // 完全透明 - 不绘制任何背景
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 不绘制任何前景文字（包括Inventory等标签）
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    /**
     * AI随机效果选择 - 不重复选择机制
     */
    private void selectRandomAIEffects() {
        // 检查权限
        if (!hasPermission) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(
                    Text.literal(LanguageManager.getUI("ai.permission.denied"))
                        .formatted(Formatting.RED, Formatting.BOLD),
                    false
                );
            }
            return;
        }
        
        // 根据当前语言选择对应的组合列表
        List<AIEffectCombinations.EffectCombination> combinations = 
            LanguageManager.getCurrentLanguage() == LanguageManager.Language.ENGLISH ? 
                AIEffectCombinationsEN.ALL_COMBINATIONS_EN : AIEffectCombinations.ALL_COMBINATIONS;
        
        // 如果所有组合都用过了，重置池子
        if (usedCombinations.size() >= combinations.size()) {
            usedCombinations.clear();
        }
        
        // 找到还没用过的组合
        List<Integer> availableIndexes = new ArrayList<>();
        for (int i = 0; i < combinations.size(); i++) {
            if (!usedCombinations.contains(i)) {
                availableIndexes.add(i);
            }
        }
        
        if (availableIndexes.isEmpty()) {
            // 理论上不应该发生，但保险起见
            usedCombinations.clear();
            for (int i = 0; i < combinations.size(); i++) {
                availableIndexes.add(i);
            }
        }
        
        // 随机选择一个组合
        int randomIndex = availableIndexes.get(ThreadLocalRandom.current().nextInt(availableIndexes.size()));
        usedCombinations.add(randomIndex);
        
        AIEffectCombinations.EffectCombination selectedCombo = combinations.get(randomIndex);
        
        Map<String, Boolean> changes = new LinkedHashMap<>();
        for (String key : getCurrentLabels().keySet()) {
            changes.put(key, false);
        }
        for (String effectKey : selectedCombo.effects) {
            changes.put(effectKey, true);
        }
        ClientPlayNetworking.send(new ConfigToggleC2SPacket(changes));
        
        // 发送聊天消息
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(
                Text.literal("[ChaosMod AI] " + selectedCombo.chatMessage)
                    .formatted(Formatting.YELLOW, Formatting.BOLD),
                false
            );
            
            // 显示启用的效果列表
            StringBuilder effectList = new StringBuilder(LanguageManager.getUI("ai.effects.enabled"));
            for (int i = 0; i < selectedCombo.effects.length; i++) {
                String effectKey = selectedCombo.effects[i];
                String effectName = getCurrentLabels().get(effectKey);
                if (effectName != null) {
                    effectList.append(effectName);
                    if (i < selectedCombo.effects.length - 1) {
                        effectList.append(" + ");
                    }
                }
            }
            
            this.client.player.sendMessage(
                Text.literal(effectList.toString())
                    .formatted(Formatting.GREEN),
                false
            );
        }
        
        // 更新按钮显示
        updateButtonTexts();
    }
    
    /**
     * 获取简洁的Tooltip文本
     */
    private String getTooltipText(String key) {
        String language = com.example.config.LanguageManager.getCurrentLanguage().code;
        
        if ("en_us".equals(language)) {
            return switch (key) {
                case "allHostileEnabled" -> "All mobs attack players";
                case "mobIgniteEnabled" -> "Mob hits set on fire";
                case "mobSlownessEnabled" -> "Mob hits give slowness";
                case "mobBlindnessEnabled" -> "Mob hits give blindness";
                case "mobThornsEnabled" -> "Attack mobs = 50% reflect damage";
                case "foodPoisonEnabled" -> "Eating food = poison chance";
                case "enderDragonBucketEnabled" -> "Dragon turns water→milk";
                case "enderDragonKillEnabled" -> "Kill dragon = suicide";
                case "playerDamageShareEnabled" -> "Share damage with nearby players";
                case "sharedHealthEnabled" -> "All players share one health and hunger state";
                case "sharedDamageSplitEnabled" -> "All damage split server-wide";
                case "randomDamageEnabled" -> "Damage transfers randomly";
                case "shieldNerfEnabled" -> "Shield blocks only 80%";
                case "lowHealthNoHealEnabled" -> "≤1 heart = no heal 10s";
                case "waterToLavaEnabled" -> "Water 50% becomes lava";
                case "endKeepOverrideEnabled" -> "End drops, others keep";
                case "reverseDamageEnabled" -> "Safe=damage, hurt=stop";
                case "sunburnEnabled" -> "Sunlight = auto fire";
                case "healReverseEnabled" -> "Heal 50% becomes damage";
                case "fallTrapEnabled" -> "Jump land 20% damage";
                case "acrophobiaEnabled" -> "High altitude = more damage";
                case "blockRevengeEnabled" -> "Break blocks = revenge";
                case "containerCurseEnabled" -> "Open containers = damage";
                case "inventoryCurseEnabled" -> "Switch items = damage";
                case "craftingTrapEnabled" -> "Craft items = damage";
                case "playerHealOnAttackEnabled" -> "Attack players = heal self";
                case "positionSwapEnabled" -> "Hurt = swap with teammate";
                case "craftingBombEnabled" -> "Workbench = bomb after 5s";
                case "waterDamageEnabled" -> "Water = continuous damage";
                case "randomDamageAmountEnabled" -> "Damage = random amount";
                case "delayedDamageEnabled" -> "Damage delayed 0-5s";
                case "keyDisableEnabled" -> "10 hits = disable key";
                case "randomEffectsEnabled" -> "Hurt = random effects";
                case "damageScapegoatEnabled" -> "Random scapegoat takes damage";
                case "painSpreadEnabled" -> "Hurt = electrify nearby";
                case "panicMagnetEnabled" -> "Hurt = magnetize teammates";
                case "pickupDrainEnabled" -> "Pickup = health cost";
                case "vertigoScapegoatEnabled" -> "Random suffers others' pain";
                case "windowViolentShakeEnabled" -> "Death = window shake";
                case "desktopPrankInvasionEnabled" -> "Low health = desktop files";
                case "randomKeyPressEnabled" -> "2min = poison II + damage + shock";
                case "touchHellEnabled" -> "Right-click = lava teleport";
                case "movementTaxEnabled" -> "10 blocks = damage";
                case "controlSeizurePlusEnabled" -> "Death = disable WASD key";
                case "jumpTaxEnabled" -> "Jump = damage";
                case "forcedTetherEnabled" -> "120s = random bind two players for 90s";
                case "hpAveragingEnabled" -> "60s = random two players HP average";
                case "multiplayerRouletteEnabled" -> "90s = random lottery punishment";
                case "timedPositionSwapEnabled" -> "60s = random swap positions";
                case "forcedSprintEnabled" -> "90s = one must keep moving";
                case "periodicNegativeEffectEnabled" -> "Every interval, receive one random harmful effect only";
                case "weaponSlipEnabled" -> "Melee hit a hostile mob = chance to drop held weapon";
                case "magmaBetrayalEnabled" -> "Random player's solid footing becomes magma for 10s";
                case "timeReboundEnabled" -> "Return to the recorded position after 5s and lose 2 hearts";
                case "burdenCollapseEnabled" -> "Inventory load is settled after 5s as damage and Slowness II";
                case "profanityPenaltyEnabled" -> "Speak profanity into mic = lose hearts (needs Shriek mod)";
                default -> "Unknown effect";
            };
        } else {
            return switch (key) {
                case "allHostileEnabled" -> "所有生物攻击玩家";
                case "mobIgniteEnabled" -> "被怪物命中着火";
                case "mobSlownessEnabled" -> "被怪物命中缓慢";
                case "mobBlindnessEnabled" -> "被怪物命中失明";
                case "mobThornsEnabled" -> "攻击怪物=50%反伤";
                case "foodPoisonEnabled" -> "吃食物=中毒概率";
                case "enderDragonBucketEnabled" -> "龙把水桶变牛奶";
                case "enderDragonKillEnabled" -> "杀龙=自杀";
                case "playerDamageShareEnabled" -> "与附近玩家分担伤害";
                case "sharedHealthEnabled" -> "全服玩家共享同一生命、饥饿和饱和度";
                case "sharedDamageSplitEnabled" -> "全服分担伤害";
                case "randomDamageEnabled" -> "伤害随机转移";
                case "shieldNerfEnabled" -> "盾牌只挡80%";
                case "lowHealthNoHealEnabled" -> "≤1心=禁回血10秒";
                case "waterToLavaEnabled" -> "放水50%变岩浆";
                case "endKeepOverrideEnabled" -> "末地掉落，其他保留";
                case "reverseDamageEnabled" -> "安全=扣血，受伤=停止";
                case "sunburnEnabled" -> "阳光=自动着火";
                case "healReverseEnabled" -> "回血50%变扣血";
                case "fallTrapEnabled" -> "跳跃落地=伤害概率";
                case "acrophobiaEnabled" -> "高度=更多伤害";
                case "blockRevengeEnabled" -> "破坏方块=反伤";
                case "containerCurseEnabled" -> "开容器=伤害";
                case "inventoryCurseEnabled" -> "切换物品=伤害";
                case "craftingTrapEnabled" -> "合成物品=伤害";
                case "playerHealOnAttackEnabled" -> "攻击玩家=自己回血";
                case "positionSwapEnabled" -> "受伤=与队友换位";
                case "craftingBombEnabled" -> "工作台=5秒后爆炸";
                case "waterDamageEnabled" -> "水=持续伤害";
                case "randomDamageAmountEnabled" -> "伤害=随机数值";
                case "delayedDamageEnabled" -> "伤害延迟0-5秒";
                case "keyDisableEnabled" -> "10次受伤=禁用按键";
                case "randomEffectsEnabled" -> "受伤=随机状态效果";
                case "damageScapegoatEnabled" -> "随机背锅人承受伤害";
                case "painSpreadEnabled" -> "受伤=电击附近队友";
                case "panicMagnetEnabled" -> "受伤=磁化拉拽队友";
                case "pickupDrainEnabled" -> "拾取=扣血";
                case "vertigoScapegoatEnabled" -> "随机承受他人痛苦";
                case "windowViolentShakeEnabled" -> "死亡=窗口抖动";
                case "desktopPrankInvasionEnabled" -> "低血量=桌面文件";
                case "randomKeyPressEnabled" -> "2分钟=中毒2+扣血+电击";
                case "touchHellEnabled" -> "右键=岩浆传送";
                case "movementTaxEnabled" -> "10格移动=扣血";
                case "controlSeizurePlusEnabled" -> "死亡=禁用WASD键";
                case "jumpTaxEnabled" -> "跳跃=扣血";
                case "forcedTetherEnabled" -> "120秒=随机绑定两人90秒";
                case "hpAveragingEnabled" -> "60秒=随机两人血量平均";
                case "multiplayerRouletteEnabled" -> "90秒=随机抽奖惩罚";
                case "timedPositionSwapEnabled" -> "60秒=随机交换位置";
                case "forcedSprintEnabled" -> "90秒=一人必须持续移动";
                case "periodicNegativeEffectEnabled" -> "每隔设定秒数获得一个随机负面效果，不含增益";
                case "weaponSlipEnabled" -> "近战命中敌对生物时概率丢出主手武器";
                case "magmaBetrayalEnabled" -> "随机玩家脚下实体方块变成岩浆块10秒";
                case "timeReboundEnabled" -> "5秒后回到记录位置并固定扣2颗心";
                case "burdenCollapseEnabled" -> "5秒后按背包占用槽位扣血并获得缓慢II";
                case "profanityPenaltyEnabled" -> "对麦克风说脏话=扣血，越多扣越多(需Shriek前置)";
                default -> "未知效果";
            };
        }
    }
    
}
