package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import com.mittel.ssttaallkkeerr.util.HorrorMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SignAction extends Behavior<HorrorSteveEntity> {

    public SignAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    /**
     * プレイヤー周囲に不気味な看板を設置する（HuntAction等から直接呼び出し可能）。
     */
    public static void placeCreepySign(ServerLevel level, Player target) {
        BlockPos targetPos = null;
        BlockPos origin = target.blockPosition();
        
        for (int i = 0; i < 30; i++) {
            int dx = level.random.nextInt(15) - 7;
            int dz = level.random.nextInt(15) - 7;
            int dy = level.random.nextInt(5) - 2;
            
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) {
                continue;
            }
            
            BlockPos candidate = origin.offset(dx, dy, dz);
            if (level.isEmptyBlock(candidate) && level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())) {
                targetPos = candidate;
                break;
            }
        }
        
        if (targetPos != null) {
            double dx = target.getX() - targetPos.getX();
            double dz = target.getZ() - targetPos.getZ();
            float angle = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            int rotation = net.minecraft.util.Mth.floor((double)((angle + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
            
            BlockState signState = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation);
            level.setBlock(targetPos, signState, 3);
            
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity instanceof SignBlockEntity signEntity) {
                String message = HorrorMessages.getRandomMessage(level.random);
                
                signEntity.updateText((signText) -> {
                    return signText.setMessage(1, Component.literal(message))
                                   .setColor(DyeColor.RED)
                                   .setHasGlowingText(true);
                }, true);
                
                signEntity.setChanged();
                level.sendBlockUpdated(targetPos, signState, signState, 3);
            }
        }
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            placeCreepySign(level, optionalPlayers.get().get(0));
        }
        
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false;
    }
}
