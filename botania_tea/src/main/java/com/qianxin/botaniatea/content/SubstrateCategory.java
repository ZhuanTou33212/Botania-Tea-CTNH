package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Automatic product-to-substrate classification.
 *
 * <p>Every crafting recipe names one of the nine substrates. Rather than hand-maintaining that
 * mapping, this resolves a product to its category by namespace and tag, in precedence order:
 * <ol>
 *   <li>Create machinery -> gear</li>
 *   <li>Botania flowers and seeds -> grass</li>
 *   <li>Tea mod essences, crystals, runes and wands -> magic</li>
 *   <li>Ores, ingots, gems, nuggets -> mineral</li>
 *   <li>Logs, planks, saplings, flowers, crops, seeds -> plant</li>
 *   <li>Leather, fish, meat, eggs -> animal</li>
 *   <li>Bones, gunpowder, pearls, blaze rods, string, slime -> monster</li>
 *   <li>Building blocks -> block</li>
 *   <li>Anything else -> ritual</li>
 * </ol>
 */
public final class SubstrateCategory {
    private SubstrateCategory() {}

    public static final String DEFAULT = "ritual";

    /** Machinery keywords for Create; ingots and ores deliberately do not match. */
    private static final List<String> GEAR_HINTS = List.of(
            "mechanical", "encased", "cogwheel", "gearbox", "gearshift", "clutch", "bearing",
            "pulley", "gantry", "flywheel", "crank", "turntable", "chassis", "conveyor", "piston",
            "millstone", "crushing_wheel", "press", "mixer", "saw", "drill", "pump", "deployer",
            "funnel", "tunnel", "valve", "shaft", "spout", "basin", "depot", "crafter");

    /** Botania flora keywords: these get the grass substrate rather than plain plant. */
    private static final List<String> FLORA_HINTS = List.of(
            "petal", "flower", "mushroom", "seed", "sapling", "grass", "vine", "lotus", "daisy",
            "orchid", "angelica", "dandelion", "lily", "begonia", "glory", "hydrangea", "hibiscus",
            "epiphyllum", "chrysanthemum", "bellflower", "dendrobium", "camellia");

    /** Gaia-tier gear is tagged, never inferred from the namespace. */
    private static final List<String> GAIA_TAGS = List.of("botania_tea:gaia_equipment");
    /** Equipment-style tags: the gear family. Gaia gear is claimed earlier by its own tag. */
    private static final List<String> GEAR_TAGS = List.of("botania_tea:enchantable_equipment");

    private static final List<String> MINERAL_TAGS = List.of(
            "forge:ores", "forge:raw_materials", "forge:ingots", "forge:gems", "forge:nuggets",
            "forge:dusts", "forge:storage_blocks", "minecraft:coals");

    private static final List<String> PLANT_TAGS = List.of(
            "minecraft:logs", "minecraft:planks", "minecraft:saplings", "minecraft:flowers",
            "forge:crops", "forge:seeds", "forge:fruits", "forge:vegetables", "minecraft:leaves");

    // Forge names meat per animal (forge:raw_beef, forge:cooked_chicken, forge:raw_fishes, ...),
    // so this family is matched by prefix instead of one generic tag.
    private static final List<String> ANIMAL_PREFIXES = List.of("forge:raw_", "forge:cooked_");

    // Vanilla meats carry no Forge family tag in this setup, so they are listed explicitly.
    private static final List<String> ANIMAL_ITEMS = List.of(
            "minecraft:beef", "minecraft:cooked_beef", "minecraft:porkchop", "minecraft:cooked_porkchop",
            "minecraft:chicken", "minecraft:cooked_chicken", "minecraft:mutton", "minecraft:cooked_mutton",
            "minecraft:rabbit", "minecraft:cooked_rabbit", "minecraft:rotten_flesh");

    private static final List<String> ANIMAL_TAGS = List.of(
            "forge:leather", "minecraft:fishes", "forge:eggs", "forge:feathers", "minecraft:wool");

    private static final List<String> MONSTER_TAGS = List.of(
            "forge:bones", "forge:gunpowder", "forge:ender_pearls", "forge:rods/blaze",
            "forge:string", "forge:slimeballs", "forge:magic_salts");

    /** Path suffixes that unambiguously name building or decoration blocks. */
    private static final List<String> BLOCK_SUFFIXES = List.of(
            "_concrete", "_concrete_powder", "_glazed_terracotta", "_terracotta", "_shulker_box",
            "_carpet", "_candle", "_banner", "_bed", "_sign", "_hanging_sign", "_glass", "_glass_pane",
            "_door", "_trapdoor", "_fence_gate", "_slab", "_stairs", "_wall", "_button", "_pressure_plate", "_rail");

    /** Path suffixes for metal containers. */
    private static final List<String> MINERAL_SUFFIXES = List.of("_bucket", "_cauldron");

    /** Path suffixes for wood-derived vehicles and plant parts. */
    private static final List<String> PLANT_SUFFIXES = List.of(
            "_boat", "_chest_boat", "_dye", "_seeds", "_sapling", "_roots", "_stem", "_vine", "_coral");

