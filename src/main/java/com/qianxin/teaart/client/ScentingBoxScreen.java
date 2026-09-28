package com.qianxin.teaart.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qianxin.teaart.menu.ScentingBoxMenu;

public class ScentingBoxScreen extends AbstractContainerScreen<ScentingBoxMenu> {

    private static final ResourceLocation FURNACE_LOCATION = ResourceLocation
            .tryParse("minecraft:textures/gui/container/furnace.png");

    public ScentingBoxScreen(ScentingBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(FURNACE_LOCATION, x, y, 0, 0, imageWidth, imageHeight);
        graphics.blit(FURNACE_LOCATION, x + 79, y + 16, 55, 16, 18, 18);
        int burn = menu.getBurnScale(13);
        if (burn > 0) {
            graphics.blit(FURNACE_LOCATION, x + 56, y + 36 + 12 - burn, 176, 12 - burn, 14, burn + 1);
        }
        int progress = menu.getProgressScale(24);
        if (progress > 0) {
            graphics.blit(FURNACE_LOCATION, x + 79, y + 34, 176, 14, progress + 1, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
