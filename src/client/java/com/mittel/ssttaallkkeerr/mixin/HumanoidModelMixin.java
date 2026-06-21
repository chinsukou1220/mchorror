package com.mittel.ssttaallkkeerr.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow public ModelPart head;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity.hasCustomName() && "skinwalker".equals(entity.getCustomName().getString())) {
            Random random = new Random(entity.getUUID().getMostSignificantBits());
            
            // 頭は乱数を使わず、上下逆向き（Z軸で180度回転）に固定
            head.zRot = (float) Math.PI;
            
            // 四肢の乱数（シードが固定なので毎回同じ角度が選ばれる）
            // xRot, yRot はバニラのアニメーションで毎フレーム上書きされるため += でオフセットを足す。
            // zRot 等はバニラで上書きされない場合があり、+= だと毎フレーム加算されてプロペラ回転してしまうため = で固定する。
            
            rightArm.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightArm.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightArm.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
            
            leftArm.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftArm.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftArm.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
            
            rightLeg.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightLeg.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            rightLeg.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
            
            leftLeg.xRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftLeg.yRot += (random.nextFloat() * 2 - 1) * Math.PI;
            leftLeg.zRot = (random.nextFloat() * 2 - 1) * (float) Math.PI;
        } else {
            // スキンウォーカーではない通常のモブ（通常のスティーブなど）が同じモデルインスタンスを使う場合、
            // 壊れた関節（特に自動リセットされないzRotやyRot）が引き継がれてしまう「モデル汚染」を防ぐためのリセット処理
            head.zRot = 0.0F;
            rightArm.zRot = 0.0F;
            leftArm.zRot = 0.0F;
            rightLeg.zRot = 0.0F;
            leftLeg.zRot = 0.0F;
            rightLeg.yRot = 0.0F;
            leftLeg.yRot = 0.0F;
        }
    }
}
