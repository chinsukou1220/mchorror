package com.example.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import java.util.List;

public class HorrorSteveEntity extends PathfinderMob {

    // --- ワープ状態管理用変数 ---
    public long lastWarpTime = 0;
    public boolean hasBeenSeenSinceWarp = true; // 最初は true にして初回ワープを許可する
    public boolean wasSeenLastTick = false; // 前のTickで見られていたかどうか
    public int eyeContactTicks = 0; // 追加：目が合っている時間をカウント
    public long timeWhenSeen = 0; // 目が合って見つかった瞬間の時間（ワープまでの待機時間用）
    public boolean isActionActive = false;
    public boolean isAggressiveStalking = false; // 歩いて近づくモードかどうか
    public boolean isChargingToAttack = false; // 至近距離で見つかった際の突進攻撃モード
    public int postHitWaitTicks = 0; // 殴った後の硬直時間カウンター
    public int chargeTicks = 0; // 突進にかかっている時間（スタック時のタイムアウト用）
    public boolean isWaitingForWarp = false; // ランダムワープ（次の出番）を待機している状態
    public int invisibleStuckTicks = 0; // 透明状態でのスタック検知用カウンター
    public net.minecraft.world.phys.Vec3 lastInvisiblePos = net.minecraft.world.phys.Vec3.ZERO; // 前回の座標記録用
    public long lastBedActionTime = -72000; // ベッドアクションの30分クールダウン管理用
    public long lastTimerActionTime = -72000; // タイマーアクションの40分クールダウン管理用
    public long lastHuntActionTime = -72000; // ハントアクションのクールダウン管理用

    // --- デバッグ＆拡張用変数 ---
    public com.example.entity.action.ActionController.ActionType forcedDebugAction = com.example.entity.action.ActionController.ActionType.NONE;
    public int forcedUndergroundPhase = 0; // 0の場合はランダム
    public net.minecraft.world.phys.Vec3 ambushStartPos = null; // 特定の場所から強襲を開始するための座標保持用
    
    // --- チャンク維持用変数 ---
    public net.minecraft.world.level.ChunkPos lastForcedChunk = null;

    // --- 同期用データ ---
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_ID = SynchedEntityData.defineId(HorrorSteveEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_TOTAL_ALIVE_TICKS_ID = SynchedEntityData.defineId(HorrorSteveEntity.class, EntityDataSerializers.INT);

    public HorrorSteveEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean startRiding(net.minecraft.world.entity.Entity vehicle, boolean force) {
        // トロッコやボートなどに勝手に乗るのを防ぐ
        return false;
    }



    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_CHARGING_ID, false);
        this.entityData.define(DATA_TOTAL_ALIVE_TICKS_ID, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10000.0D) // とにかく多く（1万）
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D) // 吹っ飛ばし完全無効（その他耐性）
                .add(Attributes.FOLLOW_RANGE, 128.0D); // 50~60ブロック先の壁裏からのルート計算（迂回分）に余裕を持たせるため128に拡張
    }

    @Override
    public boolean fireImmune() {
        return true; // 火炎耐性
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false; // 落下ダメージ無効
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // 窒息ダメージ無効（その他耐性）
    }

    @Override
    public boolean isPushedByFluid() {
        return false; // 水流で流されない（その他耐性）
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_CHARGING_ID, this.isChargingToAttack);
            
            // 生存時間をカウントアップ
            int aliveTicks = this.entityData.get(DATA_TOTAL_ALIVE_TICKS_ID);
            this.entityData.set(DATA_TOTAL_ALIVE_TICKS_ID, aliveTicks + 1);
            
            ServerLevel serverLevel = (ServerLevel) this.level();
            
            // 毎秒自動回復（再生）
            if (this.tickCount % 20 == 0) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(50.0F); // 1秒ごとに50回復
                }
            }
            
            // チャンク強制読み込み（Forceload）の処理
            net.minecraft.world.level.ChunkPos currentChunk = new net.minecraft.world.level.ChunkPos(this.blockPosition());
            if (this.lastForcedChunk == null || !this.lastForcedChunk.equals(currentChunk)) {
                if (this.lastForcedChunk != null) {
                    serverLevel.setChunkForced(this.lastForcedChunk.x, this.lastForcedChunk.z, false);
                }
                serverLevel.setChunkForced(currentChunk.x, currentChunk.z, true);
                this.lastForcedChunk = currentChunk;
            }
        } else {
            this.isChargingToAttack = this.entityData.get(DATA_CHARGING_ID);
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.lastForcedChunk != null) {
            // KILLED（倒された）または DISCARDED（ダミーが用済みで消去された）場合のみ強制読み込みを解除する。
            // これにより、サーバー再起動時（UNLOADED）に強制読み込みが解除されてしまうのを防ぎ、
            // 再起動後もスティーブのいるチャンクが自動的に読み込まれるようにする。
            if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) {
                ((ServerLevel) this.level()).setChunkForced(this.lastForcedChunk.x, this.lastForcedChunk.z, false);
                this.lastForcedChunk = null;
            }
        }
        super.remove(reason);
    }

    // --- ここから追加の全耐性（無敵化）処理 ---

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // アクション中にプレイヤーから攻撃されたらアクションを解除（SkinDebugAction等の解除用）
        if (this.isActionActive && source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            this.isActionActive = false;
            // 必要であれば少し離れた場所にワープして逃げる等の処理をここに追加できます
        }

        // 奈落（/killコマンド等、無敵を貫通するダメージ）は許可
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        // クリエイティブモードのプレイヤーからの攻撃は許可（デバッグ・削除用）
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) {
            return super.hurt(source, amount);
        }
        // それ以外のあらゆるダメージ（通常攻撃、弓、爆発、魔法、毒、サボテン、窒息などすべて）を無効化
        return false;
    }

    @Override
    public boolean ignoreExplosion() {
        return true; // 爆発によるノックバックや影響を完全に無視
    }

    @Override
    public boolean canFreeze() {
        return false; // 粉雪などによる凍結と移動速度低下を無効化
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance potioneffect) {
        // アクションで使う発光（GLOWING）以外の、マイナスな状態異常をすべて無効化
        net.minecraft.world.effect.MobEffect effect = potioneffect.getEffect();
        if (effect == net.minecraft.world.effect.MobEffects.POISON || 
            effect == net.minecraft.world.effect.MobEffects.WITHER ||
            effect == net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN ||
            effect == net.minecraft.world.effect.MobEffects.WEAKNESS ||
            effect == net.minecraft.world.effect.MobEffects.BLINDNESS ||
            effect == net.minecraft.world.effect.MobEffects.LEVITATION ||
            effect == net.minecraft.world.effect.MobEffects.HARM) {
            return false;
        }
        return super.canBeAffected(potioneffect);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // 遠くに離れても絶対に自然デスポーン（消滅）しないようにする
    }

    @Override
    protected Brain.Provider<HorrorSteveEntity> brainProvider() {
        return HorrorSteveAi.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return HorrorSteveAi.makeBrain(this, this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<HorrorSteveEntity> getBrain() {
        return (Brain<HorrorSteveEntity>) super.getBrain();
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("horrorSteveBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        HorrorSteveAi.updateActivity(this);
        super.customServerAiStep();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TotalAliveTicks", this.entityData.get(DATA_TOTAL_ALIVE_TICKS_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("TotalAliveTicks")) {
            this.entityData.set(DATA_TOTAL_ALIVE_TICKS_ID, compound.getInt("TotalAliveTicks"));
        }
    }
}
