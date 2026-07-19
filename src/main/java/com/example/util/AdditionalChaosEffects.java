package com.example.util;

import com.example.ChaosMod;
import com.example.config.LanguageManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** 第52-55项服务端权威效果。 */
public final class AdditionalChaosEffects {
    private static final long MAGMA_DURATION_TICKS = 200L;
    private static final long DELAY_TICKS = 100L;
    private static final float TIME_REBOUND_DAMAGE = 4.0F;

    private static MinecraftServer stateServer;
    private static long lastMagmaBetrayalTick;
    private static long lastTimeReboundTick;
    private static long lastBurdenCollapseTick;

    private static final Map<BlockLocation, MagmaReplacement> MAGMA_REPLACEMENTS = new HashMap<>();
    private static final Map<UUID, PendingRebound> PENDING_REBOUNDS = new HashMap<>();
    private static final Map<UUID, PendingBurden> PENDING_BURDENS = new HashMap<>();

    private AdditionalChaosEffects() {}

    public static void handleWeaponSlip(
            ServerPlayerEntity attacker,
            Entity victim,
            DamageSource source,
            boolean damageSucceeded
    ) {
        if (!ChaosMod.config.weaponSlipEnabled || !damageSucceeded) return;
        if (!(victim instanceof Monster) || attacker.isCreative() || attacker.isSpectator()) return;
        if (!source.isDirect() || source.getSource() != attacker) return;

        ItemStack mainHand = attacker.getMainHandStack();
        if (!isSupportedWeapon(mainHand.getItem())) return;
        if (ThreadLocalRandom.current().nextInt(100) >= ChaosMod.config.weaponSlipChancePercent) return;

        int selectedSlot = attacker.getInventory().selectedSlot;
        ItemStack droppedStack = attacker.getInventory().getStack(selectedSlot).copyAndEmpty();
        attacker.getInventory().markDirty();
        if (droppedStack.isEmpty()) return;

        ItemEntity dropped = attacker.dropItem(droppedStack, false, true);
        if (dropped == null) {
            attacker.getInventory().setStack(selectedSlot, droppedStack);
            attacker.getInventory().markDirty();
        } else {
            dropped.setVelocity(Vec3d.ZERO);
        }
    }

    private static boolean isSupportedWeapon(Item item) {
        return item instanceof SwordItem
            || item instanceof AxeItem
            || item instanceof MaceItem
            || item instanceof TridentItem;
    }

    public static void tick(MinecraftServer server) {
        ensureServer(server);
        long now = server.getTicks();

        tickMagmaBetrayal(server, now);
        tickTimeRebound(server, now);
        tickBurdenCollapse(server, now);
    }

    private static void ensureServer(MinecraftServer server) {
        if (stateServer == server) return;
        restoreAllMagmaBlocks();
        stateServer = server;
        MAGMA_REPLACEMENTS.clear();
        PENDING_REBOUNDS.clear();
        PENDING_BURDENS.clear();
        long now = server.getTicks();
        lastMagmaBetrayalTick = now;
        lastTimeReboundTick = now;
        lastBurdenCollapseTick = now;
    }

    private static List<ServerPlayerEntity> eligiblePlayers(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList().stream()
            .filter(ServerPlayerEntity::isAlive)
            .filter(player -> !player.isCreative() && !player.isSpectator())
            .filter(player -> !player.isDisconnected() && !player.isRemoved())
            .toList();
    }

    private static void tickMagmaBetrayal(MinecraftServer server, long now) {
        restoreExpiredMagmaBlocks(server, now);
        if (!ChaosMod.config.magmaBetrayalEnabled) {
            restoreAllMagmaBlocks();
            lastMagmaBetrayalTick = now;
            return;
        }

        long interval = ChaosMod.config.magmaBetrayalIntervalSeconds * 20L;
        if (now - lastMagmaBetrayalTick < interval) return;
        lastMagmaBetrayalTick = now;

        List<ServerPlayerEntity> players = eligiblePlayers(server);
        if (players.isEmpty()) return;
        ServerPlayerEntity player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        tryPlaceMagma(player, now);
    }

