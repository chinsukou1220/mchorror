package com.mittel.ssttaallkkeerr.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HouseDetector {

    /**
     * プレイヤーが「家」の中にいるかどうかを判定します。
     * 判定基準：
     * 1. プレイヤーの頭上にブロックがあり、空が見えないこと。
     * 2-a. ベッド、ドア、作業台などの即判定ブロックが1つでもあれば家と判定。
     * 2-b. チェスト類・かまど類（チェスト、トラップチェスト、バレル、かまど、燻製器、溶鉱炉）は合計3つ以上で家と判定。
     */
    public static boolean isPlayerInHouse(ServerLevel level, Player player) {
        BlockPos playerPos = player.blockPosition();

        // 条件1: 屋根がある（空が見えない）
        if (level.canSeeSky(playerPos)) {
            return false;
        }

        // 条件2: 生活ブロックが周囲にあるか
        int storageBlockCount = 0; // チェスト類・かまど類のカウント
        int radius = 10;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    // ベッド、ドア、作業台 → 1つでもあれば即座に家と判定
                    if (isInstantLifeBlock(state)) {
                        return true;
                    }

                    // チェスト類・かまど類 → 3つ以上で家と判定
                    if (isStorageBlock(state)) {
                        storageBlockCount++;
                        if (storageBlockCount >= 3) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 周辺にある「ドア」の座標を1つ探して返します。ノック用。
     */
    public static Optional<BlockPos> findNearestDoor(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> doors = new ArrayList<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    if (state.is(BlockTags.WOODEN_DOORS) || state.is(Blocks.IRON_DOOR)) {
                        doors.add(pos);
                    }
                }
            }
        }
        
        if (!doors.isEmpty()) {
            doors.sort((p1, p2) -> Double.compare(p1.distSqr(center), p2.distSqr(center)));
            return Optional.of(doors.get(0));
        }
        
        return Optional.empty();
    }

    /**
     * 周辺にある「窓ガラス」を探します。
     */
    public static Optional<BlockPos> findNearestGlass(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> glasses = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    // ガラス系のブロック（音で大まかに判定、または一般的に使われるガラスのタグ/インスタンス）
                    if (state.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS) {
                        glasses.add(pos);
                    }
                }
            }
        }
        if (!glasses.isEmpty()) {
            glasses.sort((p1, p2) -> Double.compare(p1.distSqr(center), p2.distSqr(center)));
            return Optional.of(glasses.get(0));
        }
        return Optional.empty();
    }

    /**
     * 外と中を隔てている「壁」ブロックを探します。
     * 条件：ブロック自体が不透過(Solid)で、片側が「空が見える空気(外)」、反対側が「空が見えない空気(内)」であること。
     */
    public static Optional<BlockPos> findNearestWall(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> walls = new ArrayList<>();
        BlockPos[] directions = { BlockPos.ZERO.north(), BlockPos.ZERO.south(), BlockPos.ZERO.east(), BlockPos.ZERO.west() };

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    // 壁になりうる固いブロックか？ (壊せない岩盤などは除外)
                    if (state.canOcclude() && state.getDestroySpeed(level, pos) >= 0.0f && state.getDestroySpeed(level, pos) < 50.0f) {
                        
                        // 東西南北のいずれかで、外と中を分けているかチェック
                        for (BlockPos dir : directions) {
                            BlockPos side1 = pos.offset(dir);
                            BlockPos side2 = pos.offset(-dir.getX(), 0, -dir.getZ()); // 反対側
                            
                            BlockState state1 = level.getBlockState(side1);
                            BlockState state2 = level.getBlockState(side2);
                            
                            if (state1.isAir() && state2.isAir()) {
                                boolean canSeeSky1 = level.canSeeSky(side1);
                                boolean canSeeSky2 = level.canSeeSky(side2);
                                
                                // 片方が外、片方が中なら壁と判定
                                if (canSeeSky1 != canSeeSky2) {
                                    walls.add(pos);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!walls.isEmpty()) {
            // 一番近い壁を選ぶ
            walls.sort((p1, p2) -> Double.compare(p1.distSqr(center), p2.distSqr(center)));
            return Optional.of(walls.get(0));
        }
        return Optional.empty();
    }

    /**
     * 1つでもあれば即座に家と判定するブロック（ベッド、ドア、作業台）
     */
    private static boolean isInstantLifeBlock(BlockState state) {
        return state.is(BlockTags.BEDS) ||
               state.is(BlockTags.WOODEN_DOORS) ||
               state.is(Blocks.IRON_DOOR) ||
               state.is(Blocks.CRAFTING_TABLE);
    }

    /**
     * 3つ以上で家と判定するブロック（チェスト類・かまど類）
     */
    private static boolean isStorageBlock(BlockState state) {
        return state.is(Blocks.CHEST) ||
               state.is(Blocks.TRAPPED_CHEST) ||
               state.is(Blocks.BARREL) ||
               state.is(Blocks.FURNACE) ||
               state.is(Blocks.SMOKER) ||
               state.is(Blocks.BLAST_FURNACE);
    }

    /**
     * すべての生活ブロック（デバッグ表示用）
     */
    private static boolean isLifeBlock(BlockState state) {
        return isInstantLifeBlock(state) || isStorageBlock(state);
    }
}
