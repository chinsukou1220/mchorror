package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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

public class HuntAction extends Behavior<HorrorSteveEntity> {

    private int tickCount = 0;
    private Player targetPlayer = null;
    private int phase = 1;
    private double wanderTargetX = 0;
    private double wanderTargetZ = 0;

    public HuntAction() {
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
            this.tickCount = 0;
            this.targetPlayer = target;
            this.phase = 1;
            this.wanderTargetX = target.getX();
            this.wanderTargetZ = target.getZ();

            // 最初に「弱ってるな？」と英語でチャット（文字化けなし）
            target.sendSystemMessage(Component.literal("You look weak...").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            
            // 画面に大きくHIDEと表示
            if (target instanceof net.minecraft.server.level.ServerPlayer) {
                ((net.minecraft.server.level.ServerPlayer) target).connection.send(
                    new ClientboundSetTitleTextPacket(Component.literal("HIDE").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD))
                );
            }

            // スティーブ自身は見えない場所で待機
            owner.setInvisible(true);
            owner.setSilent(true);
            owner.teleportTo(target.getX(), target.getY() + 30, target.getZ());
            owner.setNoGravity(true);
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
            // 前半10秒（200ティック）：1秒おきにシェパードトーン
            if (this.tickCount % 20 == 0) {
                float basePitch = (this.tickCount / 200.0f); // 0.0 to 1.0
                level.playSound(null, this.targetPlayer.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 1.0f, 0.5f + basePitch * 0.5f);
                level.playSound(null, this.targetPlayer.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 1.0f, 1.0f + basePitch * 1.0f);
                level.playSound(null, this.targetPlayer.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, basePitch, 0.5f + basePitch * 1.0f);
            }

            if (this.tickCount >= 200) {
                this.phase = 2;
                
                // 10秒後に常時暗闇、ノイズと揺れ（アクション終了まで）
                this.targetPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 1200, 0, false, false));
                if (this.targetPlayer instanceof net.minecraft.server.level.ServerPlayer) {
                    com.example.network.ModNetworking.sendShakeToPlayer((net.minecraft.server.level.ServerPlayer) this.targetPlayer, 1200, 3.0f);
                }
                
                // チェイス開始
                owner.setInvisible(false);
                owner.setSilent(false);
                owner.setNoGravity(false); // ★重力を戻す（start()でtrueにしたまま忘れていた致命的バグ修正）
                owner.setMaxUpStep(10.0f);
                
                // プレイヤーの背後から迫る
                Vec3 look = this.targetPlayer.getViewVector(1.0f);
                Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
                double px = this.targetPlayer.getX() - flatLook.x * 25.0;
                double pz = this.targetPlayer.getZ() - flatLook.z * 25.0;
                
                // テレポート先の地形チェック：安全な地面の上に出現させる
                int targetY = this.targetPlayer.blockPosition().getY();
                BlockPos safePos = null;
                for (int y = targetY + 15; y >= targetY - 15; y--) {
                    BlockPos check = new BlockPos((int)px, y, (int)pz);
                    if (level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir() && level.getBlockState(check.below()).canOcclude()) {
                        safePos = check;
                        break;
                    }
                }
                if (safePos != null) {
                    owner.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                } else {
                    // 安全な場所が見つからなければプレイヤーの座標にそのまま出現
                    owner.teleportTo(px, this.targetPlayer.getY(), pz);
                }
                
