package com.qianxin.botaniatea.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;

public final class ClientDreamState {
    private static ClientLevel world;
    private static int remaining, duration;
    private ClientDreamState() {}
    public static void start(int ticks) { world=Minecraft.getInstance().level; remaining=duration=ticks; }
    public static void tick() {
        if (world != Minecraft.getInstance().level) { world=null; remaining=0; }
        if (remaining>0) remaining--;
    }
    public static void render(GuiGraphics graphics) {
        if (remaining<=0 || world==null || world!=Minecraft.getInstance().level) return;
        float progress=1F-(float)remaining/duration;
        int alpha=(int)(220 * Math.sin(Math.PI*progress));
        graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),(alpha<<24)|0x111022);
    }
}
