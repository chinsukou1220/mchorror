package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.player.Player;
import com.mittel.ssttaallkkeerr.SsttaallkkeerrMod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * プレイヤーの周辺で不気味な音を鳴らすアクション。
 * 足音、モブの音、ドアを叩く音や、将来追加するカスタムサウンドをランダムで再生します。
 */
public class SoundAction extends Behavior<HorrorSteveEntity> {

    private final Random random = new Random();

    public SoundAction() {
        super(Map.of(net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_PLAYERS, net.minecraft.world.entity.ai.memory.MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    /**
     * プレイヤー周辺で不気味な音を鳴らす（HuntAction等から直接呼び出し可能）。
     */
    public static void playCreepySound(ServerLevel level, Player target) {
        Random rng = new Random();
        SoundEvent[] sounds = {
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH,
            SoundEvents.STONE_STEP
        };
        SoundEvent selectedSound = sounds[rng.nextInt(sounds.length)];
        double angle = rng.nextDouble() * Math.PI * 2;
        double distance = 3.0 + rng.nextDouble() * 5.0;
        double x = target.getX() + Math.cos(angle) * distance;
        double y = target.getY() + 1.0;
        double z = target.getZ() + Math.sin(angle) * distance;
        level.playSound(null, x, y, z, selectedSound, SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    /**
     * ランダムなcompressed音（1〜10）を指定位置で鳴らす。
     */
    public static void playRandomCompressedSound(ServerLevel level, net.minecraft.core.BlockPos pos) {
        Random rng = new Random();
        SoundEvent[] sounds = {
            SsttaallkkeerrMod.HORROR_ACCENT_1, SsttaallkkeerrMod.HORROR_ACCENT_2,
            SsttaallkkeerrMod.HORROR_ACCENT_3, SsttaallkkeerrMod.HORROR_ACCENT_4,
            SsttaallkkeerrMod.HORROR_ACCENT_5, SsttaallkkeerrMod.HORROR_ACCENT_6,
            SsttaallkkeerrMod.HORROR_ACCENT_7, SsttaallkkeerrMod.HORROR_ACCENT_8,
            SsttaallkkeerrMod.HORROR_ACCENT_9, SsttaallkkeerrMod.HORROR_ACCENT_10
        };
        SoundEvent selectedSound = sounds[rng.nextInt(sounds.length)];
        level.playSound(null, pos, selectedSound, SoundSource.HOSTILE, 2.0f, 1.0f);
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> players = owner.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_PLAYERS);
        if (players.isPresent() && !players.get().isEmpty()) {
            playCreepySound(level, players.get().get(0));
        }
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false; // 1度鳴らしたらすぐ終了
    }
}
