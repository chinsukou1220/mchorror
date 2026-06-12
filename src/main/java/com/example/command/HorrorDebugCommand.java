package com.example.command;

import com.example.entity.HorrorSteveEntity;
import com.example.entity.action.ActionController;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class HorrorDebugCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("horror_action")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("action", StringArgumentType.string())
                .executes(context -> executeCommand(context, StringArgumentType.getString(context, "action"), 0))
                .then(Commands.argument("phase", IntegerArgumentType.integer())
                    .executes(context -> executeCommand(context, StringArgumentType.getString(context, "action"), IntegerArgumentType.getInteger(context, "phase")))
                )
            )
        );
    }

    private static int executeCommand(CommandContext<CommandSourceStack> context, String actionStr, int phase) {
        ActionController.ActionType actionType;
        try {
            actionType = ActionController.ActionType.valueOf(actionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid action type: " + actionStr));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        // 半径1000ブロック以内のスティーブを検索
        List<HorrorSteveEntity> steves = level.getEntitiesOfClass(HorrorSteveEntity.class, new AABB(context.getSource().getPosition(), context.getSource().getPosition()).inflate(1000.0));

        if (steves.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No Horror Steve found nearby. Spawn one first."));
            return 0;
        }

        HorrorSteveEntity steve = steves.get(0);
        steve.forcedDebugAction = actionType;
        
        if (phase != 0) {
            steve.forcedUndergroundPhase = phase;
        }

        context.getSource().sendSuccess(() -> Component.literal("Forced action " + actionType.name() + (phase != 0 ? " (Phase " + phase + ")" : "") + " on Horror Steve."), true);
        return 1;
    }
}
