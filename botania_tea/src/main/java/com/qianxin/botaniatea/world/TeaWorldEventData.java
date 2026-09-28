package com.qianxin.botaniatea.world;

import com.qianxin.botaniatea.network.FogSyncPacket;
import com.qianxin.botaniatea.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-dimension event state; event UUIDs prevent reloads from repeating a reward or disaster. */
public final class TeaWorldEventData extends SavedData {
    private static final String DATA_ID = "botania_tea_events";
    private final Map<UUID, FogZone> fogZones = new HashMap<>();
    private final Set<UUID> handledEvents = new HashSet<>();
    private boolean ancientSeedGranted;

    public static TeaWorldEventData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TeaWorldEventData::load, TeaWorldEventData::new, DATA_ID);
    }

    public static TeaWorldEventData load(CompoundTag tag) {
        TeaWorldEventData data = new TeaWorldEventData();
        ListTag fog = tag.getList("FogZones", 10);
        for (int i = 0; i < fog.size(); i++) {
            CompoundTag entry = fog.getCompound(i);
            if (!entry.hasUUID("Id")) continue;
            UUID id = entry.getUUID("Id");
            data.fogZones.put(id, new FogZone(id,
                    BlockPos.of(entry.getLong("Center")), Math.max(0, entry.getInt("Remaining"))));
        }
        ListTag handled = tag.getList("HandledEvents", 10);
        for (int i = 0; i < handled.size(); i++) {
            CompoundTag entry = handled.getCompound(i);
            if (entry.hasUUID("Id")) data.handledEvents.add(entry.getUUID("Id"));
        }
        data.ancientSeedGranted = tag.getBoolean("AncientSeedGranted");
        return data;
    }

    public boolean claimAncientSeed() {
        if (ancientSeedGranted) return false;
        ancientSeedGranted = true;
        setDirty();
        return true;
    }

    public boolean hasFog(BlockPos pos) {
        for (FogZone zone : fogZones.values()) {
            if (zone.remaining > 0 && within(pos, zone.center, 7, 2)) return true;
        }
        return false;
    }

    public void clearFog(ServerLevel level, net.minecraft.world.phys.AABB area) {
        for (FogZone zone : new ArrayList<>(fogZones.values())) {
            if (area.intersects(new net.minecraft.world.phys.AABB(zone.center.offset(-7, -2, -7), zone.center.offset(8, 3, 8)))) {
                fogZones.remove(zone.id);
                ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> level.dimension()),
                        new FogSyncPacket(zone.id, zone.center, 0, false));
                setDirty();
            }
        }
    }

    public void syncTo(ServerPlayer player) {
        for (FogZone zone : fogZones.values()) {
            if (zone.remaining > 0) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new FogSyncPacket(zone.id, zone.center, zone.remaining, true));
            }
        }
    }

    public void tick(ServerLevel level) {
        boolean changed = !fogZones.isEmpty();
        for (FogZone zone : new ArrayList<>(fogZones.values())) {
            if (--zone.remaining <= 0) {
                fogZones.values().remove(zone);
                ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> level.dimension()),
                        new FogSyncPacket(zone.id, zone.center, 0, false));
                changed = true;
            } else if (level.getGameTime() % 10 == 0) {
                showFog(level, zone);
            }
        }
        if (changed) setDirty();
    }

    private static void showFog(ServerLevel level, FogZone zone) {
        // hasFog uses inclusive block coordinates: the actual volume is 15 x 5 x 15.
        double x = zone.center.getX() - 7, y = zone.center.getY() - 2, z = zone.center.getZ() - 7;
        var boundary = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.55F, .45F, .65F), 1.0F);
        // 48 edge markers and 8 interior wisps per ten ticks, independent of volume/chunk count.
        for (int side = 0; side < 4; side++) {
            int a = side & 1, b = side >> 1;
            for (int step = 0; step < 4; step++) {
                double t = step / 4.0;
                fogParticle(level, boundary, x + 15 * t, y + 5 * a, z + 15 * b);
                fogParticle(level, boundary, x + 15 * a, y + 5 * t, z + 15 * b);
                fogParticle(level, boundary, x + 15 * a, y + 5 * b, z + 15 * t);
            }
        }
        for (int i = 0; i < 8; i++) {
            fogParticle(level, net.minecraft.core.particles.ParticleTypes.SMOKE,
                    x + level.random.nextDouble() * 15, y + level.random.nextDouble() * 5,
                    z + level.random.nextDouble() * 15);
        }
    }

    private static void fogParticle(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle,
                                    double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return;
        level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
    }

    public static void trigger(ServerLevel level, BlockPos center, UUID eventId) {
        trigger(level, center, eventId, -1);
    }

    /** A flower persists the selected event before dispatch; replay uses the same UUID and type. */
    public static void trigger(ServerLevel level, BlockPos center, UUID eventId, int selectedEvent) {
        TeaWorldEventData data = get(level);
        if (!data.handledEvents.add(eventId)) return;
        data.setDirty();
        switch (selectedEvent >= 0 && selectedEvent < 4 ? selectedEvent : level.random.nextInt(4)) {
            case 0 -> butterflyBreak(level, center);
            case 1 -> {
                FogZone zone = new FogZone(eventId, center.immutable(), 3000);
                data.fogZones.put(eventId, zone);
                ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> level.dimension()),
                        new FogSyncPacket(eventId, zone.center, zone.remaining, true));
            }
            case 2 -> forceSleep(level, center);
            case 3 -> spawnPhantom(level, center, eventId);
        }
        data.setDirty();
    }

    private static void butterflyBreak(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            if (pos.equals(center) || pos.equals(center.below())) continue;
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
            var actor = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            if (!level.mayInteract(actor, pos)) continue;
            var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, pos, state, actor);
            if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) continue;
            // destroyBlock already sends vanilla event 2001 (block fragments + the block's break sound).
            if (level.destroyBlock(pos, !state.is(com.qianxin.teaart.registry.ModBlockTags.FINISHED_TEA_BLOCKS))) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2, .2, .2, .2, 0);
            }
        }
    }

    private static void forceSleep(ServerLevel level, BlockPos center) {
        for (Player player : level.players()) {
            if (within(player.blockPosition(), center, 12, 2) && player.isAlive() && !player.isSpectator()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(level.dimensionType().hasFixedTime()
                        ? "message.botania_tea.dream_fixed_time" : "message.botania_tea.forced_dream"), true);
                if (!level.dimensionType().hasFixedTime() && player instanceof ServerPlayer recipient)
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new com.qianxin.botaniatea.network.DreamPacket(80));
            }
        }
        if (level.dimensionType().hasFixedTime()) return;
        level.setDayTime(((level.getDayTime() / 24000L) + 1L) * 24000L);
    }

    private static void spawnPhantom(ServerLevel level, BlockPos center, UUID eventId) {
        Player linked = level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(center.offset(-12,-2,-12),center.offset(13,3,13)),
                        player -> player.isAlive() && !player.isSpectator())
                .stream().min(java.util.Comparator.comparingDouble(player -> player.distanceToSqr(
                        center.getX() + .5D, center.getY() + .5D, center.getZ() + .5D))).orElse(null);
        if (linked == null) return;
        com.qianxin.botaniatea.entity.TeaMountainSpiritEntity spirit =
                com.qianxin.botaniatea.registry.ModEntities.TEA_MOUNTAIN_SPIRIT.get().create(level);
        if (spirit == null) return;
        spirit.moveTo(center.getX() + .5D, center.getY() + 1.0D, center.getZ() + .5D, 0.0F, 0.0F);
        spirit.setPhantom(linked.getUUID());
        spirit.setEventId(eventId);
        level.addFreshEntity(spirit);
    }

    private static boolean within(BlockPos pos, BlockPos center, int radius, int height) {
        return Math.abs(pos.getX() - center.getX()) <= radius
                && Math.abs(pos.getZ() - center.getZ()) <= radius
                && Math.abs(pos.getY() - center.getY()) <= height;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag fog = new ListTag();
        for (Map.Entry<UUID, FogZone> entry : fogZones.entrySet()) {
            CompoundTag value = new CompoundTag();
            value.putUUID("Id", entry.getKey());
            value.putLong("Center", entry.getValue().center.asLong());
            value.putInt("Remaining", entry.getValue().remaining);
            fog.add(value);
        }
        tag.put("FogZones", fog);
        ListTag handled = new ListTag();
        for (UUID id : handledEvents) {
            CompoundTag value = new CompoundTag();
            value.putUUID("Id", id);
            handled.add(value);
        }
        tag.put("HandledEvents", handled);
        tag.putBoolean("AncientSeedGranted", ancientSeedGranted);
        return tag;
    }

    private static final class FogZone {
        private final UUID id;
        private final BlockPos center;
        private int remaining;

        private FogZone(UUID id, BlockPos center, int remaining) {
            this.id = id;
            this.center = center;
            this.remaining = remaining;
        }
    }
}
