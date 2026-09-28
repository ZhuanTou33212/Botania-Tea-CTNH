package com.qianxin.botaniatea.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public enum TeaEssence {
    WHITE("white", "white_tea_liquor", "white_tea_leaf", "iron_ingot", 2, 1, 0xFFF2E2BD),
    GREEN("green", "green_tea_liquor", "green_tea_leaf", "glowstone_dust", 4, 3, 0xFF78B85A),
    YELLOW("yellow", "yellow_tea_liquor", "yellow_tea_leaf", "gold_nugget", 8, 4, 0xFFE2C54D),
    RED("red", "red_tea_liquor", "red_tea_leaf", "redstone", 8, 4, 0xFFB64032),
    OOLONG("oolong", "oolong_tea_liquor", "oolong_tea_leaf", "copper_ingot", 3, 5, 0xFFB1783D),
    BLACK("black", "black_tea_liquor", "black_tea_leaf", "gold_ingot", 2, 4, 0xFF59402E),
    JASMINE("jasmine", "jasmine_scented_tea_liquor", "jasmine_flower_tea_leaf", "lapis_lazuli", 6, 3, 0xFFFFEDAA),
    GARDENIA("gardenia", "gardenia_scented_tea_liquor", "gardenia_tea_leaf", "emerald", 1, 3, 0xFFFFD85A);

    public final String id;
    public final String liquorId;
    public final String leafId;
    public final String outputId;
    public final int outputCount;
    public final int processSteps;
    public final int tint;

    TeaEssence(String id, String liquorId, String leafId, String outputId, int outputCount, int processSteps, int tint) {
        this.id = id;
        this.liquorId = liquorId;
        this.leafId = leafId;
        this.outputId = outputId;
        this.outputCount = outputCount;
        this.processSteps = processSteps;
        this.tint = tint;
    }

    public int ascendedYield() {
        return processSteps * 50;
    }

    public Fluid liquor() {
        return ForgeRegistries.FLUIDS.getValue(ResourceLocation.fromNamespaceAndPath("tea_art", liquorId));
    }

    public Item leaf() {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("tea_art", leafId));
    }

    public ItemStack output() {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", outputId));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, outputCount);
    }

    public static TeaEssence fromLeaf(ItemStack stack) {
        for (TeaEssence type : values()) if (stack.is(type.leaf())) return type;
        return null;
    }

    @Nullable
    public static TeaEssence byId(String id) {
        for (TeaEssence type : values()) if (type.id.equals(id)) return type;
        return null;
    }

    public static TeaEssence fromLiquor(Fluid fluid) {
        for (TeaEssence type : values()) if (fluid == type.liquor()) return type;
        return null;
    }

    public static TeaEssence fromEssenceFluid(Fluid fluid) {
        for (TeaEssence type : values()) {
            if (fluid == com.qianxin.botaniatea.registry.ModFluids.SOURCES.get(type).get()
                    || fluid == com.qianxin.botaniatea.registry.ModFluids.FLOWING.get(type).get()) return type;
        }
        return null;
    }
}
