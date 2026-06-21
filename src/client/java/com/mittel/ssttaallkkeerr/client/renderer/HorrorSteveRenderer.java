package com.mittel.ssttaallkkeerr.client.renderer;

import com.mittel.ssttaallkkeerr.SsttaallkkeerrMod;
import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HorrorSteveRenderer extends MobRenderer<HorrorSteveEntity, PlayerModel<HorrorSteveEntity>> {
    private static final ResourceLocation[] DECAY_TEXTURES = new ResourceLocation[] {
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve1.png"),
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve2.png"),
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve3.png"),
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve4.png"),
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve5.png"),
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve6.png"),
        new ResourceLocation(SsttaallkkeerrMod.MOD_ID, "textures/entity/steve7.png")
    };

    public HorrorSteveRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<HorrorSteveEntity>(context.bakeLayer(ModelLayers.PLAYER), false) {
            @Override
            public void setupAnim(HorrorSteveEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
                super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                
                // 突進攻撃時に首をかしげる（横に45度ロール）
                if (entity.isChargingToAttack) {
                    this.head.zRot = (float) Math.toRadians(45);
                    this.hat.zRot = (float) Math.toRadians(45);
                }
            }
        }, 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HorrorSteveEntity entity) {
        if (entity.hasCustomName() && "skinwalker".equals(entity.getCustomName().getString())) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                net.minecraft.world.entity.player.Player closestPlayer = mc.level.getNearestPlayer(entity, 64.0);
                if (closestPlayer instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
                    return clientPlayer.getSkinTextureLocation();
                }
            }
        }
        
        int aliveTicks = entity.getEntityData().get(HorrorSteveEntity.DATA_TOTAL_ALIVE_TICKS_ID);
        // 15分(900秒) = 18000 ticks。6段階の進行なので 18000 / 6 = 3000 ticks ごとに悪化
        int stage = aliveTicks / 3000;
        if (stage < 0) stage = 0;
        if (stage > 6) stage = 6;
        
        return DECAY_TEXTURES[stage];
    }

    @Override
    public void render(HorrorSteveEntity entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        // 襲うとき（突進攻撃モード時）に点滅（フリッカー）させる
        if (entity.isChargingToAttack) {
            // tickCountを使って、3ティックのうち1ティックだけ描画をスキップする（チカチカさせる）
            // より不規則なノイズ感を出したい場合はランダムを使うことも可能
            if (entity.tickCount % 3 == 0) {
                return; // 描画を完全にスキップして透明になる
            }
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
