package com.qianxin.teaart.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import com.qianxin.teaart.TeaArtMod;

import javax.annotation.Nullable;

public class TeaTreeGrower extends AbstractTreeGrower {

    private static final ResourceKey<ConfiguredFeature<?, ?>> TEA_TREE = ResourceKey
            .create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID, "tea_tree"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_TEA_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(TeaArtMod.MOD_ID, "large_tea_tree"));

    @Nullable
    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        return random.nextFloat() < 0.05F ? LARGE_TEA_TREE : TEA_TREE;
    }
}
