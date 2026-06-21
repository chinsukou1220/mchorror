package com.mittel.ssttaallkkeerr.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class RedNightState extends SavedData {
    public float currentProbability = 0.30f; // 初期確率は30%
    public int weaknessLevel = 0; // 弱体化のレベル

    public RedNightState() {
        super();
    }

    public static RedNightState load(CompoundTag tag) {
        RedNightState state = new RedNightState();
        if (tag.contains("Probability")) {
            state.currentProbability = tag.getFloat("Probability");
        }
        if (tag.contains("WeaknessLevel")) {
            state.weaknessLevel = tag.getInt("WeaknessLevel");
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putFloat("Probability", currentProbability);
        tag.putInt("WeaknessLevel", weaknessLevel);
        return tag;
    }

    public static RedNightState get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(RedNightState::load, RedNightState::new, "ssttaallkkeerr_rednight_state");
    }
}
