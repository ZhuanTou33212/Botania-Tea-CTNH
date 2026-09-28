package com.qianxin.teaart.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import com.qianxin.teaart.TeaArtMod;

public class ModBiomes {

    public static final ResourceKey<Biome> TEA_LEAF_FOREST = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID, "tea_leaf_forest"));

    private ModBiomes() {}
}
