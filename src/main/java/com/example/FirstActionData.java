package com.example;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * ワールドごとに一度だけ発生するイベントのフラグを保存するクラス。
 */
public class FirstActionData extends SavedData {
    public boolean hasFired = false;

    public static FirstActionData load(CompoundTag tag) {
        FirstActionData data = new FirstActionData();
        data.hasFired = tag.getBoolean("HasFiredFirstAction");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("HasFiredFirstAction", this.hasFired);
        return tag;
    }

    public static FirstActionData getServerState(MinecraftServer server) {
        // オーバーワールドのデータを取得
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return new FirstActionData(); // 安全対策
        
        DimensionDataStorage storage = level.getDataStorage();
        // 1.20における SavedData の取得・生成
        return storage.computeIfAbsent(FirstActionData::load, FirstActionData::new, "template_mod_first_action");
    }
}
