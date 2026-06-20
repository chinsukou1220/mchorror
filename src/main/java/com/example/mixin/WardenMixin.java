package com.example.mixin;

import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Warden.class)
public abstract class WardenMixin extends Monster {

    protected WardenMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        // 2秒(40ティック)ごとに周囲のウィザーを索敵
        if (!this.level().isClientSide() && this.tickCount % 40 == 0) {
            if (com.example.world.RedNightManager.isRedNightActive) {
                java.util.List<WitherBoss> withers = this.level().getEntitiesOfClass(
                    WitherBoss.class, 
                    this.getBoundingBox().inflate(64.0D) // 64ブロックの超広範囲感知
                );
                if (!withers.isEmpty()) {
                    WitherBoss target = withers.get(0);
                    // 攻撃されたと錯覚させることで、ウォーデンの怒りシステムを強制的にウィザーに向けさせる
                    this.setLastHurtByMob(target);
                    this.setTarget(target);
                }
            }
        }
    }
}
