package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.content.ElementType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BotaniaTeaMod.MOD_ID);
    public static final Map<TeaEssence, RegistryObject<FluidType>> TYPES = new EnumMap<>(TeaEssence.class);
    public static final Map<ElementType, RegistryObject<FluidType>> ELEMENT_TYPES = new EnumMap<>(ElementType.class);
    public static final int ASCENDED_TEA_TINT = 0xFFFFB7D5;
    public static final RegistryObject<FluidType> ASCENDED_TEA_LIQUOR;
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath("tea_art", "block/tea_liquor_still");
    private static final ResourceLocation FLOW = ResourceLocation.fromNamespaceAndPath("tea_art", "block/tea_liquor_flow");
    private static final ResourceLocation ELEMENT_STILL = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static final ResourceLocation ELEMENT_FLOW = ResourceLocation.withDefaultNamespace("block/lava_flow");

    static {
        for (TeaEssence essence : TeaEssence.values()) {
            TYPES.put(essence, FLUID_TYPES.register(essence.id + "_tea_essence", () -> new FluidType(
                    FluidType.Properties.create().density(1200).viscosity(1500).temperature(300)
                            .descriptionId("fluid." + BotaniaTeaMod.MOD_ID + "." + essence.id + "_tea_essence")) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                        @Override public int getTintColor() { return essence.tint; }
                    });
                }
            }));
        }
        for (ElementType element : ElementType.values()) {
            ELEMENT_TYPES.put(element, FLUID_TYPES.register(element.id + "_source_essence", () -> new FluidType(
                    FluidType.Properties.create().density(1000).viscosity(1000).temperature(300)
                            .descriptionId("fluid." + BotaniaTeaMod.MOD_ID + "." + element.id + "_source_essence")) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return ELEMENT_STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return ELEMENT_FLOW; }
                        @Override public int getTintColor() { return element.color; }
                    });
                }
            }));
        }
        ASCENDED_TEA_LIQUOR = FLUID_TYPES.register("ascended_tea_liquor", () -> new FluidType(
                FluidType.Properties.create().density(1100).viscosity(1300).temperature(310).lightLevel(10)
                        .descriptionId("fluid." + BotaniaTeaMod.MOD_ID + ".ascended_tea_liquor")) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override public ResourceLocation getStillTexture() { return STILL; }
                    @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                    @Override public int getTintColor() { return ASCENDED_TEA_TINT; }
                });
            }
        });
    }

    private ModFluidTypes() {}
}
