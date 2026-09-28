package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.block.TeaEssenceDistillerBlock;
import com.qianxin.botaniatea.block.TeaEssenceStillbloomBlock;
import com.qianxin.botaniatea.block.SublimationBloomBlock;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.block.FinishedTeaCropBlock;
import com.qianxin.botaniatea.block.HeteromorphicCamelliaBlock;
import com.qianxin.botaniatea.block.WaterSupplyLilyBlock;
import com.qianxin.botaniatea.block.ButterflyOrchidBlock;
import com.qianxin.botaniatea.block.TeaRitualTableBlock;
import com.qianxin.botaniatea.block.MagicCrucibleBlock;
import com.qianxin.teaart.content.FinishedTea;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;
import vazkii.botania.common.block.FloatingSpecialFlowerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<Block> MANA_OFFERING = BLOCKS.register("mana_offering",
            () -> new com.qianxin.botaniatea.block.ManaOfferingBlock(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).noOcclusion()));
    public static final Map<TeaEssence, RegistryObject<LiquidBlock>> ESSENCE_BLOCKS = new EnumMap<>(TeaEssence.class);
    public static final Map<TeaFlower, RegistryObject<ForgeSpecialFlowerBlock>> FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<TeaFlower, RegistryObject<FloatingSpecialFlowerBlock>> FLOATING_FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<TeaFlower, RegistryObject<ForgeSpecialFlowerBlock>> CHIBI_FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<TeaFlower, RegistryObject<FloatingSpecialFlowerBlock>> FLOATING_CHIBI_FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<FinishedTea, RegistryObject<FinishedTeaCropBlock>> FINISHED_TEA_TREES = new EnumMap<>(FinishedTea.class);
    public static final RegistryObject<Block> TEA_SEAT = BLOCKS.register("tea_seat",
            () -> new com.qianxin.botaniatea.block.TeaUtilityBlock(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).noOcclusion()));
    public static final RegistryObject<Block> TEA_RITUAL_TABLE = BLOCKS.register("tea_ritual_table",
            () -> new TeaRitualTableBlock(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).noOcclusion()));
    public static final RegistryObject<Block> EQUIPMENT_PEDESTAL = BLOCKS.register("equipment_pedestal",
            () -> new com.qianxin.botaniatea.block.TeaUtilityBlock(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).noOcclusion()));
    public static final RegistryObject<Block> ELEMENT_TANK = BLOCKS.register("element_tank",
            () -> new com.qianxin.botaniatea.block.ElementTankBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .strength(2.5F).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<TeaEssenceDistillerBlock> TEA_ESSENCE_DISTILLER = BLOCKS.register("tea_essence_distiller",
            () -> new TeaEssenceDistillerBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.COPPER)));
    public static final RegistryObject<TeaEssenceStillbloomBlock> TEA_ESSENCE_STILLBLOOM = BLOCKS.register("tea_essence_stillbloom",
            () -> new TeaEssenceStillbloomBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS)));
    public static final RegistryObject<SublimationBloomBlock> SUBLIMATION_BLOOM = BLOCKS.register("sublimation_bloom",
            () -> new SublimationBloomBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).lightLevel(state -> 7)));
    public static final RegistryObject<LiquidBlock> ASCENDED_TEA_LIQUOR = BLOCKS.register("ascended_tea_liquor",
            () -> new LiquidBlock(ModFluids.ASCENDED_TEA_LIQUOR, BlockBehaviour.Properties.of()
                    .replaceable().noCollission().strength(100F).noLootTable().liquid().lightLevel(state -> 10)));
    public static final RegistryObject<MagicCrucibleBlock> MAGIC_CRUCIBLE = BLOCKS.register("magic_crucible",
            () -> new MagicCrucibleBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).strength(3.5F).sound(SoundType.METAL)));

    static {
        for (TeaFlower flower : TeaFlower.values()) {
            FLOWERS.put(flower, BLOCKS.register(flower.id,
                    () -> {
                        BlockBehaviour.Properties properties = BlockBehaviour.Properties.copy(Blocks.DANDELION);
                        if (flower == TeaFlower.NIGHT_EPIPHYLLUM) properties.lightLevel(state -> state.getValue(com.qianxin.botaniatea.block.TeaSpecialFlowerBlock.LIT) ? 15 : 0);
                        if (flower == TeaFlower.HETEROMORPHIC_CAMELLIA) return new HeteromorphicCamelliaBlock(properties);
                        if (flower == TeaFlower.WATER_SUPPLY_LILY) return new WaterSupplyLilyBlock(properties);
                        if (flower == TeaFlower.BUTTERFLY_ORCHID) return new ButterflyOrchidBlock(properties);
                        return new com.qianxin.botaniatea.block.TeaSpecialFlowerBlock(MobEffects.DIG_SPEED, 5, properties,
                            () -> flower.generating ? ModBlockEntities.REGISTERED_GENERATING_FLOWER.get()
                                        : ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get());
                    }));
            FLOATING_FLOWERS.put(flower, BLOCKS.register("floating_" + flower.id,
                    () -> createFloatingFlower(flower, false)));
            if (flower.supportsChibi()) {
                CHIBI_FLOWERS.put(flower, BLOCKS.register(flower.id + "_chibi",
                        () -> flower == TeaFlower.HETEROMORPHIC_CAMELLIA
                                ? new HeteromorphicCamelliaBlock(BlockBehaviour.Properties.copy(Blocks.DANDELION))
                                : flower == TeaFlower.WATER_SUPPLY_LILY
                                ? new WaterSupplyLilyBlock(BlockBehaviour.Properties.copy(Blocks.DANDELION))
                                : new com.qianxin.botaniatea.block.TeaSpecialFlowerBlock(MobEffects.DIG_SPEED, 5,
                                BlockBehaviour.Properties.copy(Blocks.DANDELION),
                                () -> ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get())));
                FLOATING_CHIBI_FLOWERS.put(flower, BLOCKS.register("floating_" + flower.id + "_chibi",
                        () -> createFloatingFlower(flower, true)));
            }
        }
        for (FinishedTea tea : FinishedTea.values()) {
            FINISHED_TEA_TREES.put(tea, BLOCKS.register(tea.id + "_tea_tree",
                    () -> new FinishedTeaCropBlock(tea, BlockBehaviour.Properties.copy(Blocks.WHEAT))));
        }
        for (TeaEssence essence : TeaEssence.values()) {
            ESSENCE_BLOCKS.put(essence, BLOCKS.register(essence.id + "_tea_essence",
                    () -> new LiquidBlock(ModFluids.SOURCES.get(essence), BlockBehaviour.Properties.of()
                            .replaceable().noCollission().strength(100F).noLootTable().liquid())));
        }
    }

    public static boolean isFlowerBlock(TeaFlower flower, Block block) {
        return (FLOWERS.containsKey(flower) && FLOWERS.get(flower).get() == block)
                || (FLOATING_FLOWERS.containsKey(flower) && FLOATING_FLOWERS.get(flower).get() == block)
                || (CHIBI_FLOWERS.containsKey(flower) && CHIBI_FLOWERS.get(flower).get() == block)
                || (FLOATING_CHIBI_FLOWERS.containsKey(flower) && FLOATING_CHIBI_FLOWERS.get(flower).get() == block);
    }

    private static FloatingSpecialFlowerBlock createFloatingFlower(TeaFlower flower, boolean chibi) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.copy(Blocks.DANDELION);
        if (flower == TeaFlower.NIGHT_EPIPHYLLUM) {
            properties = properties.lightLevel(state -> state.getValue(com.qianxin.botaniatea.block.TeaSpecialFlowerBlock.LIT) ? 15 : 0);
        }
        java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<? extends vazkii.botania.api.block_entity.SpecialFlowerBlockEntity>> type =
                flower.generating ? () -> ModBlockEntities.REGISTERED_GENERATING_FLOWER.get()
                        : () -> ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get();
        if (flower == TeaFlower.HETEROMORPHIC_CAMELLIA)
            return new com.qianxin.botaniatea.block.TeaFloatingHeteromorphicCamelliaBlock(properties, type);
        if (flower == TeaFlower.WATER_SUPPLY_LILY)
            return new com.qianxin.botaniatea.block.TeaFloatingWaterSupplyLilyBlock(properties, type);
        if (flower == TeaFlower.BUTTERFLY_ORCHID)
            return new com.qianxin.botaniatea.block.TeaFloatingButterflyOrchidBlock(properties, type);
        return new com.qianxin.botaniatea.block.TeaFloatingSpecialFlowerBlock(properties, type);
    }

    private ModBlocks() {}
}
