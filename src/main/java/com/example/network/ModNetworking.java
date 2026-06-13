package com.example.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {

    public static final ResourceLocation SHAKE_PACKET_ID = new ResourceLocation("template-mod", "camera_shake");

    // サーバー側から特定のプレイヤーへ揺れパケットを送信するメソッド
    public static void sendShakeToPlayer(ServerPlayer player, int durationTicks, float intensity) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(durationTicks);
        buf.writeFloat(intensity);
        
        ServerPlayNetworking.send(player, SHAKE_PACKET_ID, buf);
    }
}
