package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GoBehindBehavior extends Behavior<HorrorSteveEntity> {

    public GoBehindBehavior() {
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
            
            // プレイヤーの視線ベクトルを取得
            Vec3 lookVec = target.getLookAngle();
            
            // 視線の真逆（背後）へ5ブロックの位置を計算
            // Y軸は0にして水平に計算する
            Vec3 backDir = new Vec3(-lookVec.x, 0, -lookVec.z).normalize().scale(5.0);
            
            double targetX = target.getX() + backDir.x;
            double targetZ = target.getZ() + backDir.z;
            
            // 背後座標の地面の高さを探す
            BlockPos pos = BlockPos.containing(targetX, target.getY() + 2, targetZ);
            while(level.isEmptyBlock(pos) && pos.getY() > level.getMinBuildHeight()) {
                pos = pos.below();
            }
            
            double targetY = pos.getY() + 1.0;
            
            // 背後にワープ
            owner.teleportTo(targetX, targetY, targetZ);
            
            // プレイヤーの方を向く
            double dx = target.getX() - owner.getX();
            double dz = target.getZ() - owner.getZ();
            float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            owner.setYRot(yaw);
            owner.setYHeadRot(yaw);
            owner.setYBodyRot(yaw);
            owner.setXRot(0);
            
            // ワープ時間を更新
            owner.lastWarpTime = level.getGameTime();
            owner.isAggressiveStalking = true; // 背後にワープした時は歩いて詰めるようにする
            owner.hasBeenSeenSinceWarp = false;
            owner.isActionActive = false; // アクション終了
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false;
    }
}
