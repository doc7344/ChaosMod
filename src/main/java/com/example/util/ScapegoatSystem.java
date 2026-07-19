package com.example.util;

import com.example.ChaosMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 伤害背锅人系统 - 按照严格规范实现
 */
public class ScapegoatSystem {
    
    // 递归抑制ThreadLocal
    private static final ThreadLocal<Boolean> REDIRECTING_DAMAGE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    
    // 全局缓存
    private static ServerPlayerEntity currentScapegoat = null;
    private static long lastScapegoatTime = 0;
    private static long scapegoatIntervalTicks() {
        return ChaosMod.config.damageScapegoatIntervalSeconds * 20L;
    }
    
    /**
     * PersistentState用于持久化背锅人UUID - 按照正确API实现
     */
    public static class ScapegoatPersistentState extends PersistentState {
        // 静态Type<ScapegoatPersistentState>
        public static final PersistentState.Type<ScapegoatPersistentState> TYPE = new PersistentState.Type<>(
            ScapegoatPersistentState::new,  // Supplier<YourState> 新建器
            ScapegoatPersistentState::createFromNbt,  // BiFunction<NbtCompound, RegistryWrapper.WrapperLookup, YourState> 反序列化器
            null  // DataFixTypes
        );
        
        private UUID lastScapegoat = Util.NIL_UUID;        // 上一次的背锅人（使用NIL_UUID作为哨兵）
        private Set<UUID> visited = new HashSet<>();     // 已访问过的玩家
        private long nextRollTick = 0;            // 下次抽签时间
        private Set<UUID> deadUntilRespawn = new HashSet<>();  // 死亡标记存进PersistentState
        
        // 无参构造（用于空状态创建）
        public ScapegoatPersistentState() {}
        
        // 反序列化构造（读取构造）
        public static ScapegoatPersistentState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            ScapegoatPersistentState state = new ScapegoatPersistentState();
            
            // 读取lastScapegoat，缺省设为NIL_UUID
            if (nbt.containsUuid("lastScapegoat")) {
                UUID uuid = nbt.getUuid("lastScapegoat");
                state.lastScapegoat = (uuid != null) ? uuid : Util.NIL_UUID;
            } else {
                state.lastScapegoat = Util.NIL_UUID;
            }
            
            // 读取visited列表
            if (nbt.contains("visited")) {
                NbtCompound visitedNbt = nbt.getCompound("visited");
                int count = visitedNbt.getInt("count");
                for (int i = 0; i < count; i++) {
                    if (visitedNbt.containsUuid("uuid_" + i)) {
                        UUID uuid = visitedNbt.getUuid("uuid_" + i);
                        if (uuid != null && !uuid.equals(Util.NIL_UUID)) {
                            state.visited.add(uuid);
                        }
                    }
                }
            }
            
            // 读取deadUntilRespawn列表
            if (nbt.contains("deadUntilRespawn")) {
                NbtCompound deadNbt = nbt.getCompound("deadUntilRespawn");
                int count = deadNbt.getInt("count");
                for (int i = 0; i < count; i++) {
                    if (deadNbt.containsUuid("uuid_" + i)) {
                        UUID uuid = deadNbt.getUuid("uuid_" + i);
                        if (uuid != null && !uuid.equals(Util.NIL_UUID)) {
                            state.deadUntilRespawn.add(uuid);
                        }
                    }
                }
            }
            
            state.nextRollTick = nbt.getLong("nextRollTick");
            return state;
        }
        
        // 正确的writeNbt方法签名 - 修复null UUID问题
        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            // 只有非null且非NIL_UUID才写入
            if (lastScapegoat != null && !lastScapegoat.equals(Util.NIL_UUID)) {
                nbt.putUuid("lastScapegoat", lastScapegoat);
            }
            
            // 保存visited列表，过滤掉null和NIL_UUID
            NbtCompound visitedNbt = new NbtCompound();
            List<UUID> validVisited = new ArrayList<>();
            for (UUID uuid : visited) {
                if (uuid != null && !uuid.equals(Util.NIL_UUID)) {
                    validVisited.add(uuid);
                }
            }
            
