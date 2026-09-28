package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.block.entity.TeaEssenceDistillerBlockEntity;
import com.qianxin.botaniatea.block.entity.TeaEssenceStillbloomBlockEntity;
import com.qianxin.botaniatea.block.entity.SublimationBloomBlockEntity;
import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity;
import com.qianxin.botaniatea.block.entity.TeaRitualTableBlockEntity;
import com.qianxin.botaniatea.block.entity.MagicCrucibleBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<BlockEntityType<com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity>> MANA_OFFERING =
            BLOCK_ENTITIES.register("mana_offering", () -> BlockEntityType.Builder.of(
                    com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity::new, ModBlocks.MANA_OFFERING.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.qianxin.botaniatea.block.entity.TeaUtilityBlockEntity>> TEA_SEAT =
            BLOCK_ENTITIES.register("tea_seat", () -> BlockEntityType.Builder.of(
                    com.qianxin.botaniatea.block.entity.TeaUtilityBlockEntity::new, ModBlocks.TEA_SEAT.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.qianxin.botaniatea.block.entity.TeaUtilityBlockEntity>> EQUIPMENT_PEDESTAL =
            BLOCK_ENTITIES.register("equipment_pedestal", () -> BlockEntityType.Builder.of(
                    com.qianxin.botaniatea.block.entity.TeaUtilityBlockEntity::new, ModBlocks.EQUIPMENT_PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.qianxin.botaniatea.block.entity.ElementTankBlockEntity>> ELEMENT_TANK =
            BLOCK_ENTITIES.register("element_tank", () -> BlockEntityType.Builder.of(
                    com.qianxin.botaniatea.block.entity.ElementTankBlockEntity::new, ModBlocks.ELEMENT_TANK.get()).build(null));
    public static final RegistryObject<BlockEntityType<RegisteredFunctionalFlowerBlockEntity>> REGISTERED_FUNCTIONAL_FLOWER =
            BLOCK_ENTITIES.register("registered_functional_flower", () -> BlockEntityType.Builder.of(
                    RegisteredFunctionalFlowerBlockEntity::new, flowerBlocks(false)).build(null));
    public static final RegistryObject<BlockEntityType<RegisteredGeneratingFlowerBlockEntity>> REGISTERED_GENERATING_FLOWER =
            BLOCK_ENTITIES.register("registered_generating_flower", () -> BlockEntityType.Builder.of(
                    RegisteredGeneratingFlowerBlockEntity::new, flowerBlocks(true)).build(null));

    private static Block[] flowerBlocks(boolean generating) {
        java.util.List<Block> blocks = new java.util.ArrayList<>();
        ModBlocks.FLOWERS.forEach((flower, block) -> {
            if (flower.generating == generating) blocks.add(block.get());
        });
        ModBlocks.FLOATING_FLOWERS.forEach((flower, block) -> {
            if (flower.generating == generating) blocks.add(block.get());
        });
        ModBlocks.CHIBI_FLOWERS.forEach((flower, block) -> {
            if (flower.generating == generating) blocks.add(block.get());
        });
        ModBlocks.FLOATING_CHIBI_FLOWERS.forEach((flower, block) -> {
            if (flower.generating == generating) blocks.add(block.get());
        });
        return blocks.toArray(Block[]::new);
    }
    public static final RegistryObject<BlockEntityType<TeaEssenceDistillerBlockEntity>> TEA_ESSENCE_DISTILLER =
            BLOCK_ENTITIES.register("tea_essence_distiller", () -> BlockEntityType.Builder.of(
                    TeaEssenceDistillerBlockEntity::new, ModBlocks.TEA_ESSENCE_DISTILLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<TeaEssenceStillbloomBlockEntity>> TEA_ESSENCE_STILLBLOOM =
            BLOCK_ENTITIES.register("tea_essence_stillbloom", () -> BlockEntityType.Builder.of(
                    TeaEssenceStillbloomBlockEntity::new, ModBlocks.TEA_ESSENCE_STILLBLOOM.get()).build(null));
    public static final RegistryObject<BlockEntityType<SublimationBloomBlockEntity>> SUBLIMATION_BLOOM =
            BLOCK_ENTITIES.register("sublimation_bloom", () -> BlockEntityType.Builder.of(
                    SublimationBloomBlockEntity::new, ModBlocks.SUBLIMATION_BLOOM.get()).build(null));
    public static final RegistryObject<BlockEntityType<TeaRitualTableBlockEntity>> TEA_RITUAL_TABLE =
            BLOCK_ENTITIES.register("tea_ritual_table", () -> BlockEntityType.Builder.of(
                    TeaRitualTableBlockEntity::new, ModBlocks.TEA_RITUAL_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<MagicCrucibleBlockEntity>> MAGIC_CRUCIBLE =
            BLOCK_ENTITIES.register("magic_crucible", () -> BlockEntityType.Builder.of(
                    MagicCrucibleBlockEntity::new, ModBlocks.MAGIC_CRUCIBLE.get()).build(null));

    private ModBlockEntities() {}
}
