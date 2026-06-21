package com.mittel.ssttaallkkeerr.mixin;

import net.minecraft.world.entity.monster.Pillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pillager.class)
public class PillagerMixin {

	@Inject(at = @At("HEAD"), method = "registerGoals")
	private void injectPillagerGoals(CallbackInfo info) {
		// This code runs when Minecraft registers the AI Goals specifically for the Pillager.
		
		// To access the Pillager instance itself, cast 'this' like below:
		// Pillager pillager = (Pillager) (Object) this;
		
		// You can now modify the Pillager's AI behavior!
		// For example, to make them passive, friendly, or have completely new behaviors.
	}
}
