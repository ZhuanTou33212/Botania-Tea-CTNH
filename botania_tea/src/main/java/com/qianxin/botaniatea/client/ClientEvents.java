package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.registry.ModItems;
import com.qianxin.botaniatea.registry.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(com.qianxin.botaniatea.registry.ModMenus.MAGIC_CRUCIBLE.get(), MagicCrucibleScreen::new));
        event.enqueueWork(() -> MenuScreens.register(com.qianxin.botaniatea.registry.ModMenus.TEA_ESSENCE_DISTILLER.get(), TeaEssenceDistillerScreen::new));
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                com.qianxin.botaniatea.registry.ModBlocks.ELEMENT_TANK.get(),RenderType.cutout()));
    }
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        com.qianxin.botaniatea.registry.ModBlocks.FINISHED_TEA_TREES.forEach((tea, block) ->
                event.register((state, level, pos, index) -> index == 0
                        ? TeaEssence.valueOf(tea.name()).tint : 0xFFFFFFFF, block.get()));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (var entry : ModItems.ELEMENT_CRYSTALS.entrySet())
            event.register((stack, layer) -> layer == 0 ? entry.getKey().color : 0xFFFFFFFF, entry.getValue().get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.qianxin.botaniatea.registry.ModBlockEntities.MANA_OFFERING.get(), ManaOfferingRenderer::new);
        event.registerBlockEntityRenderer(com.qianxin.botaniatea.registry.ModBlockEntities.ELEMENT_TANK.get(), ElementTankRenderer::new);
        event.registerBlockEntityRenderer(com.qianxin.botaniatea.registry.ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get(), BuildingPreviewRenderer::new);
        event.registerBlockEntityRenderer(com.qianxin.botaniatea.registry.ModBlockEntities.REGISTERED_GENERATING_FLOWER.get(), ButterflyFlowerRenderer::new);
        event.registerEntityRenderer(ModEntities.WATER_SPARK.get(),
                context -> new ThrownItemRenderer<>(context, 0.8F, true));
        event.registerEntityRenderer(ModEntities.TEA_MOUNTAIN_SPIRIT.get(),
                context -> new MobRenderer<com.qianxin.botaniatea.entity.TeaMountainSpiritEntity, TeaMountainSpiritModel>(
                        context, new TeaMountainSpiritModel(context.bakeLayer(TeaMountainSpiritModel.LAYER)), 0.7F) {
                    private final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("botania_tea", "textures/entity/tea_mountain_spirit_v2.png");
                    @Override public ResourceLocation getTextureLocation(com.qianxin.botaniatea.entity.TeaMountainSpiritEntity entity) { return texture; }
                });
        event.registerEntityRenderer(ModEntities.BAMBOO_GRAPPLING_HOOK.get(),
                context -> new ThrownItemRenderer<>(context, 0.5F, true));
        event.registerEntityRenderer(ModEntities.TEA_BLADE.get(),context -> new ThrownItemRenderer<>(context,.6F,true));
        event.registerEntityRenderer(ModEntities.SHADOW_PORTAL.get(), ShadowPortalRenderer::new);
    }

    @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TeaMountainSpiritModel.LAYER,TeaMountainSpiritModel::layer);
    }

    private ClientEvents() {}
}
