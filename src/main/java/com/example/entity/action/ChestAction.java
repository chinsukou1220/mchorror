package com.example.entity.action;

import com.example.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class ChestAction extends Behavior<HorrorSteveEntity> {

    private int chestOpenTicks = 0;
    private BlockPos openedChestPos = null;

    public ChestAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (!optionalPlayers.isPresent() || optionalPlayers.get().isEmpty()) {
            owner.isActionActive = false;
            owner.lastWarpTime = level.getGameTime();
            return;
        }

        Player target = optionalPlayers.get().get(0);
        BlockPos origin = target.blockPosition();
        List<BlockPos> chestPositions = new ArrayList<>();

        // プレイヤー周辺のチェストをスキャン
        for (int dx = -15; dx <= 15; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -15; dz <= 15; dz++) {
                    BlockPos checkPos = origin.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).is(Blocks.CHEST)) {
                        chestPositions.add(checkPos);
                    }
                }
            }
        }

        if (chestPositions.isEmpty()) {
            owner.isActionActive = false;
            owner.lastWarpTime = level.getGameTime();
            return;
        }

        // ランダムなチェストを選択
        BlockPos targetChestPos = chestPositions.get(level.random.nextInt(chestPositions.size()));
        BlockEntity blockEntity = level.getBlockEntity(targetChestPos);

        if (blockEntity instanceof Container container) {
            int actionType = level.random.nextInt(4);

            switch (actionType) {
                case 0:
                    // ① 怪奇現象：チェストが勝手に開き、少し後に閉まる
                    level.blockEvent(targetChestPos, Blocks.CHEST, 1, 1); // 1,1 is open for ChestBlock
                    this.openedChestPos = targetChestPos;
                    this.chestOpenTicks = 40 + level.random.nextInt(40); // 2~4秒間開いたまま
                    owner.isActionActive = true;
                    // isActionActiveを維持してtickで閉じる処理を行う
                    return;

                case 1:
                    // ② メッセージ/すり替え
                    boolean foundEmpty = false;
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (container.getItem(i).isEmpty()) {
                            ItemStack paper = new ItemStack(Items.PAPER);
                            String message = com.example.util.HorrorMessages.getRandomMessage(level.random);
                            paper.setHoverName(Component.literal("§c" + message));
                            container.setItem(i, paper);
                            foundEmpty = true;
                            break;
                        }
                    }
                    if (!foundEmpty) {
                        // 空きがなければランダムなスロットを腐った肉にする
                        int randomSlot = level.random.nextInt(container.getContainerSize());
                        container.setItem(randomSlot, new ItemStack(Items.ROTTEN_FLESH, 1));
                    }
                    break;

                case 2:
                    // ③ ぐちゃぐちゃシャッフル
                    List<ItemStack> items = new ArrayList<>();
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (!container.getItem(i).isEmpty()) {
                            items.add(container.getItem(i).copy());
                            container.setItem(i, ItemStack.EMPTY);
                        }
                    }
                    Collections.shuffle(items);
                    List<Integer> slots = new ArrayList<>();
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        slots.add(i);
                    }
                    Collections.shuffle(slots);
                    
                    for (int i = 0; i < items.size(); i++) {
                        container.setItem(slots.get(i), items.get(i));
                    }
                    break;

                case 3:
                    // ④ 盗難（ランダムなアイテム1スタックを消去）
                    List<Integer> filledSlots = new ArrayList<>();
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (!container.getItem(i).isEmpty()) {
                            filledSlots.add(i);
                        }
                    }
                    if (!filledSlots.isEmpty()) {
                        int slotToSteal = filledSlots.get(level.random.nextInt(filledSlots.size()));
                        container.setItem(slotToSteal, ItemStack.EMPTY);
                    }
                    break;
            }
        }

        // アニメーション以外の行動は即時終了
        owner.isActionActive = false;
        owner.lastWarpTime = level.getGameTime();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return this.chestOpenTicks > 0;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (this.chestOpenTicks > 0) {
            this.chestOpenTicks--;
            if (this.chestOpenTicks <= 0 && this.openedChestPos != null) {
                // チェストを閉じる
                level.blockEvent(this.openedChestPos, Blocks.CHEST, 1, 0);
                this.openedChestPos = null;
                owner.isActionActive = false;
                owner.lastWarpTime = level.getGameTime();
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        // 万が一、途中で強制終了された場合でも必ずフラグを下ろす
        if (owner.isActionActive) {
            owner.lastWarpTime = gameTime;
        }
        owner.isActionActive = false;
        
        // 念のため開けっ放しのチェストがあれば閉じる
        if (this.openedChestPos != null) {
            level.blockEvent(this.openedChestPos, Blocks.CHEST, 1, 0);
            this.openedChestPos = null;
        }
        this.chestOpenTicks = 0;
    }
}
