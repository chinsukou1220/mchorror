package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import com.example.util.HouseDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HouseAction extends Behavior<HorrorSteveEntity> {

    private int tickCount = 0;
    private int actionPhase = 0; // 1: DoorKnock, 2: DoorKnock+Open, 3: GlassBreak, 4: WallBreak, 5: AmbientSound, 6: SuddenDoorOpen
    private BlockPos targetPos = null;
    private Player targetPlayer = null;

    public HouseAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 120, 120);
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

            if (HouseDetector.isPlayerInHouse(level, target)) {
                
                Optional<BlockPos> doorOpt = HouseDetector.findNearestDoor(level, target.blockPosition(), 10);
                Optional<BlockPos> glassOpt = HouseDetector.findNearestGlass(level, target.blockPosition(), 10);
                Optional<BlockPos> wallOpt = HouseDetector.findNearestWall(level, target.blockPosition(), 10);
                
                int rnLevel = 0;
                if (com.example.world.RedNightManager.isRedNightActive) {
                    rnLevel = com.example.world.RedNightState.get(level).weaknessLevel;
                }

                // 基準となる重み（チケット枚数）
                int baseWeight = 30; 
                // アクション1,2,3,5,6,7の重み：レベル分だけマイナス（0以下で発生しなくなる）
                int otherWeight = Math.max(0, baseWeight - rnLevel);
                // アクション4（壁ぶち抜き）の重み：レベル×6だけプラス
                int wallBreakWeight = baseWeight + (rnLevel * 6);

                List<Integer> availableActions = new ArrayList<>();
                if (doorOpt.isPresent()) {
                    for (int i = 0; i < otherWeight; i++) {
                        availableActions.add(1); // DoorKnock
                        availableActions.add(2); // DoorKnock + Open
                        availableActions.add(6); // SuddenDoorOpen
                    }
                }
                if (glassOpt.isPresent()) {
                    for (int i = 0; i < otherWeight; i++) {
                        availableActions.add(3); // GlassBreak
                    }
                }
                if (wallOpt.isPresent()) {
                    for (int i = 0; i < wallBreakWeight; i++) {
                        availableActions.add(4); // WallBreak
                    }
                }
                if (availableActions.isEmpty() && wallOpt.isEmpty() && glassOpt.isEmpty() && doorOpt.isEmpty()) {
                    for (int i = 0; i < otherWeight; i++) {
                        availableActions.add(5); // Peek (Cave sounds)
                    }
                }
                for (int i = 0; i < otherWeight; i++) {
                    availableActions.add(7); // 足音だけが鳴り続けるアクション
                }
                
                // 万が一すべて0になってしまった場合のフェイルセーフ
                if (availableActions.isEmpty()) {
                    availableActions.add(4); // 壁ぶち抜きを強制（壁があれば）
                    if (wallOpt.isEmpty()) {
                        availableActions.add(7); // 壁もなければ足音
                    }
                }
                
                this.actionPhase = availableActions.get(level.random.nextInt(availableActions.size()));
                this.targetPlayer = target;
                
                if (this.actionPhase == 1 || this.actionPhase == 2 || this.actionPhase == 6) {
                    this.targetPos = doorOpt.get();
                } else if (this.actionPhase == 3) {
                    this.targetPos = glassOpt.get();
                } else if (this.actionPhase == 4) {
                    this.targetPos = wallOpt.get();
                } else {
                    this.targetPos = target.blockPosition(); // 環境音の場合はプレイヤー周辺
                }
                
                // 物理的な本体の移動や透明化は一切行わない（遠隔ポルターガイスト現象）
                owner.isActionActive = true;
                this.tickCount = 0;
            } else {
                owner.isActionActive = false;
            }
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (!owner.isActionActive) {
            return;
        }
        
        this.tickCount++;
        
        // 常に透明化して見えないように隠れる
        owner.setInvisible(true);
        
        if (this.actionPhase == 1 || this.actionPhase == 2) {
            // ドアアクション（遠隔ノック）
            if (this.tickCount == 20 || this.tickCount == 35 || this.tickCount == 50) {
                level.playSound(null, this.targetPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.5f, 1.5f);
            }
            
            // Phase 2: ドアを遠隔で開ける
            if (this.actionPhase == 2 && this.tickCount == 65) {
                BlockState state = level.getBlockState(this.targetPos);
                if (state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN)) {
                    level.setBlock(this.targetPos, state.setValue(BlockStateProperties.OPEN, true), 10);
                    level.playSound(null, this.targetPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            
            if (this.tickCount >= 100) {
                endAction(level, owner);
            }
        } else if (this.actionPhase == 3) {
            // 窓ガラスアクション（遠隔破壊）
            if (this.tickCount == 40) {
                level.destroyBlock(this.targetPos, true);
            }
            if (this.tickCount >= 80) {
                endAction(level, owner);
            }
        } else if (this.actionPhase == 4) {
            // 壁ぶち抜きアクション（遠隔破壊）
            if (this.tickCount == 40) {
                int rnLevel = 0;
                if (com.example.world.RedNightManager.isRedNightActive) {
                    rnLevel = com.example.world.RedNightState.get(level).weaknessLevel;
                }
                int blocksToBreak = Math.max(1, rnLevel * 2);

                java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
                java.util.Set<BlockPos> visited = new java.util.HashSet<>();
                queue.add(this.targetPos);
                visited.add(this.targetPos);

                int brokenCount = 0;
                while (!queue.isEmpty() && brokenCount < blocksToBreak) {
                    BlockPos current = queue.poll();
                    BlockState state = level.getBlockState(current);

                    // 壁として破壊可能なブロックのみ対象
                    if (state.canOcclude() && state.getDestroySpeed(level, current) >= 0.0f && state.getDestroySpeed(level, current) < 50.0f) {
                        level.destroyBlock(current, true);
                        brokenCount++;

                        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                            BlockPos neighbor = current.relative(dir);
                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
            if (this.tickCount >= 80) {
                endAction(level, owner);
            }
        } else if (this.actionPhase == 5) {
            // 対象ブロックがない場合は、家の周囲で不気味な洞窟音を鳴らす
            if (this.tickCount == 20) {
                level.playSound(null, this.targetPos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0f, 0.8f);
            }
            if (this.tickCount >= 80) {
                endAction(level, owner);
            }
        } else if (this.actionPhase == 6) {
            // ノックなしで突然ドアがガチャッと開く
            if (this.tickCount == 20) {
                BlockState state = level.getBlockState(this.targetPos);
                if (state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN)) {
                    level.setBlock(this.targetPos, state.setValue(BlockStateProperties.OPEN, true), 10);
                    level.playSound(null, this.targetPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            if (this.tickCount >= 60) {
                endAction(level, owner);
            }
        } else if (this.actionPhase == 7) {
            // 家の周りで足音が複数回鳴るだけのアクション
            if (this.tickCount % 15 == 0 && this.tickCount <= 90) {
                BlockPos tpPos = findValidTpPosAroundHouse(level, this.targetPos);
                level.playSound(null, tpPos, SoundEvents.ZOMBIE_STEP, SoundSource.HOSTILE, 0.8f, 1.0f);
            }
            if (this.tickCount >= 100) {
                endAction(level, owner);
            }
        } else {
            endAction(level, owner);
        }
    }

    private void endAction(ServerLevel level, HorrorSteveEntity owner) {
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        // 万が一、途中で強制終了された場合でも必ずフラグを下ろす
        if (owner.isActionActive) {
            owner.lastWarpTime = gameTime;
        }
        owner.isActionActive = false;
        owner.setInvisible(false); // アクション終了時に透明化を解除
    }

    private BlockPos findValidTpPosAroundHouse(ServerLevel level, BlockPos center) {
        for (int i = 0; i < 5; i++) {
            int dx = level.random.nextInt(11) - 5; // -5 to +5
            int dz = level.random.nextInt(11) - 5; // -5 to +5
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue; // 近すぎる位置は避ける
            
            BlockPos pos = center.offset(dx, 0, dz);
            // Y座標を少し調整（段差対応）
            for (int y = -1; y <= 1; y++) {
                BlockPos checkPos = pos.above(y);
                if (level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.above()).isAir() && level.getBlockState(checkPos.below()).isSolidRender(level, checkPos.below())) {
                    return checkPos;
                }
            }
        }
        return center.above(); // 見つからなければターゲットの真上
    }
}
