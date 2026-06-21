package com.mittel.ssttaallkkeerr.item;

import com.mittel.ssttaallkkeerr.util.HouseDetector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class HouseDebugItem extends Item {
    public HouseDebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            boolean isHouse = HouseDetector.isPlayerInHouse(serverLevel, player);
            if (isHouse) {
                player.sendSystemMessage(Component.literal("§a[House Debug] ここは「家の中」として判定されています！"));
            } else {
                player.sendSystemMessage(Component.literal("§c[House Debug] ここは「家の中」ではありません。"));
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide());
    }
}
