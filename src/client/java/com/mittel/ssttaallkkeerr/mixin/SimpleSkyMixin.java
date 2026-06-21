package com.mittel.ssttaallkkeerr.mixin;

import com.mittel.ssttaallkkeerr.client.FogManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class SimpleSkyMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void onGetSkyColor(Vec3 pos, float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        if (com.mittel.ssttaallkkeerr.client.FogManager.fadeProgress > 0.0f) {
            Vec3 original = cir.getReturnValue();
            float p = com.mittel.ssttaallkkeerr.client.FogManager.fadeProgress;
            double r = net.minecraft.util.Mth.lerp(p, original.x, 0.3);
            double g = net.minecraft.util.Mth.lerp(p, original.y, 0.0);
            double b = net.minecraft.util.Mth.lerp(p, original.z, 0.0);
            cir.setReturnValue(new Vec3(r, g, b));
        }
    }
}
