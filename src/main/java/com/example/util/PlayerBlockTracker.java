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
        // 記録済みの完成したクラスターのリスト（上限10個）
        public final LinkedList<LinkedList<PlacedBlockRecord>> clusters = new LinkedList<>(); 
        // 現在リアルタイムで連続建築中のクラスター（まだ完成していない一時記録用）
        public final LinkedList<PlacedBlockRecord> currentBuildingCluster = new LinkedList<>();
        public LinkedList<LinkedList<PlacedBlockRecord>> duplicatedClusters = new LinkedList<>(); // 一度複製されたクラスターを保存するリスト
        public BlockPos lastPlacedPos = null;
        public PlacedBlockRecord lastPlacedRecord = null;
        public long lastPlacedTime = 0;
    }

    private static final Map<UUID, PlayerData> dataMap = new HashMap<>();

    public static void recordPlacement(UUID playerId, BlockState state, BlockPos pos, long gameTime) {
        PlayerData data = dataMap.computeIfAbsent(playerId, k -> new PlayerData());
        PlacedBlockRecord newRecord = new PlacedBlockRecord(state, pos);
        
        // Add to history (max 5)
        if (data.recentBlocks.size() >= 5) {
            data.recentBlocks.removeFirst();
        }
        data.recentBlocks.addLast(newRecord);
        
        // 地下での足場用ブロック（土、石、丸石など）はクラスター（建築物）として記録しない
        if (isIgnoredUndergroundBlock(state, pos)) {
            data.lastPlacedPos = pos;
            return; // クラスター処理を行わずに終了
        }
        
        // Cluster logic (5ブロック以内の判定と統合)
        boolean isContinuous = (data.lastPlacedRecord != null && data.lastPlacedRecord.pos.distSqr(pos) <= 25.0);

        // 1. 連続していない（直前のブロックから遠くに置かれた）場合は、今まで作っていたクラスターを一旦保存する
        if (!isContinuous) {
            if (!data.currentBuildingCluster.isEmpty()) {
                saveCurrentCluster(data);
            }
        }

        // 2. 過去のすべての保存済みクラスターに対し、今回のブロックが5ブロック以内かチェックし、繋がったクラスターを全て見つける
        java.util.List<LinkedList<PlacedBlockRecord>> clustersToMerge = new java.util.ArrayList<>();
        for (LinkedList<PlacedBlockRecord> c : data.clusters) {
            for (PlacedBlockRecord r : c) {
                if (r.pos.distSqr(pos) <= 25.0) {
                    clustersToMerge.add(c);
                    break; // 1つでも近いブロックがあれば、このクラスター全体がマージ対象
                }
            }
        }

        // 3. マージ対象になったクラスターを保存リストから取り出し、現在のクラスターに全て統合する（複数あれば全て1つにまとまる）
        for (LinkedList<PlacedBlockRecord> c : clustersToMerge) {
            data.clusters.remove(c);
            data.currentBuildingCluster.addAll(c);
        }

        // 4. 今回のブロックを追加
        data.currentBuildingCluster.add(newRecord);

        // 5. メモリ保護（巨大化しすぎた場合は古いものから消す）
        while (data.currentBuildingCluster.size() > 1000) {
            data.currentBuildingCluster.removeFirst();
        }

        // Update last placed location
        data.lastPlacedPos = pos;
        data.lastPlacedRecord = newRecord;
        data.lastPlacedTime = gameTime;
    }

    private static void saveCurrentCluster(PlayerData data) {
        if (data.clusters.size() >= 50) {
            int minSize = Integer.MAX_VALUE;
            LinkedList<PlacedBlockRecord> smallestCluster = null;
            for (LinkedList<PlacedBlockRecord> c : data.clusters) {
                if (c.size() < minSize) {
                    minSize = c.size();
                    smallestCluster = c;
                }
            }
            if (smallestCluster != null) {
                data.clusters.remove(smallestCluster);
            }
        }
        data.clusters.addLast(new LinkedList<>(data.currentBuildingCluster));
        data.currentBuildingCluster.clear();
    }

    public static void recordBreak(UUID playerId, BlockPos pos) {
        PlayerData data = dataMap.get(playerId);
        if (data == null) return;

        // currentBuildingCluster から削除
        data.currentBuildingCluster.removeIf(r -> r.pos.equals(pos));

        // clusters から削除
        java.util.Iterator<LinkedList<PlacedBlockRecord>> iterator = data.clusters.iterator();
        while (iterator.hasNext()) {
            LinkedList<PlacedBlockRecord> cluster = iterator.next();
            cluster.removeIf(r -> r.pos.equals(pos));
            // もしブロックがすべて削除されたらクラスター自体を消去
            if (cluster.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static PlayerData getPlayerData(UUID playerId) {
        return dataMap.get(playerId);
    }

    public static boolean hasLargeCluster(UUID playerId, int minSize) {
        PlayerData data = dataMap.get(playerId);
        if (data == null) return false;
        for (LinkedList<PlacedBlockRecord> cluster : data.clusters) {
            if (cluster.size() >= minSize) return true;
        }
        return false;
    }
    
    public static void clear() {
        dataMap.clear();
    }

    private static boolean isIgnoredUndergroundBlock(BlockState state, BlockPos pos) {
        // Y=60以上の地上での設置なら除外しない
        if (pos.getY() >= 60) return false;
        
        // 地下での土、丸石、焼き石、深層岩などは除外（足場や湧き潰しと判定）
        return state.is(net.minecraft.tags.BlockTags.DIRT) ||
               state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) ||
               state.is(net.minecraft.world.level.block.Blocks.COBBLESTONE) ||
               state.is(net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE) ||
               state.is(net.minecraft.world.level.block.Blocks.GRAVEL) ||
               state.is(net.minecraft.world.level.block.Blocks.SAND) ||
               state.is(net.minecraft.world.level.block.Blocks.NETHERRACK);
    }
}
