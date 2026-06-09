package com.example.entity.sensor;

import com.example.entity.HorrorSteveEntity;
import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GlobalPlayerSensor extends Sensor<HorrorSteveEntity> {

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
                MemoryModuleType.NEAREST_PLAYERS,
                MemoryModuleType.NEAREST_VISIBLE_PLAYER
        );
    }

    @Override
    protected void doTick(ServerLevel level, HorrorSteveEntity entity) {
        // ワールド内の全プレイヤーを取得
        List<ServerPlayer> allPlayers = level.players();

        if (allPlayers.isEmpty()) {
            entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_PLAYERS);
            entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
            return;
        }

        // スティーブからの距離が近い順に並び替え
        List<Player> sortedPlayers = allPlayers.stream()
                .sorted(Comparator.comparingDouble(entity::distanceToSqr))
                .collect(Collectors.toList());

        // 「近くのプレイヤー」として強制的に書き込む（無限射程、ブロック貫通）
        entity.getBrain().setMemory(MemoryModuleType.NEAREST_PLAYERS, sortedPlayers);
        
        // 視線が通っていなくても、「見えている」と錯覚させてバニラの行動プログラムを騙す
        entity.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, sortedPlayers.get(0));
    }
}
