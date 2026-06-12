package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import com.example.util.UndergroundDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UndergroundAction extends Behavior<HorrorSteveEntity> {

    private int tickCount = 0;
    private int actionPhase = 0;
    private Player targetPlayer = null;
    private List<BlockPos> torchPositions = new ArrayList<>();

    public UndergroundAction() {
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

            if (UndergroundDetector.isPlayerUnderground(level, target)) {
                owner.isActionActive = true;
                this.tickCount = 0;
                this.targetPlayer = target;
                this.torchPositions.clear();

                // アクションパターンの選択（1〜6）
                List<Integer> availableActions = new ArrayList<>();
                availableActions.add(2); // Mining Sounds
                availableActions.add(3); // Fake Creeper
                availableActions.add(5); // Rushing Footsteps
                availableActions.add(6); // Ceiling Collapse

                // 松明があるかどうかをスキャン（半径8ブロック）
                BlockPos center = target.blockPosition();
                for (int x = -8; x <= 8; x++) {
                    for (int y = -4; y <= 4; y++) {
                        for (int z = -8; z <= 8; z++) {
                            BlockPos pos = center.offset(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                                this.torchPositions.add(pos);
                            }
                        }
                    }
                }

                if (!this.torchPositions.isEmpty()) {
                    availableActions.add(1); // Extinguish Torches
                    availableActions.add(4); // Torches to Redstone Torches
                }

                // --- デバッグ用フェーズ強制 ---
                if (owner.forcedUndergroundPhase != 0) {
                    this.actionPhase = owner.forcedUndergroundPhase;
                    owner.forcedUndergroundPhase = 0;
                } else {
                    this.actionPhase = availableActions.get(level.random.nextInt(availableActions.size()));
                }
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
        if (!owner.isActionActive || this.targetPlayer == null) {
            return;
        }

        this.tickCount++;
        BlockPos playerPos = this.targetPlayer.blockPosition();

        switch (this.actionPhase) {
            case 1: // Extinguish Torches
                if (this.tickCount == 20) {
                    for (BlockPos pos : this.torchPositions) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 1.0f, 1.0f);
                    }
                }
                if (this.tickCount >= 60) endAction(owner, level);
                break;

            case 2: // Mining Sounds
                if (this.tickCount % 12 == 0 && this.tickCount <= 120) {
                    // 壁の奥から音が聞こえるように少し離れた位置を計算
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double dist = 6.0 + level.random.nextDouble() * 4.0;
                    double fx = playerPos.getX() + Math.cos(angle) * dist;
                    double fz = playerPos.getZ() + Math.sin(angle) * dist;
                    BlockPos soundPos = new BlockPos((int)fx, playerPos.getY() + level.random.nextInt(4) - 2, (int)fz);
                    
                    level.playSound(null, soundPos, SoundEvents.STONE_HIT, SoundSource.AMBIENT, 1.5f, 0.8f);
                }
                if (this.tickCount >= 140) endAction(owner, level);
                break;

            case 3: // Fake Creeper
                if (this.tickCount == 15) {
                    // プレイヤーの背後からクリーパーの起爆音
                    net.minecraft.world.phys.Vec3 lookVec = this.targetPlayer.getLookAngle();
                    double fx = playerPos.getX() - lookVec.x * 2.0;
                    double fz = playerPos.getZ() - lookVec.z * 2.0;
                    BlockPos soundPos = new BlockPos((int)fx, playerPos.getY(), (int)fz);
                    
                    level.playSound(null, soundPos, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.2f, 1.0f);
                }
                if (this.tickCount >= 60) endAction(owner, level);
                break;

            case 4: // Redstone Torches
                if (this.tickCount == 20) {
                    for (BlockPos pos : this.torchPositions) {
                        BlockState state = level.getBlockState(pos);
                        if (state.is(Blocks.TORCH)) {
                            level.setBlock(pos, Blocks.REDSTONE_TORCH.defaultBlockState().setValue(BlockStateProperties.LIT, true), 3);
                        } else if (state.is(Blocks.WALL_TORCH)) {
                            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                            level.setBlock(pos, Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.LIT, true), 3);
                        }
                    }
                    level.playSound(null, playerPos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0f, 0.5f);
                }
                if (this.tickCount >= 60) endAction(owner, level);
                break;

            case 5: // Rushing Footsteps
                if (this.tickCount >= 10 && this.tickCount <= 40 && this.tickCount % 3 == 0) {
                    // 徐々に近づく足音
                    double progress = (this.tickCount - 10) / 30.0; // 0.0 to 1.0
                    net.minecraft.world.phys.Vec3 lookVec = this.targetPlayer.getLookAngle();
                    double distance = 15.0 - (12.0 * progress); // 15ブロック先から3ブロック先まで迫る
                    
                    double fx = playerPos.getX() - lookVec.x * distance;
                    double fz = playerPos.getZ() - lookVec.z * distance;
                    BlockPos soundPos = new BlockPos((int)fx, playerPos.getY(), (int)fz);
                    
                    level.playSound(null, soundPos, SoundEvents.STONE_STEP, SoundSource.HOSTILE, (float)(0.5 + progress), 1.2f);
                }
                if (this.tickCount >= 60) endAction(owner, level);
                break;

            case 6: // Ceiling Collapse
                if (this.tickCount == 20) {
                    // 頭上数ブロックの石を破壊
                    BlockPos targetBlock = null;
                    // y=2はプレイヤーの頭のすぐ上のブロック（天井）
                    for (int y = 2; y <= 6; y++) {
                        BlockPos checkPos = playerPos.above(y);
                        BlockState state = level.getBlockState(checkPos);
                        if (!state.isAir() && state.getFluidState().isEmpty()) {
                            targetBlock = checkPos;
                            break;
                        }
                    }
                    
                    if (targetBlock != null) {
                        level.destroyBlock(targetBlock, true);
                    } else {
                        // 見つからなければ音だけ鳴らす
                        level.playSound(null, playerPos.above(3), SoundEvents.STONE_BREAK, SoundSource.AMBIENT, 1.5f, 0.8f);
                    }
                }
                if (this.tickCount >= 50) endAction(owner, level);
                break;

            default:
                endAction(owner, level);
                break;
        }
    }

    private void endAction(HorrorSteveEntity owner, ServerLevel level) {
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        if (owner.isActionActive) {
            owner.lastWarpTime = gameTime;
        }
        owner.isActionActive = false;
    }
}
