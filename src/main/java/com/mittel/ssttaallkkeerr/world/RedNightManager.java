package com.mittel.ssttaallkkeerr.world;

import com.mittel.ssttaallkkeerr.SsttaallkkeerrMod;
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
                
                // 現在の確率とレベルを取得
                RedNightState state = RedNightState.get(level);
                
                // 確率判定
                if (level.random.nextFloat() < state.currentProbability) {
                    // 赤い夜発生！確率を10%上げ、弱体化レベルを1上げる
                    state.currentProbability = Math.min(1.0f, state.currentProbability + 0.10f);
                    state.weaknessLevel += 1;
                    state.setDirty(); // 変更を保存
                    
                    startRedNight(level);
                }
            }
            
            // 赤い夜がアクティブな間、敵モブにバフをかけ続ける
            if (isRedNightActive) {
                if (level.getGameTime() % 100 == 0) {
                    RedNightState state = RedNightState.get(level);
                    // 2回に1回レベルが上がるように調整 (weaknessLevel=1,2で0、3,4で1、5で2...)
                    int amplifier = Math.max(0, (state.weaknessLevel - 1) / 2); 
                    for (ServerPlayer player : level.players()) {
                        // プレイヤーの周囲128ブロック以内の敵モブを取得
                        for (net.minecraft.world.entity.monster.Monster monster : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, player.getBoundingBox().inflate(128.0))) {
                            // 攻撃力上昇 (Strength)
                            monster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, amplifier, false, false, false));
                            
                            // 耐性 (Resistance) - レベル5で無敵になるのを防ぐため最大レベル3 (80%カット) まで
                            int resAmp = Math.min(amplifier, 3);
                            monster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, resAmp, false, false, false));
                        }
                    }
                }
                
                // 強力なエンティティの自然スポーン処理 (レベルに応じて間隔が短くなる)
                RedNightState bossState = RedNightState.get(level);
                // レベル5で1200tick(60秒)、レベルが上がるごとに100tick(5秒)短縮、最短200tick(10秒)
                int bossInterval = Math.max(200, 1200 - (bossState.weaknessLevel - 5) * 100);
                if (level.getGameTime() % bossInterval == 0) {
                    if (bossState.weaknessLevel >= 5) {
                        for (ServerPlayer player : level.players()) {
                            net.minecraft.world.entity.EntityType<?> toSpawn = null;
                            net.minecraft.world.entity.EntityType<?>[] bosses;
                            if (bossState.weaknessLevel == 5) {
                                bosses = new net.minecraft.world.entity.EntityType<?>[]{
                                    net.minecraft.world.entity.EntityType.RAVAGER,
                                    net.minecraft.world.entity.EntityType.BLAZE
                                };
                            } else if (bossState.weaknessLevel == 6) {
                                bosses = new net.minecraft.world.entity.EntityType<?>[]{
                                    net.minecraft.world.entity.EntityType.WARDEN,
                                    net.minecraft.world.entity.EntityType.RAVAGER,
                                    net.minecraft.world.entity.EntityType.BLAZE
                                };
                            } else if (bossState.weaknessLevel == 7) {
                                bosses = new net.minecraft.world.entity.EntityType<?>[]{
                                    net.minecraft.world.entity.EntityType.WITHER,
                                    net.minecraft.world.entity.EntityType.BLAZE
                                };
                            } else {
                                bosses = new net.minecraft.world.entity.EntityType<?>[]{
                                    net.minecraft.world.entity.EntityType.RAVAGER, 
                                    net.minecraft.world.entity.EntityType.WARDEN, 
                                    net.minecraft.world.entity.EntityType.WITHER, 
                                    net.minecraft.world.entity.EntityType.BLAZE
                                };
                            }
                            toSpawn = bosses[level.random.nextInt(bosses.length)];
                            
                            if (toSpawn != null) {
                                // プレイヤーから15～25ブロック離れた位置にスポーン
                                double dx = (level.random.nextBoolean() ? 1 : -1) * (15 + level.random.nextInt(10));
                                double dz = (level.random.nextBoolean() ? 1 : -1) * (15 + level.random.nextInt(10));
                                net.minecraft.core.BlockPos spawnPos = new net.minecraft.core.BlockPos(
                                    (int)(player.getX() + dx), 
                                    (int)player.getY(), 
                                    (int)(player.getZ() + dz)
                                );
                                // 地表の高さを取得
                                spawnPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos);
                                
                                net.minecraft.world.entity.Entity entity = toSpawn.create(level);
                                if (entity != null) {
                                    entity.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                                    level.addFreshEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 朝になったら終了
            if (isRedNightActive) {
                endRedNight(level);
            }
            checkedThisNight = false; // 次の夜に向けてリセット
        }
        
        RedNightState state = RedNightState.get(level);
        // [RESTORED] チャット間隔: (13 - レベル) 分 = ((13 - レベル) * 1200) ティック、最低1分
        int chatIntervalTicks = Math.max(1, 13 - state.weaknessLevel) * 1200;
        
        // 3回目の赤い夜以降、計算された間隔ごとに不気味なチャットを送信する
        if (level.getGameTime() % chatIntervalTicks == 0) {
            if (state.weaknessLevel >= 3) {
                // ホラースティーブがプレイヤーの周囲（128ブロック以内）に存在するかどうかを確認
                boolean hasHorrorSteve = false;
                for (ServerPlayer p : level.players()) {
                    if (!level.getEntitiesOfClass(com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity.class, p.getBoundingBox().inflate(128.0)).isEmpty()) {
                        hasHorrorSteve = true;
                        break;
                    }
                }
                
                if (hasHorrorSteve) {
                    String[] messages = {
                    "Why won't you die?",
                    "Just die already.",
                    "Why? Why? Why? Why? Why?",
                    "Give up.",
                    "It hurts.",
                    "You can't escape.",
                    "Need more?",
                    "I am bleeding.",
                    "The sky is weeping.",
                    "Stop running.",
                    "We are all dead down here.",
                    "There is no morning.",
                    "Close your eyes.",
                    "Don't look up.",
                    "Why won't you give up?",
                    "Haven't you had enough?"
                };
                String msg = messages[level.random.nextInt(messages.length)];
                
                // エンティティの名前（文字化け）を生成
                net.minecraft.network.chat.Component senderName = net.minecraft.network.chat.Component.literal("UnknownEntity")
                    .withStyle(net.minecraft.ChatFormatting.OBFUSCATED, net.minecraft.ChatFormatting.DARK_GRAY);
                
                // チャットの形式: <[文字化け]> メッセージ
                net.minecraft.network.chat.Component chatMsg = net.minecraft.network.chat.Component.empty()
                    .append(net.minecraft.network.chat.Component.literal("<").withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(senderName)
                    .append(net.minecraft.network.chat.Component.literal("> ").withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(net.minecraft.network.chat.Component.literal(msg).withStyle(net.minecraft.ChatFormatting.RED));
                
                for (ServerPlayer player : level.players()) {
                    player.sendSystemMessage(chatMsg);
                }
            }
            }
        }
    }

    public static void startRedNight(ServerLevel level) {
        isRedNightActive = true;
        
        for (ServerPlayer player : level.players()) {
            // 雷音を鳴らす（エルダーガーディアンの音はコンパイルエラー回避のため削除）
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 0.8f);
            
            // 画面のノイズ（激しい揺れ）を3秒間（60ティック）発生させる
            FriendlyByteBuf shakeBuf = PacketByteBufs.create();
            shakeBuf.writeInt(60);
            shakeBuf.writeFloat(1.5f); // 激しい揺れ
            ServerPlayNetworking.send(player, com.mittel.ssttaallkkeerr.network.ModNetworking.SHAKE_PACKET_ID, shakeBuf);
            
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