    private static final List<String> BLOCK_TAGS = List.of(
            "minecraft:walls", "minecraft:slabs", "minecraft:stairs", "minecraft:fences", "minecraft:fence_gates",
            "forge:stone", "forge:glass", "minecraft:terracotta", "minecraft:candles",
            "forge:cobblestone", "forge:sandstone", "minecraft:beds",
            // Decoration and furniture. Unknown tags simply never match, so the list can be generous.
            "forge:chests", "forge:chests/wooden", "forge:chests/ender", "forge:chests/trapped", "forge:barrels",
            "minecraft:banners", "minecraft:signs", "minecraft:all_signs", "minecraft:standing_signs",
            "minecraft:wall_signs", "minecraft:wooden_doors", "minecraft:wooden_trapdoors",
            "minecraft:wooden_buttons", "minecraft:wooden_pressure_plates", "minecraft:doors",
            "minecraft:trapdoors", "minecraft:buttons", "minecraft:pressure_plates", "minecraft:shulker_boxes",
            "minecraft:flower_pots", "minecraft:wool_carpets", "minecraft:campfires", "minecraft:anvil",
            "forge:glass_panes"); 

    /** Category id for a product; never null. */
    public static String categoryOf(ItemStack product) {
        if (product.isEmpty()) return DEFAULT;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(product.getItem());
        if (id == null) return DEFAULT;
        String namespace = id.getNamespace();
        String path = id.getPath();

        // Must precede the namespace rules: Botania gear would otherwise fall through to grass.
        if (isAny(product, GAIA_TAGS)) return "gaia";
        if (namespace.equals("create") && GEAR_HINTS.stream().anyMatch(path::contains)) return "gear";
        // Botanical magic (Botania) always uses the grass substrate, flora or not.
        if (namespace.equals("botania")) return isFlora(path) ? "grass" : "magic";
        // This mod's own spells (wands) use the spell substrate; other tea products are magical.
        if (namespace.equals("botania_tea") && path.endsWith("_wand")) return "ritual";
        if (namespace.equals("tea_art")) {
            if (teaArtPlant(path)) return "plant";
            // Unbreakable technical blocks (bedrock, barrier, command blocks) belong to no material family,
        // so they stay on ritual; every other unclaimed placeable block is a block.
        if (product.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock().getExplosionResistance() < 3600000.0F) return "block";
        }

        if (isAny(product, MINERAL_TAGS) || endsWithAny(path, MINERAL_SUFFIXES)) return "mineral";
        if (isAny(product, PLANT_TAGS) || endsWithAny(path, PLANT_SUFFIXES)) return "plant";
        if (ANIMAL_ITEMS.contains(id.toString()) || isAny(product, ANIMAL_TAGS)
                || hasTagPrefix(product, ANIMAL_PREFIXES)) return "animal";
        if (isAny(product, MONSTER_TAGS)) return "monster";
        if (isAny(product, BLOCK_TAGS) || endsWithAny(path, BLOCK_SUFFIXES)) return "block";
        // Equipment is the gear family: nothing else claims it and the gear substrate is otherwise unused.
        if (isAny(product, GEAR_TAGS)) return "gear";
        // Any placeable block nothing above claimed is a block; only non-block misc stays ritual.
        // Unbreakable technical blocks (bedrock, barrier, command blocks) belong to no material family,
        // so they stay on ritual; every other unclaimed placeable block is a block.
        if (product.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock().getExplosionResistance() < 3600000.0F) return "block";
        if (namespace.equals("tea_art") || namespace.equals("botania_tea")) return "magic";
        return DEFAULT;
    }

    /** The substrate item required for a product. */
    public static ItemStack substrateFor(ItemStack product) {
        var entry = ModItems.SUBSTRATES.get(categoryOf(product));
        Item item = entry == null ? ModItems.RITUAL_SUBSTRATE.get() : entry.get();
        return new ItemStack(item);
    }

    /** True when the given substrate is the one this product requires. */
    public static boolean accepts(ItemStack product, ItemStack substrate) {
        return ItemStack.isSameItemSameTags(substrate, substrateFor(product));
    }

    /** True when any of the item's tags starts with one of the given prefixes. */
    /** Botania-family flora keywords; only consulted for the botania / tea_art namespaces. */
    /** True when the path ends with any of the given suffixes. */
    private static boolean endsWithAny(String path, List<String> suffixes) {
        return suffixes.stream().anyMatch(path::endsWith);
    }

    private static boolean isFlora(String path) {
        return FLORA_HINTS.stream().anyMatch(path::contains);
    }

    /** tea_art plants: the shared flora keywords plus its tea leaves and seeds. */
    private static boolean teaArtPlant(String path) {
        return isFlora(path) || path.contains("leaf") || path.contains("seed") || path.contains("sapling");
    }

    private static boolean hasTagPrefix(ItemStack stack, List<String> prefixes) {
        return stack.getTags().anyMatch(tag -> {
            String name = tag.location().toString();
            return prefixes.stream().anyMatch(name::startsWith);
        });
    }
    private static boolean isAny(ItemStack stack, List<String> tags) {
        for (String tag : tags) {
            if (stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)))) return true;
        }
        return false;
    }
}
