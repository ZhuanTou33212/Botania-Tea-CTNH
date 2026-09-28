package com.qianxin.botaniatea.compat.create;

import com.qianxin.botaniatea.compat.OptionalCompat;

/** Create adapter entry point; deliberately has no hard Create class reference. */
public final class CreateElementAdapter {
    private CreateElementAdapter() {}
    public static boolean isAvailable() { return OptionalCompat.createLoaded(); }
    public static String scope() { return "crushing,milling,pressing,mixing,compacting,cutting,splashing,haunting,filling,emptying,deploying,mechanical_crafting,sequenced_assembly"; }
    public static com.qianxin.botaniatea.content.NormalizedProcess normalize(net.minecraft.world.item.crafting.Recipe<?> recipe, net.minecraft.core.RegistryAccess access) {
        if (recipe instanceof com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe sequence) {
            var inputs = new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();
            var tools = new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();
            var fluids = new java.util.ArrayList<String>();
            inputs.add(sequence.getIngredient());
            if (sequence.getLoops()<1 || sequence.getLoops()>1000) return null;
            for (int loop=0; loop<sequence.getLoops(); loop++) for (var step:sequence.getSequence()) {
                var normalized=normalize(step.getRecipe(),access);
                if (normalized==null) return null;
                // First input is the circulating workpiece; count only net added materials.
                if (normalized.inputs().size()>1) inputs.addAll(normalized.inputs().subList(1,normalized.inputs().size()));
                tools.addAll(normalized.retainedTools()); fluids.addAll(normalized.fluids());
            }
            return new com.qianxin.botaniatea.content.NormalizedProcess(recipe.getId(),inputs,tools,
                    sequence.resultPool.stream().map(r->new com.qianxin.botaniatea.content.NormalizedProcess.Output(r.getStack(),Float.toString(r.getChance()))).toList(),
                    fluids,net.minecraft.world.item.ItemStack.EMPTY,false,"SEQUENCE_WEIGHTED_ALTERNATIVES_REQUIRE_ALLOCATION");
        }
        if (!(recipe instanceof com.simibubi.create.content.processing.recipe.ProcessingRecipe<?> processing)) return null;
        var inputs=new java.util.ArrayList<>(recipe.getIngredients());
        var tools=new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();
        if (processing instanceof com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe application && application.shouldKeepHeldItem()) {
            inputs.clear(); inputs.add(application.getProcessedItem()); tools.add(application.getRequiredHeldItem());
        }
        var fluids=new java.util.ArrayList<String>();
        processing.getFluidIngredients().forEach(f->fluids.add("input:"+f.serialize()));
        processing.getFluidResults().forEach(f->fluids.add("output:"+f.writeToNBT(new net.minecraft.nbt.CompoundTag())));
        return new com.qianxin.botaniatea.content.NormalizedProcess(recipe.getId(),inputs,tools,
                processing.getRollableResults().stream().map(r->new com.qianxin.botaniatea.content.NormalizedProcess.Output(r.getStack(),Float.toString(r.getChance()))).toList(),
                fluids,net.minecraft.world.item.ItemStack.EMPTY,false,"");
    }
}
