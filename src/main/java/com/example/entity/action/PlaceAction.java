package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import com.example.util.PlayerBlockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlaceAction extends Behavior<HorrorSteveEntity> {

    public PlaceAction() {
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
            
            if (data != null && !data.recentBlocks.isEmpty() && data.lastPlacedPos != null) {
                // ランダムなブロックを選択
                int randomIndex = level.random.nextInt(data.recentBlocks.size());
                BlockState blockToPlace = data.recentBlocks.get(randomIndex).state;
                
                // 最後に置いたブロックの周囲（半径4ブロック以内）の空きスペースを探す
                BlockPos origin = data.lastPlacedPos;
                BlockPos placePos = null;
                
                for (int i = 0; i < 50; i++) {
                    int dx = level.random.nextInt(9) - 4; // -4 to 4
                    int dz = level.random.nextInt(9) - 4;
                    int dy = level.random.nextInt(5) - 2; // -2 to 2
                    
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    // 候補地が空気ブロックで、かつその下が固体ブロックであること
                    if (level.isEmptyBlock(candidate) && level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())) {
                        placePos = candidate;
                        break;
                    }
                }
                
                if (placePos != null) {
                    // ブロックを設置
                    level.setBlock(placePos, blockToPlace, 3);
                    
                    // スティーブ自身は出現させず、ブロックだけを不気味に増殖させる
                }
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
