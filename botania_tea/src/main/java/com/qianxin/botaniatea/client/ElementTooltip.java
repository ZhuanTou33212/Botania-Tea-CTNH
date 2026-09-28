package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.content.ElementType;
import com.qianxin.botaniatea.network.ElementProfilesPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/**
 * Element readout for items, contributed as native tooltip lines.
 *
 * <p>Minecraft owns the tooltip layout, wrapping and positioning, so this adds rows through
 * {@link ItemTooltipEvent} instead of painting a badge of its own.
 *
 * <p>Enchanted books carry their enchantments in NBT, so they bypass the item-level profile:
 * their magic equals {@code sum(level * 10)} over every enchantment on the stack.
 */
@Mod.EventBusSubscriber(modid="botania_tea", value=Dist.CLIENT)
public final class ElementTooltip {
    private static final Map<ResourceLocation,Map<ElementType,String>> PROFILES=new HashMap<>();

    /** First private-use codepoint of the glyph sheet; glyphs follow ElementType order. */
    public static final char GLYPH_BASE = '\uE000';
    private static final ResourceLocation ELEMENT_FONT = ResourceLocation.fromNamespaceAndPath("botania_tea","elements");
    private static final ResourceLocation DEFAULT_FONT = ResourceLocation.fromNamespaceAndPath("minecraft","default");

    /** Magic granted per enchantment level on an enchanted book. */
    public static final int MAGIC_PER_ENCHANT_LEVEL = 10;

    private ElementTooltip() {}

    public static void accept(ElementProfilesPacket packet) {
        if (packet.reset()) PROFILES.clear();
        PROFILES.putAll(packet.entries());
    }
    public static Map<ElementType,String> profile(ResourceLocation item) { return PROFILES.get(item); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PROFILES.clear(); }

    /** Enchanted-book magic: every level on every enchantment is worth {@link #MAGIC_PER_ENCHANT_LEVEL}. */
    public static int enchantedBookMagic(ItemStack stack) {
        Map<Enchantment,Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        int total=0;
        for (int level : enchantments.values()) total += level * MAGIC_PER_ENCHANT_LEVEL;
        return total;
    }

    /**
     * NBT that leaves the item-level element profile valid. Enchantments, durability, trims and
     * display data do not change what an item is made of, so enchanted gear still reports elements.
     */
    private static final java.util.Set<String> SAFE_TAGS=java.util.Set.of("display","Damage","RepairCost",
            "Enchantments","StoredEnchantments","Unbreakable","AttributeModifiers","HideFlags","CanDestroy","CanPlaceOn","Trim");

    private static boolean safeNbt(ItemStack stack) {
        return !stack.hasTag() || SAFE_TAGS.containsAll(stack.getTag().getAllKeys());
    }
    /** Adds enchantment magic to a displayed amount, which the server may send as a rational. */
    private static String addMagic(String base,String bonus) {
        int slash=base.indexOf('/');
        long num,den;
        try {
            if (slash<0) { num=Long.parseLong(base.trim()); den=1; }
            else { num=Long.parseLong(base.substring(0,slash).trim()); den=Long.parseLong(base.substring(slash+1).trim()); }
        } catch (NumberFormatException unparsable) { return base; }
        num += Long.parseLong(bonus) * den;
        if (den==1) return Long.toString(num);
        long g=gcd(Math.abs(num),den);
        return (num/g)+"/"+(den/g);
    }
    private static long gcd(long a,long b) { while (b!=0) { long t=a%b; a=b; b=t; } return a==0?1:a; }

    /** Appends the element rows to the native tooltip. */
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        event.getToolTip().addAll(describe(event.getItemStack()));
    }

    /**
     * One row per element in {@link ElementType} order. A stack with no profile -- or one whose
     * NBT makes the item-level profile wrong -- reports that its elements are unresolved instead.
     */
    static List<Component> describe(ItemStack stack) {
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id==null) return List.of();
        Map<ElementType,String> display=PROFILES.get(id);
        if (stack.is(Items.ENCHANTED_BOOK)) {
            int magic=enchantedBookMagic(stack);
            display = magic>0 ? Map.of(ElementType.MAGIC,Integer.toString(magic)) : null;
        } else if (display!=null && !safeNbt(stack)) {
            display = null;
        }
        if (display==null) {
            return List.of(Component.translatable("gui.botania_tea.elements_unresolved")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        // Each row leads with the source crystal icon taken from the bitmap font glyphs, then
        // the amount in the default font.
        // Enchantments add magic on top of the base vector, matching the enchanted-book rule.
        Map<ElementType,String> shown=new java.util.EnumMap<>(ElementType.class);
        shown.putAll(display);
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            int bonus=enchantedBookMagic(stack);
            if (bonus>0) shown.merge(ElementType.MAGIC,Integer.toString(bonus),ElementTooltip::addMagic);
        }
        List<Component> rows=new ArrayList<>();
        // Which substrate the ritual requires for this item. Without it the mapping is guesswork.
        ItemStack substrate=com.qianxin.botaniatea.content.SubstrateCategory.substrateFor(stack);
        if (!substrate.isEmpty()) rows.add(Component.translatable("tooltip.botania_tea.substrate")
                .append(" ").append(substrate.getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
        // All elements share a single tooltip line: one row per element made the box grow with the
        // element count and eat the screen while hovering. Each entry is still glyph + amount, in
        // ElementType order, separated by a space.
        MutableComponent line=Component.empty();
        boolean first=true;
        for (ElementType type : ElementType.values()) {
            String amount=shown.get(type);
            if (amount==null) continue;
            if (!first) {
                line.append(Component.literal(" ").withStyle(Style.EMPTY.withFont(DEFAULT_FONT)));
            }
            first=false;
            line.append(Component.literal(String.valueOf((char)(GLYPH_BASE+type.ordinal())))
                            .withStyle(Style.EMPTY.withFont(ELEMENT_FONT)))
                    .append(Component.literal(" "+amount)
                            .withStyle(Style.EMPTY.withFont(DEFAULT_FONT).withColor(ChatFormatting.GRAY)));
        }
        if (!first) rows.add(line);
        return rows;
    }
}