package com.example.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端IP探测器 - 通过HTTP探测客户端的公网IP
 * 每个客户端独立探测自己的公网IP并报告给服务器
 */
public class ClientIPProber {
    
    // HTTP回显端点（优先国内API，访问更快）
    private static final String[] IP_ENDPOINTS = {
        // 国内API优先（访问速度快，延迟低）
        "https://ip.3322.net",                                // 3322.net
        "https://openapi.lddgo.net/base/gtool/api/v1/GetIp", // LDDGO IP查询
        "https://myip.ipip.net",                               // IPIP.NET
        "https://api-ipv4.ip.sb/ip",                          // IP.SB（国内节点）
        
        // 国际API备用（国内访问可能较慢）
        "https://api.ipify.org",
        "https://ifconfig.me/ip", 
        "https://icanhazip.com",
        "https://checkip.amazonaws.com"
    };
    
    private static String cachedIP = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL = 10 * 60 * 1000; // 10分钟缓存
    
    /**
     * 异步探测公网IP并报告给服务器
     */
    public static void probeAndReportAsync() {
        // 检查缓存
        if (cachedIP != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL) {
            sendIPToServer(cachedIP);
            return;
        }
        
        // 异步探测
        CompletableFuture.runAsync(() -> {
            String probedIP = probePublicIPSync();
            if (probedIP != null && isValidIPFormat(probedIP)) {
                // 缓存结果
                cachedIP = probedIP;
                cacheTimestamp = System.currentTimeMillis();
                
                // 发送给服务器
                sendIPToServer(probedIP);
            } else {
                // 探测失败，发送默认值
                sendIPToServer("127.0.0.1");
            }
        });
    }
    
    /**
     * 同步探测公网IP
     */
    private static String probePublicIPSync() {
        for (String endpoint : IP_ENDPOINTS) {
            try {
                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                connection.setRequestProperty("User-Agent", "ChaosMod/1.8.0");
                
                if (connection.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        String ip = reader.readLine();
                        if (ip != null) {
                            ip = ip.trim();
                            if (isValidIPFormat(ip)) {
                                System.out.println("[ChaosMod Client] 探测到公网IP: " + ip);
                                return ip;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 尝试下一个端点
            }
        }
        return null;
    }
    
    /**
     * 验证IP格式
     */
    private static boolean isValidIPFormat(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 发送探测到的IP给服务器
     */
    private static void sendIPToServer(String ip) {
        try {
            ClientPlayNetworking.send(new com.example.network.PlayerIPReportC2SPacket(ip));
            System.out.println("[ChaosMod Client] 已向服务器报告IP: " + ip);
        } catch (Exception e) {
            System.err.println("[ChaosMod Client] 发送IP报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除缓存（用于强制重新探测）
     */
    public static void clearCache() {
        cachedIP = null;
        cacheTimestamp = 0;
    }
}
