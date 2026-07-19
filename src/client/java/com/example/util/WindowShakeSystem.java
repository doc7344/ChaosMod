package com.example.util;

import com.example.ChaosMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 窗口暴力抖动系统 - 第39种终极第四面墙效果
 * 技术实现：GLFW窗口位置控制 + 抖动时间轴系统
 */
public class WindowShakeSystem {
    
    // === 抖动状态管理 ===
    private static boolean isShaking = false;
    private static long shakeStartTime = 0;
    private static long shakeDuration = 0;
    private static float shakeAmplitude = 0.0f;
    private static int baseWindowX = 0;
    private static int baseWindowY = 0;
    private static boolean basePositionSet = false;
    
    // 移除持续颤抖相关变量，只保留死亡抖动功能
    
    // 移除扣血计数器，只保留死亡抖动
    
    // === 死亡状态检测（isDead() 边沿检测） ===
    private static boolean wasDeadLastTick = false;
    
    /**
     * 客户端Tick - 处理窗口抖动逻辑
     * 按照用户指导：用每帧读数阈值判断生命值 ≤5♥ / ≤2♥
     */
    public static void clientTick() {
        if (!ChaosMod.config.windowViolentShakeEnabled) {
            if (isShaking || wasDeadLastTick) {
                resetWindowPosition();
            } else {
                basePositionSet = false;
            }
            wasDeadLastTick = false;
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        
        long window = client.getWindow().getHandle();
        if (window == 0) return;
        
        // 初始化基准位置
        if (!basePositionSet) {
            int[] xPos = new int[1];
            int[] yPos = new int[1];
            GLFW.glfwGetWindowPos(window, xPos, yPos);
            baseWindowX = xPos[0];
            baseWindowY = yPos[0];
            basePositionSet = true;
        }
        
        PlayerEntity player = client.player;
        if (player == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // 用 isDead() 的由假变真边沿检测死亡瞬间
        boolean isDeadNow = player.isDead();
        if (!wasDeadLastTick && isDeadNow) {
            // 死亡瞬间：疯狂抖动3秒（50px振幅）
            triggerDeathShake();
        }
        wasDeadLastTick = isDeadNow;
        
        // 简化完毕：只需要死亡抖动，无其他逻辑
        
        // 处理主动抖动
        if (isShaking) {
            long elapsed = currentTime - shakeStartTime;
            if (elapsed >= shakeDuration) {
                // 抖动结束，恢复原位置
                stopShaking(window);
            } else {
                // 继续抖动
                performShake(window, shakeAmplitude, elapsed, shakeDuration);
            }
        }
        
        // 不需要持续颤抖了，用户只要死亡抖动！
    }
    
    // 移除复杂的伤害分级抖动系统
    // 用户只要死亡时的剧烈抖动！
    
    /**
     * 处理死亡抖动 - 超级震撼的死亡特效！💀
     * 用户专门要求：只有死亡时才剧烈抖动！
     */
    public static void triggerDeathShake() {
        if (!ChaosMod.config.windowViolentShakeEnabled) return;
        
        // 💀 死亡终极特效：10秒超级疯狂抖动，200像素巨大振幅！
        startShake(10000, 200.0f);
    }
    
    /**
     * 开始抖动序列
     */
    private static void startShake(long duration, float amplitude) {
        isShaking = true;
        shakeStartTime = System.currentTimeMillis();
        shakeDuration = duration;
        shakeAmplitude = amplitude;
    }
    
    /**
     * 执行抖动 - 带指数衰减曲线
     */
    private static void performShake(long window, float amplitude, long elapsed, long duration) {
        // 指数衰减曲线
        float progress = (float) elapsed / duration;
        float decay = (float) Math.exp(-progress * 3); // 指数衰减
        float currentAmplitude = amplitude * decay;
        
        // 随机单位向量
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        int offsetX = (int) (Math.cos(angle) * currentAmplitude);
        int offsetY = (int) (Math.sin(angle) * currentAmplitude);
        
        // 设置窗口位置
        GLFW.glfwSetWindowPos(window, baseWindowX + offsetX, baseWindowY + offsetY);
        
        // 移除额外扣血功能
    }
    
    // 移除轻微颤抖功能，用户只要死亡抖动
    
    /**
     * 停止抖动，恢复原位置
     */
    private static void stopShaking(long window) {
        isShaking = false;
        GLFW.glfwSetWindowPos(window, baseWindowX, baseWindowY);
    }
    
    // 移除额外伤害功能，用户只要死亡抖动
    
    /**
     * 重置窗口位置（用于清理）
     */
    public static void resetWindowPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (basePositionSet && client != null && client.getWindow() != null) {
            long window = client.getWindow().getHandle();
            if (window != 0) {
                GLFW.glfwSetWindowPos(window, baseWindowX, baseWindowY);
            }
        }
        
        isShaking = false;
        shakeStartTime = 0;
        shakeDuration = 0;
        shakeAmplitude = 0.0f;
        wasDeadLastTick = false;
        basePositionSet = false;
        // 简化完毕：只保留基本的窗口重置功能
    }
}
