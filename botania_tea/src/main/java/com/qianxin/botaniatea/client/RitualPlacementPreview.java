package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.content.OfferingRitual;
import com.qianxin.botaniatea.content.RitualAssembly;
import com.qianxin.botaniatea.registry.ModBlocks;
import com.qianxin.botaniatea.server.RitualPreviewInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.common.block.BotaniaBlocks;

/** Purely local guide: no client-supplied position ever controls server placement. */
@Mod.EventBusSubscriber(modid=BotaniaTeaMod.MOD_ID,value=Dist.CLIENT)
public final class RitualPlacementPreview {
    private static ClientLevel world;
    private static BlockPos center;
    @SubscribeEvent(receiveCanceled=true)
    public static void select(PlayerInteractEvent.RightClickBlock event) {
        var client=Minecraft.getInstance();
        if (event.getLevel()!=client.level || event.getEntity()!=client.player || !RitualPreviewInteraction.matches(event)) return;
        world=client.level; center=event.getPos().immutable();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var client=Minecraft.getInstance();
        if (world!=client.level || world==null) { center=null; world=null; return; }
        if (center==null) return;
        if (!world.hasChunkAt(center) || !world.getBlockState(center).is(BotaniaBlocks.runeAltar)
                || RitualAssembly.complete(world,center)) { center=null; return; }
        if (event.getCamera().getPosition().distanceToSqr(center.getCenter())>64*64) return;
        var pose=event.getPoseStack();
        var camera=event.getCamera().getPosition();
        var buffers=client.renderBuffers().bufferSource();
        pose.pushPose(); pose.translate(-camera.x,-camera.y,-camera.z);
        for (var p:OfferingRitual.positions(center)) {
            if (world.getBlockState(p).is(ModBlocks.MANA_OFFERING.get())) continue;
            boolean free=world.getBlockState(p).canBeReplaced();
            float red=free?.25F:1F, green=free?.9F:.2F;
            // Three translucent pedestal-shaped volumes, rather than an opaque block placed in the world.
            double[][] parts={{.125,0,.125,.875,.1875,.875},{.3125,.1875,.3125,.6875,.75,.6875},{.0625,.75,.0625,.9375,1,.9375}};
            for (var part:parts) {
                var box=new net.minecraft.world.phys.AABB(p.getX()+part[0],p.getY()+part[1],p.getZ()+part[2],
                        p.getX()+part[3],p.getY()+part[4],p.getZ()+part[5]);
                LevelRenderer.addChainedFilledBoxVertices(pose,buffers.getBuffer(RenderType.debugFilledBox()),
                        box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ,red,green,1F,.22F);
                LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),box,red,green,1F,.75F);
            }
        }
        buffers.endBatch(RenderType.debugFilledBox()); buffers.endBatch(RenderType.lines()); pose.popPose();
    }
    private RitualPlacementPreview() {}
}
