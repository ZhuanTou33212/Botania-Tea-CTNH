package com.qianxin.botaniatea.recipe;

import com.google.gson.JsonObject;
import com.qianxin.botaniatea.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/** Directional enchantments are explicit recipes; no arbitrary NBT or enchantment recovery is implied. */
public final class OfferingEnchantingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient target;
    private final Map<net.minecraft.world.item.Item, Integer> elements;
    private final int ascended, mana, duration, level;
    private final ResourceLocation enchantmentId;
    /** Vestigial: the Gaia tier is gated by the recipe catalyst now, so this value is never enforced. */
    private final ResourceLocation requiredAdvancement;
    /** When true the enchantment is drawn at random from the vanilla pool; enchantmentId/level are then only a fallback. */
    private final boolean random;
    /** Optional extra item the offering must contain (the Gaia gate for Gaia-tier gear). */
    private final Ingredient catalyst;
    private final int catalystCount;

    public OfferingEnchantingRecipe(ResourceLocation id, Ingredient target, Map<net.minecraft.world.item.Item, Integer> elements,
                                    int ascended, int mana, int duration, ResourceLocation enchantmentId, int level) {
        this(id,target,elements,ascended,mana,duration,enchantmentId,level,null,false);
    }
    public OfferingEnchantingRecipe(ResourceLocation id, Ingredient target, Map<net.minecraft.world.item.Item, Integer> elements,
                                    int ascended, int mana, int duration, ResourceLocation enchantmentId, int level, ResourceLocation requiredAdvancement) {
        this(id,target,elements,ascended,mana,duration,enchantmentId,level,requiredAdvancement,false,Ingredient.EMPTY,0);
    }
    public OfferingEnchantingRecipe(ResourceLocation id, Ingredient target, Map<net.minecraft.world.item.Item, Integer> elements,
                                    int ascended, int mana, int duration, ResourceLocation enchantmentId, int level,
                                    ResourceLocation requiredAdvancement, boolean random) {
        this(id,target,elements,ascended,mana,duration,enchantmentId,level,requiredAdvancement,random,Ingredient.EMPTY,0);
    }
    public OfferingEnchantingRecipe(ResourceLocation id, Ingredient target, Map<net.minecraft.world.item.Item, Integer> elements,
                                    int ascended, int mana, int duration, ResourceLocation enchantmentId, int level,
                                    ResourceLocation requiredAdvancement, boolean random, Ingredient catalyst, int catalystCount) {
        this.id = id; this.target = target; this.elements = Map.copyOf(elements); this.ascended = ascended; this.mana = mana;
        this.duration = duration; this.enchantmentId = enchantmentId; this.level = level;
        this.random = random; this.catalyst = catalyst == null ? Ingredient.EMPTY : catalyst; this.catalystCount = catalystCount;
        Enchantment enchantment=ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        if(enchantment==null || level>enchantment.getMaxLevel()) throw new IllegalArgumentException("Unknown enchantment or invalid level: "+id);
        this.requiredAdvancement=requiredAdvancement!=null?requiredAdvancement:enchantment.isTreasureOnly()?ResourceLocation.parse("botania:main/gaia_guardian_kill"):null;
        if (target.isEmpty() || elements.size()>12 || ascended<1 || ascended>64 || mana<0 || mana>100000 || duration<1 || duration>32767 || level<1)
            throw new IllegalArgumentException("Invalid enchanting limits: "+id);
        for (var entry:elements.entrySet()) if (entry.getKey()==null || !com.qianxin.botaniatea.registry.ModItems.isElementCrystal(new ItemStack(entry.getKey())) || entry.getValue()<1 || entry.getValue()>768)
            throw new IllegalArgumentException("Invalid enchanting source crystal: "+id);
        if (elements.values().stream().mapToInt(count -> (count+63)/64).sum()>12) throw new IllegalArgumentException("Enchanting crystals exceed 12 slots: "+id);
    }
    public Map<net.minecraft.world.item.Item, Integer> elements() { return elements; }
    public int ascended() { return ascended; }
    public int mana() { return mana; }
    public int duration() { return duration; }
    public ResourceLocation enchantmentId() { return enchantmentId; }
    public int level() { return level; }
    public ResourceLocation requiredAdvancement() { return requiredAdvancement; }
    public boolean random() { return random; }
    public Ingredient catalyst() { return catalyst; }
    public int catalystCount() { return catalystCount; }

    public Ingredient target() { return target; }

    /**
     * Target-legality check that ignores where the crystals live, so the runic-altar multiblock
     * can reuse the same ladder/compatibility rules as the crucible container.
     */
    public boolean acceptsTarget(ItemStack targetStack) {
        if (targetStack.isEmpty() || targetStack.getCount() != 1 || !target.test(targetStack) || !safeTarget(targetStack)) return false;
        if (random) {
            return !com.qianxin.botaniatea.content.EnchantPool.poolForTarget(targetStack).isEmpty();
        }
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        if (enchantment == null || this.level > enchantment.getMaxLevel()
                || (!targetStack.is(Items.BOOK) && !targetStack.is(Items.ENCHANTED_BOOK) && !enchantment.canEnchant(targetStack))) return false;
        int current = EnchantmentHelper.getEnchantments(targetStack).getOrDefault(enchantment,0);
        if (current != this.level - 1) return false;
        for (Enchantment existing : EnchantmentHelper.getEnchantments(targetStack).keySet()) {
            if (existing != enchantment && !existing.isCompatibleWith(enchantment)) return false;
        }
        return true;
    }
    /** Magic crystals required by this recipe; drives the enchantment level in random mode. */
    public int magicCrystalCount() {
        var magic = com.qianxin.botaniatea.registry.ModItems.ELEMENT_CRYSTALS.get(com.qianxin.botaniatea.content.ElementType.MAGIC);
        return magic == null ? 0 : elements.getOrDefault(magic.get(), 0);
    }
    private static final java.util.Set<String> SAFE_TAGS=java.util.Set.of("display","Damage","RepairCost","Enchantments","StoredEnchantments","Unbreakable","AttributeModifiers","HideFlags","CanDestroy","CanPlaceOn","Trim");
    private static boolean safeTarget(ItemStack stack) {
        if(stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()
                || stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()
                || stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent()) return false;
        var serialized=stack.serializeNBT();
        if(serialized.contains("ForgeCaps") && !serialized.getCompound("ForgeCaps").isEmpty()) return false;
        return !stack.hasTag() || SAFE_TAGS.containsAll(stack.getTag().getAllKeys());
    }
    public boolean matchesContainer(Container container, Level world) {
        ItemStack targetStack = container.getItem(12);
        if (targetStack.isEmpty() || targetStack.getCount() != 1 || !target.test(targetStack) || !safeTarget(targetStack)) return false;
        ItemStack ascendedStack = container.getItem(13);
        if (ascended == 0 ? !ascendedStack.isEmpty() : ascendedStack.isEmpty() || ascendedStack.getCount() < ascended) return false;
        if (!ascendedStack.is(com.qianxin.botaniatea.registry.ModItems.ASCENDED_CRYSTAL.get())) return false;
        if (random) {
            // Random mode consumes a plain book and lets the station draw the enchantment, so no
            // fixed-enchantment ladder, compatibility or level checks apply here.
            if (com.qianxin.botaniatea.content.EnchantPool.poolForTarget(targetStack).isEmpty()) return false;
        } else {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (enchantment == null || this.level > enchantment.getMaxLevel() || (!targetStack.is(Items.BOOK) && !targetStack.is(Items.ENCHANTED_BOOK) && !enchantment.canEnchant(targetStack))) return false;
            int current = EnchantmentHelper.getEnchantments(targetStack).getOrDefault(enchantment,0);
            if (current != this.level - 1) return false;
            for (Enchantment existing : EnchantmentHelper.getEnchantments(targetStack).keySet()) {
                if (existing != enchantment && !existing.isCompatibleWith(enchantment)) return false;
            }
        }
        Map<net.minecraft.world.item.Item, Integer> found = new LinkedHashMap<>();
        for (int slot = 0; slot < 12; slot++) { ItemStack stack = container.getItem(slot); if (!stack.isEmpty()) found.merge(stack.getItem(), stack.getCount(), Integer::sum); }
        return found.equals(elements);
    }
    @Override public boolean matches(Container container, Level level) { return matchesContainer(container, level); }
    @Override public ItemStack assemble(Container container, net.minecraft.core.RegistryAccess access) {
        return assembleFor(container.getItem(12), net.minecraft.util.RandomSource.create());
    }

    /** Layout-agnostic assembly, also usable by the runic-altar multiblock which has no 12/13 slots. */
    public ItemStack assembleFor(ItemStack target, net.minecraft.util.RandomSource randomSource) {
        return assembleFor(target, randomSource, magicCrystalCount());
    }

    /** Random mode rolls every entry from the pool this target accepts, using the invested crystals. */
    public ItemStack assembleFor(ItemStack target, net.minecraft.util.RandomSource randomSource, int investedCrystals) {
        ItemStack result = target.copy();
        if (result.is(Items.BOOK)) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
            if (target.hasTag()) for(String key:java.util.List.of("display","RepairCost","HideFlags"))
                if(target.getTag().contains(key)) result.getOrCreateTag().put(key,target.getTag().get(key).copy());
        }
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        int appliedLevel = level;
        if (random) {
            var rolled = com.qianxin.botaniatea.content.EnchantPool.rollForTarget(result, investedCrystals, randomSource);
            if (rolled.isEmpty()) return ItemStack.EMPTY;
            Map<Enchantment, Integer> merged = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(result));
            for (var entry : rolled.entrySet()) {
                if (merged.keySet().stream().anyMatch(existing -> existing != entry.getKey()
                        && !existing.isCompatibleWith(entry.getKey()))) continue;
                merged.put(entry.getKey(), entry.getValue());
            }
            EnchantmentHelper.setEnchantments(merged, result);
            return result;
        }
        if (enchantment == null) return ItemStack.EMPTY;
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(result));
        enchantments.put(enchantment, appliedLevel);
        EnchantmentHelper.setEnchantments(enchantments, result);
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(net.minecraft.core.RegistryAccess access) { return ItemStack.EMPTY; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.OFFERING_ENCHANTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.OFFERING_ENCHANTING.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, target); }

    public static final class Serializer implements RecipeSerializer<OfferingEnchantingRecipe> {
        @Override public OfferingEnchantingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient target = Ingredient.fromJson(json.get("target"));
            Map<net.minecraft.world.item.Item, Integer> elements = new LinkedHashMap<>();
            JsonObject values = GsonHelper.getAsJsonObject(json, "elements");
            for (String key : values.keySet()) elements.put(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(key.contains(":") ? key : "botania_tea:" + key)), GsonHelper.getAsInt(values, key));
            JsonObject enchantment = GsonHelper.getAsJsonObject(json, "enchantment");
            return new OfferingEnchantingRecipe(id, target, elements, GsonHelper.getAsInt(json, "ascended_crystals", 1), GsonHelper.getAsInt(json, "mana"),
                    GsonHelper.getAsInt(json, "duration"), ResourceLocation.parse(GsonHelper.getAsString(enchantment, "id")), GsonHelper.getAsInt(enchantment, "level"),
                    json.has("required_advancement")?ResourceLocation.parse(GsonHelper.getAsString(json,"required_advancement")):null,
                    GsonHelper.getAsBoolean(json, "random", false),
                    json.has("catalyst")?Ingredient.fromJson(json.get("catalyst")):Ingredient.EMPTY,
                    GsonHelper.getAsInt(json, "catalyst_count", 0));
        }
        @Override public OfferingEnchantingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient target = Ingredient.fromNetwork(buffer); int count = buffer.readVarInt(); Map<net.minecraft.world.item.Item, Integer> elements = new LinkedHashMap<>();
            if(count<0 || count>12) throw new IllegalArgumentException("Invalid element type count");
            for (int i = 0; i < count; i++) elements.put(ForgeRegistries.ITEMS.getValue(buffer.readResourceLocation()), buffer.readVarInt());
            int ascended=buffer.readVarInt(),mana=buffer.readVarInt(),duration=buffer.readVarInt();
            ResourceLocation enchantment=buffer.readResourceLocation(); int level=buffer.readVarInt();
            return new OfferingEnchantingRecipe(id,target,elements,ascended,mana,duration,enchantment,level,buffer.readBoolean()?buffer.readResourceLocation():null,buffer.readBoolean(),buffer.readBoolean()?Ingredient.fromNetwork(buffer):Ingredient.EMPTY,buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, OfferingEnchantingRecipe recipe) {
            recipe.target.toNetwork(buffer); buffer.writeVarInt(recipe.elements.size());
            recipe.elements.entrySet().stream().sorted(java.util.Comparator.comparing(e -> ForgeRegistries.ITEMS.getKey(e.getKey()).toString())).forEach(e -> { buffer.writeResourceLocation(ForgeRegistries.ITEMS.getKey(e.getKey())); buffer.writeVarInt(e.getValue()); });
            buffer.writeVarInt(recipe.ascended); buffer.writeVarInt(recipe.mana); buffer.writeVarInt(recipe.duration); buffer.writeResourceLocation(recipe.enchantmentId); buffer.writeVarInt(recipe.level);
            buffer.writeBoolean(recipe.requiredAdvancement!=null); if(recipe.requiredAdvancement!=null) buffer.writeResourceLocation(recipe.requiredAdvancement);
            buffer.writeBoolean(recipe.random);
            buffer.writeBoolean(!recipe.catalyst.isEmpty()); if(!recipe.catalyst.isEmpty()) recipe.catalyst.toNetwork(buffer);
            buffer.writeVarInt(recipe.catalystCount);
        }
    }
}
