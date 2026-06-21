package com.mittel.ssttaallkkeerr.mixin;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(QuadrupedModel.class)
public class QuadrupedModelMixin<T extends Entity> {
    @Shadow protected ModelPart head;
    @Shadow protected ModelPart body;
    @Shadow protected ModelPart rightHindLeg;
    @Shadow protected ModelPart leftHindLeg;
    @Shadow protected ModelPart rightFrontLeg;
    @Shadow protected ModelPart leftFrontLeg;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity.hasCustomName() && "skinwalker".equals(entity.getCustomName().getString())) {
            Random random = new Random(entity.getUUID().getMostSignificantBits());
            
            // 頭は乱数を使わず、上下逆向き（Z軸で180度回転）に固定
            head.zRot = (float) Math.PI;
            
            // 胴体は通常のままとする（処理なし）
            
            // 四肢（シードが固定なので毎回同じ角度が選ばれる）
            // zRot はバニラで上書きされないため += ではなく = で固定
            rightHindLeg.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightHindLeg.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightHindLeg.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
            
            leftHindLeg.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftHindLeg.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftHindLeg.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
            
            rightFrontLeg.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightFrontLeg.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightFrontLeg.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
            
            leftFrontLeg.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftFrontLeg.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftFrontLeg.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
        } else {
            head.zRot = 0.0F;
            body.zRot = 0.0F;
            rightHindLeg.zRot = 0.0F;
            leftHindLeg.zRot = 0.0F;
            rightFrontLeg.zRot = 0.0F;
            leftFrontLeg.zRot = 0.0F;
            rightHindLeg.yRot = 0.0F;
            leftHindLeg.yRot = 0.0F;
            rightFrontLeg.yRot = 0.0F;
            leftFrontLeg.yRot = 0.0F;
        }
    }
}
