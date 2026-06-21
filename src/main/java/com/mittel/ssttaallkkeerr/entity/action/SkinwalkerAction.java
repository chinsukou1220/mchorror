package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SkinwalkerAction extends Behavior<HorrorSteveEntity> {

    private PathfinderMob dummyMob = null;
    private Player targetPlayer = null;
    private int ticksActive = 0;
    private int maxTicks = 400;

    public SkinwalkerAction() {
        // デフォルトは60ティック(3秒)で強制終了してしまうため、400ティック(20秒)に延長
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isEmpty() || optionalPlayers.get().isEmpty()) {
            this.finishAction(owner, level);
            return;
        }

        this.targetPlayer = optionalPlayers.get().get(0);
        this.ticksActive = 0;

        int rnLevel = com.mittel.ssttaallkkeerr.world.RedNightState.get(level).weaknessLevel;
        int durationSeconds = 20 - (rnLevel * 2);
        if (durationSeconds < 3) durationSeconds = 3; // 安全柵: 最小3秒
        this.maxTicks = durationSeconds * 20;

        // ランダムな動物や敵対モブに擬態する
        EntityType<?>[] types = {
            EntityType.PIG, EntityType.COW, EntityType.SHEEP,
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER
        };
        EntityType<?> selectedType = types[level.random.nextInt(types.length)];

        // プレイヤーの視線の先（前方15〜20ブロック）の座標を計算
        Vec3 lookVec = this.targetPlayer.getLookAngle();
        double distance = 15.0 + level.random.nextDouble() * 5.0;
        double spawnX = this.targetPlayer.getX() + lookVec.x * distance;
        double spawnZ = this.targetPlayer.getZ() + lookVec.z * distance;
        double spawnY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) spawnX, (int) spawnZ);

        // ダミーを生成してスポーン
        this.dummyMob = (PathfinderMob) selectedType.create(level);
        if (this.dummyMob != null) {
            this.dummyMob.setPos(spawnX, spawnY, spawnZ);
            this.dummyMob.getTags().add("is_skinwalker"); // サーバー側の目印タグ
            // クライアント側に同期するためにカスタムネームをマーカーとして使用（タグはサーバー専用で同期されない）
            this.dummyMob.setCustomName(net.minecraft.network.chat.Component.literal("skinwalker"));
            this.dummyMob.setCustomNameVisible(false); // 名前は非表示
            
            // 歩行速度をレベルに応じて上昇させる (Base 0.23 + 0.01 * level)
            double baseSpeed = 0.23D + (0.01D * rnLevel);
            this.dummyMob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
            // 体力を1000に設定（攻撃で倒されてドロップが出るのを防ぐ）
            this.dummyMob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0D);
            this.dummyMob.setHealth(1000.0F);
            // ドロップ完全無効化（経験値もアイテムも落とさない）
            this.dummyMob.setPersistenceRequired();
            
            level.addFreshEntity(this.dummyMob);
            
            // スティーブ本体は透明化して待機
            owner.setInvisible(true);
            owner.isActionActive = true;
        } else {
            this.finishAction(owner, level);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (this.dummyMob == null || !this.dummyMob.isAlive() || this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }
        
        // もしダミーが攻撃されたら即座に終了し、CAVE_AMBUSHを強制発動する
        if (this.dummyMob.hurtTime > 0) {
            owner.ambushStartPos = this.dummyMob.position();
            owner.forcedDebugAction = com.mittel.ssttaallkkeerr.entity.action.ActionController.ActionType.CAVE_AMBUSH;
            return false;
        }

        // タイムアウト（レベルに応じた時間）
        if (this.ticksActive > this.maxTicks) {
            return false;
        }

        return true;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        this.ticksActive++;

        if (this.dummyMob != null && this.targetPlayer != null) {
            // ダミーのAIを上書きして強制的にプレイヤーへ向かわせる
            this.dummyMob.getNavigation().moveTo(this.targetPlayer, 1.0D);
            
            // ランダムで極端にピッチの低い（野太い）不気味な鳴き声を発する
            if (this.ticksActive % 40 == 0 && level.random.nextInt(3) == 0) {
                net.minecraft.sounds.SoundEvent sound = null;
                if (this.dummyMob.getType() == EntityType.PIG) sound = net.minecraft.sounds.SoundEvents.PIG_AMBIENT;
                else if (this.dummyMob.getType() == EntityType.COW) sound = net.minecraft.sounds.SoundEvents.COW_AMBIENT;
                else if (this.dummyMob.getType() == EntityType.SHEEP) sound = net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT;
                else if (this.dummyMob.getType() == EntityType.ZOMBIE) sound = net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT;
                else if (this.dummyMob.getType() == EntityType.SKELETON) sound = net.minecraft.sounds.SoundEvents.SKELETON_AMBIENT;
                
                if (sound != null) {
                    // 通常のピッチ(1.0〜1.2等)を大きく外れた 0.2〜0.4 の極低音にする
                    float pitch = 0.2f + level.random.nextFloat() * 0.2f;
                    level.playSound(null, this.dummyMob.getX(), this.dummyMob.getY(), this.dummyMob.getZ(), 
                            sound, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, pitch);
                }
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        this.finishAction(owner, level);
    }

    private void finishAction(HorrorSteveEntity owner, ServerLevel level) {
        if (this.dummyMob != null && !this.dummyMob.isRemoved()) {
            this.dummyMob.discard(); // ダミーを消去
            this.dummyMob = null;
        }
        
        // アクション終了、スティーブをワープ待機状態へ戻す
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
        this.targetPlayer = null;
    }
}
