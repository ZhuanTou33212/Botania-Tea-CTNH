package com.qianxin.botaniatea.compat.farmersdelight;

import com.qianxin.botaniatea.compat.OptionalCompat;

/** Farmer's Delight adapter entry point; loaded only after ModList has confirmed the module. */
public final class FarmersDelightElementAdapter {
    private FarmersDelightElementAdapter() {}
    public static boolean isAvailable() { return OptionalCompat.farmersDelightLoaded(); }
    public static String scope() { return "cooking,cutting,dough,food_serving"; }
    public static com.qianxin.botaniatea.content.NormalizedProcess normalize(net.minecraft.world.item.crafting.Recipe<?> recipe, net.minecraft.core.RegistryAccess access) {
        if (recipe instanceof vectorwing.farmersdelight.common.crafting.CookingPotRecipe cooking) {
            return new com.qianxin.botaniatea.content.NormalizedProcess(recipe.getId(), recipe.getIngredients(), java.util.List.of(),
                    java.util.List.of(new com.qianxin.botaniatea.content.NormalizedProcess.Output(recipe.getResultItem(access),"1.0")),java.util.List.of(),cooking.getOutputContainer(),true,"");
        }
        if (recipe instanceof vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe cutting) {
            return new com.qianxin.botaniatea.content.NormalizedProcess(recipe.getId(),recipe.getIngredients(),java.util.List.of(cutting.getTool()),
                    cutting.getRollableResults().stream().map(r -> new com.qianxin.botaniatea.content.NormalizedProcess.Output(r.getStack(),Float.toString(r.getChance()))).toList(),
                    java.util.List.of(),net.minecraft.world.item.ItemStack.EMPTY,false,"");
        }
        return null;
    }
}
