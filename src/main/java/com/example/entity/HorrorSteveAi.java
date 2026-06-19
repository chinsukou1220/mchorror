package com.example.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.valueproviders.UniformInt;

import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import com.example.SsttaallkkeerrMod;
import com.example.util.StalkerLocationCalculator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

public class HorrorSteveAi {
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super HorrorSteveEntity>>> SENSOR_TYPES = ImmutableList.of(
            SsttaallkkeerrMod.GLOBAL_PLAYER_SENSOR,
            SensorType.NEAREST_LIVING_ENTITIES
    );

    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER
    );

    public static Brain.Provider<HorrorSteveEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<HorrorSteveEntity> makeBrain(HorrorSteveEntity entity, Brain<HorrorSteveEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<HorrorSteveEntity> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                unconditionalCustomBehavior(), // ユーザー自身が中身を書くための無条件Behavior
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink()
        ));
    }

    private static void initIdleActivity(Brain<HorrorSteveEntity> brain) {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                // カスタムビヘイビア：NEAREST_PLAYERSセンサーを利用してプレイヤーを追いかける
                stalkNearestPlayer(),
                // 新しく作成したワープ専用行動を追加
                new com.example.entity.action.ActionController(),
                new RunOne<>(ImmutableList.of(
                        Pair.of(RandomStroll.stroll(0.6f), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                ))
        ));
    }

    public static void updateActivity(HorrorSteveEntity entity) {
        entity.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
    }

    // --- カスタム行動（Behavior） ---
    public static Behavior<HorrorSteveEntity> stalkNearestPlayer() {
        // この行動は「NEAREST_PLAYERSメモリが存在する」時に実行される
        return new Behavior<HorrorSteveEntity>(
                Map.of(
                        MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT,
                        MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
                )
        ) {
            @Override
            protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
                // アクション実行中（SkinDebug等）はストーカー行動（接近と見つめる処理）を停止
                if (owner.isActionActive) return false;
                
                // センサーが捉えたプレイヤーが1人以上いるか確認
                Optional<List<Player>> players = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
                return players.isPresent() && !players.get().isEmpty();
            }

            @Override
            protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
                // 一番近くにいるプレイヤーを取得
                Player target = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS).get().get(0);
                owner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 0.6f, 2));
                // プレイヤーをじっと見つめる
                owner.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
            }
        };
    }

    // --- 無条件行動（Behavior） ---
    public static Behavior<HorrorSteveEntity> unconditionalCustomBehavior() {
        return new Behavior<HorrorSteveEntity>(Map.of()) {
            @Override
            protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
                return true; // 常に実行条件をクリア
            }

            @Override
            protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
                // TODO: 行動が開始した瞬間に1回だけ実行したい処理をここに書く
                // （例：開始時に音を鳴らすなど）
            }

            @Override
            protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
                return true; // 常にこの行動を継続する
            }

            @Override
            protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
                Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
                if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
                    Player target = optionalPlayers.get().get(0);
                    
                    // カスタム強襲アクション（CAVE_AMBUSH等）が進行中の場合は、共通AIの視線・逃走判定をスキップする
                    if (owner.isActionActive && owner.isAggressiveStalking) {
                        owner.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new net.minecraft.world.entity.ai.behavior.EntityTracker(target, true));
                        owner.getLookControl().setLookAt(target, 45.0F, 90.0F);
                        return;
                    }
                    
                    // --- 突進攻撃モードの処理 ---
                    if (owner.isChargingToAttack) {
                        owner.chargeTicks++;
                        
                        // 殴った後のディレイ（0.5秒 = 10ティック）
                        if (owner.postHitWaitTicks > 0) {
                            owner.postHitWaitTicks++;
                            
                            if (owner.postHitWaitTicks >= 10) {
                                // 0.5秒後に強制透明化してワープ
                                if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.example.SsttaallkkeerrMod.KANAKIRIGOE.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.example.SsttaallkkeerrMod.WQWQWQQ.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.example.SsttaallkkeerrMod.OSOUTOKI.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                                }
                                owner.setInvisible(true);
                                owner.isAggressiveStalking = false;
                                owner.isActionActive = false; // アクション終了
                                owner.isChargingToAttack = false;
                                owner.setAggressive(false);
                                owner.setPose(net.minecraft.world.entity.Pose.STANDING);
                                owner.chargeTicks = 0;
                                owner.postHitWaitTicks = 0;
                                
                                owner.hasBeenSeenSinceWarp = false;
                                double ang = level.random.nextDouble() * Math.PI * 2;
                                double dist = 80.0 + level.random.nextDouble() * 40.0;
                                double fx = target.getX() + Math.cos(ang) * dist;
                                double fz = target.getZ() + Math.sin(ang) * dist;
                                double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
                                owner.teleportTo(fx, fy, fz);
                                owner.isWaitingForWarp = true;
                                owner.lastWarpTime = level.getGameTime();
                            }
                            return; // 硬直中は他の処理をスキップ
                        }
                        
                        // 音をループ再生
                        if (owner.chargeTicks % 30 == 0) {
                            level.playSound(null, owner.blockPosition(), com.example.SsttaallkkeerrMod.KANAKIRIGOE, net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.0F);
                            level.playSound(null, owner.blockPosition(), com.example.SsttaallkkeerrMod.WQWQWQQ, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.0F);
                        }
                        
                        owner.getNavigation().moveTo(target, 20.0); // 超高速でプレイヤーに向かう
                        owner.getLookControl().setLookAt(target, 45.0F, 90.0F);
                        
                        // 攻撃範囲（2.5ブロック以内）に到達するか、スタック時のタイムアウト（40ティック = 2秒）で終了
                        if (owner.distanceTo(target) <= 2.5 || owner.chargeTicks >= 40) {
                            if (owner.distanceTo(target) <= 4.0) {
                                owner.doHurtTarget(target); // 一発殴る
                                // 殴れた場合は硬直フェーズへ移行（すぐには消えない）
                                owner.postHitWaitTicks = 1;
                            } else {
                                // タイムアウト等で殴れなかった場合は即座に消滅
                                if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.example.SsttaallkkeerrMod.KANAKIRIGOE.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.example.SsttaallkkeerrMod.WQWQWQQ.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(com.example.SsttaallkkeerrMod.OSOUTOKI.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE));
                                }
                                owner.setInvisible(true);
                                owner.isAggressiveStalking = false;
                                owner.isActionActive = false; // アクション終了
                                owner.isChargingToAttack = false;
                                owner.setAggressive(false);
                                owner.setPose(net.minecraft.world.entity.Pose.STANDING);
                                owner.chargeTicks = 0;
                                
                                owner.hasBeenSeenSinceWarp = false;
                                double ang = level.random.nextDouble() * Math.PI * 2;
                                double dist = 80.0 + level.random.nextDouble() * 40.0;
                                double fx = target.getX() + Math.cos(ang) * dist;
                                double fz = target.getZ() + Math.sin(ang) * dist;
                                double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
                                owner.teleportTo(fx, fy, fz);
                                owner.isWaitingForWarp = true;
                                owner.lastWarpTime = level.getGameTime();
                            }
                        }
                        return; // 突進中は通常の視線判定や逃走処理をスキップ
                    }
                    
                    // --- 視線と画面内判定 ---
                    if (!owner.isActionActive) {
                        boolean canSee = owner.getSensing().hasLineOfSight(target);
                        
                        // プレイヤーとの距離を計算
                        double distanceToPlayer = owner.distanceTo(target);
                        
                        // プレイヤーの向いている方向と、プレイヤーからスティーブへの方向の内積（Dot Product）を計算
                        Vec3 viewVector = target.getViewVector(1.0F).normalize();
                        Vec3 vectorToEntity = owner.getEyePosition().subtract(target.getEyePosition()).normalize();
                        double dotProduct = viewVector.dot(vectorToEntity);
                        
                        // 距離に応じて判定基準を変更（目が合ったかどうかの判定をより厳しく）
                        // 5ブロック以下：0.5（前方広角） / 0秒(0ティック)
                        // 20ブロック以下：0.7（前方45度程度） / 0.1秒(2ティック)
                        // 20ブロック超：0.95（ほぼ画面中央） / 1秒(20ティック)
                        double thresholdDot;
                        int thresholdTicks;
                        
                        if (distanceToPlayer <= 5.0) {
                            thresholdDot = 0.5;
                            thresholdTicks = 0;
                        } else if (distanceToPlayer <= 20.0) {
                            thresholdDot = 0.7;
                            thresholdTicks = 2;
                        } else {
                            thresholdDot = 0.95;
                            thresholdTicks = 20;
                        }
                        
                        // 基準以上なら画面に捉えたと判定
                        boolean isLookingAtMe = dotProduct > thresholdDot;
                        // 視界の半球（前方）にいるかどうかの判定（内積が0より大きければ前方）
                        boolean isInFrontOfPlayer = dotProduct > 0.0;
                        
                        if (canSee && isInFrontOfPlayer) {
                            if (!owner.hasBeenSeenSinceWarp) {
                                // 【最優先】まだ見つかってない（観察中）かつプレイヤーの前にいるなら、即座に立ち止まって透明化を解除する
                                owner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                                owner.setInvisible(false);
                                
                                // その上で、プレイヤーがこちらをしっかり見つめている（目が合っている）かを判定する
                                if (isLookingAtMe) {
                                    owner.eyeContactTicks++; // 目が合っている時間をカウントアップ
                                    
                                    // 距離に応じた時間（ティック数）見つめ合った場合のみ「見られた」と判定
                                    if (owner.eyeContactTicks >= thresholdTicks) {
                                        if (distanceToPlayer <= 7.0) {
                                            // 7ブロック以内の近距離で見られた場合は、即座にワープせず突進モードに移行
                                            owner.isChargingToAttack = true;
                                            owner.setAggressive(true);
                                            owner.chargeTicks = 0;
                                            owner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                                            // 突っ込んでくるときの暗闇エフェクトを付与
                                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, false));
                                            
                                            // 追加された「襲う時の音」を鳴らす（ループしないosoutokiをここで一回だけ鳴らす）
                                            level.playSound(null, owner.blockPosition(), com.example.SsttaallkkeerrMod.OSOUTOKI, net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 1.0F);
                                        } else {
                                            owner.hasBeenSeenSinceWarp = true;
                                            owner.timeWhenSeen = level.getGameTime(); // 通常の逃走開始時間を記録
                                            owner.isActionActive = true; // アクション開始を宣言
                                            com.example.entity.action.SoundAction.playRandomCompressedSound(level, target.blockPosition());
                                        }
                                    }
                                } else {
                                    // 視界には入っているが、カメラはそっぽを向いている場合は見つめ合いカウントをリセット
                                    owner.eyeContactTicks = 0;
                                }
                            } else {
                                // すでに見つかった後（逃走中）
                                // 逃走中の透明化タイミングは ActionController が管理するためここでは上書きしない
                            }
                        } else {
                            // 壁の裏など完全に視線が遮られている場合、または視線は通っているが「プレイヤーの背後」にいる場合
                            owner.eyeContactTicks = 0;
                            
                            if (!owner.hasBeenSeenSinceWarp) {
                                if (!canSee) {
                                    // 射線が通っていない（壁の裏などにワープした）場合は、射線が通るまで近づく
                                    owner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(target, false), 1.2f, 3));
                                    
                                    // --- スタック検知とワープ処理 ---
                                    if (owner.tickCount % 20 == 0) { // 1秒ごとにチェック
                                        if (owner.position().distanceToSqr(owner.lastInvisiblePos) < 2.0) {
                                            owner.invisibleStuckTicks++;
                                        } else {
                                            owner.invisibleStuckTicks = 0;
                                        }
                                        owner.lastInvisiblePos = owner.position();
                                        
                                        // 3秒間（3回のチェック）あまり動いていなかったらワープ
                                        if (owner.invisibleStuckTicks >= 3) {
                                            owner.invisibleStuckTicks = 0;
                                            
                                            // プレイヤーの方へ 5〜15ブロック近づく（水平方向で計算）
                                            double dx = target.getX() - owner.getX();
                                            double dz = target.getZ() - owner.getZ();
                                            double dist = Math.sqrt(dx * dx + dz * dz);
                                            double moveDist = Math.min(dist - 3.0, 5.0 + level.random.nextDouble() * 10.0);
                                            
                                            if (moveDist > 0 && dist > 0) {
                                                double baseAngle = Math.atan2(dz, dx);
                                                net.minecraft.core.BlockPos validP = null;
                                                
                                                // 広めの扇形（-60度〜+60度）の範囲で空洞を最大10回探す
                                                for (int i = 0; i < 10; i++) {
                                                    double angleOffset = (level.random.nextDouble() - 0.5) * (Math.PI * 2.0 / 3.0);
                                                    double finalAngle = baseAngle + angleOffset;
                                                    
                                                    int px = (int)(owner.getX() + Math.cos(finalAngle) * moveDist);
                                                    int pz = (int)(owner.getZ() + Math.sin(finalAngle) * moveDist);
                                                    int py = target.blockPosition().getY();
                                                    
                                                    // プレイヤーの高さ周辺で安全な床を探す
                                                    for (int y = py + 15; y >= py - 15; y--) {
                                                        net.minecraft.core.BlockPos check = new net.minecraft.core.BlockPos(px, y, pz);
                                                        if (level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir() && level.getBlockState(check.below()).canOcclude()) {
                                                            validP = check;
                                                            break;
                                                        }
                                                    }
                                                    if (validP != null) {
                                                        break; // 見つかったら即終了
                                                    }
                                                }
                                                
                                                if (validP != null) {
                                                    owner.teleportTo(validP.getX() + 0.5, validP.getY(), validP.getZ() + 0.5);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 射線が通った場合
                                    if (owner.isAggressiveStalking) {
                                        // GoBehindモードならそのまま3ブロックまで詰める
                                        owner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(target, false), 1.2f, 3));
                                    } else {
                                        // ランダムワープなら射線が通った場所で立ち止まって見つめる
                                        owner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                                    }
                                }
                                
                                // 壁の裏（canSee=false）なら透明化＆無音、射線が通った（canSee=true）なら実体化＆音復活
                                owner.setInvisible(!canSee);
                                owner.setSilent(!canSee);
                            }
                        }
                    }
                    
                    // --- 歩行・視線の制御 ---
                    if (owner.hasBeenSeenSinceWarp) {
                        // 見つかった後は、プレイヤーから遠ざかる（逃げる）ように歩く
                        if (owner.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty()) {
                            Vec3 posAway = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(owner, 16, 7, target.position());
                            if (posAway != null) {
                                owner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(posAway, 1.2f, 0));
                            }
                        }
                    }
                    
                    // ストーキング中も逃走中も、常にプレイヤーの目をじっと見つめ続ける
                    owner.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
                    owner.getLookControl().setLookAt(target, 45.0F, 90.0F);
                    
                    // （※強制的に体ごと回転させる処理は、逃走時に逆にプレイヤーに向かって走ってしまう原因になるため削除）
                }
            }
        };
    }
}