package com.mittel.ssttaallkkeerr.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WarningScreen extends Screen {

    private final Screen previousScreen;

    public WarningScreen(Screen previousScreen) {
        // ナレーター用のタイトル
        super(Component.literal("Mod Explanation Screen"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();
        
        // テストサウンド再生ボタン
        this.addRenderableWidget(Button.builder(Component.literal("Play Test Sound"), button -> {
            // テストサウンドとしてピー音を流す（音量をさらに今の5倍の15.0Fに設定）
            this.minecraft.getSoundManager().play(
                new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                    com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.BEEP.getLocation(),
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    15.0F, 1.0F,
                    net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom(),
                    false, 0,
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                    0.0D, 0.0D, 0.0D, true
                )
            );
        }).bounds(this.width / 2 - 130, this.height - 75, 120, 20).build());

        // 設定画面を開くボタン
        this.addRenderableWidget(Button.builder(Component.literal("Options..."), button -> {
            // マイクラ標準の設定画面を開く（戻るボタンでこの画面に戻ってくる）
            this.minecraft.setScreen(new net.minecraft.client.gui.screens.OptionsScreen(this, this.minecraft.options));
        }).bounds(this.width / 2 + 10, this.height - 75, 120, 20).build());

        // 次へ進むボタン（2枚目へ）
        this.addRenderableWidget(Button.builder(Component.literal("Next"), button -> {
            // 2枚目の画面へ切り替え
            this.minecraft.setScreen(new WarningScreenPage2(this.previousScreen));
        }).bounds(this.width / 2 - 100, this.height - 35, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int y = Math.max(10, this.height / 2 - 110);
        int spacing = 12;
        
        guiGraphics.drawCenteredString(this.font, "[ WARNING ]", this.width / 2, y, 0xFF5555); y += spacing;
        
        y += 4;
        guiGraphics.drawCenteredString(this.font, "This mod contains the following elements.", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "Please read carefully before playing.", this.width / 2, y, 0xFFFFFF); y += spacing;
        
        y += 4;
        guiGraphics.drawCenteredString(this.font, "- Sudden loud noises, horror elements, and jumpscares", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "- Intense screen flashing", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "- World modification", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "- Intentional forced crash", this.width / 2, y, 0xFFFFFF); y += spacing;
        
        y += 8;
        guiGraphics.drawCenteredString(this.font, "[ DISCLAIMER ]", this.width / 2, y, 0xFF5555); y += spacing;
        
        guiGraphics.drawCenteredString(this.font, "Do not play if you have a weak heart or are sensitive to horror.", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "I strongly recommend backing up your world data before playing.", this.width / 2, y, 0xFFFFFF); y += spacing;
        guiGraphics.drawCenteredString(this.font, "The creator is not responsible for any data corruption or health issues.", this.width / 2, y, 0xFFFFFF); y += spacing;
        
        guiGraphics.drawCenteredString(this.font, "Please click the button below to play a test sound and adjust your volume.", this.width / 2, this.height - 90, 0xAAAAAA); 
        
        guiGraphics.drawCenteredString(this.font, "Adjust your Hostile Creatures volume until the test sound is slightly loud.", this.width / 2, this.height - 50, 0x55FF55);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        // プレイヤーがESCキーで勝手に閉じられないようにする
        return false;
    }
}
