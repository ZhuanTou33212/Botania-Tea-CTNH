package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.recipe.OfferingCraftingRecipe;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Quantity-weighted random pigment draw for colorable crucible recipes.
 *
 * <p>Pigments share the twelve crystal slots with element crystals. A craft with no pigment
 * yields the white/default variant; otherwise exactly one dye stack is drawn, its chance
 * proportional to its stack size, and {@code dyeCost} items are consumed from it.
 */
public final class DyePool {
    private DyePool() {}

    /** A drawn pigment: which slot it came from and which colour word it represents. */
    public record Draw(int slot, String color) {}

    /** Weighted draw over every dye stack in the crystal slots; null when no pigment is present. */
    public static Draw pick(ItemStackHandler inventory, RandomSource random) {
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        int total = 0;
        for (int slot = OfferingCraftingRecipe.CRYSTAL_SLOT_START; slot < OfferingCraftingRecipe.CRYSTAL_SLOT_END; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof DyeItem dye) {
                String color = ColorVariants.colorOf(dye);
                if (color == null) continue;
                stacks.add(stack);
                slots.add(slot);
                total += stack.getCount();
            }
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (int i = 0; i < stacks.size(); i++) {
            roll -= stacks.get(i).getCount();
            if (roll < 0) return new Draw(slots.get(i), ColorVariants.colorOf(stacks.get(i).getItem()));
        }
        return null;
    }

    /** Consumes the drawn amount from the drawn slot. */
    public static void consume(ItemStackHandler inventory, Draw draw, int amount) {
        if (draw == null || amount <= 0) return;
        inventory.extractItem(draw.slot(), amount, false);
    }
}
