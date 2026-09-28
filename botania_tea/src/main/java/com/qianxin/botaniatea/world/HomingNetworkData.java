package com.qianxin.botaniatea.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-dimension registry and links. Unloading a chunk must never remove its nodes. */
public final class HomingNetworkData extends SavedData {
    public static final int MAX_NAME_LENGTH = 32;
    private final Map<BlockPos, BlockPos> nodes = new HashMap<>();
    private final Map<BlockPos, String> names = new HashMap<>();

    public static HomingNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(HomingNetworkData::load,
                HomingNetworkData::new, "botania_tea_homing");
    }

    public List<BlockPos> getPositions() { return List.copyOf(nodes.keySet()); }
    public boolean contains(BlockPos pos) { return nodes.containsKey(pos); }
    @Nullable public BlockPos getDestination(BlockPos source) { return nodes.get(source); }
    public String getName(BlockPos pos) { return names.getOrDefault(pos, ""); }

    /** Empty restores the coordinate-only default; duplicate names are distinguished by coordinates. */
    public boolean rename(BlockPos pos, String name) {
        if (!contains(pos) || name.length() > MAX_NAME_LENGTH
                || name.chars().anyMatch(c -> Character.isISOControl(c) || c == '\u00a7')) return false;
        name = name.strip();
        if (name.isEmpty()) names.remove(pos); else names.put(pos.immutable(), name);
        setDirty(); return true;
    }

    public void register(BlockPos pos) {
        if (!nodes.containsKey(pos)) {
            nodes.put(pos.immutable(), null);
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (!nodes.containsKey(pos)) return;
        nodes.remove(pos);
        names.remove(pos);
        nodes.replaceAll((source, target) -> pos.equals(target) ? null : target);
        setDirty();
    }

    /** Structural check only; callers must validate loaded blocks and player authority first. */
    public boolean selectDestination(BlockPos source, @Nullable BlockPos target) {
        if (!contains(source) || source.equals(target) || target != null && !contains(target)) return false;
        if (!java.util.Objects.equals(nodes.get(source), target)) {
            nodes.put(source.immutable(), target == null ? null : target.immutable());
            setDirty();
        }
        return true;
    }

    public static HomingNetworkData load(CompoundTag tag) {
        var data = new HomingNetworkData();
        for (var entry : tag.getList("Nodes", 10)) {
            var node = (CompoundTag) entry;
            if (!node.contains("Pos", 4)) continue;
            data.nodes.put(BlockPos.of(node.getLong("Pos")), node.contains("Target", 4)
                    ? BlockPos.of(node.getLong("Target")) : null);
            String name = node.getString("Name");
            if (!name.isEmpty()) data.rename(BlockPos.of(node.getLong("Pos")), name);
        }
        data.nodes.replaceAll((source, target) -> source.equals(target) || !data.nodes.containsKey(target) ? null : target);
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        nodes.forEach((pos, target) -> {
            var node = new CompoundTag();
            node.putLong("Pos", pos.asLong());
            if (!getName(pos).isEmpty()) node.putString("Name", getName(pos));
            if (target != null) node.putLong("Target", target.asLong());
            list.add(node);
        });
        tag.put("Nodes", list);
        return tag;
    }
}
