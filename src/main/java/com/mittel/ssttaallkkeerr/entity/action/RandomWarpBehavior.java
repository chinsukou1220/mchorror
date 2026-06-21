package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import com.mittel.ssttaallkkeerr.util.StalkerLocationCalculator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HorrorSteveEntity専用の「ランダムワープ行動」を定義したクラス。
 * 毎フレーム（tick）ではなく、条件を満たした時（start）に1回だけ実行されます。
 */
public class RandomWarpBehavior extends Behavior<HorrorSteveEntity> {

    public RandomWarpBehavior() {
        // この行動を開始するための最低条件：
        // 「近くにプレイヤーがいる（NEAREST_PLAYERSメモリが存在する）」こと。
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        // コントローラー（ActionController）側で確率や条件を管理するため、
        // 呼び出されたら無条件で（100%）実行するように変更
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // 行動が開始された瞬間に1回だけ実行される処理（ワープ）
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            Player target = optionalPlayers.get().get(0);
            
            // アプローチ3: チラ見せポイントを探す（ご指定通りプレイヤーから50〜60ブロック離れた距離）
            Optional<Vec3> peekPos = StalkerLocationCalculator.findPeekingLocation(level, target, 50.0, 60.0);
            
            if (peekPos.isPresent()) {
                // 絶好の壁裏ポイントが見つかった場合、そこにワープする
                Vec3 pos = peekPos.get();
                owner.teleportTo(pos.x, pos.y, pos.z);
                owner.setInvisible(false); // 必ず透明化を解除する
                
                // --- 確実なガン見処理（アプローチ1） ---
                // プレイヤーの方を向くための角度（Yaw と Pitch）を数学的に計算
                double dx = target.getX() - owner.getX();
                double dz = target.getZ() - owner.getZ();
                double dy = target.getEyeY() - owner.getEyeY();
                double dist = Math.sqrt(dx * dx + dz * dz);
                
                float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                float pitch = (float)(-(Math.atan2(dy, dist) * (180.0 / Math.PI)));
                
                // 体、首、視線のすべてを強制的にプレイヤーの方へねじ曲げる
                owner.setYRot(yaw);
                owner.setYHeadRot(yaw);
                owner.setYBodyRot(yaw);
                owner.setXRot(pitch);
                
                // ワープ状態を記録
                owner.lastWarpTime = level.getGameTime();
                owner.hasBeenSeenSinceWarp = false;
                owner.isWaitingForWarp = false; // ストーキング開始！
                owner.isWaiting = false; // ストーキング開始！
                owner.isActionActive = false; // アクション終了
            } else {
                // 壁が見つからなかった場合は遠くにワープして待機を継続する
                double ang = level.random.nextDouble() * Math.PI * 2;
                double dist = 80.0 + level.random.nextDouble() * 40.0;
                double fx = target.getX() + Math.cos(ang) * dist;
                double fz = target.getZ() + Math.sin(ang) * dist;
                double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
                
                owner.teleportTo(fx, fy, fz);
                
                owner.lastWarpTime = level.getGameTime();
                owner.hasBeenSeenSinceWarp = false;
                owner.isWaitingForWarp = false; // 妥協ワープでもストーキング開始
                owner.isWaiting = false; // 妥協ワープでもストーキング開始
                owner.isActionActive = false;
            }
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // 1回ワープしたら「もうこの行動は終了」とシステムに伝えてすぐ終わらせる
        return false;
    }
}
