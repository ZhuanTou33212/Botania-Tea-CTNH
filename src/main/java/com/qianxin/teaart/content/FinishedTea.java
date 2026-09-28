package com.qianxin.teaart.content;

import net.minecraft.world.item.Item;

import com.qianxin.teaart.registry.ModItems;

import java.util.function.Supplier;

/** Shared tea identities; the base mod owns tea products, not magical tea trees. */
public enum FinishedTea {

    WHITE("white", () -> ModItems.WHITE_TEA_LEAF.get()),
    GREEN("green", () -> ModItems.GREEN_TEA_LEAF.get()),
    YELLOW("yellow", () -> ModItems.YELLOW_TEA_LEAF.get()),
    RED("red", () -> ModItems.RED_TEA_LEAF.get()),
    OOLONG("oolong", () -> ModItems.OOLONG_TEA_LEAF.get()),
    BLACK("black", () -> ModItems.BLACK_TEA_LEAF.get()),
    JASMINE("jasmine", () -> ModItems.JASMINE_FLOWER_TEA_LEAF.get()),
    GARDENIA("gardenia", () -> ModItems.GARDENIA_TEA_LEAF.get());

    public final String id;
    private final Supplier<Item> leaf;

    FinishedTea(String id, Supplier<Item> leaf) {
        this.id = id;
        this.leaf = leaf;
    }

    public Item leaf() {
        return leaf.get();
    }
}
