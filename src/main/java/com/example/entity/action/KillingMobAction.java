package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KillingMobAction extends Behavior<HorrorSteveEntity> {

    private int tickCount = 0;
    private Player targetPlayer = null;
    private LivingEntity targetMob = null;

    public KillingMobAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 60, 60);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            Player player = optionalPlayers.get().get(0);

            // プレイヤーの半径30ブロック以内の対象エンティティを取得
            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30.0D), 
                entity -> entity instanceof Villager || entity instanceof AbstractIllager || entity instanceof Cat || entity instanceof Wolf || entity instanceof AbstractGolem);

            for (LivingEntity mob : nearbyEntities) {
                if (!isMobVisibleToPlayer(player, mob, level)) {
                    // 対象の種類に応じた悲鳴を設定
                    net.minecraft.sounds.SoundEvent scream = null;
                    if (mob instanceof Villager) scream = SoundEvents.VILLAGER_HURT;
                    else if (mob instanceof AbstractIllager) scream = SoundEvents.PILLAGER_HURT;
                    else if (mob instanceof Cat) scream = SoundEvents.CAT_HURT;
                    else if (mob instanceof Wolf) scream = SoundEvents.WOLF_HURT;
                    else if (mob instanceof IronGolem) scream = SoundEvents.IRON_GOLEM_HURT;
                    else if (mob instanceof SnowGolem) scream = SoundEvents.SNOW_GOLEM_HURT;
                    
                    if (scream != null) {
                        level.playSound(null, mob.blockPosition(), scream, SoundSource.HOSTILE, 2.0f, 1.0f);
                    }

                    // スティーブは移動せず、その場で対象を即死させてクリティカル音を鳴らす
                    mob.hurt(level.damageSources().mobAttack(owner), 9999.0f);
                    level.playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5f, 0.7f);
                    break;
                }
            }
        }
        
        // 即座にアクション終了とする
        owner.isActionActive = false;
    }

    private boolean isMobVisibleToPlayer(Player player, LivingEntity mob, ServerLevel level) {
        Vec3 playerEye = player.getEyePosition();
        Vec3 mobEye = mob.getEyePosition();
        
        // 1. 視界の前方か後方かを内積で判定
        Vec3 viewVector = player.getViewVector(1.0F).normalize();
        Vec3 toMob = mobEye.subtract(playerEye).normalize();
        
        // 前方（画面内）にいる場合は、ブロックで遮られているかチェック
        if (viewVector.dot(toMob) > 0.0) {
            HitResult result = level.clip(new ClipContext(playerEye, mobEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            // ブロックに当たっていれば見えていない(false)、当たっていなければ見えている(true)
            return result.getType() == HitResult.Type.MISS;
        }
        
        // 背後にいるなら見えていない
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false; // start()で完結するため継続しない
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // 使用しない
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        owner.isActionActive = false;
    }
}
