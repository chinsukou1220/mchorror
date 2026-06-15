package com.example.mixin;

import com.example.client.FogManager;
import net.minecraft.client.renderer.BiomeColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class SimpleBiomeColorsMixin {

    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true)
    private static void onGetAverageWaterColor(CallbackInfoReturnable<Integer> cir) {
        if (FogManager.isFogActive) {
            // 海（水）の色を濃い赤色に変更 (16進数で 0x880000)
            cir.setReturnValue(0x880000);
        }
    }
}
