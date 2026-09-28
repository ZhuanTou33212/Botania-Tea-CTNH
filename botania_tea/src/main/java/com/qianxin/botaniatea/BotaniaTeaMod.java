package com.qianxin.botaniatea;

import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModBlocks;
import com.qianxin.botaniatea.registry.ModCreativeTabs;
import com.qianxin.botaniatea.registry.ModFluidTypes;
import com.qianxin.botaniatea.registry.ModFluids;
import com.qianxin.botaniatea.registry.ModItems;
import com.qianxin.botaniatea.registry.ModEntities;
import com.qianxin.botaniatea.network.ModNetwork;
import com.qianxin.botaniatea.registry.ModRecipes;
import com.qianxin.botaniatea.registry.ModMenus;
import com.qianxin.botaniatea.registry.ModEffects;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BotaniaTeaMod.MOD_ID)
public final class BotaniaTeaMod {
    public static final String MOD_ID = "botania_tea";

    public BotaniaTeaMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER,
                com.qianxin.botaniatea.config.TeaGardenConfig.SERVER_SPEC);
        ModNetwork.register();
        IEventBus bus = context.getModEventBus();
        ModFluidTypes.FLUID_TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        ModEntities.register(bus);
        com.qianxin.botaniatea.registry.ModSounds.SOUNDS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModRecipes.RECIPE_TYPES.register(bus);
        ModRecipes.RECIPE_SERIALIZERS.register(bus);
        ModMenus.MENUS.register(bus);
        ModEffects.EFFECTS.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                net.minecraftforge.common.world.ForgeChunkManager.setForcedChunkLoadingCallback(MOD_ID, (level, tickets) -> {
                    for (var pos : tickets.getBlockTickets().keySet()) {
                        if (com.qianxin.botaniatea.content.TeaFlower.fromBlock(level.getBlockState(pos).getBlock())
                                != com.qianxin.botaniatea.content.TeaFlower.CHUNK_DANDELION)
                            tickets.removeAllTickets(pos);
                    }
                })));
    }
}
