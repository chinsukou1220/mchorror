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
                
                // デバッグ用表示：クラスター数と長さの表示
                PlayerBlockTracker.PlayerData data = PlayerBlockTracker.getPlayerData(player.getUUID());
                if (data != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    int maxLen = 0;
                    for (java.util.LinkedList<PlayerBlockTracker.PlacedBlockRecord> c : data.clusters) {
                        if (c.size() > maxLen) maxLen = c.size();
                    }
                    String debugText = String.format("§e[Debug] §fSaved Clusters: §a%d/50§f | Current Building: §b%d§f | Max Size: §c%d", 
                        data.clusters.size(), data.currentBuildingCluster.size(), maxLen);
                    serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(debugText), true);
                }
            }
        }
    }
}
