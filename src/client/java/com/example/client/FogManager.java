package com.example.client;

import net.minecraft.client.Minecraft;

public class FogManager {
    public static boolean isFogActive = false;
    public static float fadeProgress = 0.0f; // 0.0 (通常) から 1.0 (完全に赤い霧) までの進行度

    public static void tick(Minecraft client) {
        // フェードの速度 (1秒に20ティックなので、0.02で約2.5秒かけて変化)
        float fadeSpeed = 0.02f;

        if (isFogActive) {
            if (fadeProgress < 1.0f) {
                fadeProgress += fadeSpeed;
                if (fadeProgress > 1.0f) fadeProgress = 1.0f;
            }
        } else {
            if (fadeProgress > 0.0f) {
                fadeProgress -= fadeSpeed;
                if (fadeProgress < 0.0f) fadeProgress = 0.0f;
            }
        }
    }
}
