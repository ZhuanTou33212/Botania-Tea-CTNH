package com.qianxin.botaniatea.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianxin.botaniatea.block.entity.ElementTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

/** Renders the actual tank FluidStack, with height tied directly to amount/capacity. */
public final class ElementTankRenderer implements BlockEntityRenderer<ElementTankBlockEntity> {
    private static final float INSET=3.0F/16.0F;
    private static final float BOTTOM=2.0F/16.0F;
    private static final float TOP=14.0F/16.0F;

    public ElementTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(ElementTankBlockEntity tank,float partialTick,PoseStack pose,
                                 MultiBufferSource buffers,int light,int overlay) {
        FluidStack fluid=tank.fluid();
        if (fluid.isEmpty()) return;
        float fraction=(float)fluid.getAmount()/tank.capacity();
        float y1=BOTTOM+(TOP-BOTTOM)*Math.min(1.0F,fraction);
        if (y1<=BOTTOM) return;

        IClientFluidTypeExtensions extensions=IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still=extensions.getStillTexture(fluid);
        if (still==null) return;
        TextureAtlasSprite sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int tint=extensions.getTintColor(fluid);
        float red=((tint>>16)&255)/255.0F;
        float green=((tint>>8)&255)/255.0F;
        float blue=(tint&255)/255.0F;
        float alpha=((tint>>>24)&255)/255.0F;
        if (alpha<=0) alpha=1;
        VertexConsumer buffer=buffers.getBuffer(RenderType.translucent());
        Matrix4f matrix=pose.last().pose();
        float u0=sprite.getU0(),u1=sprite.getU1(),v0=sprite.getV0(),v1=sprite.getV1();

        quad(buffer,matrix,new float[][]{{INSET,y1,INSET},{INSET,y1,1-INSET},{1-INSET,y1,1-INSET},{1-INSET,y1,INSET}},u0,u1,v0,v1,red,green,blue,alpha,light,overlay,0,1,0);
        quad(buffer,matrix,new float[][]{{INSET,BOTTOM,INSET},{1-INSET,BOTTOM,INSET},{1-INSET,BOTTOM,1-INSET},{INSET,BOTTOM,1-INSET}},u0,u1,v0,v1,red,green,blue,alpha,light,overlay,0,-1,0);
        quad(buffer,matrix,new float[][]{{INSET,BOTTOM,INSET},{INSET,y1,INSET},{1-INSET,y1,INSET},{1-INSET,BOTTOM,INSET}},u0,u1,v0,v1,red,green,blue,alpha,light,overlay,0,0,-1);
        quad(buffer,matrix,new float[][]{{INSET,BOTTOM,1-INSET},{1-INSET,BOTTOM,1-INSET},{1-INSET,y1,1-INSET},{INSET,y1,1-INSET}},u0,u1,v0,v1,red,green,blue,alpha,light,overlay,0,0,1);
        quad(buffer,matrix,new float[][]{{INSET,BOTTOM,INSET},{INSET,BOTTOM,1-INSET},{INSET,y1,1-INSET},{INSET,y1,INSET}},u0,u1,v0,v1,red,green,blue,alpha,light,overlay,-1,0,0);
        quad(buffer,matrix,new float[][]{{1-INSET,BOTTOM,INSET},{1-INSET,y1,INSET},{1-INSET,y1,1-INSET},{1-INSET,BOTTOM,1-INSET}},u0,u1,v0,v1,red,green,blue,alpha,light,overlay,1,0,0);
    }

    private static void quad(VertexConsumer buffer,Matrix4f matrix,float[][] positions,
                             float u0,float u1,float v0,float v1,float red,float green,float blue,float alpha,
                             int light,int overlay,float nx,float ny,float nz) {
        float[][] uvs={{u0,v1},{u0,v0},{u1,v0},{u1,v1}};
        for (int i=0;i<4;i++) buffer.vertex(matrix,positions[i][0],positions[i][1],positions[i][2])
                .color(red,green,blue,alpha).uv(uvs[i][0],uvs[i][1]).overlayCoords(overlay).uv2(light)
                .normal(nx,ny,nz).endVertex();
    }
}
