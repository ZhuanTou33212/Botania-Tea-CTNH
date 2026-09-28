package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.config.TeaGardenConfig;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.network.HomingOptionsPacket;
import com.qianxin.botaniatea.network.ModNetwork;
import com.qianxin.botaniatea.world.HomingNetworkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** Homing owns its links in SavedData; the flower's legacy arbitrary Target is never consulted. */
public final class HomingService {
    public static final double ENTRY_RADIUS = 1.5;
    public static final int COOLDOWN_TICKS = 40;
    private static final String COOLDOWN = "TeaTeleportUntil";
    private static final String ARRIVAL = "TeaHomingArrival";

    private HomingService() {}

    /** Hook from BE.onLoad, after super. Also discovers old-world flowers when their chunks load. */
    public static void register(RegisteredFunctionalFlowerBlockEntity flower) {
        if (flower.flower() == TeaFlower.HOMING_MORNING_GLORY && flower.getLevel() instanceof ServerLevel level)
            HomingNetworkData.get(level).register(flower.getBlockPos());
    }

    /** Hook on actual block replacement, before super.onRemove; NOT on chunk unload/setRemoved. */
    public static void remove(ServerLevel level, BlockPos pos) { HomingNetworkData.get(level).remove(pos); }

    @Nullable public static RegisteredFunctionalFlowerBlockEntity loadedFlower(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower
                && !flower.isRemoved() && flower.flower() == TeaFlower.HOMING_MORNING_GLORY ? flower : null;
    }

