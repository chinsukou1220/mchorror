package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
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
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 400, 400);
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

            // パス計算のために一時的にプレイヤーと同じ座標にテレポート
            owner.setInvisible(true);
            owner.setSilent(true); // 足音などを完全に消す
            Vec3 originalPos = owner.position();
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

            if (bestPos != null) {
                // 目的地が見つかったら直接テレポート
                owner.teleportTo(bestPos.x, bestPos.y, bestPos.z);
                owner.setInvisible(false);
                owner.setSilent(false);
                
                // プレイヤーの方を振り向く
                double dx = this.targetPlayer.getX() - owner.getX();
                double dz = this.targetPlayer.getZ() - owner.getZ();
                float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                owner.setYRot(yaw);
                owner.setYHeadRot(yaw);
                owner.setYBodyRot(yaw);

                // ストーキング待機モードへ移行
                owner.hasBeenSeenSinceWarp = false;
                owner.isWaitingForWarp = false;
                owner.lastWarpTime = level.getGameTime();
            } else {
                // 行き先が見つからない場合は元の場所に戻って何もしない（アクションキャンセル）
                owner.teleportTo(originalPos.x, originalPos.y, originalPos.z);
            }
            
            // アクション終了
            owner.isActionActive = false;
        } else {
            owner.isActionActive = false;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // startで完結するため、継続不要
        return false;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // 使用しない
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
