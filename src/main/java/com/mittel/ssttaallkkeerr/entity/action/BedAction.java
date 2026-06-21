package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import com.mittel.ssttaallkkeerr.util.UndergroundDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BedAction extends Behavior<HorrorSteveEntity> {

    private int tickCount = 0;
    private Player targetPlayer = null;
    private boolean isUndergroundOrNether = false;
    private double footstepAngle = 0;

    public BedAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 100, 100);
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

            if (target.isSleeping()) {
                owner.isActionActive = true;
                this.tickCount = 0;
                this.targetPlayer = target;
                this.footstepAngle = level.random.nextDouble() * Math.PI * 2;
                
                boolean isUnderground = UndergroundDetector.isPlayerUnderground(level, target);
                boolean isNether = level.dimension() == net.minecraft.world.level.Level.NETHER;
                this.isUndergroundOrNether = isUnderground || isNether;
            } else {
                owner.isActionActive = false;
            }
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive && this.targetPlayer != null && this.targetPlayer.isSleeping();
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (!owner.isActionActive || this.targetPlayer == null) {
            return;
        }

        this.tickCount++;
        BlockPos playerPos = this.targetPlayer.blockPosition();

        if (this.isUndergroundOrNether) {
            // 洞窟、ネザーならブロック破壊音
            if (this.tickCount == 10 || this.tickCount == 30 || this.tickCount == 50) {
                // 少し離れた場所から音がする
                double angle = level.random.nextDouble() * Math.PI * 2;
                double dist = 4.0 + level.random.nextDouble() * 3.0;
                double fx = playerPos.getX() + Math.cos(angle) * dist;
                double fz = playerPos.getZ() + Math.sin(angle) * dist;
                BlockPos soundPos = new BlockPos((int)fx, playerPos.getY(), (int)fz);
                
                level.playSound(null, soundPos, SoundEvents.STONE_BREAK, SoundSource.AMBIENT, 1.5f, 0.8f);
            }
        } else {
            // 外なら足音
            if (this.tickCount % 5 == 0 && this.tickCount <= 50) {
                // 徐々に近づく足音
                double progress = this.tickCount / 50.0; // 0.0 to 1.0
                double distance = 10.0 - (8.0 * progress); // 10ブロック先から2ブロック先まで迫る
                
                double fx = playerPos.getX() + Math.cos(this.footstepAngle) * distance;
                double fz = playerPos.getZ() + Math.sin(this.footstepAngle) * distance;
                BlockPos soundPos = new BlockPos((int)fx, playerPos.getY(), (int)fz);
                
                level.playSound(null, soundPos, SoundEvents.GRASS_STEP, SoundSource.HOSTILE, (float)(0.5 + progress), 1.0f);
            }
        }

        if (this.tickCount >= 80 || !this.targetPlayer.isSleeping()) {
            endAction(owner, level);
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
