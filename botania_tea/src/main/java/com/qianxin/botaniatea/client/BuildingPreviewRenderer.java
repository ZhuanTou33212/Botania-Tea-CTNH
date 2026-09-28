package com.qianxin.botaniatea.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.server.BuildingSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import java.util.List;

/** Preview reads server-synced selection only; never edits the world or loads chunks. */
public final class BuildingPreviewRenderer implements BlockEntityRenderer<RegisteredFunctionalFlowerBlockEntity> {
    public BuildingPreviewRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(RegisteredFunctionalFlowerBlockEntity flower, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (TeaFlower.isFloating(flower.getBlockState().getBlock()))
            vazkii.botania.client.render.block_entity.FloatingFlowerBlockEntityRenderer.renderFloatingIsland(
                    flower, partialTick, pose, buffers, overlay);
        var client = Minecraft.getInstance();
        var player = client.player;
        var selection = flower.extraWork().buildingSelection();
        if (flower.flower() != TeaFlower.BUILDING_BEGONIA || player == null || !selection.ownedBy(player.getUUID())
                || player.distanceToSqr(flower.getBlockPos().getCenter()) > 24 * 24) return;
        BlockPos origin = flower.getBlockPos();
        List<BlockPos> targets = selection.targets(origin);
        if (!selection.ready()) {
            var eye = player.getEyePosition(partialTick);
            var hit = flower.getLevel().clip(new ClipContext(eye, eye.add(player.getViewVector(partialTick).scale(8)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                var target = hit.getBlockPos().relative(hit.getDirection());
                targets = selection.first() == null ? BuildingSelection.shape(origin, BuildingSelection.Mode.SINGLE, target, null)
                        : BuildingSelection.shape(origin, selection.mode(), selection.first(), target);
            }
        }
        // Finite geometry: <=289 target cells and one 17x7x17 enclosing box.
        for (int i = selection.running() ? selection.cursor() : 0; i < targets.size(); i++) {
            BlockPos target = targets.get(i);
            boolean valid = !target.equals(origin) && flower.getLevel().hasChunkAt(target)
                    && flower.getLevel().getBlockState(target).isAir() && flower.getLevel().getFluidState(target).isEmpty();
            float red = valid ? .3F : 1F, green = valid ? 1F : .25F;
            AABB box = new AABB(target.subtract(origin)).inflate(.002);
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, red, green, .7F, .8F);
            LevelRenderer.addChainedFilledBoxVertices(pose, buffers.getBuffer(RenderType.debugFilledBox()),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, .7F, .12F);
        }
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(-8, -3, -8, 9, 4, 9), .3F, .8F, .65F, .3F);
    }

    @Override public boolean shouldRenderOffScreen(RegisteredFunctionalFlowerBlockEntity flower) {
        return flower.extraWork().buildingSelection().active();
    }
}
