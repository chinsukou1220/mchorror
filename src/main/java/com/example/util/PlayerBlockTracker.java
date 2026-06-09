package com.example.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class PlayerBlockTracker {
    public static class PlacedBlockRecord {
        public final BlockState state;
        public final BlockPos pos;
        public PlacedBlockRecord(BlockState state, BlockPos pos) {
            this.state = state;
            this.pos = pos;
        }
    }

    public static class PlayerData {
        public final LinkedList<PlacedBlockRecord> recentBlocks = new LinkedList<>();
        public BlockPos lastPlacedPos = null;
        public long lastPlacedTime = 0;
    }

    private static final Map<UUID, PlayerData> dataMap = new HashMap<>();

    public static void recordPlacement(UUID playerId, BlockState state, BlockPos pos, long gameTime) {
        PlayerData data = dataMap.computeIfAbsent(playerId, k -> new PlayerData());
        
        // Add to history (max 5)
        if (data.recentBlocks.size() >= 5) {
            data.recentBlocks.removeFirst();
        }
        data.recentBlocks.addLast(new PlacedBlockRecord(state, pos));
        
        // Update last placed location
        data.lastPlacedPos = pos;
        data.lastPlacedTime = gameTime;
    }

    public static PlayerData getPlayerData(UUID playerId) {
        return dataMap.get(playerId);
    }
    
    public static void clear() {
        dataMap.clear();
    }
}
