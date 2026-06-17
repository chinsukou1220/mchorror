package com.example.world;

import com.example.SsttaallkkeerrMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class RedNightManager {
    public static boolean isRedNightActive = false;
    private static boolean checkedThisNight = false;

    public static void tick(ServerLevel level) {
        long timeOfDay = level.getDayTime() % 24000;

        // 夕暮れ時 (13000) ～ 朝 (23000) までが夜
        if (timeOfDay >= 13000 && timeOfDay < 23000) {
            if (!checkedThisNight) {
                checkedThisNight = true;
                // 30%の確率でRed Night発生
                if (level.random.nextFloat() < 0.30f) {
                    startRedNight(level);
                }
            }
        } else {
            // 朝になったら終了
            if (isRedNightActive) {
                endRedNight(level);
            }
            checkedThisNight = false; // 次の夜に向けてリセット
        }
    }

    private static void startRedNight(ServerLevel level) {
        isRedNightActive = true;
        
        for (ServerPlayer player : level.players()) {
            // 雷音を鳴らす（エルダーガーディアンの音はコンパイルエラー回避のため削除）
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 0.8f);
            
            // 画面のノイズ（激しい揺れ）を3秒間（60ティック）発生させる
            FriendlyByteBuf shakeBuf = PacketByteBufs.create();
            shakeBuf.writeInt(60);
            shakeBuf.writeFloat(1.5f); // 激しい揺れ
            ServerPlayNetworking.send(player, com.example.network.ModNetworking.SHAKE_PACKET_ID, shakeBuf);
            
            // クライアントへ赤い霧をオンにするパケットを送信
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(true);
            ServerPlayNetworking.send(player, SsttaallkkeerrMod.RED_NIGHT_PACKET, buf);
        }
    }

    private static void endRedNight(ServerLevel level) {
        isRedNightActive = false;
        
        for (ServerPlayer player : level.players()) {
            // クライアントへ赤い霧をオフにするパケットを送信
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(false);
            ServerPlayNetworking.send(player, SsttaallkkeerrMod.RED_NIGHT_PACKET, buf);
        }
    }
}
