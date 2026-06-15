package com.example.mixin;

import com.example.client.CameraShakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void renderRedFilter(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (com.example.client.FogManager.fadeProgress > 0.0f) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            
            // 最大アルファ値は 0x55 (約33%)。fadeProgress に応じて0〜0x55に変化
            int maxAlpha = 0x55;
            int currentAlpha = (int)(maxAlpha * com.example.client.FogManager.fadeProgress);
            
            // 色コード組み立て: (Alpha << 24) | (Red << 16) | (Green << 8) | Blue
            int color = (currentAlpha << 24) | (0xFF << 16) | (0x00 << 8) | 0x00;
            
            guiGraphics.fill(0, 0, screenWidth, screenHeight, color);
        }
    }

    private static final RandomSource RANDOM = RandomSource.create();

    @Inject(method = "render", at = @At("TAIL"))
    private void renderGlitchNoise(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (CameraShakeHandler.shakeTicks > 0 && CameraShakeHandler.shakeIntensity > 0) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // 揺れの強さに応じて描画するノイズブロックの数を変える（元の1/4に削減）
            int noiseCount = (int) (CameraShakeHandler.shakeIntensity * 5);
            
            // 揺れが特に強い場合は画面全体を少し暗くするフラッシュを入れる（確率と濃さを低下）
            if (CameraShakeHandler.shakeIntensity > 1.5f && RANDOM.nextFloat() < 0.05f) {
                guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x33000000); // さらに薄い半透明の黒
            }

            // ランダムな位置・サイズの矩形を描画してグリッチエフェクトを作る
            for (int i = 0; i < noiseCount; i++) {
                int x = RANDOM.nextInt(screenWidth);
                int y = RANDOM.nextInt(screenHeight);
                int width = 10 + RANDOM.nextInt((int)(30 * CameraShakeHandler.shakeIntensity)); // 横幅も控えめに
                int height = 1 + RANDOM.nextInt(5); // 縦幅も細く
                
                // 色は黒・グレー系・ランダムで不気味な白（透明度を大きく下げる）
                int alpha = 20 + RANDOM.nextInt(60);
                int colorValue = RANDOM.nextInt(150);
                
                // ARGBフォーマット: (Alpha << 24) | (Red << 16) | (Green << 8) | Blue
                int color = (alpha << 24) | (colorValue << 16) | (colorValue << 8) | colorValue;

                guiGraphics.fill(x, y, x + width, y + height, color);
            }
        }
    }
}
