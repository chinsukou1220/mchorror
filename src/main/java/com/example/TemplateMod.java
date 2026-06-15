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

public class TemplateMod implements ModInitializer {
	public static final String MOD_ID = "template-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceLocation RED_NIGHT_PACKET = new ResourceLocation(MOD_ID, "red_night");

	// Register Horror Steve Entity
	public static final EntityType<com.example.entity.HorrorSteveEntity> HORROR_STEVE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			new ResourceLocation(MOD_ID, "horror_steve"),
			FabricEntityTypeBuilder.create(MobCategory.MISC, com.example.entity.HorrorSteveEntity::new)
					.dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // Standard Player dimensions
					.build()
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

	// Register Custom Sound (for future custom audio files)
	public static final SoundEvent CREEPY_SOUND_1 = Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			new ResourceLocation(MOD_ID, "creepy_sound_1"),
			SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "creepy_sound_1"))
	);

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

		// Initialize First Action (10-minute event)
		FirstActionManager.init();

		// Register Entity attributes
		FabricDefaultAttributeRegistry.register(HORROR_STEVE, com.example.entity.HorrorSteveEntity.createAttributes());

		// Add Items to the Creative Mode Spawn Eggs tab
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
			content.accept(HORROR_STEVE_SPAWN_EGG);
			content.accept(HORROR_DEBUG_WAND);
		});

	// Register Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			HorrorDebugCommand.register(dispatcher);
		});

		// Register Red Night Tick Event
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				com.example.world.RedNightManager.tick(level);
			}
		});
	}
}