package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.content.ElementType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, BotaniaTeaMod.MOD_ID);
    public static final Map<TeaEssence, RegistryObject<ForgeFlowingFluid.Source>> SOURCES = new EnumMap<>(TeaEssence.class);
    public static final Map<TeaEssence, RegistryObject<ForgeFlowingFluid.Flowing>> FLOWING = new EnumMap<>(TeaEssence.class);
    public static final Map<ElementType, RegistryObject<ForgeFlowingFluid.Source>> ELEMENT_SOURCES = new EnumMap<>(ElementType.class);
    public static final Map<ElementType, RegistryObject<ForgeFlowingFluid.Flowing>> ELEMENT_FLOWING = new EnumMap<>(ElementType.class);
    public static final RegistryObject<ForgeFlowingFluid.Source> ASCENDED_TEA_LIQUOR;
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_ASCENDED_TEA_LIQUOR;

    static {
        for (TeaEssence essence : TeaEssence.values()) {
            String name = essence.id + "_tea_essence";
            SOURCES.put(essence, FLUIDS.register(name,
                    () -> new ForgeFlowingFluid.Source(properties(essence))));
            FLOWING.put(essence, FLUIDS.register("flowing_" + name,
                    () -> new ForgeFlowingFluid.Flowing(properties(essence))));
        }
        for (ElementType element : ElementType.values()) {
            String name = element.id + "_source_essence";
            ELEMENT_SOURCES.put(element, FLUIDS.register(name,
                    () -> new ForgeFlowingFluid.Source(elementProperties(element))));
            ELEMENT_FLOWING.put(element, FLUIDS.register("flowing_" + name,
                    () -> new ForgeFlowingFluid.Flowing(elementProperties(element))));
        }
        ASCENDED_TEA_LIQUOR = FLUIDS.register("ascended_tea_liquor",
                () -> new ForgeFlowingFluid.Source(ascendedProperties()));
        FLOWING_ASCENDED_TEA_LIQUOR = FLUIDS.register("flowing_ascended_tea_liquor",
                () -> new ForgeFlowingFluid.Flowing(ascendedProperties()));
    }

    private static ForgeFlowingFluid.Properties properties(TeaEssence essence) {
        return new ForgeFlowingFluid.Properties(ModFluidTypes.TYPES.get(essence), SOURCES.get(essence), FLOWING.get(essence))
                .block(ModBlocks.ESSENCE_BLOCKS.get(essence))
                .bucket(ModItems.ESSENCE_BUCKETS.get(essence));
    }

    private static ForgeFlowingFluid.Properties elementProperties(ElementType element) {
        return new ForgeFlowingFluid.Properties(ModFluidTypes.ELEMENT_TYPES.get(element),
                ELEMENT_SOURCES.get(element), ELEMENT_FLOWING.get(element))
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }

    public static ElementType elementType(Fluid fluid) {
        for (var entry : ELEMENT_SOURCES.entrySet()) {
            if (entry.getValue().get() == fluid || ELEMENT_FLOWING.get(entry.getKey()).get() == fluid) return entry.getKey();
        }
        return null;
    }

    public static boolean isElementEssence(Fluid fluid) { return elementType(fluid) != null; }

    private static ForgeFlowingFluid.Properties ascendedProperties() {
        return new ForgeFlowingFluid.Properties(ModFluidTypes.ASCENDED_TEA_LIQUOR,
                ASCENDED_TEA_LIQUOR, FLOWING_ASCENDED_TEA_LIQUOR)
                .block(ModBlocks.ASCENDED_TEA_LIQUOR)
                .bucket(ModItems.ASCENDED_TEA_LIQUOR_BUCKET);
    }

    private ModFluids() {}
}
