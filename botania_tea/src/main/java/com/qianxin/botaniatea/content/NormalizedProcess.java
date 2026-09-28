package com.qianxin.botaniatea.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

/** A read-only view: probability is metadata, never an expected-value price multiplier. */
public record NormalizedProcess(ResourceLocation id, List<Ingredient> inputs, List<Ingredient> retainedTools,
        List<Output> outputs, List<String> fluids, ItemStack servingContainer, boolean craftingRemainders, String warning) {
    public NormalizedProcess {
        inputs=List.copyOf(inputs); retainedTools=List.copyOf(retainedTools); outputs=List.copyOf(outputs);
        fluids=List.copyOf(fluids); servingContainer=servingContainer.copy();
    }
    public record Output(ItemStack stack, String chance) { public Output { stack=stack.copy(); } }
    public boolean deterministicSingleOutput() { return outputs.size()==1 && outputs.get(0).chance().equals("1.0") && fluids.isEmpty() && warning.isEmpty(); }
}
