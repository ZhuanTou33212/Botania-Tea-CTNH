package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.recipe.OfferingCraftingRecipe;
import com.qianxin.botaniatea.recipe.ElementDistillingRecipe;
import com.qianxin.botaniatea.recipe.OfferingEnchantingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, BotaniaTeaMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<RecipeType<OfferingCraftingRecipe>> OFFERING_CRAFTING = RECIPE_TYPES.register("offering_crafting", () -> new RecipeType<>() {});
    public static final RegistryObject<RecipeSerializer<OfferingCraftingRecipe>> OFFERING_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("offering_crafting", OfferingCraftingRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<ElementDistillingRecipe>> ELEMENT_DISTILLING = RECIPE_TYPES.register("element_distilling", () -> new RecipeType<>() {});
    public static final RegistryObject<RecipeSerializer<ElementDistillingRecipe>> ELEMENT_DISTILLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("element_distilling", ElementDistillingRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<OfferingEnchantingRecipe>> OFFERING_ENCHANTING = RECIPE_TYPES.register("offering_enchanting", () -> new RecipeType<>() {});
    public static final RegistryObject<RecipeSerializer<OfferingEnchantingRecipe>> OFFERING_ENCHANTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("offering_enchanting", OfferingEnchantingRecipe.Serializer::new);

    private ModRecipes() {}
}