    /** Includes unloaded known nodes; checks only already-loaded chunks for stale entries. */
    public static List<BlockPos> getPositions(ServerLevel level) {
        var data = HomingNetworkData.get(level);
        for (BlockPos pos : data.getPositions()) {
            if (level.hasChunkAt(pos) && loadedFlower(level, pos) == null) data.remove(pos);
        }
        return data.getPositions().stream().sorted(Comparator.<BlockPos>comparingInt(pos -> pos.getX())
                .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ)).toList();
    }

    public static boolean selectDestination(ServerLevel level, BlockPos source, @Nullable BlockPos target) {
        var flower = loadedFlower(level, source);
        if (flower == null || !level.getWorldBorder().isWithinBounds(source) || source.equals(target)) return false;
        register(flower);
        if (target != null) {
            var other = loadedFlower(level, target);
            if (other == null || !level.getWorldBorder().isWithinBounds(target)) return false;
            register(other);
        }
        return HomingNetworkData.get(level).selectDestination(source, target);
    }

    public static boolean canConfigure(ServerPlayer player, ResourceLocation dimension, BlockPos source) {
        var level = player.serverLevel();
        return player.isAlive() && !player.isSpectator() && player.mayBuild()
                && level.dimension().location().equals(dimension)
                && player.distanceToSqr(Vec3.atCenterOf(source)) <= 64
                && level.getWorldBorder().isWithinBounds(source) && loadedFlower(level, source) != null
                && level.mayInteract(player, source);
    }

    public static boolean selectDestination(ServerPlayer player, ResourceLocation dimension,
                                            BlockPos source, @Nullable BlockPos target) {
        return canConfigure(player, dimension, source) && selectDestination(player.serverLevel(), source, target);
    }

    public static boolean rename(ServerPlayer player, ResourceLocation dimension, BlockPos source, String name) {
        return canConfigure(player, dimension, source) && HomingNetworkData.get(player.serverLevel()).rename(source, name);
    }

    private static String readiness(RegisteredFunctionalFlowerBlockEntity flower) {
        var level = (ServerLevel) flower.getLevel();
        if (HomingNetworkData.get(level).getDestination(flower.getBlockPos()) == null) return "未配置目标";
        if (destination(flower) == null) return "目标未加载或已失效";
        if (!level.hasNeighborSignal(flower.getBlockPos())) return "等待红石信号";
        if (flower.getMana() < TeaGardenConfig.TELEPORT_COST.get()) return "魔力不足：" + flower.getMana()
                + "/" + TeaGardenConfig.TELEPORT_COST.get() + "；请绑定魔力池";
        if (!safeDestination(flower)) return "目标落点受阻或脚下不安全";
        return "就绪：进入花附近传送，每次" + TeaGardenConfig.TELEPORT_COST.get() + "魔力";
    }

    /** Right-click hook: call on the server; return sidedSuccess from the block's use method. */
    public static void openSelection(ServerPlayer player, BlockPos source) {
        sendOptions(player, player.serverLevel().dimension().location(), source, 0, "");
    }

    public static void sendOptions(ServerPlayer player, ResourceLocation dimension, BlockPos source, int requestedPage, String status) {
        if (!canConfigure(player, dimension, source)) return;
        var level = player.serverLevel();
        register(loadedFlower(level, source));
        var data = HomingNetworkData.get(level);
        var positions = getPositions(level).stream().filter(pos -> !pos.equals(source)).toList();
        int pages = Math.max(1, (positions.size() + HomingOptionsPacket.PAGE_SIZE - 1) / HomingOptionsPacket.PAGE_SIZE);
        int page = Mth.clamp(requestedPage, 0, pages - 1);
        var options = positions.stream().skip((long) page * HomingOptionsPacket.PAGE_SIZE)
                .limit(HomingOptionsPacket.PAGE_SIZE).map(pos -> new HomingOptionsPacket.Option(pos,
                        loadedFlower(level, pos) != null && level.getWorldBorder().isWithinBounds(pos), data.getName(pos))).toList();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HomingOptionsPacket(
                dimension, source, data.getDestination(source), page, pages, options, status,
                data.getName(source), data.getName(data.getDestination(source)), readiness(loadedFlower(level, source))));
    }

    @Nullable private static BlockPos destination(RegisteredFunctionalFlowerBlockEntity flower) {
        if (!(flower.getLevel() instanceof ServerLevel level) || loadedFlower(level, flower.getBlockPos()) != flower) return null;
        register(flower);
        var data = HomingNetworkData.get(level);
        BlockPos target = data.getDestination(flower.getBlockPos());
        if (target == null || !level.hasChunkAt(target)) return null;
        if (loadedFlower(level, target) == null) { data.remove(target); return null; }
        return target;
    }

    public static boolean safeDestination(RegisteredFunctionalFlowerBlockEntity flower) {
        BlockPos target = destination(flower);
        if (target == null) return false;
        Vec3 feet = landingFeet((ServerLevel) flower.getLevel(), target);
        if (feet == null) return false;
        return safeLanding((ServerLevel) flower.getLevel(), null,
                new AABB(feet.x - .3, feet.y, feet.z - .3, feet.x + .3, feet.y + 1.8, feet.z + .3));
    }

    /** The whole collision envelope (including the collision iterator's one-block margin) must be loaded. */
    private static boolean loadedArea(ServerLevel level, AABB box) {
        for (int x = Mth.floor(box.minX - 1) >> 4; x <= Mth.floor(box.maxX + 1) >> 4; x++)
            for (int z = Mth.floor(box.minZ - 1) >> 4; z <= Mth.floor(box.maxZ + 1) >> 4; z++)
                if (!level.hasChunk(x, z)) return false;
        return true;
    }

    private static boolean dangerous(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE)
                || state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT);
    }

    @Nullable private static Vec3 landingFeet(ServerLevel level, BlockPos target) {
        BlockPos support = target.below();
        if (!level.hasChunkAt(support)) return null;
        var shape = level.getBlockState(support).getCollisionShape(level, support);
        if (shape.isEmpty()) return null;
        return new Vec3(target.getX() + .5, support.getY() + shape.max(Direction.Axis.Y), target.getZ() + .5);
    }

    private static boolean safeLanding(ServerLevel level, @Nullable LivingEntity entity, AABB box) {
        if (box.minY - 1 < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box) || !loadedArea(level, box)) return false;
        // Require support beneath the entire footprint, also for wide mobs such as ravagers.
        int supportY = Mth.floor(box.minY - 1e-6);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX + 1e-6), supportY,
                Mth.floor(box.minZ + 1e-6), Mth.floor(box.maxX - 1e-6), supportY, Mth.floor(box.maxZ - 1e-6))) {
            BlockState state = level.getBlockState(pos);
            if (dangerous(state) || !level.getFluidState(pos).isEmpty()) return false;
            // Check the actual top surface: farmland/path are 15/16 high, not full-height sturdy faces.
            boolean supported = state.getCollisionShape(level, pos).toAabbs().stream().anyMatch(surface ->
                    Math.abs(pos.getY() + surface.maxY - box.minY) < 1e-6
                    && pos.getX() + surface.minX <= Math.max(pos.getX(), box.minX) + 1e-6
                    && pos.getX() + surface.maxX >= Math.min(pos.getX() + 1, box.maxX) - 1e-6
                    && pos.getZ() + surface.minZ <= Math.max(pos.getZ(), box.minZ) + 1e-6
                    && pos.getZ() + surface.maxZ >= Math.min(pos.getZ() + 1, box.maxZ) - 1e-6);
            if (!supported) return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX - 1e-6, box.maxY - 1e-6, box.maxZ - 1e-6))) {
            if (dangerous(level.getBlockState(pos)) || !level.getFluidState(pos).isEmpty()) return false;
        }
        return level.noCollision(entity, box);
    }

    public static AABB entryArea(BlockPos pos) { return new AABB(pos).inflate(ENTRY_RADIUS); }

    private static AABB entryArea(RegisteredFunctionalFlowerBlockEntity flower) {
        return new AABB(flower.getBlockPos()).inflate(flower.isChibi() ? ENTRY_RADIUS / 2 : ENTRY_RADIUS);
    }

    /** Called by the independent LivingTick hook, so leaving and re-entering the arrival area works. */
    public static void updateArrival(LivingEntity entity) {
        var persistent = entity.getPersistentData();
        if (!persistent.contains(ARRIVAL, 10)) return;
        var arrival = persistent.getCompound(ARRIVAL);
        if (!entity.level().dimension().location().toString().equals(arrival.getString("Dimension"))
                || !entity.getBoundingBox().intersects(entryArea(BlockPos.of(arrival.getLong("Pos")))))
            persistent.remove(ARRIVAL);
    }

    public static boolean teleport(RegisteredFunctionalFlowerBlockEntity flower) {
        if (!(flower.getLevel() instanceof ServerLevel level) || flower.flower() != TeaFlower.HOMING_MORNING_GLORY
                || !level.hasNeighborSignal(flower.getBlockPos())) return false;
        BlockPos target = destination(flower);
        int cost = TeaGardenConfig.TELEPORT_COST.get();
        if (target == null || flower.getMana() < cost) return false;
        boolean worked = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, entryArea(flower))) {
            if (flower.getMana() < cost) break;
            if (teleportEntity(flower, entity, target)) worked = true;
        }
        return worked;
    }

    /** Public for tests and other server callers; enforces the same entry/redstone/mana checks. */
    public static boolean teleportEntity(RegisteredFunctionalFlowerBlockEntity flower, LivingEntity entity) {
        BlockPos target = destination(flower);
        return target != null && teleportEntity(flower, entity, target);
    }

    private static boolean teleportEntity(RegisteredFunctionalFlowerBlockEntity flower, LivingEntity entity, BlockPos target) {
        if (!(flower.getLevel() instanceof ServerLevel level) || entity.level() != level
                || !level.hasNeighborSignal(flower.getBlockPos()) || flower.getMana() < TeaGardenConfig.TELEPORT_COST.get()
                || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()
                || !entity.getBoundingBox().intersects(entryArea(flower))) return false;
        updateArrival(entity);
        if (entity.getPersistentData().getLong(COOLDOWN) > level.getGameTime()
                || entity.getPersistentData().contains(ARRIVAL, 10)) return false;
        Vec3 feet = landingFeet(level, target);
        if (feet == null) return false;
        AABB box = entity.getBoundingBox().move(feet.subtract(entity.position()));
        if (!safeLanding(level, entity, box)) return false;
        // Move each LivingEntity independently. Passengers are not carried unchecked or free of charge.
        entity.stopRiding();
        entity.ejectPassengers();
        if (entity.isPassenger() || entity.isVehicle()) return false;
        if (entity instanceof ServerPlayer player) {
            if (player.isSleeping()) player.stopSleepInBed(true, true);
            player.teleportTo(level, feet.x, feet.y, feet.z, player.getYRot(), player.getXRot());
        } else entity.teleportTo(feet.x, feet.y, feet.z);
        if (entity.level() != level || entity.position().distanceToSqr(feet) > 1e-6) return false;
        entity.fallDistance = 0;
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof Mob mob) mob.getNavigation().stop();
        entity.getPersistentData().putLong(COOLDOWN, level.getGameTime() + COOLDOWN_TICKS);
        var arrival = new CompoundTag();
        arrival.putString("Dimension", level.dimension().location().toString());
        arrival.putLong("Pos", target.asLong());
        entity.getPersistentData().put(ARRIVAL, arrival);
        flower.addMana(-TeaGardenConfig.TELEPORT_COST.get());
        flower.setChanged();
        return true;
    }
}
