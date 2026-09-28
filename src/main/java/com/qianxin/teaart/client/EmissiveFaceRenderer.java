package com.qianxin.teaart.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class EmissiveFaceRenderer {

    private static final float OFFSET = 0.001F;

    private EmissiveFaceRenderer() {}

    static void renderFace(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                           Direction direction, float minHorizontal, float maxHorizontal,
                           float minY, float maxY, float inset, float minV, float maxV) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV(minV * 16.0F);
        float v1 = sprite.getV(maxV * 16.0F);

        switch (direction) {
            case NORTH -> {
                float z = inset - OFFSET;
                vertex(consumer, matrix, normal, maxHorizontal, minY, z, u1, v1, 0, 0, -1);
                vertex(consumer, matrix, normal, minHorizontal, minY, z, u0, v1, 0, 0, -1);
                vertex(consumer, matrix, normal, minHorizontal, maxY, z, u0, v0, 0, 0, -1);
                vertex(consumer, matrix, normal, maxHorizontal, maxY, z, u1, v0, 0, 0, -1);
            }
            case SOUTH -> {
                float z = 1.0F - inset + OFFSET;
                vertex(consumer, matrix, normal, minHorizontal, minY, z, u1, v1, 0, 0, 1);
                vertex(consumer, matrix, normal, maxHorizontal, minY, z, u0, v1, 0, 0, 1);
                vertex(consumer, matrix, normal, maxHorizontal, maxY, z, u0, v0, 0, 0, 1);
                vertex(consumer, matrix, normal, minHorizontal, maxY, z, u1, v0, 0, 0, 1);
            }
            case WEST -> {
                float x = inset - OFFSET;
                vertex(consumer, matrix, normal, x, minY, minHorizontal, u1, v1, -1, 0, 0);
                vertex(consumer, matrix, normal, x, minY, maxHorizontal, u0, v1, -1, 0, 0);
                vertex(consumer, matrix, normal, x, maxY, maxHorizontal, u0, v0, -1, 0, 0);
                vertex(consumer, matrix, normal, x, maxY, minHorizontal, u1, v0, -1, 0, 0);
            }
            case EAST -> {
                float x = 1.0F - inset + OFFSET;
                vertex(consumer, matrix, normal, x, minY, maxHorizontal, u1, v1, 1, 0, 0);
                vertex(consumer, matrix, normal, x, minY, minHorizontal, u0, v1, 1, 0, 0);
                vertex(consumer, matrix, normal, x, maxY, minHorizontal, u0, v0, 1, 0, 0);
                vertex(consumer, matrix, normal, x, maxY, maxHorizontal, u1, v0, 1, 0, 0);
            }
            default -> {}
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
