package com.qianxin.botaniatea.content;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Makes Botania own crafting stations visible to {@link ElementAnalyzer}.
 *
 * <p>Without this, anything whose only recipe lives in the petal apothecary, the mana pool or
 * the runic altar falls back to hand-written rules, so its element value does not match what it
 * is actually made of. Station recipes are matched on the type id; only the petal apothecary's
 * seed reagent needs the typed API, because Botania keeps it outside the vanilla ingredient list.
 */
public final class BotaniaCraftingNormalizer {
    private BotaniaCraftingNormalizer() {}

    /** Station recipe types this normalizer understands. */
    public static final Set<String> TYPE_IDS = Set.of(
            "botania:petal_apothecary",
            "botania:mana_infusion",
            "botania:runic_altar");

    /** True when the given recipe belongs to one of the stations above. */
    public static boolean handles(Recipe<?> recipe) {
        var id = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return id != null && TYPE_IDS.contains(id.toString());
    }

    public static NormalizedProcess normalize(Recipe<?> recipe, RegistryAccess access) {
        List<Ingredient> inputs = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) if (!ingredient.isEmpty()) inputs.add(ingredient);
        // The apothecary consumes one seed reagent per craft, but PetalsRecipe keeps it in a
        // separate field instead of the vanilla ingredient list. Add it back so the output is
        // priced from everything that is actually consumed.
        if (recipe instanceof vazkii.botania.api.recipe.RecipeWithReagent withReagent) {
            Ingredient reagent = withReagent.getReagent();
            if (reagent != null && !reagent.isEmpty()) inputs.add(reagent);
        }
        if (inputs.isEmpty()) return null;
        ItemStack result = recipe.getResultItem(access);
        if (result.isEmpty()) return null;
        return new NormalizedProcess(recipe.getId(), List.copyOf(inputs), List.of(),
                List.of(new NormalizedProcess.Output(result, "1.0")),
                List.of(), ItemStack.EMPTY, false, "");
    }

    /** Human-readable scope for diagnostics. */
    public static String scope() {
        return "petal_apothecary/mana_infusion/runic_altar";
    }
}
