package com.mittel.ssttaallkkeerr.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

public class RandomBlockFeature extends Feature<NoneFeatureConfiguration> {
    private static final List<Block> VALID_BLOCKS = new ArrayList<>();

    public RandomBlockFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static void initBlocks() {
        if (VALID_BLOCKS.isEmpty()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                // 液体、ポータル、ゲートウェイなどを除外
                if (block instanceof LiquidBlock) continue;
                if (block instanceof NetherPortalBlock) continue;
                if (block instanceof EndPortalBlock) continue;
                if (block instanceof EndGatewayBlock) continue;
                
                // 水や溶岩の明示的除外と、氷などの溶けるブロックの除外
                if (block == Blocks.WATER || block == Blocks.LAVA) continue;
                if (block == Blocks.ICE || block == Blocks.FROSTED_ICE || block == Blocks.BLUE_ICE || block == Blocks.PACKED_ICE) continue;
                if (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW) continue;
                
                // 空気やシステム用ブロックも除外
                if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) continue;
                if (block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK || block == Blocks.REPEATING_COMMAND_BLOCK) continue;
                if (block == Blocks.STRUCTURE_BLOCK || block == Blocks.STRUCTURE_VOID || block == Blocks.JIGSAW) continue;
                if (block == Blocks.BARRIER || block == Blocks.LIGHT) continue;
                
                // 内部に流体を持つ状態（水没ブロックなど）を除外
                if (!block.defaultBlockState().getFluidState().isEmpty()) continue;
                
                VALID_BLOCKS.add(block);
            }
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        initBlocks();
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        RandomSource random = context.random();

        if (VALID_BLOCKS.isEmpty()) return false;

        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        // チャンク内の Y=20 〜 100 に 1% の確率でブロックを配置
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 20; y <= 100; y++) {
                    if (random.nextFloat() < 0.01f) { // 1%
                        Block block = VALID_BLOCKS.get(random.nextInt(VALID_BLOCKS.size()));
                        BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                        level.setBlock(pos, block.defaultBlockState(), 2);
                    }
                }
            }
        }
        return true;
    }
}
