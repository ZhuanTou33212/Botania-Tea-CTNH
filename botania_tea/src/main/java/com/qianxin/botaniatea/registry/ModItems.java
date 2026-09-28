package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.content.ElementType;
import com.qianxin.botaniatea.item.RegisteredItem;
import com.qianxin.botaniatea.item.RegisteredBlockItem;
import com.qianxin.teaart.content.FinishedTea;
import com.qianxin.botaniatea.item.EssenceBottleItem;
import com.qianxin.botaniatea.item.AscendedTeaBottleItem;
import com.qianxin.botaniatea.item.WaterSparkItem;
import com.qianxin.botaniatea.item.ShanhaiSealItem;
import com.qianxin.botaniatea.item.TravelTeapotItem;
import com.qianxin.botaniatea.item.RoastingFanItem;
import com.qianxin.botaniatea.item.WaterStridingRingItem;
import com.qianxin.botaniatea.item.PorcelainGuardItem;
import com.qianxin.botaniatea.item.BambooGrapplingHookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vazkii.botania.common.item.block.SpecialFlowerBlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BotaniaTeaMod.MOD_ID);
    public static final Map<TeaEssence, RegistryObject<Item>> ESSENCE_BUCKETS = new EnumMap<>(TeaEssence.class);
    public static final Map<TeaEssence, RegistryObject<Item>> ESSENCE_BOTTLES = new EnumMap<>(TeaEssence.class);
    public static final Map<TeaFlower, RegistryObject<Item>> FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<TeaFlower, RegistryObject<Item>> FLOATING_FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<TeaFlower, RegistryObject<Item>> CHIBI_FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<TeaFlower, RegistryObject<Item>> FLOATING_CHIBI_FLOWERS = new EnumMap<>(TeaFlower.class);
    public static final Map<FinishedTea, RegistryObject<Item>> FINISHED_TEA_TREES = new EnumMap<>(FinishedTea.class);
    public static final Map<ElementType, RegistryObject<Item>> ELEMENT_CRYSTALS = new EnumMap<>(ElementType.class);

    public static final RegistryObject<Item> TRAVEL_TEAPOT = ITEMS.register("travel_teapot",
            () -> new TravelTeapotItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WATER_STRIDING_RING = ITEMS.register("water_striding_ring",
            () -> new WaterStridingRingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BAMBOO_GRAPPLING_HOOK = ITEMS.register("bamboo_grappling_hook",
            () -> new BambooGrapplingHookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ROASTING_FAN = ITEMS.register("roasting_fan",
            () -> new RoastingFanItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PORCELAIN_GUARD = ITEMS.register("porcelain_guard",
            () -> new PorcelainGuardItem(new Item.Properties().stacksTo(1).durability(672)));
    public static final RegistryObject<Item> SHANHAI_SEAL = ITEMS.register("shanhai_seal",
            () -> new ShanhaiSealItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MOUNTAIN_INVITATION = plannedItem("mountain_invitation", 16);
    public static final RegistryObject<Item> MOUNTAIN_HEARTWOOD = plannedItem("mountain_heartwood", 64);
    public static final RegistryObject<Item> ANCIENT_TEA_SEED = plannedItem("ancient_tea_seed", 64);
    public static final RegistryObject<Item> CLARITY_PORCELAIN_SHARD = plannedItem("clarity_porcelain_shard", 64);
    public static final RegistryObject<Item> TEA_MOUNTAIN_MEMORY = plannedItem("tea_mountain_memory", 64);
    public static final RegistryObject<Item> TEA_SEAT = ITEMS.register("tea_seat",
            () -> new RegisteredBlockItem(ModBlocks.TEA_SEAT.get(), new Item.Properties()));
    public static final RegistryObject<Item> TEA_RITUAL_TABLE = ITEMS.register("tea_ritual_table",
            () -> new RegisteredBlockItem(ModBlocks.TEA_RITUAL_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> EQUIPMENT_PEDESTAL = ITEMS.register("equipment_pedestal",
            () -> new RegisteredBlockItem(ModBlocks.EQUIPMENT_PEDESTAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> ELEMENT_TANK = ITEMS.register("element_tank",
            () -> new BlockItem(ModBlocks.ELEMENT_TANK.get(),new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> TEA_ESSENCE_DISTILLER = ITEMS.register("tea_essence_distiller",
            () -> new BlockItem(ModBlocks.TEA_ESSENCE_DISTILLER.get(), new Item.Properties()));
    public static final RegistryObject<Item> TEA_ESSENCE_STILLBLOOM = ITEMS.register("tea_essence_stillbloom",
            () -> new BlockItem(ModBlocks.TEA_ESSENCE_STILLBLOOM.get(), new Item.Properties()));
    public static final RegistryObject<Item> SUBLIMATION_BLOOM = ITEMS.register("sublimation_bloom",
            () -> new BlockItem(ModBlocks.SUBLIMATION_BLOOM.get(), new Item.Properties()));
    public static final RegistryObject<Item> WATER_SPARK = ITEMS.register("water_spark",
            () -> new WaterSparkItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ASCENDED_TEA_LIQUOR_BUCKET = ITEMS.register("ascended_tea_liquor_bucket",
            () -> new BucketItem(ModFluids.ASCENDED_TEA_LIQUOR,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<Item> ASCENDED_TEA_LIQUOR_BOTTLE = ITEMS.register("ascended_tea_liquor_bottle",
            () -> new AscendedTeaBottleItem(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)));
    public static final RegistryObject<Item> ASCENDED_CRYSTAL = plannedItem("ascended_crystal", 64);
    public static final RegistryObject<Item> RITUAL_SUBSTRATE = plannedItem("ritual_substrate", 64);
    // Eight category substrates. Crafting recipes name the substrate matching their product's
    // category; SubstrateCategory resolves a product to the right one automatically.
    public static final RegistryObject<Item> MINERAL_SUBSTRATE = plannedItem("mineral_substrate", 64);
    public static final RegistryObject<Item> PLANT_SUBSTRATE = plannedItem("plant_substrate", 64);
    public static final RegistryObject<Item> MAGIC_SUBSTRATE = plannedItem("magic_substrate", 64);
    public static final RegistryObject<Item> BLOCK_SUBSTRATE = plannedItem("block_substrate", 64);
    public static final RegistryObject<Item> ANIMAL_SUBSTRATE = plannedItem("animal_substrate", 64);
    public static final RegistryObject<Item> MONSTER_SUBSTRATE = plannedItem("monster_substrate", 64);
    public static final RegistryObject<Item> GEAR_SUBSTRATE = plannedItem("gear_substrate", 64);
    public static final RegistryObject<Item> GRASS_SUBSTRATE = plannedItem("grass_substrate", 64);
    /** Gaia-tier substrate: for gear that costs Gaia drops to make. */
    public static final RegistryObject<Item> GAIA_SUBSTRATE = plannedItem("gaia_substrate", 64);

    /** Every substrate item, keyed by its category id. */
    public static final Map<String, RegistryObject<Item>> SUBSTRATES;
    static {
        Map<String, RegistryObject<Item>> substrates = new java.util.LinkedHashMap<>();
        substrates.put("mineral", MINERAL_SUBSTRATE);
        substrates.put("plant", PLANT_SUBSTRATE);
        substrates.put("magic", MAGIC_SUBSTRATE);
        substrates.put("block", BLOCK_SUBSTRATE);
        substrates.put("animal", ANIMAL_SUBSTRATE);
        substrates.put("monster", MONSTER_SUBSTRATE);
        substrates.put("gear", GEAR_SUBSTRATE);
        substrates.put("grass", GRASS_SUBSTRATE);
        substrates.put("gaia", GAIA_SUBSTRATE);
        substrates.put("ritual", RITUAL_SUBSTRATE);
        SUBSTRATES = java.util.Collections.unmodifiableMap(substrates);
    }
    public static final RegistryObject<Item> MAGIC_CRUCIBLE = ITEMS.register("magic_crucible",
            () -> new BlockItem(ModBlocks.MAGIC_CRUCIBLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MANA_OFFERING = ITEMS.register("mana_offering",
            () -> new BlockItem(ModBlocks.MANA_OFFERING.get(), new Item.Properties()));
    public static final RegistryObject<Item> SHADOW_SHIFT_WAND = ITEMS.register("shadow_shift_wand",
            () -> new com.qianxin.botaniatea.item.ShadowShiftWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AFTERTASTE_GUARD_WAND = ITEMS.register("aftertaste_guard_wand",
            () -> new com.qianxin.botaniatea.item.AftertasteGuardWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EMBER_BEAM_WAND = ITEMS.register("ember_beam_wand",
            () -> new com.qianxin.botaniatea.item.EmberBeamWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BRANCH_LINK_WAND = ITEMS.register("branch_link_wand",
            () -> new com.qianxin.botaniatea.item.BranchLinkWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DREAM_BIND_WAND = ITEMS.register("dream_bind_wand",
            () -> new com.qianxin.botaniatea.item.DreamBindWandItem(new Item.Properties().stacksTo(1)));

    static {
        for (TeaFlower flower : TeaFlower.values()) {
            FLOWERS.put(flower, ITEMS.register(flower.id,
                    () -> flower == TeaFlower.BUILDING_BEGONIA
                            ? new com.qianxin.botaniatea.item.BuildingBegoniaBlockItem(ModBlocks.FLOWERS.get(flower).get(), new Item.Properties())
                            : new RegisteredBlockItem(ModBlocks.FLOWERS.get(flower).get(), new Item.Properties())));
            FLOATING_FLOWERS.put(flower, ITEMS.register("floating_" + flower.id,
                    () -> new SpecialFlowerBlockItem(ModBlocks.FLOATING_FLOWERS.get(flower).get(), new Item.Properties())));
            if (flower.supportsChibi()) {
                CHIBI_FLOWERS.put(flower, ITEMS.register(flower.id + "_chibi",
                        () -> new SpecialFlowerBlockItem(ModBlocks.CHIBI_FLOWERS.get(flower).get(), new Item.Properties())));
                FLOATING_CHIBI_FLOWERS.put(flower, ITEMS.register("floating_" + flower.id + "_chibi",
                        () -> new SpecialFlowerBlockItem(ModBlocks.FLOATING_CHIBI_FLOWERS.get(flower).get(), new Item.Properties())));
            }
        }
        for (FinishedTea tea : FinishedTea.values()) {
            FINISHED_TEA_TREES.put(tea, ITEMS.register(tea.id + "_tea_tree",
                    () -> new BlockItem(ModBlocks.FINISHED_TEA_TREES.get(tea).get(), new Item.Properties())));
        }
        for (TeaEssence essence : TeaEssence.values()) {
            ESSENCE_BUCKETS.put(essence, ITEMS.register(essence.id + "_tea_essence_bucket",
                    () -> new BucketItem(ModFluids.SOURCES.get(essence), new Item.Properties()
                            .craftRemainder(Items.BUCKET).stacksTo(1))));
            ESSENCE_BOTTLES.put(essence, ITEMS.register(essence.id + "_tea_essence_bottle",
                    () -> new EssenceBottleItem(essence, new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16))));
        }
        for (ElementType type : ElementType.values()) {
            ELEMENT_CRYSTALS.put(type, ITEMS.register(type.id + "_crystal", () -> new Item(new Item.Properties().stacksTo(64))));
        }
    }

    public static boolean isElementCrystal(ItemStack stack) {
        return ELEMENT_CRYSTALS.values().stream().anyMatch(entry -> stack.is(entry.get()));
    }

    public static ElementType elementCrystalType(ItemStack stack) {
        for (var entry:ELEMENT_CRYSTALS.entrySet()) if (stack.is(entry.getValue().get())) return entry.getKey();
        return null;
    }

    /** Dyes act as element-adjacent pigment inputs so colorable recipes can share the crystal slots. */
    public static boolean isPigment(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.DyeItem;
    }

    /** Crystal slots accept both element crystals and dyes. */
    public static boolean isCrucibleInput(ItemStack stack) {
        return isElementCrystal(stack) || isPigment(stack);
    }

    private static RegistryObject<Item> plannedItem(String id, int stackSize) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(stackSize)));
    }

    private ModItems() {}
}
