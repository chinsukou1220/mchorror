package com.example.client;

import com.example.SsttaallkkeerrMod;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class JumpscareOverlay implements HudRenderCallback {
    private static final ResourceLocation JUMPSCARE_TEXTURE = new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/gui/jumpscare.png");
    private static long showUntil = 0;

    public static void trigger(long durationMs) {
        showUntil = System.currentTimeMillis() + durationMs;
    }

    @Override
    public void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        if (System.currentTimeMillis() < showUntil) {
            Minecraft client = Minecraft.getInstance();
            if (client.getWindow() == null) return;
            
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            
            // 画面全体に画像を描画（必要に応じてサイズやアスペクト比を調整可能）
            // Zオフセットを高くして最前面に表示
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 1000); // 1000ほどZ軸を手前に持ってくる
            guiGraphics.blit(JUMPSCARE_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
            guiGraphics.pose().popPose();
        }
    }
}
