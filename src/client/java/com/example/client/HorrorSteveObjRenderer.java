package com.example.client;

import com.example.client.obj.ObjModelLoader;
import com.example.entity.HorrorSteveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HorrorSteveObjRenderer extends EntityRenderer<HorrorSteveEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("ssttaallkkeerr", "textures/entity/horror_steve.png");
    private static final ResourceLocation MODEL_OBJ = new ResourceLocation("ssttaallkkeerr", "models/horror_steve_5.obj");

    private ObjModelLoader.ObjModel model;

    public HorrorSteveObjRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = null; // Lazy load
    }

    @Override
    public ResourceLocation getTextureLocation(HorrorSteveEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(HorrorSteveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (this.model == null) {
            this.model = ObjModelLoader.load(MODEL_OBJ);
        }

        poseStack.pushPose();

        // Entity yaw rotation (Minecraft coordinates)
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        
        // Minor scale adjustment if necessary (default 1.0)
        poseStack.scale(4.5f, 4.5f, 4.5f);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));

        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float headPitch = entity.getXRot();
        float headYaw = net.minecraft.util.Mth.lerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float netHeadYaw = headYaw - entityYaw;

        for (String partName : this.model.parts.keySet()) {
            ObjModelLoader.MeshPart part = this.model.parts.get(partName);
            poseStack.pushPose();

            String name = partName.toLowerCase();
            
            // Animation logic based on naming convention
            if (name.contains("head")) {
                // 1.5だと高すぎるので1.0まで下げる（必要に応じて後で微調整可能）
                poseStack.translate(0, 1.0, 0);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-netHeadYaw)); // 横振り（背中側が顔なので反転させる）
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                poseStack.translate(0, -1.0, 0);
            } else if (name.contains("right_arm") || name.contains("arm_r") || name.equals("rightarm")) {
                // 肩幅を狭く(+X)、下に下げる(-Y)、前方に出す(+Z)
                poseStack.translate(0.06, -0.1, 0.1);
                // 回転軸（肩の関節位置）を少し下げる（1.5 -> 1.4）
                poseStack.translate(0, 1.4, 0);
                // 振る角度を0.7倍にする（0.2F * 0.7F = 0.14F）
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(net.minecraft.util.Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.14F));
                poseStack.translate(0, -1.4, 0);
            } else if (name.contains("left_arm") || name.contains("arm_l") || name.equals("leftarm")) {
                // 肩幅を狭く(-X)、下に下げる(-Y)、前方に出す(+Z)
                poseStack.translate(-0.06, -0.1, 0.1);
                poseStack.translate(0, 1.4, 0);
                // 振る角度を0.7倍にする
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(net.minecraft.util.Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.14F));
                poseStack.translate(0, -1.4, 0);
            } else if (name.contains("right_leg") || name.contains("leg_r") || name.equals("rightleg")) {
                // 回転軸を上げる（0.7 -> 1.2）
                poseStack.translate(0, 1.2, 0);
                // 振れ幅を約1度（0.017ラジアン）に極小化
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(net.minecraft.util.Mth.cos(limbSwing * 0.6662F) * 0.017F * limbSwingAmount));
                poseStack.translate(0, -1.2, 0);
            } else if (name.contains("left_leg") || name.contains("leg_l") || name.equals("leftleg")) {
                // 回転軸を上げる（0.7 -> 1.2）
                poseStack.translate(0, 1.2, 0);
                // 振れ幅を約1度（0.017ラジアン）に極小化
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(net.minecraft.util.Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.017F * limbSwingAmount));
                poseStack.translate(0, -1.2, 0);
            }

            drawPart(poseStack, consumer, part, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void drawPart(PoseStack poseStack, VertexConsumer consumer, ObjModelLoader.MeshPart part, int packedLight) {
        PoseStack.Pose entry = poseStack.last();
        for (ObjModelLoader.Face face : part.faces) {
            int vCount = face.vertices.length;
            if (vCount < 3) continue;

            // Render as a QUAD. If it's a triangle, vertex 3 is a duplicate of vertex 2.
            for (int i = 0; i < 4; i++) {
                ObjModelLoader.VertexData v = face.vertices[Math.min(i, vCount - 1)];
                consumer.vertex(entry.pose(), v.x, v.y, v.z)
                        .color(255, 255, 255, 255)
                        .uv(v.u, v.v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(entry.normal(), v.nx, v.ny, v.nz)
                        .endVertex();
            }
        }
    }
}
