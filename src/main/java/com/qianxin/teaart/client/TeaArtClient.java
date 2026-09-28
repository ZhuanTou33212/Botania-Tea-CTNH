package com.qianxin.teaart.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.block.BambooPipeBlock;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModMenus;

@Mod.EventBusSubscriber(modid = TeaArtMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TeaArtClient {

    private TeaArtClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlockEntities.SCENTING_BOX.get(), ScentingBoxRenderer::new);
            MenuScreens.register(ModMenus.SCENTING_BOX.get(), ScentingBoxScreen::new);
            MenuScreens.register(ModMenus.TEA_TABLE.get(), TeaTableScreen::new);
            MenuScreens.register(ModMenus.BAMBOO_PIPE_INPUT_PORT.get(), BambooPipeInputPortScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ?
                state.getValue(BambooPipeBlock.COLOR).getTintColor() : 0xFFFFFF, ModBlocks.BAMBOO_PIPE.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? BambooPipeBlock.PipeColor.NATURAL.getTintColor() : 0xFFFFFF,
                ModBlocks.BAMBOO_PIPE.get());
    }
}
