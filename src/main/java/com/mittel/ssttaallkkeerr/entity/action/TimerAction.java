package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TimerAction extends Behavior<HorrorSteveEntity> {

    private int tickCount = 0;
    private Player targetPlayer = null;
    private int fakeTimeSeconds = 0;
    private int realTriggerTicks = 0;
    private int phase = 1; // 1: Countdown, 2: Ambush

    public TimerAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 600, 600);
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
            owner.isWaiting = false;
            this.tickCount = 0;
            this.targetPlayer = target;
            this.phase = 1;

            // 500〜2000秒のランダムな時間（フェイク）
            this.fakeTimeSeconds = 500 + level.random.nextInt(1501);
            
            // 7〜15秒後（140〜300ティック）に本当の襲撃
            this.realTriggerTicks = 140 + level.random.nextInt(161);

            // 文字化けした送信者名を生成
            net.minecraft.network.chat.MutableComponent senderName = Component.literal("Steve").withStyle(net.minecraft.ChatFormatting.OBFUSCATED);
            // <文字化け> xxxx seconds というチャットフォーマットにする
            net.minecraft.network.chat.MutableComponent message = Component.literal("<").append(senderName).append("> " + this.fakeTimeSeconds + " seconds");
            target.sendSystemMessage(message);
            
            // スティーブ自身は遠くに待機
            owner.setInvisible(true);
            owner.setSilent(true);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive && this.targetPlayer != null && this.targetPlayer.isAlive();
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (!owner.isActionActive || this.targetPlayer == null) return;

        this.tickCount++;

        if (this.phase == 1) {
            int ticksLeft = this.realTriggerTicks - this.tickCount;
            int displaySecs = this.fakeTimeSeconds;

            if (ticksLeft <= 60 && ticksLeft > 0) {
                // 残り3秒（60ティック）を切ったら、とんでもない勢いで0に向かって減らす
                displaySecs = (int) (this.fakeTimeSeconds * ((double)ticksLeft / 60.0));
                
                // シェパードトーン風に音符ブロックのピッチを徐々に上げる（0.5〜2.0）
                float pitch = 0.5f + ((60 - ticksLeft) / 60.0f) * 1.5f;
                if (this.tickCount % 2 == 0) {
                    level.playSound(null, this.targetPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, pitch);
                }
            } else if (ticksLeft <= 0) {
                displaySecs = 0;
            } else {
                // 通常のカウントダウン（1秒ごとに減らす）
                if (this.tickCount % 20 == 0) {
                    this.fakeTimeSeconds--;
                    // フェイクタイマーがゆっくりの時も、秒数が減るペース（1秒に1回）で低音を鳴らす
                    level.playSound(null, this.targetPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.5f);
                }
                displaySecs = this.fakeTimeSeconds;
            }

            if (ticksLeft > 0) {
                // アクションバーにカウントダウンを表示（秒単位）
                String timeStr = Math.max(0, displaySecs) + " seconds";
                this.targetPlayer.displayClientMessage(Component.literal(timeStr), true);
            }

            // 本物のトリガー時間になったらフェーズ移行
            if (this.tickCount >= this.realTriggerTicks) {
                this.phase = 2;
                
                // 暗闇効果をタイマーが0になった瞬間に付与（アクション終了まで確実に持たせるため長めに設定）
                this.targetPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 600, 0, false, false));
                if (this.targetPlayer instanceof net.minecraft.server.level.ServerPlayer) {
                    com.mittel.ssttaallkkeerr.network.ModNetworking.sendShakeToPlayer((net.minecraft.server.level.ServerPlayer) this.targetPlayer, 600, 5.0f); // 特に強く揺らす
                }
                
                // 真上（空中）にいるプレイヤーにも向かっていけるよう、一時的に重力をオフにする
                owner.setNoGravity(true);
                
                // タイマーが0になった瞬間、音符ブロックのピッチ限界突破を表現するため、高い楽器を複数同時に鳴らす
                level.playSound(null, this.targetPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 2.0f);
                level.playSound(null, this.targetPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 2.0f);
                level.playSound(null, this.targetPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 2.0f);
                
                // プレイヤーの20ブロック前にTP
                Vec3 look = this.targetPlayer.getViewVector(1.0f);
                // プレイヤーの向いている方向（水平）のみを取り出す
                Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
                
                double px = this.targetPlayer.getX() + flatLook.x * 20.0;
                double pz = this.targetPlayer.getZ() + flatLook.z * 20.0;
                double py = this.targetPlayer.getY();
                
                owner.teleportTo(px, py, pz);
                owner.setInvisible(false);
                owner.setSilent(false);
                
                // 突撃の瞬間にOSOUTOKIと金切り声を同時に鳴らす
                level.playSound(null, owner.blockPosition(), com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.OSOUTOKI, net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 1.0F);
                level.playSound(null, owner.blockPosition(), com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.KANAKIRIGOE, net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.0F);
                level.playSound(null, owner.blockPosition(), com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.WQWQWQQ, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.0F);
                
                // 崖登り用のステップ設定（ブロック破壊しながら進むため）
                owner.setMaxUpStep(10.0f);
            }
        } else if (this.phase == 2) {
            // フェーズ2に入ってからの経過時間でループ再生を管理
            int phase2Ticks = this.tickCount - this.realTriggerTicks;
            if (phase2Ticks > 0 && phase2Ticks % 30 == 0) {
                level.playSound(null, owner.blockPosition(), com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.KANAKIRIGOE, net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.0F);
                level.playSound(null, owner.blockPosition(), com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.WQWQWQQ, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.0F);
            }
            // アクションバーの「0 seconds」を赤文字で震えさせる演出
            String jitterSpaces = level.random.nextBoolean() ? " " : (level.random.nextBoolean() ? "  " : "");
            String jitterPrefix = level.random.nextBoolean() ? "§k||§r " : "";
            String jitterSuffix = level.random.nextBoolean() ? " §k||§r" : "";
            net.minecraft.network.chat.MutableComponent zeroMsg = Component.literal(jitterPrefix + jitterSpaces + "0 seconds" + jitterSpaces + jitterSuffix)
                .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD);
            this.targetPlayer.displayClientMessage(zeroMsg, true);

            // 0秒になってもアクション終了まで一番高いピッチ（2.0）でアラームのように音を鳴らし続ける
            if (this.tickCount % 2 == 0) {
                level.playSound(null, this.targetPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 2.0f);
            }

            // プレイヤーに向かって一直線に進む（AIを使わず直接座標を計算）
            owner.getLookControl().setLookAt(this.targetPlayer, 45.0F, 90.0F);

            Vec3 toPlayer = this.targetPlayer.position().subtract(owner.position());
            double distance = toPlayer.length();

            // プレイヤーに到達するかタイムアウト（突撃から10秒経過）で終了
            if (distance <= 2.5 || this.tickCount > this.realTriggerTicks + 200) {
                if (distance <= 4.0) {
                    owner.doHurtTarget(this.targetPlayer);
                    // （暗闇は0秒到達時に付与済み）
                }
                endAction(owner, level);
                return;
            }

            // 直線移動ベクトル（1ティックあたり0.6ブロックに変更：スピード2倍）
            Vec3 dir = toPlayer.normalize();
            double speed = 0.6;
            owner.setDeltaMovement(0, 0, 0); // 落下の慣性を打ち消す
            double nx = owner.getX() + dir.x * speed;
            double ny = owner.getY() + dir.y * speed;
            double nz = owner.getZ() + dir.z * speed;
            
            // 強制テレポートによる直線移動（重力や地形を無視して一直線に飛ぶ）
            owner.teleportTo(nx, ny, nz);
            
            BlockPos currentPos = owner.blockPosition(); // 移動後の現在位置
            boolean brokeSomething = false;

            // 自分の体が通る空間（Y〜Y+2）を強引に破壊して道を作る
            for (int i = 0; i <= 2; i++) {
                BlockPos breakPos = currentPos.above(i);
                BlockState state = level.getBlockState(breakPos);
                // 岩盤などの破壊不能ブロック以外を破壊
                if (!state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, breakPos) >= 0) {
                    level.destroyBlock(breakPos, true);
                    brokeSomething = true;
                }
            }

            // 足元（Y-1）が空気や液体、草などの場合、足場を作る
            BlockPos belowPos = currentPos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.isAir() || !belowState.getFluidState().isEmpty() || belowState.canBeReplaced()) {
                // ゴーストブロック（専用の黒い足場）を設置
                level.setBlock(belowPos, com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.GHOST_BLOCK.defaultBlockState(), 3);
            }

            if (brokeSomething && this.tickCount % 5 == 0) {
                // TODO: ここに今後追加する効果音（ブロック破壊時のバキバキ音など）を入れる
                // level.playSound(null, owner.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0f, 0.5f);
            }
        }
    }

    private void endAction(HorrorSteveEntity owner, ServerLevel level) {
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
        owner.setMaxUpStep(0.6f); // 元に戻す
        owner.setNoGravity(false); // 重力を元に戻す
        
        // アクション終了時にエフェクトと画面揺れを強制的に終わらせる
        if (this.targetPlayer != null) {
            this.targetPlayer.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            if (this.targetPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.mittel.ssttaallkkeerr.network.ModNetworking.sendShakeToPlayer(sp, 0, 0.0f);
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.KANAKIRIGOE.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.WQWQWQQ.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.OSOUTOKI.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
            }
        }
        
        // そのまま逃走（ワープ待機）状態に移行
        owner.isWaitingForWarp = true;
        owner.isWaiting = true;
        owner.hasBeenSeenSinceWarp = false;
        
        if (this.targetPlayer != null) {
            double ang = level.random.nextDouble() * Math.PI * 2;
            double dist = 80.0 + level.random.nextDouble() * 40.0;
            double fx = this.targetPlayer.getX() + Math.cos(ang) * dist;
            double fz = this.targetPlayer.getZ() + Math.sin(ang) * dist;
            double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
            owner.teleportTo(fx, fy, fz);
        }
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        if (owner.isActionActive) {
            owner.lastWarpTime = gameTime;
        }
        owner.setMaxUpStep(0.6f);
        owner.setNoGravity(false);
        if (this.targetPlayer != null) {
            this.targetPlayer.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            if (this.targetPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.mittel.ssttaallkkeerr.network.ModNetworking.sendShakeToPlayer(sp, 0, 0.0f);
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.KANAKIRIGOE.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.WQWQWQQ.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.OSOUTOKI.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
            }
        }
        owner.isActionActive = false;
    }
}
