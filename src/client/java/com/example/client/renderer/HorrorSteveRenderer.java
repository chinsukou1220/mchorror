package com.example.client.renderer;

import com.example.TemplateMod;
import com.example.entity.HorrorSteveEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HorrorSteveRenderer extends MobRenderer<HorrorSteveEntity, PlayerModel<HorrorSteveEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/entity/horror_steve.png");

    public HorrorSteveRenderer(EntityRendererProvider.Context context) {
        // false specifies the "wide" arm model (Steve), true would be "slim" (Alex)
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HorrorSteveEntity entity) {
        return TEXTURE;
    }
}
