package com.qianxin.botaniatea.client;

import com.qianxin.botaniatea.network.HomingOptionsPacket;
import com.qianxin.botaniatea.network.HomingSelectionPacket;
import com.qianxin.botaniatea.network.HomingRenamePacket;
import com.qianxin.botaniatea.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Coordinates come from the server registry; no client world scan or chunk-loading request. */
public final class HomingSelectionScreen extends Screen {
    private final HomingOptionsPacket options;
    private EditBox name;

    public HomingSelectionScreen(HomingOptionsPacket options) {
        super(Component.literal("归巢牵牛 · 选择同维度目标"));
        this.options = options;
    }

    public static void open(HomingOptionsPacket options) {
        var client = Minecraft.getInstance();
        if (client.level != null && client.player != null && client.level.dimension().location().equals(options.dimension()))
            client.setScreen(new HomingSelectionScreen(options));
    }

    private static String coordinates(@Nullable BlockPos pos) {
        return pos == null ? "未设置" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private void request(int page, boolean select, @Nullable BlockPos target) {
        // Wait for the server's acknowledgement before showing a successful selection.
        for (var child : children()) if (child instanceof Button button) button.active = false;
        ModNetwork.CHANNEL.sendToServer(new HomingSelectionPacket(options.dimension(), options.source(), page, select, target));
    }

    @Override protected void init() {
        int left = (width - 300) / 2;
        int top = Math.max(4, (height - 250) / 2);
        name = addRenderableWidget(new EditBox(font, left, top + 54, 220, 18, Component.literal("本站名称")));
        name.setMaxLength(32);
        name.setValue(options.sourceName());
        name.setHint(Component.literal("本站名称（留空恢复坐标）"));
        name.setTooltip(Tooltip.create(Component.literal("本站：" + coordinates(options.source()) + "；最多32字符")));
        addRenderableWidget(Button.builder(Component.literal("保存名称"), ignored -> {
            for (var child : children()) if (child instanceof Button button) button.active = false;
            ModNetwork.CHANNEL.sendToServer(new HomingRenamePacket(options.dimension(), options.source(), options.page(), name.getValue()));
        }).bounds(left + 225, top + 53, 75, 20).build());
        for (int i = 0; i < options.options().size(); i++) {
            var option = options.options().get(i);
            String label = (option.pos().equals(options.selected()) ? "✓ " : "")
                    + (option.name().isEmpty() ? "" : option.name() + " · ") + coordinates(option.pos())
                    + (option.available() ? "" : "（未加载/边界外）");
            var button = Button.builder(Component.literal(font.plainSubstrByWidth(label, 286)), ignored -> request(options.page(), true, option.pos()))
                    .tooltip(Tooltip.create(Component.literal(label)))
                    .bounds(left, top + 96 + i * 20, 300, 18).build();
            button.active = option.available();
            addRenderableWidget(button);
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("上一页"), ignored -> request(options.page() - 1, false, null))
                .bounds(left, top + 204, 70, 20).build());
        previous.active = options.page() > 0;
        var next = addRenderableWidget(Button.builder(Component.literal("下一页"), ignored -> request(options.page() + 1, false, null))
                .bounds(left + 230, top + 204, 70, 20).build());
        next.active = options.page() + 1 < options.pages();
        addRenderableWidget(Button.builder(Component.literal("清除目标"), ignored -> request(options.page(), true, null))
                .bounds(left + 75, top + 204, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("刷新状态"), ignored -> request(options.page(), false, null))
                .bounds(left + 152, top + 204, 72, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int top = Math.max(4, (height - 250) / 2);
        graphics.drawCenteredString(font, title, width / 2, top + 3, 0xFFFFFF);
        graphics.drawCenteredString(font, font.plainSubstrByWidth("当前目标：" + options.selectedName() + " " + coordinates(options.selected()), 300), width / 2, top + 20, 0xCCE8B4);
        graphics.drawCenteredString(font, options.readiness(), width / 2, top + 35, 0xBBBBBB);
        graphics.drawCenteredString(font, "第 " + (options.page() + 1) + " / " + options.pages() + " 页 · ESC关闭", width / 2, top + 80, 0xBBBBBB);
        if (options.options().isEmpty()) graphics.drawCenteredString(font, "本维度暂无其他已登记的归巢牵牛", width / 2, top + 120, 0xBBBBBB);
        graphics.drawCenteredString(font, options.status(), width / 2, top + 232, 0xFFE099);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
