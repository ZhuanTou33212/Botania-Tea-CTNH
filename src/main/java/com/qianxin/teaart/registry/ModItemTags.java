package com.qianxin.teaart.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.qianxin.teaart.TeaArtMod;

public final class ModItemTags {

    public static final TagKey<Item> FINISHED_TEAS = tag("finished_teas");
    public static final TagKey<Item> TEA_RESIDUES = tag("tea_residues");
    public static final TagKey<Item> FINISHED_TEA_BLOCKS = tag("finished_tea_blocks");

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID, name));
    }

    private ModItemTags() {}
}
