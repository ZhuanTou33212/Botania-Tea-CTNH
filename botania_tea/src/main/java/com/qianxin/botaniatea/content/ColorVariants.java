package com.qianxin.botaniatea.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Maps a white/default product onto its colored variant.
 *
 * <p>Colour names come from the dye item's registry path (for example {@code minecraft:red_dye}
 * yields {@code "red"}), which avoids depending on {@code DyeColor} accessor names. Two product
 * naming conventions are tried:
 * <ul>
 *   <li>vanilla prefix form: {@code minecraft:white_wool} -&gt; {@code minecraft:red_wool}</li>
 *   <li>tea suffix form: {@code botania_tea:rune_air} -&gt; {@code botania_tea:rune_air_red}</li>
 * </ul>
 * When neither sibling exists the white item is returned unchanged, so an unregistered colour
 * degrades to the default instead of failing the craft.
 */
public final class ColorVariants {
    private ColorVariants() {}

    /** Colour word of a dye item, or null when the stack is not a dye. */
    public static String colorOf(Item dyeItem) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(dyeItem);
        if (id == null) return null;
        String path = id.getPath();
        return path.endsWith("_dye") ? path.substring(0, path.length() - "_dye".length()) : null;
    }

    public static ItemStack apply(ItemStack white, String color) {
        if (white.isEmpty() || color == null || color.isEmpty() || "white".equals(color)) return white;
        Item resolved = resolve(white.getItem(), color);
        if (resolved == null) return white;
        return new ItemStack(resolved, white.getCount());
    }

    /** Pure colour decision, usable from both the crafting result and the ritual filter. */
    public static Item resolve(Item white, String color) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(white);
        if (id == null) return null;
        if (color == null || color.isEmpty() || "white".equals(color)) return white;
        String path = id.getPath();
        Item prefixed = lookup(id.getNamespace(), path.replaceFirst("^white_", color + "_"));
        if (prefixed != null) return prefixed;
        return lookup(id.getNamespace(), path + "_" + color);
    }

    private static Item lookup(String namespace, String path) {
        if (path.isBlank()) return null;
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }
}
