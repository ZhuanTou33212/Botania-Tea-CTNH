package com.qianxin.teaart.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.block.BambooPipeBlock;
import com.qianxin.teaart.block.BambooPipeInputPortBlock;
import com.qianxin.teaart.block.BambooPipeOutputPortBlock;
import com.qianxin.teaart.block.BambooPipeRedstonePortBlock;
import com.qianxin.teaart.block.FermentationVatBlock;
import com.qianxin.teaart.block.KneadingTableBlock;
import com.qianxin.teaart.block.PileFermentationBasinBlock;
import com.qianxin.teaart.block.ScentingBoxBlock;
import com.qianxin.teaart.block.TeaCropBlock;
import com.qianxin.teaart.block.TeaCupBlock;
import com.qianxin.teaart.block.TeaPlantBlock;
import com.qianxin.teaart.block.TeaProcessingBlock;
import com.qianxin.teaart.block.TeaRoastingPanBlock;
import com.qianxin.teaart.block.TeaSieveBlock;
import com.qianxin.teaart.block.TeaSieveRackBlock;
import com.qianxin.teaart.block.TeaStoveBlock;
import com.qianxin.teaart.block.TeaTableBlock;
import com.qianxin.teaart.block.TeaTreeGrower;
import com.qianxin.teaart.block.TeapotBlock;
import com.qianxin.teaart.block.WildFlowerBushBlock;
import com.qianxin.teaart.content.FinishedTea;
import com.qianxin.teaart.content.TeaLiquor;
import com.qianxin.teaart.item.TeaProcessingData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModBlocks {

    private static final int TORCH_LIGHT_LEVEL = 14;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            TeaArtMod.MOD_ID);

    public static final Map<TeaLiquor, RegistryObject<LiquidBlock>> TEA_LIQUOR_BLOCKS = new EnumMap<>(TeaLiquor.class);

    public static final Map<FinishedTea, RegistryObject<Block>> FINISHED_TEA_BLOCKS = new EnumMap<>(FinishedTea.class);

    static {
        for (FinishedTea tea : FinishedTea.values()) {
            FINISHED_TEA_BLOCKS.put(tea, registerBlock(tea.id + "_tea_block",
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                            .strength(0.7F).sound(SoundType.GRASS))));
        }
    }

    public static final RegistryObject<Block> TEA_TREE = BLOCKS.register("tea_tree",
            () -> new TeaCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));

    public static final RegistryObject<Block> TEA_CROP = BLOCKS.register("tea_crop",
            () -> new TeaPlantBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));

    public static final RegistryObject<Block> JASMINE_BUSH = BLOCKS.register("jasmine_bush",
            () -> new WildFlowerBushBlock(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH),
                    ModItems.JASMINE_FLOWER));

    public static final RegistryObject<Block> GARDENIA_BUSH = BLOCKS.register("gardenia_bush",
            () -> new WildFlowerBushBlock(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH),
                    ModItems.GARDENIA_FLOWER));

    public static final RegistryObject<Block> TEA_SAPLING = registerBlock("tea_sapling",
            () -> new SaplingBlock(new TeaTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));

    public static final RegistryObject<Block> TEA_LOG = registerBlock("tea_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));

    public static final RegistryObject<Block> TEA_PLANKS = registerBlock("tea_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));

    public static final RegistryObject<Block> TEA_STAIRS = registerBlock("tea_stairs",
            () -> new StairBlock(() -> TEA_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.OAK_STAIRS)));

    public static final RegistryObject<Block> TEA_SLAB = registerBlock("tea_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)));

    public static final RegistryObject<Block> TEA_FENCE = registerBlock("tea_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)));

    public static final RegistryObject<Block> TEA_FENCE_GATE = registerBlock("tea_fence_gate",
            () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE_GATE), WoodType.OAK));

    public static final RegistryObject<Block> TEA_BUTTON = registerBlock("tea_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON), BlockSetType.OAK, 30, true));

    public static final RegistryObject<Block> TEA_PRESSURE_PLATE = registerBlock("tea_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE), BlockSetType.OAK));

    public static final RegistryObject<Block> TEA_LEAVES = registerBlock("tea_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));

    public static final RegistryObject<Block> TEA_FLOWER = registerBlock("tea_flower",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));

    public static final RegistryObject<Block> TEA_BLOCK = registerBlock("tea_block",
            () -> new Block(
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.7F).sound(SoundType.GRASS)));

    public static final RegistryObject<Block> TEA_ROASTING_PAN = registerBlock("tea_roasting_pan",
            () -> new TeaRoastingPanBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL)
                            .noOcclusion()));

    public static final RegistryObject<Block> WITHERING_TRAY = registerBlock("withering_tray",
            () -> new TeaProcessingBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.BAMBOO_WOOD)
                            .noOcclusion(),
                    () -> List.of(
                            new TeaProcessingBlock.TeaProcess(TeaProcessingData.WITHERED, 8 * 20))));

    public static final RegistryObject<Block> ROLLING_TABLE = registerBlock("rolling_table",
            () -> new KneadingTableBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD)
                            .noOcclusion()));

    public static final RegistryObject<Block> TEA_SIEVE = BLOCKS.register("tea_sieve",
            () -> new TeaSieveBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.4F).sound(SoundType.BAMBOO_WOOD)
                            .noOcclusion()));

    public static final RegistryObject<Block> TEA_SIEVE_RACK = registerBlock("tea_sieve_rack",
            () -> new TeaSieveRackBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.2F).sound(SoundType.BAMBOO_WOOD)
                            .noOcclusion()));

    public static final RegistryObject<Block> FERMENTATION_VAT = registerBlock("fermentation_vat",
            () -> new FermentationVatBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.8F)
                            .sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> PILE_FERMENTATION_BASIN = registerBlock("pile_fermentation_basin",
            () -> new PileFermentationBasinBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.2F)
                            .sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> CHARCOAL_ROASTER = registerBlock("charcoal_roaster",
            () -> new TeaProcessingBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.2F).sound(SoundType.STONE)
                            .noOcclusion().lightLevel(state -> TORCH_LIGHT_LEVEL),
                    () -> List.of(
                            new TeaProcessingBlock.TeaProcess(TeaProcessingData.YELLOWED, ModItems.SPATULA, 8 * 20))));

    public static final RegistryObject<Block> TEA_STOVE = registerBlock("tea_stove",
            () -> new TeaStoveBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE)
                            .noOcclusion().lightLevel(state -> TORCH_LIGHT_LEVEL)));

    public static final RegistryObject<Block> TEA_TABLE = registerBlock("tea_table",
            () -> new TeaTableBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD)
                            .noOcclusion()));

    public static final RegistryObject<Block> SCENTING_BOX = registerBlock("scenting_box",
            () -> new ScentingBoxBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.8F).sound(SoundType.WOOD)
                            .noOcclusion()));

    public static final RegistryObject<Block> TEAPOT = registerBlock("teapot",
            () -> new TeapotBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F)
                            .sound(SoundType.DECORATED_POT).noOcclusion(),
                    List::of));

    public static final RegistryObject<Block> TEA_CUP = BLOCKS.register("tea_cup",
            () -> new TeaCupBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.2F)
                            .sound(SoundType.DECORATED_POT).noOcclusion()));

    public static final RegistryObject<Block> BAMBOO_PIPE = registerBlock("bamboo_pipe",
            () -> new BambooPipeBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.BAMBOO_WOOD)
                            .noOcclusion()));

    public static final RegistryObject<Block> BAMBOO_PIPE_OUTPUT_PORT = registerBlock("bamboo_pipe_output_port",
            () -> new BambooPipeOutputPortBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL)
                            .noOcclusion()));

    public static final RegistryObject<Block> BAMBOO_PIPE_INPUT_PORT = registerBlock("bamboo_pipe_input_port",
            () -> new BambooPipeInputPortBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL)
                            .noOcclusion()));

    public static final RegistryObject<Block> BAMBOO_PIPE_REDSTONE_PORT = registerBlock("bamboo_pipe_redstone_port",
            () -> new BambooPipeRedstonePortBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.7F)
                            .sound(SoundType.BAMBOO_WOOD).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(registeredBlock.get(), new Item.Properties()));
        return registeredBlock;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
