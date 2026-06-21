package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import com.mittel.ssttaallkkeerr.util.PlayerBlockTracker;
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

    /**
     * プレイヤーが最近置いたブロックを周囲にランダム設置する（HuntAction等から直接呼び出し可能）。
     */
    public static void placeCreepyBlock(ServerLevel level, Player target) {
        PlayerBlockTracker.PlayerData data = PlayerBlockTracker.getPlayerData(target.getUUID());
        
        if (data != null && !data.recentBlocks.isEmpty() && data.lastPlacedPos != null) {
            int randomIndex = level.random.nextInt(data.recentBlocks.size());
            BlockState blockToPlace = data.recentBlocks.get(randomIndex).state;
            
            // 1. 最後に置いたブロックの周囲
            tryPlaceBlockAround(level, data.lastPlacedPos, blockToPlace);
            
            // 2. プレイヤーの周囲
            tryPlaceBlockAround(level, target.blockPosition(), blockToPlace);
        }
    }

    private static void tryPlaceBlockAround(ServerLevel level, BlockPos origin, BlockState blockToPlace) {
        BlockPos placePos = null;
        
        for (int i = 0; i < 50; i++) {
            int dx = level.random.nextInt(9) - 4;
            int dz = level.random.nextInt(9) - 4;
            int dy = level.random.nextInt(5) - 2;
            
            BlockPos candidate = origin.offset(dx, dy, dz);
            if (level.isEmptyBlock(candidate) && level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())) {
                placePos = candidate;
                break;
            }
        }
        
        if (placePos != null) {
            level.setBlock(placePos, blockToPlace, 3);
        }
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            placeCreepyBlock(level, optionalPlayers.get().get(0));
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
