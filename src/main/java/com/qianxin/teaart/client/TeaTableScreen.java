package com.qianxin.teaart.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qianxin.teaart.menu.TeaTableMenu;

public class TeaTableScreen extends AbstractContainerScreen<TeaTableMenu> {

    private static final ResourceLocation FURNACE_LOCATION = ResourceLocation
            .tryParse("minecraft:textures/gui/container/furnace.png");
    private static final ResourceLocation WATER_STILL = ResourceLocation.tryParse("minecraft:block/water_still");

    public TeaTableScreen(TeaTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 236;
        inventoryLabelY = 142;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawPanel(graphics, x, y, imageWidth, imageHeight);
        drawTank(graphics, x + 8, y + 24, 20, 96, menu.getWaterScale(92));
        drawSlot(graphics, x + 69, y + 17);   // 茶叶
        drawSlot(graphics, x + 51, y + 71);   // 茶杯
        drawSlot(graphics, x + 87, y + 71);   // 燃料
        drawSlot(graphics, x + 149, y + 71);  // 茶汤输出
        drawSlot(graphics, x + 149, y + 121); // 茶渣输出
        drawSlot(graphics, x + 16, y + 121);  // 水桶
        drawProgress(graphics, x + 111, y + 76, menu.getBrewScale(24));
        drawFlame(graphics, x + 89, y + 92, menu.getBurnScale(13));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, y + 153 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, y + 211);
        }
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // 原版容器底色 #C6C6C6：左上 1px 白色高光、右下 1px #555555 阴影
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);
        graphics.fill(x, y, x + width - 1, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + height - 1, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, 0xFF555555);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, 0xFF555555);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        // 原版槽位：1px #373737 边框 + #8B8B8B 内底
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    private static void drawTank(GuiGraphics graphics, int x, int y, int width, int height, int water) {
        graphics.fill(x, y, x + width, y + height, 0xFF1F2428);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF6F777E);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF101417);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF9EA7AD);
        if (water > 0) {
            TextureAtlasSprite waterSprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(WATER_STILL);
            RenderSystem.setShaderColor(0.35F, 0.55F, 1.0F, 0.9F);
            int innerLeft = x + 3;
            int innerRight = x + width - 3;
            int innerBottom = y + height - 3;
            int top = innerBottom - water;
            for (int ty = top; ty < innerBottom; ty += 16) {
                for (int tx = innerLeft; tx < innerRight; tx += 16) {
                    int drawWidth = Math.min(16, innerRight - tx);
                    int drawHeight = Math.min(16, innerBottom - ty);
                    graphics.blit(tx, ty, 0, drawWidth, drawHeight, waterSprite);
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.fill(innerLeft, top, innerRight, Math.min(top + 1, innerBottom), 0xAA8FD7FF);
        }
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, 0xFF0B0D0F);
        graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, 0xFF0B0D0F);
    }

    private static void drawProgress(GuiGraphics graphics, int x, int y, int progress) {
        if (FURNACE_LOCATION == null) {
            return;
        }
        if (progress > 0) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(FURNACE_LOCATION, x, y, 176, 14, progress + 1, 16);
        }
    }

    private static void drawFlame(GuiGraphics graphics, int x, int y, int burn) {
        if (FURNACE_LOCATION == null || burn <= 0) {
            return;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(FURNACE_LOCATION, x, y + 12 - burn, 176, 12 - burn, 14, burn + 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
