package com.example.client;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class CameraShakeHandler {
    public static int shakeTicks = 0;
    public static float shakeIntensity = 0.0f;
    private static final RandomSource random = RandomSource.create();

    public static void tick() {
        if (shakeTicks > 0) {
            shakeTicks--;
            if (shakeTicks <= 0) {
                shakeIntensity = 0.0f; // 揺れ終了時にリセット
            }
        }
    }

    public static float getShakePitchOffset() {
        if (shakeTicks <= 0 || shakeIntensity <= 0) return 0.0f;
        // -intensity から +intensity までのランダムな角度（度）を返す
        return (random.nextFloat() - 0.5f) * 2.0f * shakeIntensity;
    }

    public static float getShakeYawOffset() {
        if (shakeTicks <= 0 || shakeIntensity <= 0) return 0.0f;
        return (random.nextFloat() - 0.5f) * 2.0f * shakeIntensity;
    }
    
    public static float getShakeRollOffset() {
        if (shakeTicks <= 0 || shakeIntensity <= 0) return 0.0f;
        // ロール（傾き）も少し揺らすとよりリアルになる
        return (random.nextFloat() - 0.5f) * 1.5f * shakeIntensity;
    }
}
