package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class InFogAction extends Behavior<HorrorSteveEntity> {
    private int waitTicks = 0;
    private int currentTicks = 0;

    private int phase = 0;
    private int escapeTicks = 0;

    public InFogAction() {
        super(java.util.Map.of(), 1200, 1200); // タイムアウトを最大60秒（1200ティック）に設定（デフォルトの60ティック=3秒で強制終了されるのを防ぐため）
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return !owner.isActionActive;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity entity, long gameTime) {
        Optional<List<Player>> players = entity.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (players.isPresent() && !players.get().isEmpty()) {
            Player target = players.get().get(0);
            Vec3 look = target.getLookAngle();
            double distance = 13.0 + level.random.nextDouble() * 2.0;
            double targetX = target.getX() + look.x * distance;
            double targetZ = target.getZ() + look.z * distance;
            
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) targetX, (int) targetZ);
            BlockPos spawnPos = new BlockPos((int)targetX, y, (int)targetZ);

            int rnLevel = com.mittel.ssttaallkkeerr.world.RedNightState.get(level).weaknessLevel;
            waitTicks = Math.max(1, 15 - rnLevel) * 20;
            currentTicks = 0;
            this.phase = 0;
            this.escapeTicks = 0;
            
            // tp
            entity.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            entity.setInvisible(false); // 透明化を解除して姿を現す
            entity.getNavigation().stop(); // 過去の移動命令を停止
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); // 移動先メモリを消去

            entity.isActionActive = true;
            entity.isWaiting = false;
        } else {
            entity.isActionActive = false;
            entity.isWaiting = true;
        }
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity entity, long gameTime) {
        super.tick(level, entity, gameTime);
        
        Optional<List<Player>> players = entity.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (players.isPresent() && !players.get().isEmpty()) {
            Player target = players.get().get(0);
            
            // 毎フレーム強制的に移動を停止させ、歩行を遮断する
            entity.getNavigation().stop();
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            
            // プレイヤーの顔を注視する（強制的に体と顔の向きを上書き固定する）
            double dx = target.getX() - entity.getX();
            double dz = target.getZ() - entity.getZ();
            double dy = target.getEyeY() - entity.getEyeY();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float)-(Math.atan2(dy, horizDist) * (180.0 / Math.PI));
            
            entity.setYRot(yaw);
            entity.setXRot(pitch);
            entity.yBodyRot = yaw;
            entity.yHeadRot = yaw;
            
            if (this.phase == 0 && entity.distanceTo(target) <= 6.0) {
                this.phase = 1;
                this.escapeTicks = 0;
                entity.setLockLimbs(true); // 後退時に手足を動かさないようにする
            }
            
            if (this.phase == 1) {
                this.escapeTicks++;
                // プレイヤーと反対方向へ滑るように下がる（スピード3倍）
                Vec3 dir = entity.position().subtract(target.position()).normalize();
                entity.setDeltaMovement(dir.x * 0.9, entity.getDeltaMovement().y, dir.z * 0.9);
                
                if (this.escapeTicks >= 20) { // 1秒間（20ティック）下がってから消滅処理へ強制移行
                    currentTicks = waitTicks;
                }
            }
        }

        currentTicks++;
        if (currentTicks >= waitTicks) {
            entity.setInvisible(true); // ポーションではなくシステムから姿を消す
            double ang = level.random.nextDouble() * Math.PI * 2;
            double dist = 80.0 + level.random.nextDouble() * 40.0;
            double fx = entity.getX() + Math.cos(ang) * dist;
            double fz = entity.getZ() + Math.sin(ang) * dist;
            double fy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
            entity.teleportTo(fx, fy, fz);
            entity.isActionActive = false;
            entity.isWaiting = true;
            entity.isWaitingForWarp = true;
            entity.lastWarpTime = level.getGameTime(); // クールダウンリセット
        }
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        if (owner.isActionActive) {
            owner.isActionActive = false;
            owner.isWaiting = true;
            owner.isWaitingForWarp = true;
            owner.lastWarpTime = level.getGameTime(); // クールダウンリセット
        }
        owner.setLockLimbs(false); // 終了時にロック解除
    }
}
