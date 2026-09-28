package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.menu.TeaEssenceDistillerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public final class TeaEssenceDistillerScreen extends AbstractContainerScreen<TeaEssenceDistillerMenu> {
    public TeaEssenceDistillerScreen(TeaEssenceDistillerMenu menu, Inventory inventory, Component title) { super(menu,inventory,title); imageHeight=176; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.botania_tea.mode"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+8,topPos+20,72,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.botania_tea.next_recipe"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+84,topPos+20,84,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF352A2A);
        for (var slot:menu.slots) g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFF777777);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {
        g.drawString(font, Component.translatable("gui.botania_tea.mode_"+menu.value(0)),8,6,0xFFFFFF,false);
        g.drawString(font,Item.byId(menu.value(1)).getDescription(),8,43,0xFFFFFF,false);
        g.drawString(font,menu.value(2)+" mB | "+menu.value(3)+" tick",8,78,0xFFFFFF,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) { renderBackground(g); super.render(g,x,y,partial); renderTooltip(g,x,y); }
}
