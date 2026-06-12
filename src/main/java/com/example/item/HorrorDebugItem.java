package com.example.item;

import com.example.entity.action.ActionController.ActionType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

public class HorrorDebugItem extends Item {

    public HorrorDebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        CompoundTag tag = stack.getOrCreateTag();
        int currentActionOrdinal = tag.getInt("SelectedAction");
        ActionType[] values = ActionType.values();

        if (player.isShiftKeyDown()) {
            // アクションの切り替え
            currentActionOrdinal = (currentActionOrdinal + 1) % values.length;
            tag.putInt("SelectedAction", currentActionOrdinal);
            player.displayClientMessage(Component.literal("Selected Action: " + values[currentActionOrdinal].name()), true);
        } else {
            // アクションの実行（コマンドを内部で呼び出す）
            ActionType actionToExecute = values[currentActionOrdinal];
            
            // プレイヤーの権限を最大にしてコマンドを実行
            level.getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4), 
                "horror_action " + actionToExecute.name()
            );
        }

        return InteractionResultHolder.success(stack);
    }
}
