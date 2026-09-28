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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-side recipe contract for the solid-input, mana-powered crucible. */
public final class OfferingCraftingRecipe implements Recipe<Container> {
    public static final int CRYSTAL_SLOT_START = 0;
    public static final int CRYSTAL_SLOT_END = 12;
    public static final int SUBSTRATE_SLOT = 12;
    public static final int ASCENDED_SLOT = 13;

    private final ResourceLocation id;
    private final Ingredient substrate;
    private final int substrateCount;
    private final Map<Item, Integer> elements;
    private final int ascendedCount;
    private final int mana;
    private final int duration;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    /** When true the product is tinted by a randomly drawn dye from the crystal slots (no dye => white). */
    private final boolean colorable;
    /** Dyes consumed per craft once a colour is drawn. */
    private final int dyeCost;

    /** When true the product is an enchanted book filled by a random draw whose level scales with magic crystals. */
    private final boolean enchantable;

    public OfferingCraftingRecipe(ResourceLocation id, Ingredient substrate, int substrateCount, Map<Item, Integer> elements,
                                  int ascendedCount, int mana, int duration, ItemStack result) {
        this(id, substrate, substrateCount, elements, ascendedCount, mana, duration, result, false, 1);
    }

    public OfferingCraftingRecipe(ResourceLocation id, Ingredient substrate, int substrateCount, Map<Item, Integer> elements,
                                  int ascendedCount, int mana, int duration, ItemStack result, boolean colorable, int dyeCost) {
        this(id, substrate, substrateCount, elements, ascendedCount, mana, duration, result, colorable, dyeCost, false);
    }

