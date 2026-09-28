package com.qianxin.teaart.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, TeaArtMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TEA_ART_TAB = CREATIVE_MODE_TABS.register("tea_art_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.GREEN_TEA.get()))
                    .title(Component.translatable("itemGroup.tea_art"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TEA_SEED.get());
                        output.accept(ModItems.JASMINE_SEED.get());
                        output.accept(ModItems.GARDENIA_SEED.get());
                        output.accept(ModItems.JASMINE_FLOWER.get());
                        output.accept(ModItems.GARDENIA_FLOWER.get());
                        output.accept(ModItems.TEA_LEAF.get());
                        output.accept(ModItems.TEA_FRUIT.get());
                        output.accept(ModBlocks.TEA_SAPLING.get());
                        output.accept(ModBlocks.TEA_LOG.get());
                        output.accept(ModBlocks.TEA_PLANKS.get());
                        output.accept(ModBlocks.TEA_STAIRS.get());
                        output.accept(ModBlocks.TEA_SLAB.get());
                        output.accept(ModBlocks.TEA_FENCE.get());
                        output.accept(ModBlocks.TEA_FENCE_GATE.get());
                        output.accept(ModBlocks.TEA_BUTTON.get());
                        output.accept(ModBlocks.TEA_PRESSURE_PLATE.get());
                        output.accept(ModBlocks.TEA_LEAVES.get());
                        output.accept(ModBlocks.TEA_FLOWER.get());
                        output.accept(ModBlocks.TEA_BLOCK.get());
                        ModBlocks.FINISHED_TEA_BLOCKS.values().forEach(block -> output.accept(block.get()));
                        output.accept(ModItems.TEA_RESIDUE.get());
                        output.accept(ModItems.GREEN_TEA_LEAF.get());
                        output.accept(ModItems.WHITE_TEA_LEAF.get());
                        output.accept(ModItems.YELLOW_TEA_LEAF.get());
                        output.accept(ModItems.RED_TEA_LEAF.get());
                        output.accept(ModItems.BLACK_TEA_LEAF.get());
                        output.accept(ModItems.OOLONG_TEA_LEAF.get());
                        output.accept(ModItems.JASMINE_FLOWER_TEA_LEAF.get());
                        output.accept(ModItems.GARDENIA_TEA_LEAF.get());
                        output.accept(ModItems.GREEN_TEA.get());
                        output.accept(ModItems.WHITE_TEA.get());
                        output.accept(ModItems.YELLOW_TEA.get());
                        output.accept(ModItems.RED_TEA.get());
                        output.accept(ModItems.BLACK_TEA.get());
                        output.accept(ModItems.OOLONG_TEA.get());
                        output.accept(ModItems.JASMINE_FLOWER_TEA.get());
                        output.accept(ModItems.GARDENIA_TEA.get());
                        output.accept(ModItems.TEA_CUP.get());
                        output.accept(ModItems.TEA_SIEVE.get());
                        output.accept(ModBlocks.TEAPOT.get());
                        output.accept(ModBlocks.TEA_STOVE.get());
                        output.accept(ModBlocks.TEA_ROASTING_PAN.get());
                        output.accept(ModBlocks.WITHERING_TRAY.get());
                        output.accept(ModBlocks.ROLLING_TABLE.get());
                        output.accept(ModBlocks.TEA_SIEVE_RACK.get());
                        output.accept(ModBlocks.FERMENTATION_VAT.get());
                        output.accept(ModBlocks.PILE_FERMENTATION_BASIN.get());
                        output.accept(ModBlocks.CHARCOAL_ROASTER.get());
                        output.accept(ModBlocks.TEA_TABLE.get());
                        output.accept(ModBlocks.SCENTING_BOX.get());
                        output.accept(ModBlocks.BAMBOO_PIPE.get());
                        output.accept(ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get());
                        output.accept(ModBlocks.BAMBOO_PIPE_INPUT_PORT.get());
                        output.accept(ModBlocks.BAMBOO_PIPE_REDSTONE_PORT.get());
                        output.accept(ModItems.SPATULA.get());
                        output.accept(ModItems.BAMBOO_WRENCH.get());
                        ModItems.TEA_LIQUOR_BUCKETS.values().forEach(bucket -> output.accept(bucket.get()));
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
