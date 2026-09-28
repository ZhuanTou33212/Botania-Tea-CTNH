package com.qianxin.botaniatea.recipe;

import com.google.gson.JsonObject;
import com.qianxin.botaniatea.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;

public final class ElementDistillingRecipe implements Recipe<Container> {
    public enum Mode { ELEMENT, ASCENDED }
    private final ResourceLocation id;
    private final Mode mode;
    private final ResourceLocation fluidId;
    private final int fluidAmount;
    private final Ingredient leaf;
    private final Ingredient catalyst;
    private final ItemStack result;
    private final int duration;

    public ElementDistillingRecipe(ResourceLocation id, Mode mode, ResourceLocation fluidId, int fluidAmount,
                                   Ingredient leaf, Ingredient catalyst, ItemStack result, int duration) {
        this.id = id; this.mode = mode; this.fluidId = fluidId; this.fluidAmount = fluidAmount;
        this.leaf = leaf; this.catalyst = catalyst; this.result = result; this.duration = duration;
        if (fluidAmount<1 || fluidAmount>8000 || catalyst.isEmpty() || result.isEmpty() || result.getCount()>result.getMaxStackSize() || duration<1 || duration>32767 || (mode==Mode.ELEMENT && leaf.isEmpty()))
            throw new IllegalArgumentException("Invalid distilling recipe limits: "+id);
    }
    public Mode mode() { return mode; }
    public ResourceLocation fluidId() { return fluidId; }
    public int fluidAmount() { return fluidAmount; }
    public Ingredient leaf() { return leaf; }
    public Ingredient catalyst() { return catalyst; }
    public int duration() { return duration; }
    public boolean matchesInput(FluidStack fluid, IItemHandler inventory, Level level) {
        ItemStack leafStack = inventory.getStackInSlot(0), catalystStack = inventory.getStackInSlot(1);
        return !fluid.isEmpty() && fluid.getFluid() == ForgeRegistries.FLUIDS.getValue(fluidId) && fluid.getAmount() >= fluidAmount
                && (mode == Mode.ASCENDED ? leafStack.isEmpty() : leaf.test(leafStack))
                && catalyst.test(catalystStack);
    }
    @Override public boolean matches(Container container, Level level) { return false; }
    @Override public ItemStack assemble(Container container, net.minecraft.core.RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(net.minecraft.core.RegistryAccess access) { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ELEMENT_DISTILLING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.ELEMENT_DISTILLING.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, leaf, catalyst); }

    public static final class Serializer implements RecipeSerializer<ElementDistillingRecipe> {
        @Override public ElementDistillingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String modeValue = GsonHelper.getAsString(json, "mode");
            Mode mode = Mode.valueOf(modeValue.toUpperCase(java.util.Locale.ROOT));
            JsonObject fluid = GsonHelper.getAsJsonObject(json, "fluid");
            Ingredient leaf = json.has("leaf") ? Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "leaf")) : Ingredient.EMPTY;
            Ingredient catalyst = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "catalyst"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ElementDistillingRecipe(id, mode, ResourceLocation.parse(GsonHelper.getAsString(fluid, "fluid")),
                    GsonHelper.getAsInt(fluid, "amount"), leaf, catalyst, result, GsonHelper.getAsInt(json, "duration"));
        }
        @Override public ElementDistillingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Mode mode = buffer.readEnum(Mode.class); ResourceLocation fluid = buffer.readResourceLocation(); int amount = buffer.readVarInt();
            Ingredient leaf = Ingredient.fromNetwork(buffer), catalyst = Ingredient.fromNetwork(buffer);
            return new ElementDistillingRecipe(id, mode, fluid, amount, leaf, catalyst, buffer.readItem(), buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ElementDistillingRecipe recipe) {
            buffer.writeEnum(recipe.mode); buffer.writeResourceLocation(recipe.fluidId); buffer.writeVarInt(recipe.fluidAmount);
            recipe.leaf.toNetwork(buffer); recipe.catalyst.toNetwork(buffer); buffer.writeItem(recipe.result); buffer.writeVarInt(recipe.duration);
        }
    }
}
