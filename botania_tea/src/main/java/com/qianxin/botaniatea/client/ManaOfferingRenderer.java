package com.qianxin.botaniatea.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public final class ManaOfferingRenderer implements BlockEntityRenderer<ManaOfferingBlockEntity> {
    public ManaOfferingRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ManaOfferingBlockEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var stack = table.inventory.getStackInSlot(0);
        if (stack.isEmpty() || table.getLevel() == null) return;
        pose.pushPose();
        pose.translate(.5, 1.25, .5);
        pose.mulPose(Axis.YP.rotationDegrees((table.getLevel().getGameTime() % 720 + partial) * .5F));
        // Project the GUI item presentation onto a thin plane, including block items.
        var normal=new org.joml.Matrix3f(pose.last().normal());
        pose.scale(.65F, .65F, .001F);
        // Flatten positions, not lighting normals. The inverse scale otherwise blacks out GUI faces.
        pose.last().normal().set(normal);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, overlay, pose, buffers, table.getLevel(), 0);
        pose.popPose();
    }
}
