package com.example.mixin;

import com.example.client.CameraShakeHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Inject(method = "setup", at = @At("TAIL"))
    private void applyCameraShake(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (CameraShakeHandler.shakeTicks > 0 && CameraShakeHandler.shakeIntensity > 0) {
            float shakeYaw = CameraShakeHandler.getShakeYawOffset();
            float shakePitch = CameraShakeHandler.getShakePitchOffset();
            
            // 既存の角度に揺れを足して再設定する
            this.setRotation(this.yRot + shakeYaw, this.xRot + shakePitch);
        }
    }
}
