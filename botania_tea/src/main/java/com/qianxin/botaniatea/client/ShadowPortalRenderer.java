package com.qianxin.botaniatea.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qianxin.botaniatea.entity.ShadowPortalEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ShadowPortalRenderer extends EntityRenderer<ShadowPortalEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("botania_tea", "textures/entity/shadow_portal.png");

    public ShadowPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(ShadowPortalEntity portal, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180F - portal.getYRot()));
        pose.scale(1.25F, 2.25F, 1F);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(buffer, pose, -.5F, 0F, 0F, 1F);
        vertex(buffer, pose, .5F, 0F, 1F, 1F);
        vertex(buffer, pose, .5F, 1F, 1F, 0F);
        vertex(buffer, pose, -.5F, 1F, 0F, 0F);
        pose.popPose();
        super.render(portal, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack pose, float x, float y, float u, float v) {
        buffer.vertex(pose.last().pose(), x, y, 0).color(255, 255, 255, 255).uv(u, v).overlayCoords(0)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(), 0, 0, 1).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(ShadowPortalEntity portal) { return TEXTURE; }
}
