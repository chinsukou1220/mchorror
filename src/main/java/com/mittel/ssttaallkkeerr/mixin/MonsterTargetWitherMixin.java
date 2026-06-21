package com.mittel.ssttaallkkeerr.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractSkeleton.class, Blaze.class})
public abstract class MonsterTargetWitherMixin extends net.minecraft.world.entity.monster.Monster {

    protected MonsterTargetWitherMixin() {
        super(null, null);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    protected void onRegisterGoals(CallbackInfo ci) {
        // スケルトンとブレイズの攻撃対象にウィザーを追加（優先度3）
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, WitherBoss.class, true));
        
        // スケルトンの場合は、ウィザーやプレイヤーを発見しやすくするために感知射程を限界まで伸ばす
        if ((Object) this instanceof AbstractSkeleton) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance attr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (attr != null) {
                attr.setBaseValue(128.0D);
            }
        }
    }
}