                this.wanderTargetX = owner.getX();
                this.wanderTargetZ = owner.getZ();
            }
        } else if (this.phase == 2) {
            // === 1秒（20ティック）ごとにランダムなサブアクション（怪奇現象）を発生 ===
            if (this.tickCount % 20 == 0) {
                int rand = level.random.nextInt(6);
                switch (rand) {
                    case 0: SoundAction.playCreepySound(level, this.targetPlayer); break;
                    case 1: SignAction.placeCreepySign(level, this.targetPlayer); break;
                    case 2: DropAction.dropCreepyItems(level, this.targetPlayer); break;
                    case 3: PlaceAction.placeCreepyBlock(level, this.targetPlayer); break;
                    case 4: ChestAction.doCreepyChest(level, this.targetPlayer); break;
                    case 5: BreakAction.breakPlayerBlocks(level, this.targetPlayer); break;
                }
            }

            // 後半：チェイス＆チャット暴走
            if (this.tickCount % 5 == 0) {
                float pitch = 1.0f + ((this.tickCount % 40) / 40.0f);
                level.playSound(null, this.targetPlayer.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 1.0f, pitch);
            }

            // チャット欄を暴れさせる
            if (this.tickCount % 10 == 0) {
                String[] spamMessages = {"I CAN SEE YOU", "DONT LOOK BACK", "HIDE HIDE HIDE", "TOO WEAK", "RUN", "FOUND YOU"};
                String msg = spamMessages[level.random.nextInt(spamMessages.length)];
                if (level.random.nextBoolean()) {
                    msg = "§k|||§r " + msg + " §k|||§r";
                }
                this.targetPlayer.sendSystemMessage(Component.literal(msg).withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }

            if (this.tickCount > 500) {
                // チャットを擬似的にクリア（100行の空行）
                for (int i = 0; i < 100; i++) {
                    this.targetPlayer.sendSystemMessage(Component.literal(""));
                }
                this.targetPlayer.sendSystemMessage(Component.literal("next time.....").withStyle(net.minecraft.ChatFormatting.DARK_RED));
                endAction(owner, level);
                return;
            }

            boolean canSee = owner.getSensing().hasLineOfSight(this.targetPlayer);
            Vec3 targetPos;

            if (canSee) {
                // 視認している場合はプレイヤーに一直線
                targetPos = this.targetPlayer.position();
                owner.getLookControl().setLookAt(this.targetPlayer, 45.0F, 90.0F);
            } else {
                // 視認していない場合は自由な徘徊（ただし3ブロック以内には入らない）
                double distToWander = Math.sqrt(Math.pow(this.wanderTargetX - owner.getX(), 2) + Math.pow(this.wanderTargetZ - owner.getZ(), 2));
                
                // 半径3ブロック以内に入りそうなら強制的にプレイヤーから遠ざかる方向へ目標を修正
                Vec3 toPlayer = this.targetPlayer.position().subtract(owner.position());
                if (toPlayer.length() < 3.5) {
                    Vec3 awayFromPlayer = toPlayer.normalize().scale(-1.0);
                    this.wanderTargetX = owner.getX() + awayFromPlayer.x * 6.0; // 一気に遠ざかる
                    this.wanderTargetZ = owner.getZ() + awayFromPlayer.z * 6.0;
                }
                // 目標地点に到達したら新しいランダムな目標地点を再設定
                else if (distToWander < 1.5 || this.wanderTargetX == 0) {
                    double dist = 5.0 + level.random.nextDouble() * 5.0; // プレイヤーから5〜10ブロックの距離
                    double ang = level.random.nextDouble() * Math.PI * 2;
                    this.wanderTargetX = this.targetPlayer.getX() + Math.cos(ang) * dist;
                    this.wanderTargetZ = this.targetPlayer.getZ() + Math.sin(ang) * dist;
                }

                targetPos = new Vec3(this.wanderTargetX, this.targetPlayer.getY(), this.wanderTargetZ);
                owner.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 45.0F, 90.0F);
            }

            Vec3 toTarget = targetPos.subtract(owner.position());
            double distanceToTarget = toTarget.length();

            // プレイヤーとの距離判定（近接攻撃距離に達したら確殺）
            Vec3 toPlayer = this.targetPlayer.position().subtract(owner.position());
            if (toPlayer.length() <= 2.0) {
                owner.doHurtTarget(this.targetPlayer);
                // 確殺ダメージ
                this.targetPlayer.hurt(level.damageSources().mobAttack(owner), 100.0f);
                endAction(owner, level);
                return;
            }

            if (distanceToTarget > 0.5) {
                Vec3 dir = toTarget.normalize();
                // プレイヤーのスプリント速度(≒0.28)より速い7ブロック/秒(0.35/tick)に変更
                // まっすぐ逃げるだけでは追いつかれる恐怖感
                double speed = canSee ? 0.35 : 0.15;
                owner.setDeltaMovement(0, 0, 0); 
                double nx = owner.getX() + dir.x * speed;
                double ny = owner.getY() + dir.y * speed;
                double nz = owner.getZ() + dir.z * speed;
                
                owner.teleportTo(nx, ny, nz);
            }
            
            BlockPos currentPos = owner.blockPosition();
            boolean brokeSomething = false;

            for (int i = 0; i <= 2; i++) {
                BlockPos breakPos = currentPos.above(i);
                BlockState state = level.getBlockState(breakPos);
                if (!state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, breakPos) >= 0) {
                    level.destroyBlock(breakPos, true);
                    brokeSomething = true;
                }
            }

            BlockPos belowPos = currentPos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.isAir() || !belowState.getFluidState().isEmpty() || belowState.canBeReplaced()) {
                level.setBlock(belowPos, com.example.TemplateMod.GHOST_BLOCK.defaultBlockState(), 3);
            }
        }
    }

    private void endAction(HorrorSteveEntity owner, ServerLevel level) {
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
        owner.setMaxUpStep(0.6f);
        owner.setNoGravity(false);
        
        if (this.targetPlayer != null) {
            this.targetPlayer.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            if (this.targetPlayer instanceof net.minecraft.server.level.ServerPlayer) {
                com.example.network.ModNetworking.sendShakeToPlayer((net.minecraft.server.level.ServerPlayer) this.targetPlayer, 0, 0.0f);
                ((net.minecraft.server.level.ServerPlayer) this.targetPlayer).connection.send(
                    new ClientboundSetTitleTextPacket(Component.literal(""))
                );
            }
        }
        
        owner.isWaitingForWarp = true;
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
            if (this.targetPlayer instanceof net.minecraft.server.level.ServerPlayer) {
                com.example.network.ModNetworking.sendShakeToPlayer((net.minecraft.server.level.ServerPlayer) this.targetPlayer, 0, 0.0f);
                ((net.minecraft.server.level.ServerPlayer) this.targetPlayer).connection.send(
                    new ClientboundSetTitleTextPacket(Component.literal(""))
                );
            }
        }
        owner.isActionActive = false;
    }
}
