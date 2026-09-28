package com.qianxin.teaart;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.mojang.logging.LogUtils;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModCreativeTabs;
import com.qianxin.teaart.registry.ModItems;
import com.qianxin.teaart.registry.ModMenus;
import com.qianxin.teaart.registry.ModTeaFluidTypes;
import com.qianxin.teaart.registry.ModTeaFluids;
import com.qianxin.teaart.worldgen.TeaArtTerraBlenderCompat;
import org.slf4j.Logger;

@Mod(TeaArtMod.MOD_ID)
public class TeaArtMod {

    public static final String MOD_ID = "tea_art";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TeaArtMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModTeaFluidTypes.register(modEventBus);
        ModTeaFluids.bootstrap();
        ModTeaFluids.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Initial composting balance; tea residue is an ordinary base-mod material.
        event.enqueueWork(() -> net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES
                .put(ModItems.TEA_RESIDUE.get(), 0.65F));
        if (ModList.get().isLoaded("terrablender")) {
            event.enqueueWork(TeaArtTerraBlenderCompat::registerRegions);
        }
    }
}
