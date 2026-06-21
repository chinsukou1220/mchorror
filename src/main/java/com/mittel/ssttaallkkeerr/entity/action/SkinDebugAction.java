package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.behavior.EntityTracker;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SkinDebugAction extends Behavior<HorrorSteveEntity> {

    private float lockedYaw = 0.0F;
    private float lockedPitch = 0.0F;

    public SkinDebugAction() {
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
            
            // プレイヤーの目の前（2ブロック前）にテレポート
            double angle = target.getYRot() * (Math.PI / 180.0);
            double dx = -Math.sin(angle) * 2.0;
            double dz = Math.cos(angle) * 2.0;
            
            owner.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
            
            // 透明化を解除
            owner.setInvisible(false);

            // プレイヤーと向かい合うように体と頭の向きを固定
            this.lockedYaw = target.getYRot() + 180.0F;
            this.lockedPitch = -target.getXRot();
            
            owner.setYRot(this.lockedYaw);
            owner.setYBodyRot(this.lockedYaw);
            owner.setYHeadRot(this.lockedYaw);
            owner.setXRot(this.lockedPitch);
            
            // 動かないようにする
            owner.getNavigation().stop();
            owner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            
            // 移動速度を0にして物理的に移動できなくする
            owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
            
            // アクション継続フラグを立てる
            owner.isActionActive = true;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // ランダム歩行などが発動しないように毎フレーム記憶を消去
        owner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        owner.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        owner.getNavigation().stop();
        owner.setJumping(false);
        
        // バニラのランダムな首振りを防ぐため、毎フレーム視線を強制的に固定する
        owner.setYRot(this.lockedYaw);
        owner.setYBodyRot(this.lockedYaw);
        owner.setYHeadRot(this.lockedYaw);
        owner.setXRot(this.lockedPitch);
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // アクション終了時に移動速度を元に戻す
        owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }
}
