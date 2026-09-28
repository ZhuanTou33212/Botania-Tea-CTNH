package com.qianxin.botaniatea.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Cosmetic only: the server's current visit is authoritative, never a client entity collision. */
public final class ButterflyFlowerRenderer implements BlockEntityRenderer<RegisteredGeneratingFlowerBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("botania_tea", "textures/entity/tea_butterfly.png");
    public ButterflyFlowerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(RegisteredGeneratingFlowerBlockEntity flower, float partial, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (com.qianxin.botaniatea.content.TeaFlower.isFloating(flower.getBlockState().getBlock()))
            vazkii.botania.client.render.block_entity.FloatingFlowerBlockEntityRenderer.renderFloatingIsland(
                    flower, partial, pose, buffers, overlay);
        var target = flower.butterflyVisualTarget();
        if (target == null) return;
        float progress = flower.butterflyVisualProgress(partial);
        var delta = target.subtract(flower.getBlockPos());
        pose.pushPose();
        pose.translate(.5 + delta.getX() * progress, .8 + delta.getY() * progress + Math.sin(progress * Math.PI) * .6,
                .5 + delta.getZ() * progress);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(delta.getX(), delta.getZ())));
        float flap = (float) Math.sin((flower.ticksExisted + partial) * 1.1) * 45;
        for (int wing = 0; wing < 2; wing++) {
            pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(wing == 0 ? flap : -flap));
            var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            float x0 = wing == 0 ? -.35F : 0, x1 = wing == 0 ? 0 : .35F;
            float u0 = wing * .5F, u1 = u0 + .5F;
            vertex(buffer, pose, x0, -.35F, u0, 0, light, overlay);
            vertex(buffer, pose, x1, -.35F, u1, 0, light, overlay);
            vertex(buffer, pose, x1, .35F, u1, 1, light, overlay);
            vertex(buffer, pose, x0, .35F, u0, 1, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }
    private static void vertex(VertexConsumer buffer, PoseStack pose, float x, float z, float u, float v, int light, int overlay) {
        buffer.vertex(pose.last().pose(), x, 0, z).color(255,255,255,255).uv(u,v).overlayCoords(overlay).uv2(light)
                .normal(pose.last().normal(),0,1,0).endVertex();
    }
    @Override public boolean shouldRenderOffScreen(RegisteredGeneratingFlowerBlockEntity flower) { return flower.isButterflyActive(); }
}
