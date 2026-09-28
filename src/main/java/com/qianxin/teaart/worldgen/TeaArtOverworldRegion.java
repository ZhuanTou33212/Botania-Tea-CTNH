package com.qianxin.teaart.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import com.mojang.datafixers.util.Pair;
import com.qianxin.teaart.TeaArtMod;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class TeaArtOverworldRegion extends Region {

    public TeaArtOverworldRegion(int weight) {
        super(ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID, "overworld"), RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
                          Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.FOREST, ModBiomes.TEA_LEAF_FOREST);
            builder.replaceBiome(Biomes.CHERRY_GROVE, ModBiomes.TEA_LEAF_FOREST);
        });
    }
}
