package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WalkAwayBehavior extends Behavior<HorrorSteveEntity> {

    private Player targetPlayer = null;
    private Vec3 destination = null;
    private int timeoutTicks = 0;

    public WalkAwayBehavior() {
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

            owner.isActionActive = true;
            this.targetPlayer = target;
            this.timeoutTicks = 0;

            // プレイヤーと全く同じ座標にテレポート
            owner.setInvisible(true);
            owner.setSilent(true); // 足音などを完全に消す
            owner.teleportTo(target.getX(), target.getY(), target.getZ());

            // 20〜50ブロック先の到達可能な行き先を探す
            Vec3 bestPos = null;
            for (int i = 0; i < 30; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double dist = 20.0 + level.random.nextDouble() * 30.0;
                int px = (int)(target.getX() + Math.cos(angle) * dist);
                int pz = (int)(target.getZ() + Math.sin(angle) * dist);
                int py = target.blockPosition().getY();
                
                BlockPos validP = null;
                // 周辺のY軸を探索して床を探す
                for (int y = py + 10; y >= py - 10; y--) {
                    BlockPos check = new BlockPos(px, y, pz);
                    if (level.getBlockState(check).isAir() && level.getBlockState(check.below()).canOcclude()) {
                        validP = check;
                        break;
                    }
                }

                if (validP != null) {
                    // パスが繋がるか（到達可能か）確認
                    Path path = owner.getNavigation().createPath(validP, 0);
                    if (path != null && path.canReach()) {
                        bestPos = Vec3.atBottomCenterOf(validP);
                        break;
                    }
                }
            }

            if (bestPos == null) {
                // 見つからなかった場合のフォールバック
                bestPos = DefaultRandomPos.getPosAway(owner, 30, 15, target.position());
            }

            if (bestPos != null) {
                this.destination = bestPos;
                owner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bestPos, 1.2f, 0));
            } else {
                // 行き先がどうしても見つからない場合（閉鎖空間など）は即座にアクション終了
                owner.isActionActive = false;
                owner.setInvisible(false);
                owner.setSilent(false);
                this.destination = null;
            }
            
        } else {
            owner.isActionActive = false;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive && this.targetPlayer != null && this.destination != null;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (!owner.isActionActive || this.targetPlayer == null || this.destination == null) {
            return;
        }

        this.timeoutTicks++;
        double distSqr = owner.distanceToSqr(this.destination);
        
        // 目的地に到着した（2ブロック以内）、または時間がかかりすぎた場合（15秒 = 300ティック）
        if (distSqr < 4.0 || this.timeoutTicks > 300) {
            // 実体化＆足音復活
            owner.setInvisible(false);
            owner.setSilent(false);
            
            // 歩行を停止
            owner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            
            // プレイヤーの方を振り向く
            double dx = this.targetPlayer.getX() - owner.getX();
            double dz = this.targetPlayer.getZ() - owner.getZ();
            float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            owner.setYRot(yaw);
            owner.setYHeadRot(yaw);
            owner.setYBodyRot(yaw);

            // 既存のRandomWarpと同じ「視線判定ストーキング待機モード」へ移行
            owner.hasBeenSeenSinceWarp = false;
            owner.isWaitingForWarp = false;
            owner.lastWarpTime = level.getGameTime();
            
            // アクションを終了し、HorrorSteveAi に制御を返す
            owner.isActionActive = false;
            this.destination = null;
        } else {
            // 歩き続けている場合、パスが消えていたら再設定する
            if (owner.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty()) {
                owner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.destination, 1.2f, 0));
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        if (owner.isActionActive) {
            owner.lastWarpTime = gameTime;
        }
        if (this.destination != null) {
            owner.isActionActive = false;
        }
        owner.setSilent(false);
    }
}
