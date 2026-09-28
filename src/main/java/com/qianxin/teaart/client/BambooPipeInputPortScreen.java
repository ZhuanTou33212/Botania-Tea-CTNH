package com.qianxin.teaart.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.qianxin.teaart.menu.BambooPipeInputPortMenu;

public class BambooPipeInputPortScreen extends AbstractContainerScreen<BambooPipeInputPortMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_SHADOW = 0xFF8B8B8B;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;

    private Button modeButton;

    public BambooPipeInputPortScreen(BambooPipeInputPortMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = Button.builder(modeText(), button -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 126, topPos + 6, 42, 14).build();
        addRenderableWidget(modeButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) {
            modeButton.setMessage(modeText());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, PANEL_SHADOW);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);

        for (int i = 0; i < 10; i++) {
            drawSlot(graphics, leftPos + 25 + (i % 5) * 18, topPos + 19 + (i / 5) * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, leftPos + 7 + col * 18, topPos + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, leftPos + 7 + col * 18, topPos + 141);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component modeText() {
        return Component.translatable(menu.isWhitelist() ? "screen.tea_art.whitelist" : "screen.tea_art.blacklist");
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, SLOT_HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, PANEL_SHADOW);
    }
}