    private static void tryPlaceMagma(ServerPlayerEntity player, long now) {
        if (player.isTouchingWater() || player.isSubmergedInWater() || player.isInLava()) return;
        ServerWorld world = player.getServerWorld();
        BlockPos pos = BlockPos.ofFloored(player.getX(), player.getY() - 0.2D, player.getZ());
        BlockState original = world.getBlockState(pos);
        if (original.isAir() || original.isReplaceable() || !original.getFluidState().isEmpty()) return;
        if (!original.blocksMovement() || !original.isSolidBlock(world, pos) || original.hasBlockEntity()) return;
        if (original.isOf(Blocks.BEDROCK) || original.isOf(Blocks.MAGMA_BLOCK)) return;

        BlockLocation location = new BlockLocation(world.getRegistryKey(), pos.toImmutable());
        if (MAGMA_REPLACEMENTS.containsKey(location)) return;
        if (world.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState(), Block.NOTIFY_ALL)) {
            MAGMA_REPLACEMENTS.put(location, new MagmaReplacement(original, now + MAGMA_DURATION_TICKS));
        }
    }

    private static void restoreExpiredMagmaBlocks(MinecraftServer server, long now) {
        Iterator<Map.Entry<BlockLocation, MagmaReplacement>> iterator = MAGMA_REPLACEMENTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockLocation, MagmaReplacement> entry = iterator.next();
            if (now < entry.getValue().restoreTick()) continue;
            restoreMagmaBlock(server, entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    private static void restoreAllMagmaBlocks() {
        if (stateServer == null) return;
        for (Map.Entry<BlockLocation, MagmaReplacement> entry : MAGMA_REPLACEMENTS.entrySet()) {
            restoreMagmaBlock(stateServer, entry.getKey(), entry.getValue());
        }
        MAGMA_REPLACEMENTS.clear();
    }

    private static void restoreMagmaBlock(
            MinecraftServer server,
            BlockLocation location,
            MagmaReplacement replacement
    ) {
        ServerWorld world = server.getWorld(location.worldKey());
        if (world != null && world.getBlockState(location.pos()).isOf(Blocks.MAGMA_BLOCK)) {
            world.setBlockState(location.pos(), replacement.originalState(), Block.NOTIFY_ALL);
        }
    }

    private static void tickTimeRebound(MinecraftServer server, long now) {
        if (!ChaosMod.config.timeReboundEnabled) {
            PENDING_REBOUNDS.clear();
            lastTimeReboundTick = now;
            return;
        }

        Iterator<Map.Entry<UUID, PendingRebound>> iterator = PENDING_REBOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRebound> entry = iterator.next();
            PendingRebound rebound = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (!isEligible(player) || player.getId() != rebound.entityId()
                    || !player.getServerWorld().getRegistryKey().equals(rebound.worldKey())) {
                iterator.remove();
                continue;
            }
            if (now < rebound.executeTick()) continue;
            iterator.remove();

            if (!isSafeDestination(player, rebound.position())) continue;

            Entity teleported = player.teleportTo(new TeleportTarget(
                player.getServerWorld(), rebound.position(), Vec3d.ZERO,
                rebound.yaw(), rebound.pitch(), TeleportTarget.NO_OP
            ));
            if (teleported != null) {
                player.setVelocity(Vec3d.ZERO);
                player.fallDistance = 0.0F;
                DamageRouting.applyDirectDamage(
                    player, player.getServerWorld().getDamageSources().generic(), TIME_REBOUND_DAMAGE
                );
            }
        }

        long interval = ChaosMod.config.timeReboundIntervalSeconds * 20L;
        if (now - lastTimeReboundTick >= interval) {
            lastTimeReboundTick = now;
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (!players.isEmpty()) {
                ServerPlayerEntity player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                if (isSafeDestination(player, player.getPos())) {
                    PENDING_REBOUNDS.put(player.getUuid(), new PendingRebound(
                        player.getServerWorld().getRegistryKey(), player.getPos(),
                        player.getYaw(), player.getPitch(), player.getId(), now + DELAY_TICKS
                    ));
                }
            }
        }
    }

    private static boolean isSafeDestination(ServerPlayerEntity player, Vec3d position) {
        ServerWorld world = player.getServerWorld();
        BlockPos supportPos = BlockPos.ofFloored(position.x, position.y - 0.2D, position.z);
        BlockState support = world.getBlockState(supportPos);
        if (support.isAir() || !support.getFluidState().isEmpty()
                || support.getCollisionShape(world, supportPos).isEmpty()) {
            return false;
        }
        Vec3d offset = position.subtract(player.getPos());
        return world.isSpaceEmpty(player, player.getBoundingBox().offset(offset));
    }

    private static void tickBurdenCollapse(MinecraftServer server, long now) {
        if (!ChaosMod.config.burdenCollapseEnabled) {
            PENDING_BURDENS.clear();
            lastBurdenCollapseTick = now;
            return;
        }

        Iterator<Map.Entry<UUID, PendingBurden>> iterator = PENDING_BURDENS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingBurden> entry = iterator.next();
            PendingBurden burden = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (!isEligible(player) || player.getId() != burden.entityId()) {
                iterator.remove();
                continue;
            }
            if (now < burden.executeTick()) continue;
            iterator.remove();

            int occupiedSlots = countOccupiedSlots(player);
            float damage = Math.min(10.0F, 2.0F + (occupiedSlots / 5) * 1.0F);
            DamageRouting.applyDirectDamage(player, player.getServerWorld().getDamageSources().generic(), damage);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
            player.sendMessage(
                Text.literal(String.format(
                    LanguageManager.getMessage("burden_collapse_result"), occupiedSlots, damage / 2.0F
                )).formatted(Formatting.RED),
                true
            );
        }

        long interval = ChaosMod.config.burdenCollapseIntervalSeconds * 20L;
        if (now - lastBurdenCollapseTick >= interval) {
            lastBurdenCollapseTick = now;
            List<ServerPlayerEntity> players = eligiblePlayers(server);
            if (!players.isEmpty()) {
                ServerPlayerEntity player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                PENDING_BURDENS.put(
                    player.getUuid(), new PendingBurden(player.getId(), now + DELAY_TICKS)
                );
                player.sendMessage(
                    Text.literal(LanguageManager.getMessage("burden_collapse_warning"))
                        .formatted(Formatting.DARK_RED, Formatting.BOLD),
                    true
                );
            }
        }
    }

    private static int countOccupiedSlots(ServerPlayerEntity player) {
        int occupied = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) occupied++;
        }
        return occupied;
    }

    private static boolean isEligible(ServerPlayerEntity player) {
        return player != null && player.isAlive()
            && !player.isCreative() && !player.isSpectator()
            && !player.isDisconnected() && !player.isRemoved();
    }

    public static void cleanupPlayer(UUID playerId) {
        PENDING_REBOUNDS.remove(playerId);
        PENDING_BURDENS.remove(playerId);
    }

    public static void shutdown(MinecraftServer server) {
        if (stateServer != server) return;
        restoreAllMagmaBlocks();
        MAGMA_REPLACEMENTS.clear();
        PENDING_REBOUNDS.clear();
        PENDING_BURDENS.clear();
        stateServer = null;
    }

    private record BlockLocation(RegistryKey<World> worldKey, BlockPos pos) {}
    private record MagmaReplacement(BlockState originalState, long restoreTick) {}
    private record PendingRebound(
        RegistryKey<World> worldKey, Vec3d position, float yaw, float pitch,
        int entityId, long executeTick
    ) {}
    private record PendingBurden(int entityId, long executeTick) {}
}
