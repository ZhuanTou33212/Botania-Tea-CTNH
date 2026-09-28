package com.qianxin.teaart.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.block.entity.BambooPipeBlockEntity;
import com.qianxin.teaart.block.entity.BambooPipeInputPortBlockEntity;
import com.qianxin.teaart.block.entity.BambooPipeOutputPortBlockEntity;
import com.qianxin.teaart.block.entity.FermentationVatBlockEntity;
import com.qianxin.teaart.block.entity.KneadingTableBlockEntity;
import com.qianxin.teaart.block.entity.PileFermentationBasinBlockEntity;
import com.qianxin.teaart.block.entity.ScentingBoxBlockEntity;
import com.qianxin.teaart.block.entity.TeaProcessingBlockEntity;
import com.qianxin.teaart.block.entity.TeaRoastingPanBlockEntity;
import com.qianxin.teaart.block.entity.TeaSieveBlockEntity;
import com.qianxin.teaart.block.entity.TeaSieveRackBlockEntity;
import com.qianxin.teaart.block.entity.TeaStoveBlockEntity;
import com.qianxin.teaart.block.entity.TeaTableBlockEntity;
import com.qianxin.teaart.block.entity.TeapotBlockEntity;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, TeaArtMod.MOD_ID);
    public static final RegistryObject<BlockEntityType<TeapotBlockEntity>> TEAPOT = BLOCK_ENTITIES.register("teapot",
            () -> BlockEntityType.Builder.of(TeapotBlockEntity::new, ModBlocks.TEAPOT.get()).build(null));

    public static final RegistryObject<BlockEntityType<TeaProcessingBlockEntity>> TEA_PROCESSING = BLOCK_ENTITIES
            .register("tea_processing",
                    () -> BlockEntityType.Builder.of(
                            TeaProcessingBlockEntity::new,
                            ModBlocks.WITHERING_TRAY.get(),
                            ModBlocks.CHARCOAL_ROASTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<TeaRoastingPanBlockEntity>> TEA_ROASTING_PAN = BLOCK_ENTITIES
            .register("tea_roasting_pan",
                    () -> BlockEntityType.Builder.of(TeaRoastingPanBlockEntity::new, ModBlocks.TEA_ROASTING_PAN.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<TeaSieveBlockEntity>> TEA_SIEVE = BLOCK_ENTITIES.register(
            "tea_sieve",
            () -> BlockEntityType.Builder.of(TeaSieveBlockEntity::new, ModBlocks.TEA_SIEVE.get()).build(null));

    public static final RegistryObject<BlockEntityType<TeaSieveRackBlockEntity>> TEA_SIEVE_RACK = BLOCK_ENTITIES
            .register("tea_sieve_rack",
                    () -> BlockEntityType.Builder.of(TeaSieveRackBlockEntity::new, ModBlocks.TEA_SIEVE_RACK.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<KneadingTableBlockEntity>> KNEADING_TABLE = BLOCK_ENTITIES
            .register("kneading_table",
                    () -> BlockEntityType.Builder.of(KneadingTableBlockEntity::new, ModBlocks.ROLLING_TABLE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<FermentationVatBlockEntity>> FERMENTATION_VAT = BLOCK_ENTITIES
            .register("fermentation_vat",
                    () -> BlockEntityType.Builder.of(FermentationVatBlockEntity::new, ModBlocks.FERMENTATION_VAT.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<PileFermentationBasinBlockEntity>> PILE_FERMENTATION_BASIN = BLOCK_ENTITIES
            .register("pile_fermentation_basin",
                    () -> BlockEntityType.Builder
                            .of(PileFermentationBasinBlockEntity::new, ModBlocks.PILE_FERMENTATION_BASIN.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<TeaStoveBlockEntity>> TEA_STOVE = BLOCK_ENTITIES.register(
            "tea_stove",
            () -> BlockEntityType.Builder.of(TeaStoveBlockEntity::new, ModBlocks.TEA_STOVE.get()).build(null));

    public static final RegistryObject<BlockEntityType<TeaTableBlockEntity>> TEA_TABLE = BLOCK_ENTITIES.register(
            "tea_table",
            () -> BlockEntityType.Builder.of(TeaTableBlockEntity::new, ModBlocks.TEA_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ScentingBoxBlockEntity>> SCENTING_BOX = BLOCK_ENTITIES.register(
            "scenting_box",
            () -> BlockEntityType.Builder.of(ScentingBoxBlockEntity::new, ModBlocks.SCENTING_BOX.get()).build(null));

    public static final RegistryObject<BlockEntityType<BambooPipeBlockEntity>> BAMBOO_PIPE = BLOCK_ENTITIES.register(
            "bamboo_pipe",
            () -> BlockEntityType.Builder.of(BambooPipeBlockEntity::new, ModBlocks.BAMBOO_PIPE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BambooPipeInputPortBlockEntity>> BAMBOO_PIPE_INPUT_PORT = BLOCK_ENTITIES
            .register("bamboo_pipe_input_port",
                    () -> BlockEntityType.Builder.of(BambooPipeInputPortBlockEntity::new,
                            ModBlocks.BAMBOO_PIPE_INPUT_PORT.get()).build(null));

    public static final RegistryObject<BlockEntityType<BambooPipeOutputPortBlockEntity>> BAMBOO_PIPE_OUTPUT_PORT = BLOCK_ENTITIES
            .register("bamboo_pipe_output_port",
                    () -> BlockEntityType.Builder.of(BambooPipeOutputPortBlockEntity::new,
                            ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
