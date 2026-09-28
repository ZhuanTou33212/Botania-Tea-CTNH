package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.menu.MagicCrucibleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MagicCrucibleScreen extends AbstractContainerScreen<MagicCrucibleMenu> {
    private net.minecraft.client.gui.components.Button selection;
    public MagicCrucibleScreen(MagicCrucibleMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 172; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.botania_tea.authorize"),button->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+124,topPos+54,44,16).build());
        selection=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.botania_tea.next_enchantment"),button->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+124,topPos+1,44,16).build());
    }
    @Override protected void containerTick() { super.containerTick(); selection.setTooltip(net.minecraft.client.gui.components.Tooltip.create(menu.enchantmentDescription())); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2, top = (height - imageHeight) / 2;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF352A2A);
        graphics.fill(left + 6, top + 6, left + 170, top + 72, 0xFF1D1717);
        graphics.fill(left + 6, top + 78, left + 170, top + 166, 0xFF241B1B);
        for(int i=0;i<menu.slots.size();i++) {
            var slot=menu.slots.get(i);
            int color=i<12?0xFF765889:i==12?0xFFBCA369:i==13?0xFF6B9CB3:i==14?0xFF7AA575:0xFF777777;
            graphics.fill(left+slot.x-1,top+slot.y-1,left+slot.x+17,top+slot.y+17,color);
            graphics.fill(left+slot.x,top+slot.y,left+slot.x+16,top+slot.y+16,0xFF302B32);
        }
        graphics.drawString(font,Component.translatable("gui.botania_tea.progress_ticks",menu.progress()),left+8,top+57,0xD6B3ED,false);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics); super.render(graphics,mouseX,mouseY,partialTick); renderTooltip(graphics,mouseX,mouseY);
        if(hoveredSlot!=null && !hoveredSlot.hasItem() && hoveredSlot.index<16) {
            int slot=hoveredSlot.index;
            String type=slot<12?"crystals":slot==12?"substrate":slot==13?"ascended":slot==14?"output":"returns";
            graphics.renderTooltip(font,Component.translatable("gui.botania_tea.slot."+type),mouseX,mouseY);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.botania_tea.mana", menu.mana()), 8, 70, 0xAADDFF, false);
    }
}