    public OfferingCraftingRecipe(ResourceLocation id, Ingredient substrate, int substrateCount, Map<Item, Integer> elements,
                                  int ascendedCount, int mana, int duration, ItemStack result, boolean colorable, int dyeCost,
                                  boolean enchantable) {
        this.id = id;
        this.substrate = substrate;
        this.substrateCount = substrateCount;
        this.elements = Map.copyOf(elements);
        this.ascendedCount = ascendedCount;
        this.mana = mana;
        this.duration = duration;
        this.result = result.copy();
        this.colorable = colorable;
        this.dyeCost = colorable ? Math.max(1, dyeCost) : 0;
        this.enchantable = enchantable;
        this.ingredients = NonNullList.create();
        this.ingredients.add(substrate);
        if (substrate.isEmpty() || substrateCount<1 || substrateCount>64 || ascendedCount<0 || ascendedCount>64 || mana<0 || mana>100000 || duration<1 || duration>32767 || result.isEmpty() || result.getCount()>result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid crucible recipe limits: "+id);
        for (var entry:elements.entrySet()) if (!com.qianxin.botaniatea.registry.ModItems.isElementCrystal(new ItemStack(entry.getKey())) || entry.getValue()<1 || entry.getValue()>768)
            throw new IllegalArgumentException("Invalid source crystal requirement: "+id);
        elements.keySet().forEach(item -> this.ingredients.add(Ingredient.of(item)));
        if (elements.size() > 12) throw new IllegalArgumentException("crucible recipe has more than 12 element types: " + id);
        if (elements.values().stream().mapToInt(count -> (count+63)/64).sum()>12) throw new IllegalArgumentException("crucible crystal stacks exceed 12 slots: "+id);
    }

    public ResourceLocation id() { return id; }
    public Map<Item, Integer> elements() { return elements; }
    public int substrateCount() { return substrateCount; }
    public int ascendedCount() { return ascendedCount; }
    public int mana() { return mana; }
    public int duration() { return duration; }
    public boolean colorable() { return colorable; }
    public int dyeCost() { return dyeCost; }
    public boolean enchantable() { return enchantable; }

    /** Magic crystals this recipe declares, for fixed recipes. */
    public int magicCrystalCount() {
        var magic = magicCrystalItem();
        return magic == null ? 0 : elements.getOrDefault(magic, 0);
    }

    /** The magic source crystal item, or null while the registry is unavailable. */
    private static net.minecraft.world.item.Item magicCrystalItem() {
        var magic = com.qianxin.botaniatea.registry.ModItems.ELEMENT_CRYSTALS.get(com.qianxin.botaniatea.content.ElementType.MAGIC);
        return magic == null ? null : magic.get();
    }

    /**
     * Enchantable recipes roll their book at completion time. The invested magic crystals decide how
     * many entries the book carries and how strong each one is; the enchantments are random.
     */
    public ItemStack assembleEnchantedBook(net.minecraft.util.RandomSource random, int magicCrystals) {
        var rolled = com.qianxin.botaniatea.content.EnchantPool.roll(magicCrystals, random);
        if (rolled.isEmpty()) return ItemStack.EMPTY;
        ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(
                new java.util.HashMap<>(rolled), book);
        return book;
    }

    @Override public boolean matches(Container container, Level level) {
        ItemStack substrateStack = container.getItem(SUBSTRATE_SLOT);
        if (!substrate.test(substrateStack) || substrateStack.getCount() < substrateCount) return false;
        ItemStack ascended = container.getItem(ASCENDED_SLOT);
        if (ascendedCount == 0 ? !ascended.isEmpty() : ascended.isEmpty() || ascended.getCount() < ascendedCount) return false;
        if (ascendedCount>0 && !ascended.is(com.qianxin.botaniatea.registry.ModItems.ASCENDED_CRYSTAL.get())) return false;
        Map<Item, Integer> foundCrystals = new LinkedHashMap<>();
        int foundDyes = 0;
        for (int slot = CRYSTAL_SLOT_START; slot < CRYSTAL_SLOT_END; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (com.qianxin.botaniatea.registry.ModItems.isPigment(stack)) {
                if (!colorable) return false;
                foundDyes += stack.getCount();
            } else {
                foundCrystals.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        // Enchantable recipes accept any positive amount of magic crystals and nothing else: the
        // invested count selects the entry levels, so it is not a fixed requirement.
        if (enchantable) {
            var magic = magicCrystalItem();
            return foundDyes == 0 && magic != null && foundCrystals.size() == 1
                    && foundCrystals.containsKey(magic);
        }
        if (!foundCrystals.equals(elements)) return false;
        // Non-colorable recipes must not receive stray dyes; colorable ones accept any dye amount
        // (0 = white, otherwise one dye is drawn per craft).
        return colorable || foundDyes == 0;
    }

    @Override public ItemStack assemble(Container container, net.minecraft.core.RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResultItem(net.minecraft.core.RegistryAccess access) { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.OFFERING_CRAFTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.OFFERING_CRAFTING.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return ingredients; }

    public static final class Serializer implements RecipeSerializer<OfferingCraftingRecipe> {
        @Override public OfferingCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            ItemStack declaredResult = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            Ingredient substrate;
            int substrateCount = 1;
            if (json.has("substrate")) {
                JsonObject substrateJson = GsonHelper.getAsJsonObject(json, "substrate");
                substrate = Ingredient.fromJson(GsonHelper.getAsJsonObject(substrateJson, "ingredient"));
                substrateCount = GsonHelper.getAsInt(substrateJson, "count", 1);
            } else {
                // No explicit substrate: derive the category catalyst from the product itself.
                substrate = Ingredient.of(com.qianxin.botaniatea.content.SubstrateCategory.substrateFor(declaredResult).getItem());
            }
            Map<Item, Integer> elements = new LinkedHashMap<>();
            JsonObject elementJson = GsonHelper.getAsJsonObject(json, "elements");
            for (String key : elementJson.keySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(key.contains(":") ? key : "botania_tea:" + key));
                if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("unknown element item " + key);
                elements.put(item, GsonHelper.getAsInt(elementJson, key));
            }
            ItemStack result = declaredResult;
            boolean colorable = GsonHelper.getAsBoolean(json, "colorable", false);
            int dyeCost = GsonHelper.getAsInt(json, "dye_cost", 1);
            boolean enchantable = GsonHelper.getAsBoolean(json, "enchantable", false);
            return new OfferingCraftingRecipe(id, substrate, substrateCount, elements,
                    GsonHelper.getAsInt(json, "ascended_crystals", 0), GsonHelper.getAsInt(json, "mana"),
                    GsonHelper.getAsInt(json, "duration"), result, colorable, dyeCost, enchantable);
        }

        @Override public OfferingCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient substrate = Ingredient.fromNetwork(buffer);
            int substrateCount = buffer.readVarInt();
            int count = buffer.readVarInt();
            Map<Item, Integer> elements = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) elements.put(ForgeRegistries.ITEMS.getValue(buffer.readResourceLocation()), buffer.readVarInt());
            int ascended = buffer.readVarInt();
            int mana = buffer.readVarInt();
            int duration = buffer.readVarInt();
            ItemStack result = buffer.readItem();
            boolean colorable = buffer.readBoolean();
            int dyeCost = buffer.readVarInt();
            boolean enchantable = buffer.readBoolean();
            return new OfferingCraftingRecipe(id, substrate, substrateCount, elements, ascended, mana, duration, result, colorable, dyeCost, enchantable);
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, OfferingCraftingRecipe recipe) {
            recipe.substrate.toNetwork(buffer);
            buffer.writeVarInt(recipe.substrateCount);
            buffer.writeVarInt(recipe.elements.size());
            recipe.elements.entrySet().stream().sorted(java.util.Comparator.comparing(e -> ForgeRegistries.ITEMS.getKey(e.getKey()).toString())).forEach(e -> { buffer.writeResourceLocation(ForgeRegistries.ITEMS.getKey(e.getKey())); buffer.writeVarInt(e.getValue()); });
            buffer.writeVarInt(recipe.ascendedCount);
            buffer.writeVarInt(recipe.mana);
            buffer.writeVarInt(recipe.duration);
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.colorable);
            buffer.writeVarInt(recipe.dyeCost);
            buffer.writeBoolean(recipe.enchantable);
        }
    }
}
