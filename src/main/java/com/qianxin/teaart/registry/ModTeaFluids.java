package com.qianxin.teaart.registry;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.content.TeaLiquor;

import java.util.EnumMap;
import java.util.Map;

public final class ModTeaFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS,
            TeaArtMod.MOD_ID);
    public static final Map<TeaLiquor, RegistryObject<ForgeFlowingFluid.Source>> SOURCES = new EnumMap<>(
            TeaLiquor.class);
    public static final Map<TeaLiquor, RegistryObject<ForgeFlowingFluid.Flowing>> FLOWING = new EnumMap<>(
            TeaLiquor.class);

    static {
        for (TeaLiquor liquor : TeaLiquor.values()) register(liquor);
    }

    private static void register(TeaLiquor liquor) {
        String name = liquor.fluidName();
        RegistryObject<ForgeFlowingFluid.Source> source = FLUIDS.register(name,
                () -> new ForgeFlowingFluid.Source(properties(liquor)));
        RegistryObject<ForgeFlowingFluid.Flowing> flowing = FLUIDS.register("flowing_" + name,
                () -> new ForgeFlowingFluid.Flowing(properties(liquor)));
        SOURCES.put(liquor, source);
        FLOWING.put(liquor, flowing);
        ModBlocks.TEA_LIQUOR_BLOCKS.put(liquor, ModBlocks.BLOCKS.register(name,
                () -> new LiquidBlock(source, BlockBehaviour.Properties.of()
                        .replaceable().noCollission().strength(100F).noLootTable().liquid())));
        ModItems.TEA_LIQUOR_BUCKETS.put(liquor, ModItems.ITEMS.register(name + "_bucket",
                () -> new BucketItem(source, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))));
    }

    private static ForgeFlowingFluid.Properties properties(TeaLiquor liquor) {
        return new ForgeFlowingFluid.Properties(ModTeaFluidTypes.TYPES.get(liquor), SOURCES.get(liquor),
                FLOWING.get(liquor))
                .block(ModBlocks.TEA_LIQUOR_BLOCKS.get(liquor))
                .bucket(ModItems.TEA_LIQUOR_BUCKETS.get(liquor));
    }

    public static void register(IEventBus bus) {
        FLUIDS.register(bus);
    }

    public static void bootstrap() {}

    private ModTeaFluids() {}
}
