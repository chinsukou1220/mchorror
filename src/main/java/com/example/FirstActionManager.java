package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.example.entity.HorrorSteveEntity;

import java.util.List;
import java.util.Random;

public class FirstActionManager {

    private static final Random random = new Random();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(FirstActionManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        // ワールドの稼働時間が12000ティック（約10分）を超えているかチェック
        // ※誰もログインしていない時は進まないので、実質プレイ時間10分となる
        if (overworld.getGameTime() >= 12000) {
            FirstActionData data = FirstActionData.getServerState(server);
            
            // まだ実行されていない場合のみ実行
            if (!data.hasFired) {
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                
                // 誰もログインしていない場合は保留（次のティックに持ち越し）
                if (players.isEmpty()) return;
                
                // ランダムにターゲットを一人選ぶ
                ServerPlayer targetPlayer = players.get(random.nextInt(players.size()));
                ServerLevel targetLevel = targetPlayer.serverLevel();
                
                // スティーブをスポーンさせる（プレイヤーから見えない少し離れた位置、約30〜40ブロック先）
                spawnHorrorSteve(targetLevel, targetPlayer);
                
                // サーバー全体に偽の参加ログ（文字化け）を送信
                // §e は黄色、§k は難読化（文字化けチカチカ）、§r はリセット
                Component fakeJoinMessage = Component.literal("§e§kllll§r§e joined the game");
                server.getPlayerList().broadcastSystemMessage(fakeJoinMessage, false);
                
                // 実行済みとしてセーブデータを更新
                data.hasFired = true;
                data.setDirty();
            }
        }
    }
    
    private static void spawnHorrorSteve(ServerLevel level, ServerPlayer player) {
        HorrorSteveEntity steve = SsttaallkkeerrMod.HORROR_STEVE.create(level);
        if (steve != null) {
            // ランダムな方向の 30〜40 ブロック先
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 30.0 + random.nextDouble() * 10.0;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            
            // y座標は地表の高さを取得
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z);
            
            steve.setPos(x, y, z);
            
            // 初期状態は透明化しておく（遠くからストーキングを開始するため）
            steve.setInvisible(true);
            
            level.addFreshEntity(steve);
        }
    }
}
