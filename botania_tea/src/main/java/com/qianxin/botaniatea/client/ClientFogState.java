package com.qianxin.botaniatea.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientFogState {
    private static final Map<UUID, Zone> ZONES = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;

    private static void checkWorld() {
        var current = net.minecraft.client.Minecraft.getInstance().level;
        if (world != current) { ZONES.clear(); world = current; }
    }

    public static void apply(UUID id, BlockPos center, int remaining, boolean active) {
        checkWorld();
        if (active && remaining > 0) ZONES.put(id, new Zone(center, remaining));
        else ZONES.remove(id);
    }

    public static void tick() {
        checkWorld();
        if (world == null) return;
        ZONES.values().removeIf(zone -> --zone.remaining <= 0);
    }

    public static boolean contains(BlockPos pos) {
        checkWorld();
        return ZONES.values().stream().anyMatch(zone -> Math.abs(pos.getX() - zone.center.getX()) <= 7
                && Math.abs(pos.getZ() - zone.center.getZ()) <= 7
                && Math.abs(pos.getY() - zone.center.getY()) <= 2);
    }

    /** Visible from outside too; geometry follows the same inclusive block volume as contains(). */
    public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        checkWorld();
        if (world == null || ZONES.isEmpty()) return;
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (Zone zone : ZONES.values()) {
            if (zone.remaining <= 0 || zone.center.distToCenterSqr(camera) > 128 * 128) continue;
            var box = new net.minecraft.world.phys.AABB(zone.center.offset(-7, -2, -7), zone.center.offset(8, 3, 8));
            if (!event.getFrustum().isVisible(box)) continue;
            net.minecraft.client.renderer.LevelRenderer.addChainedFilledBoxVertices(pose,
                    buffers.getBuffer(net.minecraft.client.renderer.RenderType.debugFilledBox()),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, .4F, .32F, .48F, .10F);
            net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,
                    buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines()), box, .55F, .45F, .65F, .5F);
        }
        buffers.endBatch(net.minecraft.client.renderer.RenderType.debugFilledBox());
        buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());
        pose.popPose();
    }

    private static final class Zone {
        private final BlockPos center;
        private int remaining;

        private Zone(BlockPos center, int remaining) {
            this.center = center;
            this.remaining = remaining;
        }
    }

    private ClientFogState() {}
}
