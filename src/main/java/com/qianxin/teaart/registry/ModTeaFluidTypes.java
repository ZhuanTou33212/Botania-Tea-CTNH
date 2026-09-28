package com.qianxin.teaart.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.content.TeaLiquor;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ModTeaFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister
            .create(ForgeRegistries.Keys.FLUID_TYPES, TeaArtMod.MOD_ID);
    public static final Map<TeaLiquor, RegistryObject<FluidType>> TYPES = new EnumMap<>(TeaLiquor.class);
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID,
            "block/tea_liquor_still");
    private static final ResourceLocation FLOWING = ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID,
            "block/tea_liquor_flow");

    static {
        for (TeaLiquor liquor : TeaLiquor.values()) {
            TYPES.put(liquor, FLUID_TYPES.register(liquor.fluidName(), () -> new FluidType(
                    FluidType.Properties.create().density(1020).viscosity(1100).temperature(350)
                            .descriptionId("fluid." + TeaArtMod.MOD_ID + "." + liquor.fluidName())) {

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {

                        @Override
                        public ResourceLocation getStillTexture() {
                            return STILL;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return FLOWING;
                        }

                        @Override
                        public int getTintColor() {
                            return liquor.tint();
                        }
                    });
                }
            }));
        }
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
    }

    private ModTeaFluidTypes() {}
}
