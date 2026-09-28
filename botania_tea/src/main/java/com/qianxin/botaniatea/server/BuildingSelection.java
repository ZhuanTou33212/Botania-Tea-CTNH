package com.qianxin.botaniatea.server;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded, finite selection. Coordinates include both endpoints. */
public final class BuildingSelection {
    public enum Mode {
        SINGLE, LINE, PLATFORM;
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
        public String key() { return "message.botania_tea.building.mode." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private Mode mode = Mode.SINGLE;
    private UUID owner;
    private BlockPos origin, first, second;
    private String dimension = "";
    private boolean running;
    private int cursor;

    public Mode mode() { return mode; }
    public UUID owner() { return owner; }
    public BlockPos first() { return first; }
    public BlockPos second() { return second; }
    public boolean active() { return owner != null; }
    public boolean running() { return active() && running; }
    public boolean ready() { return first != null && (mode == Mode.SINGLE || second != null); }
    public boolean ownedBy(UUID id) { return id.equals(owner); }
    public int cursor() { return cursor; }

    public static boolean inRange(BlockPos flower, BlockPos target) {
        return Math.abs((long) target.getX() - flower.getX()) <= 8
                && Math.abs((long) target.getZ() - flower.getZ()) <= 8
                && Math.abs((long) target.getY() - flower.getY()) <= 3;
    }

    /** A 3x3 horizontal footprint; one level of planting tolerance beside/on the container. */
    public static boolean nearContainer(BlockPos flower, BlockPos container) {
        return Math.abs((long) flower.getX() - container.getX()) <= 1
                && Math.abs((long) flower.getZ() - container.getZ()) <= 1
                && Math.abs((long) flower.getY() - container.getY()) <= 1;
    }

    public static List<BlockPos> shape(BlockPos flower, Mode mode, BlockPos a, BlockPos b) {
        if (a == null || !inRange(flower, a)) return List.of();
        if (mode == Mode.SINGLE) return List.of(a.immutable());
        if (b == null || !inRange(flower, b)) return List.of();
        var result = new ArrayList<BlockPos>();
        if (mode == Mode.PLATFORM) {
            if (a.getY() != b.getY()) return List.of();
            for (int x = Math.min(a.getX(), b.getX()); x <= Math.max(a.getX(), b.getX()); x++) {
                for (int z = Math.min(a.getZ(), b.getZ()); z <= Math.max(a.getZ(), b.getZ()); z++) {
                    result.add(new BlockPos(x, a.getY(), z));
                }
            }
        } else {
            // 3D voxel line: at most 17 cells, including diagonal/vertical lines (vertical at most 7).
            int dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), dz = b.getZ() - a.getZ();
            int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            for (int i = 0; i <= steps; i++) {
                double t = steps == 0 ? 0 : (double) i / steps;
                result.add(a.offset((int) Math.round(dx * t), (int) Math.round(dy * t), (int) Math.round(dz * t)));
            }
        }
        return List.copyOf(result);
    }

    public void begin(UUID player, BlockPos flower, String dim) {
        stop(); owner = player; origin = flower.immutable(); dimension = dim;
    }
    public boolean at(BlockPos flower, String dim) { return flower.equals(origin) && dimension.equals(dim); }
    public void setMode(Mode value) { mode = value; first = null; second = null; running = false; cursor = 0; }
    public void select(BlockPos pos) {
        if (first == null || ready()) { first = pos.immutable(); second = null; }
        else second = pos.immutable();
    }
    public void start() { running = true; cursor = 0; }
    public void advance() { cursor++; }
    public List<BlockPos> targets(BlockPos flower) { return ready() ? shape(flower, mode, first, second) : List.of(); }
    public void stop() { owner = null; first = null; second = null; origin = null; dimension = ""; running = false; cursor = 0; }

    public void save(CompoundTag config, CompoundTag runtime) {
        config.putString("BuildingMode", mode.name());
        var tag = new CompoundTag();
        if (owner != null) tag.putUUID("Owner", owner);
        if (origin != null) tag.putLong("Origin", origin.asLong());
        if (first != null) tag.putLong("First", first.asLong());
        if (second != null) tag.putLong("Second", second.asLong());
        tag.putString("Dimension", dimension); tag.putBoolean("Running", running); tag.putInt("Cursor", cursor);
        runtime.put("BuildingSelection", tag);
    }
    public void load(CompoundTag config, CompoundTag runtime) {
        stop();
        try { mode = Mode.valueOf(config.getString("BuildingMode")); }
        catch (IllegalArgumentException ignored) { mode = config.getBoolean("Line") ? Mode.LINE : Mode.SINGLE; }
        var tag = runtime.getCompound("BuildingSelection");
        if (!tag.hasUUID("Owner") || !tag.contains("Origin")) return;
        owner = tag.getUUID("Owner"); origin = BlockPos.of(tag.getLong("Origin")); dimension = tag.getString("Dimension");
        first = tag.contains("First") ? BlockPos.of(tag.getLong("First")) : null;
        second = tag.contains("Second") ? BlockPos.of(tag.getLong("Second")) : null;
        running = tag.getBoolean("Running"); cursor = Math.max(0, Math.min(289, tag.getInt("Cursor")));
        if ((first != null && !inRange(origin, first)) || (second != null && !inRange(origin, second))
                || (running && targets(origin).isEmpty())) stop();
    }
}
