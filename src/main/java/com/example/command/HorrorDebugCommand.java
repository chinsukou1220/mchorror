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

        dispatcher.register(Commands.literal("stevestage")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("stage", IntegerArgumentType.integer(0, 6))
                .executes(context -> {
                    int stage = IntegerArgumentType.getInteger(context, "stage");
                    ServerLevel level = context.getSource().getLevel();
                    List<HorrorSteveEntity> steves = level.getEntitiesOfClass(HorrorSteveEntity.class, new AABB(context.getSource().getPosition(), context.getSource().getPosition()).inflate(100000.0));
                    if (steves.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("No Horror Steve found."));
                        return 0;
                    }
                    HorrorSteveEntity steve = steves.get(0);
                    steve.getEntityData().set(HorrorSteveEntity.DATA_TOTAL_ALIVE_TICKS_ID, stage * 3000);
                    context.getSource().sendSuccess(() -> Component.literal("Set Steve stage to " + stage + " (skin: steve" + (stage + 1) + ".png)"), true);
                    return 1;
                })
            )
        );

        // 新しく追加する /rednight コマンド
        dispatcher.register(Commands.literal("rednight")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ServerLevel level = context.getSource().getLevel();
                
                // コマンド実行時にも弱体化レベルと確率を上げる
                com.example.world.RedNightState state = com.example.world.RedNightState.get(level);
                state.currentProbability = Math.min(1.0f, state.currentProbability + 0.10f);
                state.weaknessLevel += 1;
                state.setDirty();
                
                com.example.world.RedNightManager.startRedNight(level);
                context.getSource().sendSuccess(() -> Component.literal("Triggered Red Night event! (Level " + state.weaknessLevel + ")"), true);
                return 1;
            })
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
        // ディメンション内のすべてのスティーブを検索（半径10万ブロック）
        List<HorrorSteveEntity> steves = level.getEntitiesOfClass(HorrorSteveEntity.class, new AABB(context.getSource().getPosition(), context.getSource().getPosition()).inflate(100000.0));

        if (steves.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No Horror Steve found in this dimension. Spawn one first."));
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
