package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.content.TeaFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Rebuilt on load; stores positions, never capabilities or strong references to worlds. */
public final class TeaAreaIndex {
    private static final Map<Level, Map<Long, Set<BlockPos>>> LEVELS = new WeakHashMap<>();
    public static void update(RegisteredFunctionalFlowerBlockEntity flower) {
        Level level = flower.getLevel();
        if (level == null || level.isClientSide) return;
        remove(flower);
        if (flower.isDomainActive()) LEVELS.computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(new ChunkPos(flower.getBlockPos()).toLong(), ignored -> new HashSet<>())
                .add(flower.getBlockPos().immutable());
    }
    public static void remove(RegisteredFunctionalFlowerBlockEntity flower) {
        var chunks = LEVELS.get(flower.getLevel());
        if (chunks == null) return;
        long key = new ChunkPos(flower.getBlockPos()).toLong();
        var positions = chunks.get(key);
        if (positions != null) { positions.remove(flower.getBlockPos()); if (positions.isEmpty()) chunks.remove(key); }
    }
    public static boolean contains(Level level, Vec3 point, TeaFlower kind) {
        var chunks = LEVELS.get(level);
        if (chunks == null) return false;
        int cx = BlockPos.containing(point).getX() >> 4, cz = BlockPos.containing(point).getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            for (BlockPos pos : chunks.getOrDefault(ChunkPos.asLong(x, z), Set.of())) {
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower
                && flower.flower() == kind && flower.isDomainActive()
                        && point.x >= pos.getX() - flower.effectiveRadius(kind == TeaFlower.NIGHT_EPIPHYLLUM ? 7 : 5)
                        && point.x < pos.getX() + flower.effectiveRadius(kind == TeaFlower.NIGHT_EPIPHYLLUM ? 7 : 5) + 1
                        && point.y >= pos.getY() - 2 && point.y < pos.getY() + 3
                        && point.z >= pos.getZ() - flower.effectiveRadius(kind == TeaFlower.NIGHT_EPIPHYLLUM ? 7 : 5)
                        && point.z < pos.getZ() + flower.effectiveRadius(kind == TeaFlower.NIGHT_EPIPHYLLUM ? 7 : 5) + 1) return true;
            }
        }
        return false;
    }
}
