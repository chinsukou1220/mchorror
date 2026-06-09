package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * HorrorSteveの行動（アクション）を統括するコントローラークラス。
 * ここで「いつ・どのアクションを起こすか」の確率や条件を管理し、
 * 条件に合致した具体的なアクション（RandomWarpBehaviorなど）を呼び出します。
 */
public class ActionController extends Behavior<HorrorSteveEntity> {

    public enum ActionType {
        NONE,
        WARP,
        GO_BEHIND,
        PLACE,
        BREAK,
        CHEST,
        HOUSE,
        SIGN,
        SOUND,
        UNDERGROUND,
        CAVE_AMBUSH,
        WALK_AWAY
    }
    
    // 呼び出すための具体的なアクションを保持しておく
    private final RandomWarpBehavior warpBehavior = new RandomWarpBehavior();
    private final GoBehindBehavior goBehindBehavior = new GoBehindBehavior();
    private final PlaceAction placeAction = new PlaceAction();  
    private final BreakAction breakAction = new BreakAction();
    private final ChestAction chestAction = new ChestAction();
    private final HouseAction houseAction = new HouseAction();
    private final SignAction signAction = new SignAction();
    private final SoundAction soundAction = new SoundAction();
    private final UndergroundAction undergroundAction = new UndergroundAction();
    private final CaveDiggingAmbushBehavior caveAmbushBehavior = new CaveDiggingAmbushBehavior();
    private final WalkAwayBehavior walkAwayBehavior = new WalkAwayBehavior();
    
    // 次に実行するアクションの種類
    private ActionType currentAction = ActionType.NONE;
    
    // プレイヤーの静止状態をトラッキングするための変数
    private Vec3 lastPlayerPos = null;
    private int playerStationaryTicks = 0;

