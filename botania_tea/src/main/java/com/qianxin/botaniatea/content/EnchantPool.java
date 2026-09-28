package com.qianxin.botaniatea.content;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Random enchantment draw for enchantable recipes.
 *
 * <p>Invested magic crystals are split into chunks of {@link #CRYSTALS_PER_ENTRY}. Each chunk yields
 * one entry whose level is {@code ceil(chunk / CRYSTALS_PER_LEVEL)}, capped at {@link #MAX_LEVEL} and
 * then at the enchantment's own maximum. One book carries at most {@link #MAX_ENTRIES} entries;
 * investing more than {@link #MAX_CRYSTALS} crystals fails the craft instead of strengthening it.
 */
public final class EnchantPool {
    private EnchantPool() {}

    /** Crystals that feed a single enchantment entry. */
    public static final int CRYSTALS_PER_ENTRY = 50;
    /** Crystals needed per enchantment level within one entry. */
    public static final int CRYSTALS_PER_LEVEL = 10;
    /** Highest level one entry reaches before the enchantment's own cap is applied. */
    public static final int MAX_LEVEL = 5;
    /** Entries one book may carry. */
    public static final int MAX_ENTRIES = 3;
    /** Crystals a single craft may hold; beyond this the craft fails. */
    public static final int MAX_CRYSTALS = CRYSTALS_PER_ENTRY * MAX_ENTRIES;

    /** True when the invested crystals exceed what one craft may hold. */
    public static boolean exceedsCapacity(int magicCrystals) {
        return magicCrystals > MAX_CRYSTALS;
    }

    /** Crystals feeding each entry, in order; empty when nothing was invested. */
    public static List<Integer> entryChunks(int magicCrystals) {
        List<Integer> chunks = new ArrayList<>();
        int left = Math.max(0, magicCrystals);
        while (left > 0 && chunks.size() < MAX_ENTRIES) {
            chunks.add(Math.min(left, CRYSTALS_PER_ENTRY));
            left -= CRYSTALS_PER_ENTRY;
        }
        return chunks;
    }

    /** One random book-applicable enchantment, or null when the registry is empty. */
    public static Enchantment draw(RandomSource random) {
        List<Enchantment> pool = bookPool();
        if (pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }
    /** Enchantments that may appear on a book, in registry order. */
    public static List<Enchantment> bookPool() {
        return ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .filter(Enchantment::isAllowedOnBooks)
                .toList();
    }

    /**
     * Level one entry reaches with {@code chunk} crystals: {@code ceil(chunk / CRYSTALS_PER_LEVEL)}
     * clamped to 1..{@link #MAX_LEVEL}, then to the enchantment's own maximum level.
     */
    public static int levelFor(Enchantment enchantment, int chunk) {
        int steps = Math.max(chunk, 1) + CRYSTALS_PER_LEVEL - 1;
        int level = Math.max(1, Math.min(steps / CRYSTALS_PER_LEVEL, MAX_LEVEL));
        return Math.min(level, enchantment.getMaxLevel());
    }

    /**
     * Rolls one book: one distinct enchantment per crystal chunk, in chunk order. Empty when nothing
     * was invested or the registry holds no book enchantments.
     */
    /**
     * Pool for one target: books take every book enchantment, while any other item only takes what
     * it actually accepts, so armour can never be given Sharpness and a pickaxe never Protection.
     */
    public static List<Enchantment> poolForTarget(net.minecraft.world.item.ItemStack target) {
        if (target.is(net.minecraft.world.item.Items.BOOK) || target.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) return bookPool();
        return poolFor(target);
    }

    /** Rolls a target from the pool it accepts, using the invested crystals for entry count and levels. */
    public static Map<Enchantment, Integer> rollForTarget(net.minecraft.world.item.ItemStack target, int magicCrystals,
                                                          RandomSource random) {
        return roll(poolForTarget(target), magicCrystals, random);
    }
    /** Enchantments that may legally apply to one specific target, in registry order. */
    public static List<Enchantment> poolFor(net.minecraft.world.item.ItemStack target) {
        return bookPool().stream().filter(enchantment -> enchantment.canEnchant(target)).toList();
    }

    /** Rolls a target from the pool it actually accepts, so armour can never receive Sharpness. */
    public static Map<Enchantment, Integer> rollFor(net.minecraft.world.item.ItemStack target, int magicCrystals,
                                                    RandomSource random) {
        return roll(poolFor(target), magicCrystals, random);
    }

    public static Map<Enchantment, Integer> roll(int magicCrystals, RandomSource random) {
        return roll(bookPool(), magicCrystals, random);
    }

    private static Map<Enchantment, Integer> roll(List<Enchantment> pool, int magicCrystals, RandomSource random) {
        Map<Enchantment, Integer> rolled = new LinkedHashMap<>();
        List<Enchantment> candidates = new ArrayList<>(pool);
        for (int chunk : entryChunks(magicCrystals)) {
            if (candidates.isEmpty()) break;
            Enchantment enchantment = candidates.remove(random.nextInt(candidates.size()));
            rolled.put(enchantment, levelFor(enchantment, chunk));
        }
        return rolled;
    }
}