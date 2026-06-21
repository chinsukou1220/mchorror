package com.example;


import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import com.example.entity.sensor.GlobalPlayerSensor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import com.example.block.GhostBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.example.command.HorrorDebugCommand;
import com.example.item.HorrorDebugItem;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.BuiltInRegistries;

public class SsttaallkkeerrMod implements ModInitializer {
	public static final String MOD_ID = "ssttaallkkeerr";

    // Register Nightmare Dimension Feature
    public static final net.minecraft.world.level.levelgen.feature.Feature<NoneFeatureConfiguration> RANDOM_BLOCKS = 
        new com.example.world.feature.RandomBlockFeature(NoneFeatureConfiguration.CODEC);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceLocation RED_NIGHT_PACKET = new ResourceLocation(MOD_ID, "red_night");
	
	// ナイトメアディメンション滞在時間のトラッキング用
	public static final java.util.Map<java.util.UUID, Integer> nightmareTicks = new java.util.HashMap<>();
	
	// クラッシュ時にバックアップを復元するためのフラグ
	public static boolean shouldRestoreBackup = false;
	
	// リスポーン時にナイトメアへ送るプレイヤーのリスト
	public static final java.util.Set<java.util.UUID> pendingNightmare = new java.util.HashSet<>();

	// Register Horror Steve Entity
	public static final EntityType<com.example.entity.HorrorSteveEntity> HORROR_STEVE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			new ResourceLocation(MOD_ID, "horror_steve"),
			FabricEntityTypeBuilder.create(MobCategory.MISC, com.example.entity.HorrorSteveEntity::new)
					.dimensions(EntityDimensions.fixed(0.6F, 1.95F)).build()
	);

	public static final EntityType<com.example.entity.FinalActionSteveEntity> FINAL_ACTION_STEVE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			new ResourceLocation(MOD_ID, "finalactionsteve"),
			FabricEntityTypeBuilder.create(MobCategory.MISC, com.example.entity.FinalActionSteveEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.95f)).build()
	);

	// Register Custom Sensor
	public static final SensorType<GlobalPlayerSensor> GLOBAL_PLAYER_SENSOR = Registry.register(
			BuiltInRegistries.SENSOR_TYPE,
			new ResourceLocation(MOD_ID, "global_player_sensor"),
			new SensorType<>(GlobalPlayerSensor::new)
	);

	// Register a Spawn Egg for Horror Steve
	public static final Item HORROR_STEVE_SPAWN_EGG = Registry.register(
			BuiltInRegistries.ITEM,
			new ResourceLocation(MOD_ID, "horror_steve_spawn_egg"),
			new SpawnEggItem(HORROR_STEVE, 0x00A8FF, 0x000000, new Item.Properties())
	);

	// Register Debug Wand
	public static final Item HORROR_DEBUG_WAND = Registry.register(
			BuiltInRegistries.ITEM,
			new ResourceLocation(MOD_ID, "horror_debug_wand"),
			new HorrorDebugItem(new Item.Properties().stacksTo(1))
	);

	// Register House Debug
	public static final Item HOUSE_DEBUG = Registry.register(
			BuiltInRegistries.ITEM,
			new ResourceLocation(MOD_ID, "house_debug"),
			new com.example.item.HouseDebugItem(new Item.Properties().stacksTo(1))
	);

	// Register Custom Sounds
	public static final SoundEvent CREEPY_SOUND_1 = Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			new ResourceLocation(MOD_ID, "creepy_sound_1"),
			SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "creepy_sound_1"))
	);
	public static final SoundEvent KANAKIRIGOE = Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			new ResourceLocation(MOD_ID, "kanakirigoe"),
			SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "kanakirigoe"))
	);
	public static final SoundEvent OSOUTOKI = Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			new ResourceLocation(MOD_ID, "osoutoki"),
			SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "osoutoki"))
	);
	public static final SoundEvent WQWQWQQ = Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			new ResourceLocation(MOD_ID, "wqwqwqq"),
			SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "wqwqwqq"))
	);
	public static final SoundEvent BEEP = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "beep"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "beep")));
	public static final SoundEvent SAKEBIGOE_HAJIME = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "sakebigoe_hajime"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "sakebigoe_hajime")));
	public static final SoundEvent SAKEBIGOE_OWARI = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "sakebigoe_owari"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "sakebigoe_owari")));

	public static final SoundEvent HORROR_ACCENT_1 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_1"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_1")));
	public static final SoundEvent HORROR_ACCENT_2 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_2"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_2")));
	public static final SoundEvent HORROR_ACCENT_3 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_3"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_3")));
	public static final SoundEvent HORROR_ACCENT_4 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_4"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_4")));
	public static final SoundEvent HORROR_ACCENT_5 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_5"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_5")));
	public static final SoundEvent HORROR_ACCENT_6 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_6"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_6")));
	public static final SoundEvent HORROR_ACCENT_7 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_7"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_7")));
	public static final SoundEvent HORROR_ACCENT_8 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_8"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_8")));
	public static final SoundEvent HORROR_ACCENT_9 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_9"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_9")));
	public static final SoundEvent HORROR_ACCENT_10 = Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(MOD_ID, "horror_accent_10"), SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "horror_accent_10")));

	// Register Ghost Block
	public static final Block GHOST_BLOCK = Registry.register(
			BuiltInRegistries.BLOCK,
			new ResourceLocation(MOD_ID, "ghost_block"),
			new GhostBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.BLACK_STAINED_GLASS).strength(-1.0F, 3600000.0F))
	);

	public static final Item GHOST_BLOCK_ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			new ResourceLocation(MOD_ID, "ghost_block"),
			new BlockItem(GHOST_BLOCK, new Item.Properties())
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		
		// 起動時にクラッシュ復旧のマーカーがあれば、サーバーが立ち上がる前にバックアップを上書き復元する
		com.example.util.WorldBackupManager.checkPendingRestore();

		// Initialize First Action (10-minute event)
		FirstActionManager.init();

		// Register Entity attributes
		FabricDefaultAttributeRegistry.register(HORROR_STEVE, com.example.entity.HorrorSteveEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FINAL_ACTION_STEVE, com.example.entity.FinalActionSteveEntity.createAttributes());

		// Add Items to the Creative Mode Spawn Eggs tab
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
			content.accept(HORROR_STEVE_SPAWN_EGG);
			content.accept(HORROR_DEBUG_WAND);
			content.accept(HOUSE_DEBUG);
		});

		// Register Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			HorrorDebugCommand.register(dispatcher);
		});
		
		// Register Features
		Registry.register(BuiltInRegistries.FEATURE, new ResourceLocation(MOD_ID, "random_blocks"), RANDOM_BLOCKS);

		// 赤い夜での死亡（致命傷）をキャンセルしてナイトメアへ強制転送
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
				if (com.example.world.RedNightManager.isRedNightActive) {
					// ダメージによって体力が0以下になる場合（致命傷）
					if (player.getHealth() - amount <= 0.0f) {
						// 死亡時にフラグを立てておき、リスポーン後にナイトメアへ転送する
						pendingNightmare.add(player.getUUID());
						return true; // 通常通り死なせる
					}
				}
			}
			return true;
		});

		// プレイヤーがリスポーンした時の処理
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (pendingNightmare.contains(newPlayer.getUUID())) {
				pendingNightmare.remove(newPlayer.getUUID());
				
				net.minecraft.server.level.ServerLevel nightmareLevel = newPlayer.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(MOD_ID, "nightmare")));
				
				// オーバーワールド（リスポーンしたディメンション）にいるストーカーを消去
				net.minecraft.server.level.ServerLevel currentLevel = newPlayer.serverLevel();
				for (com.example.entity.HorrorSteveEntity steve : currentLevel.getEntitiesOfClass(com.example.entity.HorrorSteveEntity.class, newPlayer.getBoundingBox().inflate(100000.0))) {
					steve.discard();
				}
				
				if (nightmareLevel != null) {
					// プレイヤーのインベントリを念のためクリア
					newPlayer.getInventory().clearContent();
					
					// 完全に移動不可能（Slowness 127）にし、さらにジャンプ不能（Jump Boost 251）を付与
					newPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 999999, 127, false, false));
					newPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 999999, 250, false, false));
					
					// プレイヤーをY=1に転送
					newPlayer.teleportTo(nightmareLevel, newPlayer.getX(), 1, newPlayer.getZ(), newPlayer.getYRot(), newPlayer.getXRot());
					
					// ナイトメア用のタイマーをリセット
					com.example.SsttaallkkeerrMod.nightmareTicks.put(newPlayer.getUUID(), 0);
				}
			}
		});

		// プレイヤーがディメンションを移動した時（ネザーやエンドなど）
		net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			com.example.FirstActionManager.onPlayerChangeDimension(player, origin, destination);
		});

		// Register Red Night Tick Event
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				com.example.world.RedNightManager.tick(level);
			}
		});

		// Nightmare Dimension Sequence
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			net.minecraft.server.level.ServerLevel nightmareLevel = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(MOD_ID, "nightmare")));
			if (nightmareLevel != null) {
				for (net.minecraft.server.level.ServerPlayer player : nightmareLevel.players()) {
					// マップから滞在時間を取得
					int ticks = com.example.SsttaallkkeerrMod.nightmareTicks.getOrDefault(player.getUUID(), 0);
					ticks++;
					com.example.SsttaallkkeerrMod.nightmareTicks.put(player.getUUID(), ticks);
					
					// 動きを完全に封じる（スプリントダッシュなどの慣性も殺す）
					player.setDeltaMovement(0, 0, 0);
					if (player.getY() > 1.5 || player.getY() < 0.5) {
					    player.teleportTo(nightmareLevel, player.getX(), 1.0, player.getZ(), player.getYRot(), player.getXRot());
					}
					
					// ディメンション移動直後はエフェクトが剥がれることがあるため、最初の7秒間(140ティック)は強制的に盲目を与え続ける
					if (ticks <= 140) {
						player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 20, 0, false, false, false));
					}

					if (ticks == 10) { // ディメンション読み込み直後（0.5秒後）にスポーン
						// 半径12ブロックの円状に36体のFinalActionSteveをスポーン
						int count = 36;
						double radius = 12.0;
						for (int i = 0; i < count; i++) {
							double angle = 2 * Math.PI * i / count;
							double x = player.getX() + radius * Math.cos(angle);
							double z = player.getZ() + radius * Math.sin(angle);

							com.example.entity.FinalActionSteveEntity steve = SsttaallkkeerrMod.FINAL_ACTION_STEVE.create(nightmareLevel);
							if (steve != null) {
								// Y=101にプレイヤーがいるのでそれに合わせる
								steve.setPos(x, player.getY(), z);
								// プレイヤーの方を向く
								steve.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.position());
								nightmareLevel.addFreshEntity(steve);
							}
						}
					}
					
					// 20秒後 (400ティック) に強制クラッシュ
					if (ticks == 400) {
					    // クラッシュでゲームが落ちた後、次回のマイクラ起動時にバックアップを復元するためのマーカーを作成
					    com.example.util.WorldBackupManager.setPendingRestore(server);
					    throw new RuntimeException("You lose. It was fun.");
					}
				}
			}
		});

		// ワールド初回起動時のバックアップ処理
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			com.example.util.WorldBackupManager.checkAndBackup(server);
		});
	}
}