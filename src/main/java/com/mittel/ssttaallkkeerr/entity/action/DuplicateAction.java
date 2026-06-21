package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import com.mittel.ssttaallkkeerr.util.PlayerBlockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DuplicateAction extends Behavior<HorrorSteveEntity> {

    public DuplicateAction() {
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

            if (data != null && !data.clusters.isEmpty()) {
                // 最大のクラスターを見つける
                LinkedList<PlayerBlockTracker.PlacedBlockRecord> largestCluster = null;
                int maxSize = -1;
                for (LinkedList<PlayerBlockTracker.PlacedBlockRecord> cluster : data.clusters) {
                    if (cluster.size() > maxSize) {
                        maxSize = cluster.size();
                        largestCluster = cluster;
                    }
                }

                if (largestCluster != null && !largestCluster.isEmpty()) {
                    // クラスターのバウンディングボックス（最小・最大のXとZ）を計算
                    int minX = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE;
                    int minZ = Integer.MAX_VALUE;
                    int maxZ = Integer.MIN_VALUE;

                    for (PlayerBlockTracker.PlacedBlockRecord record : largestCluster) {
                        if (record.pos.getX() < minX) minX = record.pos.getX();
                        if (record.pos.getX() > maxX) maxX = record.pos.getX();
                        if (record.pos.getZ() < minZ) minZ = record.pos.getZ();
                        if (record.pos.getZ() > maxZ) maxZ = record.pos.getZ();
                    }

                    int widthX = maxX - minX;
                    int widthZ = maxZ - minZ;

                    // オフセットの決定（ランダムに東西南北のいずれかへ、元の幅 ＋ 20ブロック離す）
                    int dx = 0;
                    int dz = 0;
                    int direction = level.random.nextInt(4);
                    switch (direction) {
                        case 0: dx = widthX + 20; break; // 東
                        case 1: dx = -(widthX + 20); break; // 西
                        case 2: dz = widthZ + 20; break; // 南
                        case 3: dz = -(widthZ + 20); break; // 北
                    }

                    // ブロックの複製（空気ブロックのみ置き換える）
                    for (PlayerBlockTracker.PlacedBlockRecord record : largestCluster) {
                        BlockPos newPos = record.pos.offset(dx, 0, dz);
                        if (level.isEmptyBlock(newPos)) {
                            level.setBlock(newPos, record.state, 3);
                        }
                    }

                    // 複製したクラスターを別のリストに保存しておく
                    data.duplicatedClusters.add(largestCluster);
                    if (data.duplicatedClusters.size() > 50) {
                        data.duplicatedClusters.removeFirst(); // メモリ保護
                    }
                    
                    // 元のリストからは削除（一度複製したものは消す）
                    data.clusters.remove(largestCluster);
                }
            }
        }

        // アクションを即座に終了し、次回のワープ待機状態へ
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false;
    }
}
