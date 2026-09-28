package com.qianxin.teaart.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.qianxin.teaart.TeaArtMod;

/** Block tags owned by tea_art and consumed by extension flowers. */
public final class ModBlockTags {

    public static final TagKey<Block> FINISHED_TEA_BLOCKS = tag("finished_tea_blocks");

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID, name));
    }

    private ModBlockTags() {}
}
