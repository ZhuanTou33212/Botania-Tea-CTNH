package com.qianxin.teaart.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.block.ScentingBoxBlock;
import com.qianxin.teaart.block.entity.ScentingBoxBlockEntity;

public class ScentingBoxRenderer implements BlockEntityRenderer<ScentingBoxBlockEntity> {

    private static final ResourceLocation BURNING_TEXTURE = ResourceLocation
            .tryParse(TeaArtMod.MOD_ID + ":block/scenting_front_burning");

    public ScentingBoxRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ScentingBoxBlockEntity box, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = box.getBlockState();
        if (!state.getValue(ScentingBoxBlock.LIT) || state.getValue(ScentingBoxBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        EmissiveFaceRenderer.renderFace(poseStack, buffer, BURNING_TEXTURE, state.getValue(ScentingBoxBlock.FACING),
                0.0F, 1.0F, 0.0F, 14.0F / 16.0F, 0.0F, 1.0F / 16.0F, 15.0F / 16.0F);
    }
}
