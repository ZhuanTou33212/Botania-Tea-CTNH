package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.recipe.OfferingCraftingRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes crucible crafting visible to {@link ElementAnalyzer}.
 *
 * <p>Without this the analyzer reports every {@code botania_tea:offering_crafting} recipe as
 * {@code UNSUPPORTED_RECIPE}, which means the rune recipes, the wand recipes and the colorable
 * recipes contribute nothing to element inheritance.
 *
 * <p>Only element crystals are passed through as inputs. Pigments deliberately are not: a
 * colorable recipe still consumes its crystal cost plus substrate, so the white/default variant
 * is the authoritative element vector and a dye never changes what an item is made of.
 */
public final class OfferingCraftingNormalizer {
    private OfferingCraftingNormalizer() {}

    public static NormalizedProcess normalize(OfferingCraftingRecipe recipe, RegistryAccess access) {
        List<Ingredient> inputs = new ArrayList<>();
        // Substrate: the only non-crystal solid input.
        Ingredient substrate = recipe.getIngredients().get(0);
        if (substrate.isEmpty()) return null;
        inputs.add(substrate);
        // Element crystals: declared cost becomes a concrete stack so evaluate() can price it.
        recipe.elements().forEach((item, count) ->
                inputs.add(Ingredient.of(new ItemStack[]{new ItemStack(item, count)})));
        // Ascended crystals are progression gates, not ordinary material; skip them.

        ItemStack result = recipe.getResultItem(access);
        if (result.isEmpty()) return null;
        return new NormalizedProcess(recipe.getId(), List.copyOf(inputs), List.of(),
                List.of(new NormalizedProcess.Output(result, "1.0")),
                List.of(), ItemStack.EMPTY, false, "");
    }

    /** Human-readable scope for diagnostics. */
    public static String scope() {
        return "offering_crafting";
    }
}
