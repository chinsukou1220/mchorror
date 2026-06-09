package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import com.example.util.PlayerBlockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BreakAction extends Behavior<HorrorSteveEntity> {

    public BreakAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            Player target = optionalPlayers.get().get(0);
            PlayerBlockTracker.PlayerData data = PlayerBlockTracker.getPlayerData(target.getUUID());
            
            if (data != null && !data.recentBlocks.isEmpty()) {
                // プレイヤーが最近置いたブロック（最大5個）を全て破壊する
                for (PlayerBlockTracker.PlacedBlockRecord record : data.recentBlocks) {
                    BlockPos pos = record.pos;
                    // その場所にまだ置いたブロックと同じ種類のブロックがあるか確認
                    if (level.getBlockState(pos).is(record.state.getBlock())) {
                        // アイテムをドロップしつつ、破壊パーティクルと音を鳴らす
                        level.destroyBlock(pos, true);
                    }
                }
                
                // 破壊した後は履歴をクリアしておく
                data.recentBlocks.clear();
            }
        }
        
        // アクション終了処理
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false;
    }
}
