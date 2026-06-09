package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import com.example.util.HorrorMessages;
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

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            Player target = optionalPlayers.get().get(0);
            
            // プレイヤーの周囲（半径5〜10ブロック）の地面を探す
            BlockPos targetPos = null;
            BlockPos origin = target.blockPosition();
            
            for (int i = 0; i < 30; i++) {
                int dx = level.random.nextInt(15) - 7;
                int dz = level.random.nextInt(15) - 7;
                int dy = level.random.nextInt(5) - 2;
                
                // 近すぎる場所は避ける（半径3ブロック以内は除外）
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
                // 看板をプレイヤーの方向に向けて設置する
                double dx = target.getX() - targetPos.getX();
                double dz = target.getZ() - targetPos.getZ();
                float angle = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                int rotation = net.minecraft.util.Mth.floor((double)((angle + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
                
                BlockState signState = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation);
                level.setBlock(targetPos, signState, 3);
                
                // 看板の文字を設定
                BlockEntity blockEntity = level.getBlockEntity(targetPos);
                if (blockEntity instanceof SignBlockEntity signEntity) {
                    String message = HorrorMessages.getRandomMessage(level.random);
                    
                    // 看板のテキストを設定（赤色、光る）
                    signEntity.updateText((signText) -> {
                        return signText.setMessage(1, Component.literal(message))
                                       .setColor(DyeColor.RED)
                                       .setHasGlowingText(true);
                    }, true); // true = front text
                    
                    // 看板の変更を保存
                    signEntity.setChanged();
                    level.sendBlockUpdated(targetPos, signState, signState, 3);
                }
            }
        }
        
        owner.isActionActive = false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return false;
    }
}
