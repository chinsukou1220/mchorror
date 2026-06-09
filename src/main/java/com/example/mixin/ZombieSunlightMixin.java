package com.example.mixin;

import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public class ZombieSunlightMixin {
	@Inject(at = @At("HEAD"), method = "isSunSensitive", cancellable = true)
	private void makeZombieCool(CallbackInfoReturnable<Boolean> info) {
		// Return false so that the zombie is not sensitive to the sun (won't burn)
		info.setReturnValue(false);
	}
}
