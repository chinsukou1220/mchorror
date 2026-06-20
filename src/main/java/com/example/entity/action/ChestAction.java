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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChestAction extends Behavior<HorrorSteveEntity> {

    private int chestOpenTicks = 0;
    private List<BlockPos> openedChestPosList = new ArrayList<>();

    public ChestAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 100, 100);
    }

    /**
     * プレイヤー周辺のチェストに怪奇現象を起こす（HuntAction等から直接呼び出し可能）。
     * チェスト開閉アニメーションは省略し、即時系アクション（メッセージ挿入/シャッフル/盗難）のみ実行。
     */
    public static void doCreepyChest(ServerLevel level, Player target) {
        BlockPos origin = target.blockPosition();
        List<BlockPos> chestPositions = new ArrayList<>();

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

        if (chestPositions.isEmpty()) return;

        BlockPos targetChestPos = chestPositions.get(level.random.nextInt(chestPositions.size()));
        BlockEntity blockEntity = level.getBlockEntity(targetChestPos);

        if (blockEntity instanceof Container container) {
            int actionType = level.random.nextInt(3); // 0〜2（開閉アニメは除外）

            switch (actionType) {
                case 0:
                    // メッセージ/すり替え
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
                        int randomSlot = level.random.nextInt(container.getContainerSize());
                        container.setItem(randomSlot, new ItemStack(Items.ROTTEN_FLESH, 1));
                    }
                    break;

                case 1:
                    // シャッフル
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

                case 2:
                    // 盗難
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
                    net.minecraft.world.level.block.state.BlockState st = level.getBlockState(checkPos);
                    if (st.is(Blocks.CHEST) || st.is(Blocks.BARREL) || st.is(Blocks.TRAPPED_CHEST)) {
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

        int rnLevel = com.example.world.RedNightState.get(level).weaknessLevel;
        int numTargets = Math.min(chestPositions.size(), Math.max(1, 1 + rnLevel / 4));
        Collections.shuffle(chestPositions);

        boolean openedAny = false;

        for (int t = 0; t < numTargets; t++) {
            BlockPos targetChestPos = chestPositions.get(t);
            BlockEntity blockEntity = level.getBlockEntity(targetChestPos);

            if (blockEntity instanceof Container container) {
                int actionType = level.random.nextInt(4);

                switch (actionType) {
                    case 0:
                        // ① 怪奇現象：チェストが勝手に開く
                        level.blockEvent(targetChestPos, Blocks.CHEST, 1, 1);
                        this.openedChestPosList.add(targetChestPos);
                        openedAny = true;
                        break;

                    case 1:
                        // ② メッセージ/すり替え（レベルに応じて枚数増加）
                        int messageCount = Math.max(1, 1 + rnLevel / 3);
                        for (int k = 0; k < messageCount; k++) {
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
                        }
                        break;

                    case 2:
                        // ③ ぐちゃぐちゃシャッフル（チェスト全体なのでそのまま）
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
                        // ④ 盗難（レベルに応じて盗むスタック数が増加）
                        int stealCount = Math.max(1, 1 + rnLevel / 5);
                        for (int k = 0; k < stealCount; k++) {
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
                        }
                        break;
                }
            }
        }

        if (openedAny) {
            this.chestOpenTicks = 40 + level.random.nextInt(40);
            owner.isActionActive = true;
            return;
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
            if (this.chestOpenTicks <= 0 && !this.openedChestPosList.isEmpty()) {
                // チェストを閉じる
                for (BlockPos pos : this.openedChestPosList) {
                    level.blockEvent(pos, Blocks.CHEST, 1, 0);
                }
                this.openedChestPosList.clear();
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
        if (!this.openedChestPosList.isEmpty()) {
            for (BlockPos pos : this.openedChestPosList) {
                level.blockEvent(pos, Blocks.CHEST, 1, 0);
            }
            this.openedChestPosList.clear();
        }
        this.chestOpenTicks = 0;
    }
}
