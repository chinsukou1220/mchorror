package com.example.mixin;

import com.example.world.RedNightManager;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public class AnimalMixin {

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void onCustomServerAiStep(CallbackInfo ci) {
        if (RedNightManager.isRedNightActive) {
            Animal animal = (Animal) (Object) this;
            
            // 近くのプレイヤーを探す
            Player target = animal.level().getNearestPlayer(animal, 16.0);
            
            if (target != null && target.isAlive() && !target.isCreative() && !target.isSpectator()) {
                // プレイヤーに向かって移動する（速度は通常の1.0倍）
                animal.getNavigation().moveTo(target, 1.0f);
                
                // プレイヤーとの距離が2ブロック未満（近接）ならダメージを与える
                double distSq = animal.distanceToSqr(target);
                if (distSq < 4.0) {
                    // 1秒(20ティック)に1回ダメージ判定
                    if (animal.tickCount % 20 == 0) {
                        target.hurt(animal.level().damageSources().mobAttack(animal), 2.0f); // ダメージ2.0 (ハート1つ)
                    }
                }
            }
        }
    }
}
