package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import com.example.network.ModNetworking;
import com.example.SsttaallkkeerrMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DisplayAction extends Behavior<HorrorSteveEntity> {

    public DisplayAction() {
        super(Map.of(net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_PLAYERS, net.minecraft.world.entity.ai.memory.MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> players = owner.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_PLAYERS);
        if (players.isPresent() && !players.get().isEmpty()) {
            Player target = players.get().get(0);
            if (target instanceof ServerPlayer serverPlayer) {
                // 1から3までのランダムなインデックスを生成
                int randomImageIndex = owner.getRandom().nextInt(3) + 1;
                
                // パケットを送信して画面に画像を表示
                ModNetworking.sendJumpscareToPlayer(serverPlayer, randomImageIndex);
                
                // ビックリさせるために非常に大きな音を鳴らす（雷の音）
                level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 5.0f, 1.0f);
                
                // 追加: osoutoki の音も同時に鳴らす
                level.playSound(null, target.blockPosition(), SsttaallkkeerrMod.OSOUTOKI, SoundSource.HOSTILE, 0.5f, 1.0f);
            }
        }
        
        // 単発アクションなので即座に終了状態へ
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false;
    }
}
