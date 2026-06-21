package com.mittel.ssttaallkkeerr.client.mixin;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * QuadrupedModelのprotectedフィールドにアクセスするためのアクセサーMixin
 */
@Mixin(QuadrupedModel.class)
public interface QuadrupedModelAccessor {
    @Accessor("head") ModelPart getHead();
    @Accessor("body") ModelPart getBody();
    @Accessor("rightHindLeg") ModelPart getRightHindLeg();
    @Accessor("leftHindLeg") ModelPart getLeftHindLeg();
    @Accessor("rightFrontLeg") ModelPart getRightFrontLeg();
    @Accessor("leftFrontLeg") ModelPart getLeftFrontLeg();
}