    public ActionController() {
        // 最低条件：近くにプレイヤーがいる時だけコントローラーが作動する
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        this.currentAction = ActionType.NONE;
        
        Optional<List<Player>> players = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        
        // --- プレイヤーの静止判定 ---
        if (players.isPresent() && !players.get().isEmpty()) {
            Player target = players.get().get(0);
            
            // （デバッグ用の家判定メッセージは削除）
            if (this.lastPlayerPos != null) {
                // 距離の2乗が0.0001以下（ごくわずかな揺れは許容）なら静止しているとみなす
                if (target.position().distanceToSqr(this.lastPlayerPos) < 0.0001) {
                    this.playerStationaryTicks++;
                } else {
                    this.playerStationaryTicks = 0;
                }
            }
            this.lastPlayerPos = target.position();
        } else {
            this.playerStationaryTicks = 0;
            this.lastPlayerPos = null;
        }

        if (owner.hasBeenSeenSinceWarp) {
            long elapsed = level.getGameTime() - owner.timeWhenSeen;
            
            // 見つかってから0.5秒（10ティック）経過した時点で完全に透明化する
            // （または至近距離の突撃後などは直ちに透明化）
            // ※ラグ等で10をスキップした場合に備え、>= 10 かつ まだ透明化していない時 に実行
            if (elapsed >= 10 && !owner.isInvisible()) {
                owner.setInvisible(true);
            }
            
            // 透明化してからさらに2秒（40ティック）、つまり合計2.5秒（50ティック）経過した時点で強制逃走（ワープ待機状態）
            if (elapsed >= 50) {
                if (players.isPresent() && !players.get().isEmpty()) {
                    Player target = players.get().get(0);
                    // 遠くに飛ばす（80〜120ブロック）
                    double ang = level.random.nextDouble() * Math.PI * 2;
                    double dist = 80.0 + level.random.nextDouble() * 40.0;
                    double fx = target.getX() + Math.cos(ang) * dist;
                    double fz = target.getZ() + Math.sin(ang) * dist;
                    double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
                    owner.teleportTo(fx, fy, fz);
                }
                owner.isWaitingForWarp = true; // 次の出番まで待機
                owner.hasBeenSeenSinceWarp = false;
                owner.isActionActive = false;
                owner.lastWarpTime = level.getGameTime();
                owner.isAggressiveStalking = false;
                return false; // 次のアクション抽選に戻る
            }
            
            // または、歩いて逃げて壁の裏に入った（視線が切れた）ら、その瞬間に即座に逃走完了
            if (players.isPresent() && !players.get().isEmpty()) {
                Player target = players.get().get(0);
                if (!owner.getSensing().hasLineOfSight(target)) {
                    owner.setInvisible(true); // 直前に透明化
                    // 遠くに飛ばす
                    double ang = level.random.nextDouble() * Math.PI * 2;
                    double dist = 80.0 + level.random.nextDouble() * 40.0;
                    double fx = target.getX() + Math.cos(ang) * dist;
                    double fz = target.getZ() + Math.sin(ang) * dist;
                    double fy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
                    owner.teleportTo(fx, fy, fz);
                    
                    owner.isWaitingForWarp = true;
                    owner.hasBeenSeenSinceWarp = false;
                    owner.isActionActive = false;
                    owner.lastWarpTime = level.getGameTime();
                    owner.isAggressiveStalking = false;
                    return false;
                }
            }
            
            return false;
        } else {
            // まだ見られていない（ストーキング中）場合
            
            // --- アクションの実行中判定 ---
            // 既に何らかのアクション（逃走など）が実行中の場合は、新しい自発的アクションを重ねない
            if (owner.isActionActive) {
                return false;
            }
            
            // アクション待機中（透明化して遠くにいる状態）でない場合は新規アクションを起こさない
            if (!owner.isWaitingForWarp) {
                return false;
            }
            
            // 夜間（日が沈んでいる間）は「背後に回り込む」以外のアクション確率を1.5倍にする
            double multiplier = level.isDay() ? 1.0 : 1.5;
            
            // --- 静止状態に応じたアクション（GO_BEHIND） ---
            if (this.playerStationaryTicks >= 200) {
                // 10秒（200ティック）以上止まっている場合、毎ティック 0.1% (0.001) の確率で背後に現れる（夜間倍率対象外）
                if (Math.random() < 0.001) {
                    this.currentAction = ActionType.GO_BEHIND;
                    return true;
                }
            }
            // --- 通常のアクション（プレイヤーが動いている時） ---
            else {
                // プレイヤーの居場所を判定
                boolean isUnderground = false;
                boolean isNether = level.dimension() == net.minecraft.world.level.Level.NETHER;
                if (players.isPresent() && !players.get().isEmpty()) {
                    isUnderground = com.example.util.UndergroundDetector.isPlayerUnderground(level, players.get().get(0));
                }
                
                if (isUnderground || isNether) {
                    // ==========================================
                    // 洞窟（地下）・ネザー専用アクション
                    // ==========================================
                    
                    // 【新規】壁を掘り破ってくる恐ろしい強襲アクション（洞窟限定）
                    // 確率: 5% / ティック (テスト用)
                    if (isUnderground && Math.random() < 0.05 * multiplier) {
                        this.currentAction = ActionType.CAVE_AMBUSH;
                        return true;
                    }
                    
                    // 【新規】透明で近づき足音だけ残して去り、遠くで見つめるアクション（洞窟・ネザー限定）
                    // 確率: 5% / ティック (テスト用)
                    if (Math.random() < 0.05 * multiplier) {
                        this.currentAction = ActionType.WALK_AWAY;
                        return true;
                    }

                    // 洞窟（地下）限定のポルターガイストアクション
                    // 確率: 10% / ティック (テスト用)
                    if (isUnderground && Math.random() < 0.1 * multiplier) {
                        this.currentAction = ActionType.UNDERGROUND;
                        return true;
                    }
                    
                } else {
                    // ==========================================
                    // 地上（通常）アクション
                    // ==========================================
                    
                    // 通常状態でのランダムワープ抽選（約100秒に1回程度：0.0005）
                    if (Math.random() < 0.0005 * multiplier) {
                        this.currentAction = ActionType.WARP;
                        return true;
                    }
                    
                    // プレイヤーの置いたブロックを設置するアクション
                    if (Math.random() < 0.00005 * multiplier) {
                        this.currentAction = ActionType.PLACE;
                        return true;
                    }
                    
                    // 置いたブロックを全て破壊するアクション
                    if (Math.random() < 0.00002 * multiplier) {
                        this.currentAction = ActionType.BREAK;
                        return true;
                    }

                    // チェストに対する怪奇現象やイタズラ
                    if (Math.random() < 0.00005 * multiplier) {
                        this.currentAction = ActionType.CHEST;
                        return true;
                    }

                    // 家にいる時限定のホラーアクション
                    if (Math.random() < 0.0001 * multiplier) {
                        if (players.isPresent() && !players.get().isEmpty()) {
                            if (com.example.util.HouseDetector.isPlayerInHouse(level, players.get().get(0))) {
                                this.currentAction = ActionType.HOUSE;
                                return true;
                            }
                        }
                    }
                    
                    // 看板設置アクション（家にいる時限定）
                    if (Math.random() < 0.00005 * multiplier) {
                        if (players.isPresent() && !players.get().isEmpty()) {
                            if (com.example.util.HouseDetector.isPlayerInHouse(level, players.get().get(0))) {
                                this.currentAction = ActionType.SIGN;
                                return true;
                            }
                        }
                    }
                    
                    // 音を鳴らすホラーアクション
                    if (Math.random() < 0.0002 * multiplier) {
                        this.currentAction = ActionType.SOUND;
                        return true;
                    }
                }
            }
            
            // まだ時間が経っていなければ、アクションは起こさない
            return false;
        }
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // 決定されたアクションを実行
        if (this.currentAction == ActionType.WARP) {
            this.warpBehavior.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.GO_BEHIND) {
            this.goBehindBehavior.tryStart(level, owner, gameTime);
            this.playerStationaryTicks = 0; // 実行後に静止カウントをリセット
        } else if (this.currentAction == ActionType.PLACE) {
            this.placeAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.BREAK) {
            this.breakAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.CHEST) {
            this.chestAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.HOUSE) {
            this.houseAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.SIGN) {
            this.signAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.SOUND) {
            this.soundAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.UNDERGROUND) {
            this.undergroundAction.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.CAVE_AMBUSH) {
            this.caveAmbushBehavior.tryStart(level, owner, gameTime);
        } else if (this.currentAction == ActionType.WALK_AWAY) {
            this.walkAwayBehavior.tryStart(level, owner, gameTime);
        }
        this.currentAction = ActionType.NONE; // リセット
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // サブアクションがまだ実行中の場合はコントローラーを維持し、サブアクション自身の停止処理を完了させる
        if (this.warpBehavior.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.goBehindBehavior.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.placeAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.breakAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.chestAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.houseAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.signAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.soundAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.undergroundAction.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.caveAmbushBehavior.getStatus() == Behavior.Status.RUNNING) return true;
        if (this.walkAwayBehavior.getStatus() == Behavior.Status.RUNNING) return true;
        
        return owner.isActionActive;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        // サブアクションが実行中(RUNNING)の場合のみtickOrStopを呼ぶ
        // これにより、isActionActiveがfalseになった次のtickで適切にdoStopが呼ばれ、ステータスがSTOPPEDに戻る
        if (this.warpBehavior.getStatus() == Behavior.Status.RUNNING) this.warpBehavior.tickOrStop(level, owner, gameTime);
        if (this.goBehindBehavior.getStatus() == Behavior.Status.RUNNING) this.goBehindBehavior.tickOrStop(level, owner, gameTime);
        if (this.placeAction.getStatus() == Behavior.Status.RUNNING) this.placeAction.tickOrStop(level, owner, gameTime);
        if (this.breakAction.getStatus() == Behavior.Status.RUNNING) this.breakAction.tickOrStop(level, owner, gameTime);
        if (this.chestAction.getStatus() == Behavior.Status.RUNNING) this.chestAction.tickOrStop(level, owner, gameTime);
        if (this.houseAction.getStatus() == Behavior.Status.RUNNING) this.houseAction.tickOrStop(level, owner, gameTime);
        if (this.signAction.getStatus() == Behavior.Status.RUNNING) this.signAction.tickOrStop(level, owner, gameTime);
        if (this.soundAction.getStatus() == Behavior.Status.RUNNING) this.soundAction.tickOrStop(level, owner, gameTime);
        if (this.undergroundAction.getStatus() == Behavior.Status.RUNNING) this.undergroundAction.tickOrStop(level, owner, gameTime);
        if (this.caveAmbushBehavior.getStatus() == Behavior.Status.RUNNING) this.caveAmbushBehavior.tickOrStop(level, owner, gameTime);
        if (this.walkAwayBehavior.getStatus() == Behavior.Status.RUNNING) this.walkAwayBehavior.tickOrStop(level, owner, gameTime);
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        
        // ActionControllerが（プレイヤーが範囲外に出る等で）強制終了された場合、
        // 実行中のすべての子アクションも強制終了させる
        if (this.warpBehavior.getStatus() == Behavior.Status.RUNNING) this.warpBehavior.doStop(level, owner, gameTime);
        if (this.goBehindBehavior.getStatus() == Behavior.Status.RUNNING) this.goBehindBehavior.doStop(level, owner, gameTime);
        if (this.placeAction.getStatus() == Behavior.Status.RUNNING) this.placeAction.doStop(level, owner, gameTime);
        if (this.breakAction.getStatus() == Behavior.Status.RUNNING) this.breakAction.doStop(level, owner, gameTime);
        if (this.chestAction.getStatus() == Behavior.Status.RUNNING) this.chestAction.doStop(level, owner, gameTime);
        if (this.houseAction.getStatus() == Behavior.Status.RUNNING) this.houseAction.doStop(level, owner, gameTime);
        if (this.signAction.getStatus() == Behavior.Status.RUNNING) this.signAction.doStop(level, owner, gameTime);
        if (this.soundAction.getStatus() == Behavior.Status.RUNNING) this.soundAction.doStop(level, owner, gameTime);
        if (this.undergroundAction.getStatus() == Behavior.Status.RUNNING) this.undergroundAction.doStop(level, owner, gameTime);
        if (this.caveAmbushBehavior.getStatus() == Behavior.Status.RUNNING) this.caveAmbushBehavior.doStop(level, owner, gameTime);
        if (this.walkAwayBehavior.getStatus() == Behavior.Status.RUNNING) this.walkAwayBehavior.doStop(level, owner, gameTime);

        // 状態を完全にリセット
        owner.isActionActive = false;
        this.currentAction = ActionType.NONE;
    }
}
