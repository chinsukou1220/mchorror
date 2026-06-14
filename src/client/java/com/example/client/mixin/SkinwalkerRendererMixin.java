package com.example.client.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class SkinwalkerRendererMixin {

    @Shadow protected EntityModel<?> model;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity.tickCount % 20 == 0) {
            String name = entity.getCustomName() != null ? entity.getCustomName().getString() : "null";
            System.out.println("[SKINWALKER DEBUG] Render called for: " + entity.getType().getDescriptionId() + ", CustomName: " + name);
        }

        if (entity.getCustomName() == null) return;
        if (!entity.getCustomName().getString().equals("skinwalker")) return;

        System.out.println("[SKINWALKER] MIXIN MATCHED! Model type: " + this.model.getClass().getSimpleName());

        float twitch = (entity.tickCount % 4 == 0) ? 0.3f : -0.3f;

        if (this.model instanceof HumanoidModel<?> humanoid) {
            humanoid.head.xRot = (float) Math.PI;
            humanoid.hat.xRot = (float) Math.PI;
            humanoid.rightArm.xRot = (float) Math.PI / 1.5f + twitch;
            humanoid.rightArm.yRot = 0.5f;
            humanoid.rightArm.zRot = 0.5f;
            humanoid.leftArm.xRot = -((float) Math.PI / 1.5f) + twitch;
            humanoid.leftArm.yRot = -0.5f;
            humanoid.leftArm.zRot = -0.5f;
            humanoid.rightLeg.xRot += 1.0f + twitch;
            humanoid.rightLeg.zRot = 0.5f;
            humanoid.leftLeg.xRot -= 1.0f + twitch;
            humanoid.leftLeg.zRot = -0.5f;
        } else if (this.model instanceof QuadrupedModel<?>) {
            QuadrupedModelAccessor accessor = (QuadrupedModelAccessor) this.model;
            accessor.getHead().xRot = (float) Math.PI;
            accessor.getRightHindLeg().xRot = (float) Math.PI / 1.5f + twitch;
            accessor.getRightHindLeg().yRot = 0.5f;
            accessor.getLeftHindLeg().xRot = -((float) Math.PI / 1.5f) + twitch;
            accessor.getLeftHindLeg().yRot = -0.5f;
            accessor.getRightFrontLeg().xRot += 1.0f + twitch;
            accessor.getRightFrontLeg().zRot = 0.5f;
            accessor.getLeftFrontLeg().xRot -= 1.0f + twitch;
            accessor.getLeftFrontLeg().zRot = -0.5f;
            accessor.getBody().zRot = 0.3f + twitch;
        }
    }
}