            visitedNbt.putInt("count", validVisited.size());
            for (int i = 0; i < validVisited.size(); i++) {
                visitedNbt.putUuid("uuid_" + i, validVisited.get(i));
            }
            nbt.put("visited", visitedNbt);
            
            // 保存deadUntilRespawn列表
            NbtCompound deadNbt = new NbtCompound();
            List<UUID> validDead = new ArrayList<>();
            for (UUID uuid : deadUntilRespawn) {
                if (uuid != null && !uuid.equals(Util.NIL_UUID)) {
                    validDead.add(uuid);
                }
            }
            
            deadNbt.putInt("count", validDead.size());
            for (int i = 0; i < validDead.size(); i++) {
                deadNbt.putUuid("uuid_" + i, validDead.get(i));
            }
            nbt.put("deadUntilRespawn", deadNbt);
            
            nbt.putLong("nextRollTick", nextRollTick);
            return nbt;
        }
        
        // 正确的getOrCreate调用
        public static ScapegoatPersistentState get(ServerWorld world) {
            PersistentStateManager manager = world.getPersistentStateManager();
            return manager.getOrCreate(TYPE, "chaosmod_scapegoat");
        }
        
        public UUID getLastScapegoat() { 
            return (lastScapegoat != null && !lastScapegoat.equals(Util.NIL_UUID)) ? lastScapegoat : null; 
        }
        public Set<UUID> getVisited() { return new HashSet<>(visited); }
        public long getNextRollTick() { return nextRollTick; }
        public Set<UUID> getDeadUntilRespawn() { return new HashSet<>(deadUntilRespawn); }
        
        public void setNewScapegoat(UUID newScapegoat, long nextRoll) {
            // PersistentState持久化：统一处理并立刻markDirty()
            this.lastScapegoat = (newScapegoat != null) ? newScapegoat : Util.NIL_UUID;
            
            // 只添加有效UUID到visited
            if (newScapegoat != null && !newScapegoat.equals(Util.NIL_UUID)) {
                this.visited.add(newScapegoat);
            }
            
            // 后续只在成功抽签后更新为now+6000，严禁在常规tick流中反复重置
            this.nextRollTick = nextRoll;
            
            // 任何修改后立刻markDirty()
            markDirty();
        }

        public void setNextRollTick(long nextRollTick) {
            this.nextRollTick = nextRollTick;
            markDirty();
        }
        
        public void clearVisited() {
            this.visited.clear();
            markDirty();
        }
        
        public void clearAll() {
            // PersistentState持久化：任何修改后立刻markDirty()
            this.lastScapegoat = Util.NIL_UUID;  // 使用NIL_UUID而不是null
            this.visited.clear();
            this.nextRollTick = 0;
            markDirty();
        }
        
        public void clearCurrentScapegoat() {
            // 离线时置回NIL_UUID并markDirty()
            this.lastScapegoat = Util.NIL_UUID;
            markDirty();
        }
        
        public void markPlayerDead(UUID playerUUID) {
            this.deadUntilRespawn.add(playerUUID);
            markDirty();
        }
        
        public void markPlayerRespawned(UUID playerUUID) {
            this.deadUntilRespawn.remove(playerUUID);
            markDirty();
        }
        
        public boolean isPlayerDeadUntilRespawn(UUID playerUUID) {
            return this.deadUntilRespawn.contains(playerUUID);
        }
    }
    
    /**
     * 服务端tick处理背锅人选择 - 按照新规范实现
     */
    public static void tickScapegoat(MinecraftServer server) {
        if (!ChaosMod.config.damageScapegoatEnabled) {
            currentScapegoat = null;
            return;
        }
        
        ServerWorld overworld = server.getOverworld();
        long currentTick = overworld.getTime();
        ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
        restoreOnlineScapegoat(server, state);
        if (getEligiblePlayerCount(server.getPlayerManager().getPlayerList()) < 2) return;
        
        // 背锅人不可用即"即时重选"：在定时器处调用ensureScapegoat()
        ensureScapegoat(server, state, currentTick);
        
        // tick门槛固定值：只在到点时才检查重选
        if (currentTick >= state.getNextRollTick()) {
            selectNewScapegoatWithVisited(server, state, currentTick);
        }
        
    }
    
    /**
     * 确保背锅人可用，不可用即即时重选
     */
    private static void ensureScapegoat(MinecraftServer server, ScapegoatPersistentState state, long currentTick) {
        // 状态机单一真源：统一只从"主世界PersistentState"读取
        ServerWorld overworld = server.getOverworld();
        if (state != ScapegoatPersistentState.get(overworld)) {
            state = ScapegoatPersistentState.get(overworld); // 确保使用同一实例
        }
        if (getEligiblePlayerCount(server.getPlayerManager().getPlayerList()) < 2) return;
        
        boolean needReselect = false;
        String reason = "";
        
        if (currentScapegoat == null) {
            needReselect = true;
            reason = "无背锅人";
        } else {
            // 修复：把"在线"仅判定为PlayerManager#getPlayer(uuid)!=null（连接存活，别看isAlive()）
            ServerPlayerEntity foundPlayer = server.getPlayerManager().getPlayer(currentScapegoat.getUuid());
            boolean offline = (foundPlayer == null);
            
            if (offline) {
                needReselect = true;
                reason = "背锅人真正离线";
                UUID oldUUID = currentScapegoat.getUuid();
                currentScapegoat = null;
                state.clearCurrentScapegoat();
                // 离线时清除死亡标记
                state.markPlayerRespawned(oldUUID);
            } else if (state.isPlayerDeadUntilRespawn(currentScapegoat.getUuid())) {
                // 死亡期间不重选，只是暂停转移
                // 这里不设置needReselect = true，保持背锅人不变
                reason = "背锅人死亡中";
            } else if (!isEligiblePlayer(foundPlayer)) {
                needReselect = true;
                reason = "背锅人已不再是有效候选者";
            } else {
                // 复活或换维度后实体对象可能变化，始终绑定实时实体。
                currentScapegoat = foundPlayer;
            }
        }
        
        if (needReselect) {
            selectNewScapegoatWithVisited(server, state, currentTick);
        }
    }
    
    /**
     * 选择新的背锅人 - 按照visited机制实现
     */
    private static void selectNewScapegoatWithVisited(MinecraftServer server, ScapegoatPersistentState state, long currentTick) {
        // 选择过程显错：外层加异常上报通路
        try {
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            if (players.isEmpty()) {
                return;
            }
        
        // 计算candidates = 在线玩家 - visited - {lastScapegoat}
        UUID lastScapegoat = state.getLastScapegoat();
        Set<UUID> visited = state.getVisited();
        List<ServerPlayerEntity> candidates = new ArrayList<>();
        
        
        // candidates = 在线玩家 − visited − {lastScapegoat≠NIL?last:∅}
        for (ServerPlayerEntity player : players) {
            // 修复：把"在线"仅判定为PlayerManager#getPlayer(uuid)!=null
            ServerPlayerEntity foundPlayer = server.getPlayerManager().getPlayer(player.getUuid());
            boolean connected = (foundPlayer != null);
            
            boolean inVisited = visited.contains(player.getUuid());
            
            // NIL_UUID规范化：仅当last≠NIL时才加入排除集，避免把所有玩家误排除
            boolean isLastScapegoat = (lastScapegoat != null && 
                !lastScapegoat.equals(Util.NIL_UUID) && 
                player.getUuid().equals(lastScapegoat));
            
            if (connected && isEligiblePlayer(player)
                    && !inVisited && !isLastScapegoat) {
                candidates.add(player);
            }
        }
        
        
        // visited兜底：当candidates为空且在线≥2时先visited.clear()再重算，并仍排除last
        if (candidates.isEmpty() && getEligiblePlayerCount(players) >= 2) {
            state.clearVisited(); // visited.clear()
            visited = state.getVisited(); // 重新获取空的visited
            
            // 重算candidates，但仍排除last，保证两人在线时必定能选出另一人
            for (ServerPlayerEntity player : players) {
                // 修复：把"在线"仅判定为PlayerManager#getPlayer(uuid)!=null
                ServerPlayerEntity foundPlayer = server.getPlayerManager().getPlayer(player.getUuid());
                boolean connected = (foundPlayer != null);
                
                if (connected && isEligiblePlayer(player)) {
                    // NIL_UUID规范化：仅当last≠NIL时才排除
                    boolean canAdd = (lastScapegoat == null || 
                                      lastScapegoat.equals(Util.NIL_UUID) || 
                                      !player.getUuid().equals(lastScapegoat));
                    if (canAdd) {
                        candidates.add(player);
                    }
                }
            }
        }
        
        // 抽签逻辑始终产出有效UUID
        if (!candidates.isEmpty()) {
            // 从candidates随机抽取设为新的背锅人
            currentScapegoat = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            
            // 成功抽签后的主间隔由服务端配置控制。
            long nextRoll = currentTick + scapegoatIntervalTicks();
            state.setNewScapegoat(currentScapegoat.getUuid(), nextRoll);
            
            // 只保留模糊警告，不告诉背锅人自己被选中
            broadcastScapegoatWarning(server); // 向全体推送模糊提示
        } else {
            // tick门槛固定值：只在"背锅人不可用"时设置nextRollTick
            long nextRoll = currentTick + 1200; // 1分钟后重试
            state.setNewScapegoat(Util.NIL_UUID, nextRoll);
        }
        
        } catch (Exception e) {
            // 一旦失败立即保底nextRollTick=now+shortTTL以便快速重试
            long shortTTL = currentTick + 600; // 30秒后重试
            try {
                state.setNewScapegoat(Util.NIL_UUID, shortTTL); // 确保markDirty()
            } catch (Exception stateEx) {
                // 状态保存也失败的极端情况，静默处理
            }
        }
    }
    
    
    
    /**
     * 重定向伤害到背锅人 - 按严格规范实现
     */
    public static boolean redirectDamageToScapegoat(LivingEntity victim, DamageSource source, float amount) {
        if (!ChaosMod.config.damageScapegoatEnabled) return false;
        if (victim.getWorld().isClient()) return false;
        
        // 递归抑制严格清理：检查ThreadLocal
        if (REDIRECTING_DAMAGE.get()) {
            return false; // 防止递归
        }
        
        // 只处理ServerPlayerEntity
        if (!(victim instanceof ServerPlayerEntity victimPlayer)) return false;
        
        // 修复：禁止在伤害拦截里调用重选逻辑（死时只放行原伤害）
        if (currentScapegoat == null) {
            return false; // 无背锅人，不调用重选
        }
        
        // 修复：把"在线"仅判定为PlayerManager#getPlayer(uuid)!=null
        ServerPlayerEntity foundScapegoat = victim.getServer().getPlayerManager().getPlayer(currentScapegoat.getUuid());
        if (foundScapegoat == null || !isEligiblePlayer(foundScapegoat)) {
            return false; // 真正离线，不转移（等待tick重选）
        }
        
        // 获取PersistentState检查死亡状态
        ServerWorld overworld = victimPlayer.getServer().getOverworld();
        ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
        
        // 若背锅人处于deadUntilRespawn则不改向（放行原伤害）
        if (state.isPlayerDeadUntilRespawn(currentScapegoat.getUuid())) {
            return false; // 死亡期间不转移伤害，放行原伤害
        }
        
        // 背锅人自己受伤时不重定向（避免循环）
        if (victim == currentScapegoat) {
            return false;
        }
        
        try {
            // 递归抑制严格清理：ThreadLocal必须try/finally复位
            REDIRECTING_DAMAGE.set(Boolean.TRUE);
            
            // 检查是否为火系伤害或原受害者着火
            boolean isFireDamage = source.isIn(DamageTypeTags.IS_FIRE) || 
                                   source.getType().equals(victimPlayer.getDamageSources().lava().getType()) ||
                                   source.getType().equals(victimPlayer.getDamageSources().hotFloor().getType());
            boolean victimOnFire = victim.isOnFire();
            
            // 记录原受害者的燃烧状态
            int originalFireTicks = 0;
            if (victimOnFire || isFireDamage) {
                originalFireTicks = victim.getFireTicks();
            }
            
            // 转发为同源伤害到背锅人
            // 以PlayerManager中的实时实体为准，支持复活和跨维度转移
            currentScapegoat = foundScapegoat;
            currentScapegoat.damage(source, amount);
            
            // 如果是火焰伤害或原受害者着火，同步燃烧效果到背锅人
            if (isFireDamage || victimOnFire) {
                int currentScapegoatFireTicks = currentScapegoat.getFireTicks();
                // setFireTicks(max(自身, 原值))
                int newFireTicks = Math.max(currentScapegoatFireTicks, originalFireTicks);
                if (newFireTicks > 0) {
                    currentScapegoat.setFireTicks(newFireTicks);
                }
                
                // 如果原受害者着火但背锅人不着火，使用setOnFireFor
                if (originalFireTicks > 0 && currentScapegoatFireTicks <= 0) {
                    int fireSeconds = Math.max(1, originalFireTicks / 20); // 转换为秒，最少1秒
                    currentScapegoat.setOnFireFor(fireSeconds);
                }
            }
            
            // 只保留核心伤害转移提示（背锅人只在承受伤害时才知道）
            currentScapegoat.sendMessage(Text.literal("[ChaosMod] 💥 " + 
                com.example.config.LanguageManager.getMessage("damage_absorbed"))
                .formatted(Formatting.GOLD), true);
            
            // 只给其他人（非背锅人）发送模糊警告
            broadcastScapegoatWarning(victim.getServer());
            
            return true; // 取消原始伤害，按你的要求返回true
            
        } catch (Exception e) {
            return false;
        } finally {
            // 递归抑制严格清理：ThreadLocal必须try/finally复位
            REDIRECTING_DAMAGE.set(Boolean.FALSE);
        }
    }
    
    /**
     * 广播模糊警告（Title/ActionBar）
     */
    private static void broadcastScapegoatWarning(MinecraftServer server) {
        Text titleWarning = Text.literal("⚡").formatted(Formatting.RED, Formatting.BOLD);
        Text actionBarWarning = Text.literal("[ChaosMod] " + com.example.config.LanguageManager.getMessage("damage_transferred"))
            .formatted(Formatting.DARK_GRAY);
        
        // 给背锅人的特殊模糊警告（不让他知道是自己）
        Text scapegoatTitleWarning = Text.literal("!").formatted(Formatting.YELLOW, Formatting.BOLD);
        Text scapegoatActionBarWarning = Text.literal("[ChaosMod] " + com.example.config.LanguageManager.getMessage("system_changed"))
            .formatted(Formatting.GRAY);
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            try {
                if (player == currentScapegoat) {
                    // 给背锅人发送不同的模糊警告
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(scapegoatTitleWarning));
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(scapegoatActionBarWarning));
                } else {
                    // 给其他人发送闪电Title和伤害转移警告
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(titleWarning));
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(actionBarWarning));
                }
            } catch (Exception e) {
                // 静默处理错误
            }
        }
    }
    
    /**
     * 获取当前背锅人
     */
    public static ServerPlayerEntity getCurrentScapegoat() {
        return currentScapegoat;
    }
    
    /**
     * 手动设置背锅人（测试用）
     */
    public static void setScapegoat(ServerPlayerEntity player, MinecraftServer server) {
        currentScapegoat = player;
        
        // 持久化到PersistentState（使用新的visited机制）
        ServerWorld overworld = server.getOverworld();
        ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
        long nextRoll = overworld.getTime() + scapegoatIntervalTicks();
        state.setNewScapegoat(player.getUuid(), nextRoll);
        
        // 只发送模糊警告，不告诉背锅人身份
        broadcastScapegoatWarning(server);
    }

    public static void onIntervalChanged(MinecraftServer server) {
        if (server == null || !ChaosMod.config.damageScapegoatEnabled) return;
        ServerWorld overworld = server.getOverworld();
        ScapegoatPersistentState.get(overworld).setNextRollTick(
            overworld.getTime() + scapegoatIntervalTicks()
        );
    }
    
    /**
     * 从PersistentState恢复背锅人
     */
    public static void loadScapegoatFromPersistentState(MinecraftServer server) {
        // 静态字段会跨集成服务器世界存活，先丢弃上一服务器实例的实体引用。
        currentScapegoat = null;
        // 状态机单一真源：统一只从"主世界PersistentState"读取
        ServerWorld overworld = server.getOverworld();
        ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
        restoreOnlineScapegoat(server, state);
        
        // tick门槛固定值：nextRollTick只在状态首次创建时设定，严禁重复初始化
        if (state.getNextRollTick() == 0) {
            long currentTick = overworld.getTime();
            // 立即选择而不是等待
            long firstRollTick = currentTick + 100; // 5秒后第一次选择（快速启动）
            
            // 每次状态变更后markDirty()
            state.setNewScapegoat(Util.NIL_UUID, firstRollTick);
        }
    }
    
    /**
     * 玩家加入时即时重算
     */
    public static void onPlayerJoin(ServerPlayerEntity player, MinecraftServer server) {
        if (!ChaosMod.config.damageScapegoatEnabled) return;
        
        ServerWorld overworld = server.getOverworld();
        ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
        restoreOnlineScapegoat(server, state);
        int onlineCount = getEligiblePlayerCount(server.getPlayerManager().getPlayerList());
        // 单人世界不抽签；第二名及后续玩家加入时立即重算，避免首位玩家永久承担
        if (onlineCount >= 2 && (currentScapegoat == null || currentScapegoat != player)) {
            selectNewScapegoatWithVisited(server, state, overworld.getTime());
        }
    }
    
    /**
     * 玩家断开连接时即时重算
     */
    public static void onPlayerDisconnect(ServerPlayerEntity player, MinecraftServer server) {
        if (!ChaosMod.config.damageScapegoatEnabled) return;
        
        // 如果离线的是当前背锅人，立即置回NIL_UUID并触发重roll
        if (currentScapegoat == player) {
            currentScapegoat = null;
            ServerWorld overworld = server.getOverworld();
            ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
            state.clearCurrentScapegoat(); // 立即置回NIL_UUID
            // DISCONNECT回调阶段离线玩家可能仍在PlayerManager列表中，交给下一次server tick重选。
            state.setNewScapegoat(Util.NIL_UUID, overworld.getTime());
        }
    }
    
    /**
     * 获取连接存活的玩家数量
     */
    private static int getEligiblePlayerCount(List<ServerPlayerEntity> players) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) != null
                    && isEligiblePlayer(player)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isEligiblePlayer(ServerPlayerEntity player) {
        return player != null
            && player.isAlive()
            && !player.isCreative()
            && !player.isSpectator()
            && !player.isDisconnected();
    }

    /** 从持久化UUID恢复当前在线实体，避免重启后丢失背锅人状态。 */
    private static void restoreOnlineScapegoat(MinecraftServer server, ScapegoatPersistentState state) {
        UUID saved = state.getLastScapegoat();
        currentScapegoat = saved == null ? null : server.getPlayerManager().getPlayer(saved);
    }
    
    /**
     * 背锅人死亡时设置标记但不触发重roll
     */
    public static void onScapegoatDeath(ServerPlayerEntity player) {
        if (currentScapegoat == player) {
            // 修复：把deadUntilRespawn存进以UUID为键的PersistentState
            ServerWorld overworld = player.getServer().getOverworld();
            ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
            state.markPlayerDead(player.getUuid());
        }
    }
    
    /**
     * 背锅人复活时清除标记
     */
    public static void onScapegoatRespawn(ServerPlayerEntity player) {
        // 修复：在AFTER_RESPAWN清掉deadUntilRespawn、重绑当前实体
        ServerWorld overworld = player.getServer().getOverworld();
        ScapegoatPersistentState state = ScapegoatPersistentState.get(overworld);
        
        if (state.isPlayerDeadUntilRespawn(player.getUuid())) {
            // 清除死亡标记
            state.markPlayerRespawned(player.getUuid());
            
            // 重绑当前实体（如果这个玩家是背锅人）
            if (currentScapegoat != null && currentScapegoat.getUuid().equals(player.getUuid())) {
                currentScapegoat = player; // 重绑当前实体
            }
        }
    }
}
