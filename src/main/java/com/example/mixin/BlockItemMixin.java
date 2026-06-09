package com.example.mixin;

import com.example.util.PlayerBlockTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void onPlaceBlock(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Player player = context.getPlayer();
            if (player != null && !player.level().isClientSide()) {
                PlayerBlockTracker.recordPlacement(
                        player.getUUID(),
                        state,
                        context.getClickedPos(),
                        player.level().getGameTime()
                );
            }
        }
    }
}
