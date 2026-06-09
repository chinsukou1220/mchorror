package com.example.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import com.mojang.serialization.Dynamic;

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
    public int chargeTicks = 0; // 突進にかかっている時間（スタック時のタイムアウト用）
    public boolean isWaitingForWarp = false; // ランダムワープ（次の出番）を待機している状態

    public HorrorSteveEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
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
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            // 毎秒自動回復（再生）
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(50.0F); // 1秒ごとに50回復
            }
        }
    }

    // --- ここから追加の全耐性（無敵化）処理 ---

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
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
}
