package com.mittel.ssttaallkkeerr.mixin;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin extends Monster {

    protected WitherBossMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bypassWitherImmunities(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.level().isClientSide()) {
            Entity attacker = source.getEntity();
            
            // バニラの「ウィザーはアンデッド（スケルトンやゾンビなど）からの攻撃を完全に無効化する」仕様を削除
            // スケルトン系、ブレイズ、ピリジャー、ヴェックスからの攻撃を無条件で通す
            if (attacker instanceof net.minecraft.world.entity.monster.AbstractSkeleton || 
                attacker instanceof net.minecraft.world.entity.monster.Blaze ||
                attacker instanceof net.minecraft.world.entity.monster.Pillager ||
                attacker instanceof net.minecraft.world.entity.monster.Vex ||
                (attacker instanceof net.minecraft.world.entity.LivingEntity && ((net.minecraft.world.entity.LivingEntity) attacker).getMobType() == net.minecraft.world.entity.MobType.UNDEAD)) {
                
                // 無敵時間中（連続ヒット防止）ならキャンセル
                // ※falseを返すと「矢がカキィンと弾かれる」ため、効いていないように見えてしまうのを防ぐためtrueを返す
                if (this.invulnerableTime > 10.0F) {
                    cir.setReturnValue(true);
                    return;
                }
                this.invulnerableTime = 20;
                
                // 被ダメージアニメーションと音を再生
                this.level().broadcastEntityEvent(this, (byte) 2);
                SoundEvent hurtSound = this.getHurtSound(source);
                if (hurtSound != null) {
                    this.playSound(hurtSound, this.getSoundVolume(), this.getVoicePitch());
                }
                
                // ウィザーの「アンデッド無効」「炎無効」「HP半減時の矢弾き」をすべて無視して強制ダメージ
                float newHealth = this.getHealth() - amount;
                if (newHealth <= 0) {
                    this.setHealth(0);
                    this.die(source);
                } else {
                    this.setHealth(newHealth);
                }
                
                // 攻撃者にヘイトを向ける（反撃させる）
                if (attacker instanceof net.minecraft.world.entity.LivingEntity) {
                    this.setLastHurtByMob((net.minecraft.world.entity.LivingEntity) attacker);
                    this.setTarget((net.minecraft.world.entity.LivingEntity) attacker);
                }
                
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void pullWitherDown(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        // 100ティック（5秒）ごとに高さをチェック
        if (this.tickCount % 100 == 0) {
            int x = net.minecraft.util.Mth.floor(this.getX());
            int z = net.minecraft.util.Mth.floor(this.getZ());
            int surfaceY = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            
            // 地表（障害物の最上面）より20ブロック以上高く飛んでいる場合、強制的に引きずり下ろす
            if (this.getY() > surfaceY + 20) {
                this.teleportTo(this.getX(), surfaceY + 5.0, this.getZ());
            }
        }
    }
}
