package com.mittel.ssttaallkkeerr.mixin;

import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import com.mittel.ssttaallkkeerr.world.RedNightManager;
import java.util.ArrayList;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void onMobsAt(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category, BlockPos pos, Holder<Biome> biome, CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        if (category == MobCategory.MONSTER && RedNightManager.isRedNightActive) {
            WeightedRandomList<MobSpawnSettings.SpawnerData> original = cir.getReturnValue();
            ArrayList<MobSpawnSettings.SpawnerData> modified = new ArrayList<>();
            if (original != null && original.unwrap() != null) {
                modified.addAll(original.unwrap());
            }
            
            // RedNightの時だけ自然スポーンの候補に追加（確率はゾンビやスケルトンと全く同じ「重み100」、湧く数も最大4匹の同等設定）
            modified.add(new MobSpawnSettings.SpawnerData(EntityType.GHAST, 100, 4, 4));
            modified.add(new MobSpawnSettings.SpawnerData(EntityType.BLAZE, 100, 4, 4));
            modified.add(new MobSpawnSettings.SpawnerData(EntityType.PILLAGER, 100, 4, 4));
            modified.add(new MobSpawnSettings.SpawnerData(EntityType.VINDICATOR, 100, 4, 4)); // イリジャー(ヴィンディケーター)も追加
            modified.add(new MobSpawnSettings.SpawnerData(EntityType.EVOKER, 20, 1, 1)); // エヴォーカーは少しレアに設定
            
            cir.setReturnValue(WeightedRandomList.create(modified));
        }
    }
}
