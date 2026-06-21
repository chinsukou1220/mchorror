package com.mittel.ssttaallkkeerr.client;

import com.mittel.ssttaallkkeerr.SsttaallkkeerrMod;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class JumpscareOverlay implements HudRenderCallback {
    private static final ResourceLocation JUMPSCARE_TEXTURE_1 = new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/gui/jumpscare_1.png");
    private static final ResourceLocation JUMPSCARE_TEXTURE_2 = new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/gui/jumpscare_2.png");
    private static final ResourceLocation JUMPSCARE_TEXTURE_3 = new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/gui/jumpscare_3.png");
    private static final ResourceLocation NOISE_TEXTURE = new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/gui/noise.png");
    private static long showUntil = 0;
    private static int currentImageIndex = 1;

    public static void trigger(long durationMs, int imageIndex) {
        showUntil = System.currentTimeMillis() + durationMs;
        currentImageIndex = imageIndex;
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
            
            ResourceLocation texture = switch (currentImageIndex) {
                case 2 -> JUMPSCARE_TEXTURE_2;
                case 3 -> JUMPSCARE_TEXTURE_3;
                default -> JUMPSCARE_TEXTURE_1;
            };
            
            guiGraphics.blit(texture, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
            
            // ノイズの描画（ブレンドを有効にして半透明表示）
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            
            guiGraphics.pose().pushPose();
            // 画面中心を基準にしてランダムに反転や少しの拡縮を行い、ジッター（静電気ノイズ）を表現
            java.util.Random rand = new java.util.Random(System.currentTimeMillis() / 30); // 30msごとにパターンが変わる
            guiGraphics.pose().translate(screenWidth / 2f, screenHeight / 2f, 0);
            if (rand.nextBoolean()) guiGraphics.pose().scale(-1, 1, 1);
            if (rand.nextBoolean()) guiGraphics.pose().scale(1, -1, 1);
            float scaleNoise = 1.0f + rand.nextFloat() * 0.5f;
            guiGraphics.pose().scale(scaleNoise, scaleNoise, 1);
            guiGraphics.pose().translate(-screenWidth / 2f, -screenHeight / 2f, 0);
            
            guiGraphics.blit(NOISE_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
            guiGraphics.pose().popPose();
            
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            
            guiGraphics.pose().popPose();
        }
    }
}
