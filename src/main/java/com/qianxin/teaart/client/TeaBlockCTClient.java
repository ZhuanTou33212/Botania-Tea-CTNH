package com.qianxin.teaart.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.content.FinishedTea;
import com.qianxin.teaart.registry.ModBlocks;

import java.util.Map;

/** 烘焙完成后把所有茶块（茶块 + 8 种成品茶块）的模型换成连接边框模型。仅客户端加载。 */
@Mod.EventBusSubscriber(modid = TeaArtMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TeaBlockCTClient {

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // 1.20.1 这里是 Map<ResourceLocation, BakedModel>，键实际是 ModelResourceLocation
        Map<ResourceLocation, BakedModel> models = event.getModels();
        swap(models, ModBlocks.TEA_BLOCK.get(), "tea_block");
        for (FinishedTea tea : FinishedTea.values()) {
            Block block = ModBlocks.FINISHED_TEA_BLOCKS.get(tea).get();
            swap(models, block, tea.id + "_tea_block");
        }
    }

    private static void swap(Map<ResourceLocation, BakedModel> models, Block block, String name) {
        ResourceLocation blockId = new ResourceLocation(TeaArtMod.MOD_ID, name);
        ResourceLocation panel = new ResourceLocation(TeaArtMod.MOD_ID, "block/" + name);
        ResourceLocation atlas = new ResourceLocation(TeaArtMod.MOD_ID, "block/" + name + "_connected");
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ResourceLocation location = BlockModelShaper.stateToModelLocation(blockId, state);
            BakedModel original = models.get(location);
            if (original != null) {
                models.put(location, new TeaBlockCTModel(original, panel, atlas));
            }
        }
    }
}
