package com.qianxin.teaart.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeaLeafItem extends Item {

    public TeaLeafItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<Component> processes = TeaProcessingData.getProcessTooltips(stack);
        if (processes.isEmpty()) {
            return;
        }

        tooltip.add(Component.translatable("tooltip.tea_art.nbt_processes").withStyle(ChatFormatting.GRAY));
        for (Component process : processes) {
            tooltip.add(Component.literal(" - ").append(process).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
