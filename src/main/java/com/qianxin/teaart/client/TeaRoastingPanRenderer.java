package com.qianxin.teaart.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qianxin.teaart.block.TeaRoastingPanBlock;
import com.qianxin.teaart.block.entity.TeaRoastingPanBlockEntity;

public class TeaRoastingPanRenderer implements BlockEntityRenderer<TeaRoastingPanBlockEntity> {

    private static final ResourceLocation FIREBOX_TEXTURE = ResourceLocation
            .tryParse("minecraft:block/campfire_log_lit");

    public TeaRoastingPanRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TeaRoastingPanBlockEntity pan, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = pan.getBlockState();
        EmissiveFaceRenderer.renderFace(poseStack, buffer, FIREBOX_TEXTURE, state.getValue(TeaRoastingPanBlock.FACING),
                4.0F / 16.0F, 12.0F / 16.0F, 2.0F / 16.0F, 7.0F / 16.0F, 2.0F / 16.0F, 0.0F, 1.0F);
    }
}
