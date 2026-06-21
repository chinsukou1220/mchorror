package com.mittel.ssttaallkkeerr.client.renderer;

import com.mittel.ssttaallkkeerr.SsttaallkkeerrMod;
import com.mittel.ssttaallkkeerr.entity.FinalActionSteveEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FinalActionSteveRenderer extends MobRenderer<FinalActionSteveEntity, PlayerModel<FinalActionSteveEntity>> {
    private static final ResourceLocation FINAL_TEXTURE = new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve7.png");

    public FinalActionSteveRenderer(EntityRendererProvider.Context context) {
        // false specifies the "wide" arm model (Steve)
        super(context, new PlayerModel<FinalActionSteveEntity>(context.bakeLayer(ModelLayers.PLAYER), false) {
            @Override
            public void setupAnim(FinalActionSteveEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                // 首は傾けない
            }
        }, 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(FinalActionSteveEntity entity) {
        // 常に最終形態のテクスチャを使用
        return FINAL_TEXTURE;
    }

    @Override
    protected void scale(FinalActionSteveEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTickTime) {
        float wScale = entity.getEntityData().get(FinalActionSteveEntity.DATA_WIDTH_SCALE_ID);
        float hScale = entity.getEntityData().get(FinalActionSteveEntity.DATA_HEIGHT_SCALE_ID);
        poseStack.scale(wScale, hScale, wScale);
    }

    @Override
    public void render(FinalActionSteveEntity entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
