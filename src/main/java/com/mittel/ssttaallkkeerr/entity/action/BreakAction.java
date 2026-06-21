package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import com.mittel.ssttaallkkeerr.util.PlayerBlockTracker;
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

    /**
     * プレイヤーが最近置いたブロックを破壊する（HuntAction等から直接呼び出し可能）。
     */
    public static void breakPlayerBlocks(ServerLevel level, Player target) {
        PlayerBlockTracker.PlayerData data = PlayerBlockTracker.getPlayerData(target.getUUID());
        
        if (data != null && !data.recentBlocks.isEmpty()) {
            for (PlayerBlockTracker.PlacedBlockRecord record : data.recentBlocks) {
                BlockPos pos = record.pos;
                if (level.getBlockState(pos).is(record.state.getBlock())) {
                    level.destroyBlock(pos, true);
                }
            }
            data.recentBlocks.clear();
        }
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            breakPlayerBlocks(level, optionalPlayers.get().get(0));
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
