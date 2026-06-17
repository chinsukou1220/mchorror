package com.example.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WarningScreenPage2 extends Screen {

    private final Screen previousScreen;

    public WarningScreenPage2(Screen previousScreen) {
        super(Component.literal("Mod Story Screen"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();
        
        // 閉じる（ゲーム開始）ボタン
        this.addRenderableWidget(Button.builder(Component.literal("Play"), button -> {
            this.minecraft.setScreen(this.previousScreen);
        }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int y = this.height / 2 - 50;
        int spacing = 20;
        
        // タイトル
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
        guiGraphics.drawCenteredString(this.font, "SSTTAALLKKEERR", (int)((this.width / 2) / 1.5f), (int)(y / 1.5f), 0xFF0000);
        guiGraphics.pose().popPose();
        
        y += 40;
        
        guiGraphics.drawCenteredString(this.font, "An eerie presence is targeting you.", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "Try to ignore it and live normally.", this.width / 2, y, 0xFFFFFF); y += spacing;
        
        y += 10;
        guiGraphics.drawCenteredString(this.font, "We strongly advise not showing any signs of weakness.", this.width / 2, y, 0xAAAAAA); y += spacing;
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
