package com.example.mixin;

import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategoryMixin {

    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true)
    private void onGetMaxInstancesPerChunk(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this == MobCategory.MONSTER && com.example.world.RedNightManager.isRedNightActive) {
            cir.setReturnValue(cir.getReturnValue() * 3);
        }
    }
}
