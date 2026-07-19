package com.example.util;

import com.example.network.DesktopFileGenerateS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 桌面文件管理器 - 防止文件堆积的单文件生成系统
 * 按照用户要求：只生成一次，删除旧文件，复活后重置
 */
public class DesktopFileManager {
    
    // === 玩家文件生成状态跟踪 ===
    private static final Map<UUID, String> playerCurrentFile = new ConcurrentHashMap<>();
    
    /**
     * 生成单个文件（删除旧文件，生成新文件）- 已废弃
     * 使用 generateSingleFileWithHint 替代，支持多语言
     */
    @Deprecated
    public static void generateSingleFile(ServerPlayerEntity player, String fileType, String content) {
        // 转发到新的多语言方法
        generateSingleFileWithHint(player, fileType, content);
    }
    
    /**
     * 生成单个文件并发送聊天提示（删除旧文件，生成新文件，发送提示）
     * 确保文件生成和聊天提示同步，防止刷屏
     * 支持多语言文件内容
     */
    public static void generateSingleFileWithHint(ServerPlayerEntity player, String fileType, String contentKey) {
        if (player == null) return;
        
        UUID playerId = player.getUuid();
        String previousFile = playerCurrentFile.get(playerId);
        
        // 获取玩家的语言设置
        String playerLanguage = getPlayerLanguage(player);
        
        // 获取玩家IP和血量（修复：使用PublicIpProvider获取每个玩家的真实IP）
        String playerIP = PublicIpProvider.getPublicIP(player);
        float currentHealth = player.getHealth();
        
        // 生成完整的邪恶文件内容
        String fullContent = com.example.config.LanguageManager.getEvilFileContent(
            playerLanguage, contentKey, playerIP, currentHealth
        );
        
        // 生成文件名
        String baseFileName = com.example.config.LanguageManager.getDesktopContentByLanguage(playerLanguage, contentKey);
        String fileName = baseFileName + "_" + getCurrentTimestamp() + ".txt";
        
        // 先授权一次回执，再发送文件请求；客户端只能消费本次授权一次。
        com.example.network.FourthWallDamageC2SPacket.authorize(player, "desktop_file", 2.0F);
        com.example.network.DesktopFileContentS2CPacket.send(player, fileName, fullContent, previousFile);
        
        // 只有在文件生成请求发送后才发送聊天提示（确保同步）
        com.example.util.FourthWallPunishmentSystem.sendDesktopHint(player, fileType);
        
        // 更新当前文件记录
        playerCurrentFile.put(playerId, fileName);
    }
    
    /**
     * 获取玩家的语言设置
     */
    private static String getPlayerLanguage(ServerPlayerEntity player) {
        // 从全局配置获取语言设置（简化版本）
        // 在更复杂的实现中，可以为每个玩家单独存储语言设置
        return com.example.ChaosMod.config.getLanguage();
    }
    
    /**
     * 复活时重置玩家的文件生成状态
     * 允许玩家复活后重新触发文件生成
     */
    public static void resetPlayerOnRespawn(UUID playerId) {
        // 重置生成状态（在ServerPlayerHealthMixin中调用）
        // 这里只清理文件记录，实际重置在ServerPlayerHealthMixin中处理
        playerCurrentFile.remove(playerId);
    }
    
    /**
     * 玩家离线时清理状态
     */
    public static void cleanupPlayerData(UUID playerId) {
        playerCurrentFile.remove(playerId);
        com.example.network.FourthWallDamageC2SPacket.cleanupPlayer(playerId);
    }
    
    /**
     * 获取当前时间戳
     */
    private static String getCurrentTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HHmmss");
        return sdf.format(new java.util.Date());
    }
    
    /**
     * 获取玩家当前文件名（调试用）
     */
    public static String getPlayerCurrentFile(UUID playerId) {
        return playerCurrentFile.get(playerId);
    }
}
