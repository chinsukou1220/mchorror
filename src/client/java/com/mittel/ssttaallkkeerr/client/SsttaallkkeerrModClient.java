package com.mittel.ssttaallkkeerr.client;

import com.mittel.ssttaallkkeerr.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.mittel.ssttaallkkeerr.client.gui.WarningScreen;

public class SsttaallkkeerrModClient implements ClientModInitializer {

    // 起動につき1回だけ警告画面を出すためのフラグ
    private static boolean hasShownWarning = false;
    
    // コマンドからの画面表示を遅延させるためのカウンター
    private static int openWarningScreenDelay = -1;

    @Override
    public void onInitializeClient() {
        // エンティティレンダラーの登録（専用OBJレンダラー）
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.HORROR_STEVE, 
            com.mittel.ssttaallkkeerr.client.renderer.HorrorSteveRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.FINAL_ACTION_STEVE, 
            com.mittel.ssttaallkkeerr.client.renderer.FinalActionSteveRenderer::new
        );


        // ゴーストブロックの透過描画設定
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
            com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.GHOST_BLOCK, 
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
        ClientPlayNetworking.registerGlobalReceiver(com.mittel.ssttaallkkeerr.SsttaallkkeerrMod.RED_NIGHT_PACKET, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            client.execute(() -> {
                FogManager.isFogActive = active;
            });
        });

        // ジャンプスケアパケット受信登録
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.JUMPSCARE_PACKET_ID, (client, handler, buf, responseSender) -> {
            int imageIndex = buf.readInt();
            client.execute(() -> {
                // 0.5秒間表示
                JumpscareOverlay.trigger(500, imageIndex);
            });
        });

        // HUD描画登録
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(new JumpscareOverlay());

        // 毎ティック、シェイクの残り時間を減らす処理や霧の処理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused()) {
                CameraShakeHandler.tick();
                FogManager.tick(client);
            }
            
            // コマンドから画面を開くための遅延処理
            if (openWarningScreenDelay > 0) {
                openWarningScreenDelay--;
            } else if (openWarningScreenDelay == 0) {
                openWarningScreenDelay = -1;
                client.setScreen(new WarningScreen(null));
            }
        });

        // サーバー参加時に警告画面（説明画面）を表示する
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!hasShownWarning) {
                // 少しだけ遅延させないと画面が切り替わらないことがあるため
                client.execute(() -> {
                    client.setScreen(new WarningScreen(client.screen));
                });
                hasShownWarning = true;
            }
        });

        // クライアントコマンドの登録
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /testfog コマンド
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("testfog")
                .executes(context -> {
                    FogManager.isFogActive = !FogManager.isFogActive;
                    context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("Fog is now: " + FogManager.isFogActive));
                    return 1;
                }));
            
            // /testwarning コマンド（警告画面を強制表示）
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("testwarning")
                .executes(context -> {
                    // ChatScreenが閉じる処理と干渉しないよう、数ティック遅らせて開く
                    openWarningScreenDelay = 2;
                    context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("Opening warning screen..."));
                    return 1;
                }));
        });
    }
}
