package com.example.mixin;

import com.example.client.FogManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class SimpleFogMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void onSetupFog(Camera camera, FogRenderer.FogMode mode, float viewDistance, boolean thickFog, float partialTicks, CallbackInfo ci) {
        if (camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            if (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && living.level().dimension().location().getPath().equals("nightmare")) {
                RenderSystem.setShaderFogStart(0.0f);
                RenderSystem.setShaderFogEnd(2.0f); // 1マス先で完全に真っ黒になる
                ci.cancel();
                return;
            }
        }
        
        if (com.example.client.FogManager.fadeProgress > 0.0f) {
            float p = com.example.client.FogManager.fadeProgress;
            
            // バニラの霧の開始・終了距離（大まかな目安）
            float defaultStart = viewDistance * 0.75f;
            float defaultEnd = viewDistance;
            
            // 完全に赤くなったとき（p=1.0）の距離（視界を狭める）
            float targetStart = 2.0f;
            float targetEnd = 15.0f;
            
            // 線形補間で徐々に霧が迫ってくるように計算
            float currentStart = net.minecraft.util.Mth.lerp(p, defaultStart, targetStart);
            float currentEnd = net.minecraft.util.Mth.lerp(p, defaultEnd, targetEnd);
            
            RenderSystem.setShaderFogStart(currentStart);
            RenderSystem.setShaderFogEnd(currentEnd);
            // 色はSimpleSkyMixinで補間された空の色が自動的に反映されるため省略
            ci.cancel();
        }
    }
    @Inject(method = "setupColor", at = @At("RETURN"))
    private static void onSetupColor(Camera camera, float partialTicks, net.minecraft.client.multiplayer.ClientLevel level, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
        if (camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            if (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && living.level().dimension().location().getPath().equals("nightmare")) {
                RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
                RenderSystem.setShaderFogColor(0.0f, 0.0f, 0.0f, 1.0f);
                return;
            }
        }
        
        float p = com.example.client.FogManager.fadeProgress;
        if (p > 0.0f) {
            // 元の霧の色を正確に取得できない環境のフォールバックとして、標準的な空の色（明るいグレーブルー）からの補間を行う
            // ※より厳密にはRenderSystemから色を取得しますが、安全のため固定値からのLerpを採用
            float defaultR = 0.6f;
            float defaultG = 0.7f;
            float defaultB = 0.8f;
            
            float targetR = 0.3f;
            float targetG = 0.0f;
            float targetB = 0.0f;
            
            float r = net.minecraft.util.Mth.lerp(p, defaultR, targetR);
            float g = net.minecraft.util.Mth.lerp(p, defaultG, targetG);
            float b = net.minecraft.util.Mth.lerp(p, defaultB, targetB);
            
            RenderSystem.clearColor(r, g, b, 0.0f);
            RenderSystem.setShaderFogColor(r, g, b, 1.0f);
        }
    }
}
