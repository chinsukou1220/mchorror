package com.example.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.network.chat.Component;

@Mixin(ServerPlayer.class)
public class ServerPlayerSleepMixin {

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void onStartSleepInBed(BlockPos bedPos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        if (com.example.world.RedNightManager.isRedNightActive) {
            ServerPlayer player = (ServerPlayer)(Object)this;
            player.displayClientMessage(Component.literal("§c赤い夜の恐怖で眠ることができない..."), true);
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
        }
    }
}
