package com.qianxin.teaart.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.qianxin.teaart.registry.ModItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TeaProcessingData {

    public static final String WITHERED = "withered";
    public static final String FIXED = "fixed";
    public static final String ROLLED = "rolled";
    public static final String VAT_PROCESSED = "vat_processed";
    public static final String YELLOWED = "yellowed";
    public static final String PILED = "piled";
    public static final String DRIED = "dried";
    public static final String JASMINE_SCENTED = "jasmine_scented";
    public static final String GARDENIA_SCENTED = "gardenia_scented";

    public static final List<String> KNOWN_PROCESSES = List.of(
            WITHERED,
            FIXED,
            ROLLED,
            VAT_PROCESSED,
            YELLOWED,
            PILED,
            DRIED,
            JASMINE_SCENTED,
            GARDENIA_SCENTED);

    private static final String PROCESSES_KEY = "TeaArtProcesses";

    private TeaProcessingData() {}

    public static boolean canApply(ItemStack stack, String process) {
        if (!stack.is(ModItems.TEA_LEAF.get())) {
            return false;
        }

        List<String> processes = getProcesses(stack);
        return switch (process) {
            case WITHERED -> processes.isEmpty();
            case FIXED -> processes.isEmpty();
            case ROLLED -> matches(processes, WITHERED) || matches(processes, FIXED) ||
                    matches(processes, WITHERED, ROLLED, VAT_PROCESSED);
            case VAT_PROCESSED -> matches(processes, WITHERED, ROLLED);
            case PILED -> matches(processes, FIXED, ROLLED);
            case YELLOWED -> matches(processes, FIXED, ROLLED);
            case DRIED -> processes.isEmpty() || matches(processes, WITHERED, ROLLED, VAT_PROCESSED) ||
                    matches(processes, FIXED, ROLLED) || matches(processes, FIXED, ROLLED, YELLOWED) ||
                    matches(processes, WITHERED, ROLLED, VAT_PROCESSED, ROLLED) ||
                    matches(processes, FIXED, ROLLED, PILED);
            default -> false;
        };
    }

    public static ItemStack apply(ItemStack input, String process) {
        ItemStack result = input.copy();
        result.setCount(1);

        List<String> processes = new ArrayList<>(getProcesses(result));
        processes.add(process);
        writeProcesses(result, processes);

        ItemStack finishedTea = getFinishedTea(processes);
        if (!finishedTea.isEmpty()) {
            return finishedTea;
        }

        return result;
    }

    public static boolean canScent(ItemStack stack) {
        return stack.is(ModItems.WHITE_TEA_LEAF.get()) && getProcesses(stack).isEmpty();
    }

    public static ItemStack scent(ItemStack input, String process) {
        ItemStack result = input.copy();
        result.setCount(1);
        writeProcesses(result, List.of(process));
        return result;
    }

    public static boolean isScentedTeaLeaf(ItemStack stack) {
        if (!stack.is(ModItems.WHITE_TEA_LEAF.get())) {
            return false;
        }
        List<String> processes = getProcesses(stack);
        return matches(processes, JASMINE_SCENTED) || matches(processes, GARDENIA_SCENTED);
    }

    public static boolean isDry(ItemStack stack) {
        if (!stack.is(ModItems.TEA_LEAF.get())) {
            return true;
        }
        return getProcesses(stack).contains(DRIED);
    }

    public static ItemStack dry(ItemStack input) {
        List<String> processes = getProcesses(input);
        if (input.is(ModItems.WHITE_TEA_LEAF.get()) && matches(processes, JASMINE_SCENTED)) {
            return new ItemStack(ModItems.JASMINE_FLOWER_TEA_LEAF.get());
        }
        if (input.is(ModItems.WHITE_TEA_LEAF.get()) && matches(processes, GARDENIA_SCENTED)) {
            return new ItemStack(ModItems.GARDENIA_TEA_LEAF.get());
        }
        return apply(input, DRIED);
    }

    public static boolean canDry(ItemStack stack) {
        return isScentedTeaLeaf(stack) || stack.is(ModItems.TEA_LEAF.get()) && canApply(stack, DRIED);
    }

    public static List<Component> getProcessTooltips(ItemStack stack) {
        List<Component> tooltips = new ArrayList<>();
        for (String process : getProcesses(stack)) {
            tooltips.add(Component.translatable("process.tea_art." + process));
        }
        return tooltips;
    }

    private static ItemStack getFinishedTea(List<String> processes) {
        if (matches(processes, DRIED)) {
            return new ItemStack(ModItems.WHITE_TEA_LEAF.get());
        }
        if (matches(processes, WITHERED, ROLLED, VAT_PROCESSED, DRIED)) {
            return new ItemStack(ModItems.RED_TEA_LEAF.get());
        }
        if (matches(processes, FIXED, ROLLED, DRIED)) {
            return new ItemStack(ModItems.GREEN_TEA_LEAF.get());
        }
        if (matches(processes, FIXED, ROLLED, YELLOWED, DRIED)) {
            return new ItemStack(ModItems.YELLOW_TEA_LEAF.get());
        }
        if (matches(processes, WITHERED, ROLLED, VAT_PROCESSED, ROLLED, DRIED)) {
            return new ItemStack(ModItems.OOLONG_TEA_LEAF.get());
        }
        if (matches(processes, FIXED, ROLLED, PILED, DRIED)) {
            return new ItemStack(ModItems.BLACK_TEA_LEAF.get());
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns an unmodifiable list of tea processing processes on this stack.
     */
    public static List<String> getProcesses(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        List<String> processes = new ArrayList<>();
        if (tag == null || !tag.contains(PROCESSES_KEY, Tag.TAG_LIST)) {
            return processes;
        }

        ListTag list = tag.getList(PROCESSES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            processes.add(list.getString(i));
        }
        return List.copyOf(processes);
    }

    /**
     * Returns true if the stack has the given process.
     */
    public static boolean hasProcess(ItemStack stack, String process) {
        return getProcesses(stack).contains(process);
    }

    /**
     * Returns true if the stack has at least one of the given processes.
     */
    public static boolean hasAnyProcess(ItemStack stack, Collection<String> processes) {
        List<String> stackProcesses = getProcesses(stack);
        for (String p : processes) {
            if (stackProcesses.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the stack has all of the given processes.
     */
    public static boolean hasAllProcesses(ItemStack stack, Collection<String> processes) {
        return getProcesses(stack).containsAll(processes);
    }

    /**
     * Returns true if the stack is a tea_leaf that has undergone at least one processing step.
     */
    public static boolean isProcessedTeaLeaf(ItemStack stack) {
        return stack.is(ModItems.TEA_LEAF.get()) && !getProcesses(stack).isEmpty();
    }

    /**
     * Checks whether a candidate item matches a filter sample.
     * For tea_leaf items with process NBT, does subset matching
     * (candidate must have at least the filter's processes).
     * For all other items, falls back to exact ItemStack match.
     */
    public static boolean matchesFilter(ItemStack filter, ItemStack candidate) {
        if (filter.is(ModItems.TEA_LEAF.get())) {
            if (!candidate.is(ModItems.TEA_LEAF.get())) {
                return false;
            }

            List<String> filterProcesses = getProcesses(filter);
            List<String> candidateProcesses = getProcesses(candidate);

            if (!filterProcesses.isEmpty()) {
                return candidateProcesses.containsAll(filterProcesses);
            } else {
                return candidateProcesses.isEmpty();
            }
        }
        return ItemStack.isSameItemSameTags(filter, candidate);
    }

    private static void writeProcesses(ItemStack stack, List<String> processes) {
        ListTag list = new ListTag();
        for (String process : processes) {
            list.add(StringTag.valueOf(process));
        }
        stack.getOrCreateTag().put(PROCESSES_KEY, list);
    }

    private static boolean matches(List<String> processes, String... expected) {
        if (processes.size() != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!processes.get(i).equals(expected[i])) {
                return false;
            }
        }
        return true;
    }
}
