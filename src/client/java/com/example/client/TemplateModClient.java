package com.example.client;

import com.example.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TemplateModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // エンティティレンダラーの登録（上書きで消えていた処理を復旧）
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.example.TemplateMod.HORROR_STEVE, 
            com.example.client.renderer.HorrorSteveRenderer::new
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

        // 毎ティック、シェイクの残り時間を減らす処理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused()) {
                CameraShakeHandler.tick();
            }
        });
    }
}
