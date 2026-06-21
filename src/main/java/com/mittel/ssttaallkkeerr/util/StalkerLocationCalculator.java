package com.mittel.ssttaallkkeerr.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Random;

public class StalkerLocationCalculator {

    private static final Random RANDOM = new Random();

    /**
     * プレイヤーの周囲から「障害物の陰から半分だけ顔を出せる」座標を探し出します。
     *
     * @param level       サーバーレベル
     * @param player      ターゲットとなるプレイヤー
     * @param minDistance プレイヤーからの最小距離
     * @param maxDistance プレイヤーからの最大距離
     * @return 条件を満たす座標が見つかった場合は Optional<Vec3>、見つからなかった場合は empty
     */
    public static Optional<Vec3> findPeekingLocation(ServerLevel level, Player player, double minDistance, double maxDistance) {
        Vec3 playerEyePos = player.getEyePosition();

        // 複数回ランダムな場所をサンプリングして探す
        for (int i = 0; i < 50; i++) {
            // 1. プレイヤーの周囲のランダムな座標を計算
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            
            double targetX = player.getX() + Math.cos(angle) * distance;
            double targetZ = player.getZ() + Math.sin(angle) * distance;

            // その場所の地表（一番上のブロック）の座標を取得
            BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos((int) targetX, 0, (int) targetZ));
            
            // プレイヤーの高さと大きく違いすぎる（崖の上など）場合はスキップ
            if (Math.abs(surfacePos.getY() - player.getY()) > 5) {
                continue;
            }

            // 2. その場所（空気ブロック）の周囲4方向に「壁（障害物）」となるブロックがあるかチェック
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adjacentPos = surfacePos.relative(dir);
                BlockPos adjacentEyePos = adjacentPos.above(); // エンティティの目の高さ付近

                // もし隣のブロックが視界を遮るようなフルブロックなら、そこは「角（コーナー）」の候補
                if (level.getBlockState(adjacentEyePos).canOcclude()) {
                    
                    // 3. 自分が立つ空気ブロックからは、プレイヤーの目線が通っているか（視線判定：Raycast）
                    Vec3 myEyePos = new Vec3(surfacePos.getX() + 0.5, surfacePos.getY() + 1.5, surfacePos.getZ() + 0.5);
                    ClipContext context = new ClipContext(
                            myEyePos, playerEyePos,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
                    );
                    
                    HitResult hitResult = level.clip(context);
                    
                    // 障害物にぶつかって視線が通らない（HitResultがMISSではない）なら完全に隠れられるポイント！
                    if (hitResult.getType() != HitResult.Type.MISS) {
                        // 壁の裏の空気ブロックの中心座標をそのまま使用する
                        Vec3 finalPos = new Vec3(
                                surfacePos.getX() + 0.5,
                                surfacePos.getY(),
                                surfacePos.getZ() + 0.5
                        );
                        
                        return Optional.of(finalPos);
                    }
                }
            }
        }

        // 50回探しても見つからなかった場合は空っぽを返す
        return Optional.empty();
    }
}
