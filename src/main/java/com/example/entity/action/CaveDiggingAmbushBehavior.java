package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CaveDiggingAmbushBehavior extends Behavior<HorrorSteveEntity> {

    private int phase = 0; // 1: Tunneling, 2: Charging
    private int digTimer = 0;
    private int phase1Timer = 0;
    private int chargeTimer = 0;
    private Player targetPlayer = null;

    public CaveDiggingAmbushBehavior() {
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
            owner.isAggressiveStalking = true; // 視認可能にする
            owner.setInvisible(false);
            
            this.targetPlayer = target;
            this.phase = 1;
            this.digTimer = 0;
            this.phase1Timer = 0;
            this.chargeTimer = 0;

            // プレイヤーから約10ブロック離れた位置にスポーン
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = 10.0;
            double fx = target.getX() + Math.cos(angle) * distance;
            double fz = target.getZ() + Math.sin(angle) * distance;
            
            // Y座標はプレイヤーと同じか少し上
            BlockPos spawnPos = new BlockPos((int)fx, target.blockPosition().getY(), (int)fz);
            
            // テレポート先のブロック（2マス分）を岩盤以外なら破壊
            breakBlockIfNotBedrock(level, spawnPos);
            breakBlockIfNotBedrock(level, spawnPos.above());
            
            owner.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            owner.setInvisible(false); // 確実に姿を現す
        } else {
            owner.isActionActive = false;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive && this.targetPlayer != null;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (!owner.isActionActive || this.targetPlayer == null) {
            return;
        }

        if (this.phase == 1) { // 採掘フェーズ
            this.digTimer++;
            this.phase1Timer++;
            
            // 15秒（300ティック）経っても追いつけず、視線も通らなかった場合は諦めて消滅
            if (this.phase1Timer >= 300) {
                endActionAndVanish(level, owner);
                return;
            }
            
            // 視線が通ったらチャージフェーズへ移行
            if (owner.getSensing().hasLineOfSight(this.targetPlayer)) {
                this.phase = 2;
                // 移動速度上昇レベル10 (アンプリファイア9) を10秒間付与
                owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 9, false, false));
                // プレイヤーに10秒間の暗闇効果を付与
                this.targetPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
                // クリーパーの起爆音などを鳴らして突撃の合図
                level.playSound(null, owner.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0f, 1.5f);
                return;
            }
            
            // 20ティック（1秒）ごとに目の前のブロックを破壊して進む
            if (this.digTimer >= 20) {
                this.digTimer = 0;
                
                Vec3 vecToPlayer = this.targetPlayer.position().subtract(owner.position()).normalize();
                
                // 次に進むべきブロックの座標を計算
                int dx = (int) Math.round(vecToPlayer.x);
                int dy = (int) Math.round(vecToPlayer.y);
                int dz = (int) Math.round(vecToPlayer.z);
                
                // 真上や真下にしか進めない場合（極端な角度）の補正
                if (dx == 0 && dz == 0) {
                    dx = level.random.nextBoolean() ? 1 : -1;
                }
                
                BlockPos nextPos = owner.blockPosition().offset(dx, dy, dz);
                
                // ブロック破壊
                boolean brokeSomething = false;
                brokeSomething |= breakBlockIfNotBedrock(level, nextPos);
                brokeSomething |= breakBlockIfNotBedrock(level, nextPos.above());
                
                if (!brokeSomething) {
                    // 何も壊さなかった場合でも足音を鳴らす
                    level.playSound(null, nextPos, SoundEvents.STONE_STEP, SoundSource.HOSTILE, 1.0f, 0.8f);
                }
                
                // スティーブを前進させる
                owner.teleportTo(nextPos.getX() + 0.5, nextPos.getY(), nextPos.getZ() + 0.5);
                
                // 万が一プレイヤーに到達してしまったら（壁越しに密着）チャージへ
                if (owner.distanceToSqr(this.targetPlayer) < 9.0) {
                    this.phase = 2;
                    owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 9, false, false));
                    this.targetPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
                }
            }
            
        } else if (this.phase == 2) { // 突撃フェーズ
            this.chargeTimer++;
            
            // プレイヤーに向かって猛スピードでナビゲーション
            owner.getNavigation().moveTo(this.targetPlayer, 2.0D); // ベーススピードも2倍
            
            // 攻撃判定（距離が2.5未満）
            if (owner.distanceTo(this.targetPlayer) < 2.5) {
                // 一発殴る（通常攻撃）
                owner.doHurtTarget(this.targetPlayer);
                endActionAndVanish(level, owner);
                return;
            }
            
            // 10秒（200ティック）経っても殴れなかったら強制終了
            if (this.chargeTimer >= 200) {
                endActionAndVanish(level, owner);
                return;
            }
        }
    }
    
    private boolean breakBlockIfNotBedrock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
            level.destroyBlock(pos, true);
            return true;
        }
        return false;
    }

    private void endActionAndVanish(ServerLevel level, HorrorSteveEntity owner) {
        // 透明化して遠方へワープ
        owner.setInvisible(true);
        owner.isAggressiveStalking = false;
        
        double ang = level.random.nextDouble() * Math.PI * 2;
        double dist = 80.0 + level.random.nextDouble() * 40.0;
        double fx = owner.getX() + Math.cos(ang) * dist;
        double fz = owner.getZ() + Math.sin(ang) * dist;
        double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
        
        owner.teleportTo(fx, fy, fz);
        
        owner.isWaitingForWarp = true;
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
        owner.isAggressiveStalking = false;
    }
}
