package com.example.client;

import com.example.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TemplateModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // エンティティレンダラーの登録（専用OBJレンダラー）
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.example.TemplateMod.HORROR_STEVE, 
            com.example.client.HorrorSteveObjRenderer::new
        );


        // ゴーストブロックの透過描画設定
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
            com.example.TemplateMod.GHOST_BLOCK, 
            net.minecraft.client.renderer.RenderType.translucent()
        );

        // パケットの受信登録
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SHAKE_PACKET_ID, (client, handler, buf, responseSender) -> {
            int duration = buf.readInt();
            float intensity = buf.readFloat();
            
            // クライアントのメインスレッドで実行する必要がある
            client.execute(() -> {
                CameraShakeHandler.shakeTicks = duration;
                CameraShakeHandler.shakeIntensity = intensity;
            });
        });

        // Red Night の霧パケット受信登録
        ClientPlayNetworking.registerGlobalReceiver(com.example.TemplateMod.RED_NIGHT_PACKET, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            client.execute(() -> {
                FogManager.isFogActive = active;
            });
        });

        // 毎ティック、シェイクの残り時間を減らす処理や霧の処理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused()) {
                CameraShakeHandler.tick();
                FogManager.tick(client);
            }
        });
        // /testfog コマンドの登録
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("testfog")
                .executes(context -> {
                    FogManager.isFogActive = !FogManager.isFogActive;
                    context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("Fog is now: " + FogManager.isFogActive));
                    return 1;
                }));
        });
    }
}
