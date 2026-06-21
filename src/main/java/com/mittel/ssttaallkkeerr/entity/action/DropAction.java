package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DropAction extends Behavior<HorrorSteveEntity> {

    private Player targetPlayer = null;
    private int phase = 0;
    private int tickCounter = 0;

    public DropAction() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT), 60, 60);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        Optional<List<Player>> optionalPlayers = owner.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (optionalPlayers.isPresent() && !optionalPlayers.get().isEmpty()) {
            this.targetPlayer = optionalPlayers.get().get(0);
            owner.isActionActive = true;
            this.phase = 1;
            this.tickCounter = 0;
            
            // スティーブ自身は透明のまま遠くに待機させておく
            owner.setInvisible(true);
        } else {
            owner.isActionActive = false;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive && this.targetPlayer != null && this.phase == 1;
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        if (!owner.isActionActive || this.targetPlayer == null) return;

        this.tickCounter++;

        // 少しだけ時間差をつけてアイテムをドロップ
        if (this.tickCounter == 10) {
            dropCreepyItems(level, this.targetPlayer);
            this.phase = 2; // 終了
            owner.isActionActive = false;
            owner.isWaitingForWarp = true; // 次のアクションへ移行可能な状態に
        }
    }

    /**
     * プレイヤーの背後に不気味なアイテムを落とす（HuntAction等から直接呼び出し可能）。
     */
    public static void dropCreepyItems(ServerLevel level, Player target) {
        Vec3 playerPos = target.position();
        Vec3 look = target.getLookAngle();
        
        Vec3 dropPos = playerPos.subtract(look.x * 2.5, 0, look.z * 2.5);
        
        ItemStack itemToDrop = null;
        int r = level.random.nextInt(4);
        
        if (r == 0) {
            itemToDrop = new ItemStack(Items.PAPER);
            String chosenMessage = com.mittel.ssttaallkkeerr.util.HorrorMessages.getRandomMessage(level.random);
            itemToDrop.setHoverName(Component.literal(chosenMessage).withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.ITALIC));
        } else if (r == 1) {
            itemToDrop = new ItemStack(Items.RED_DYE, 3 + level.random.nextInt(4));
        } else if (r == 2) {
            itemToDrop = level.random.nextBoolean() ? new ItemStack(Items.BONE, 2) : new ItemStack(Items.ROTTEN_FLESH, 5);
        } else {
            int toolType = level.random.nextInt(3);
            if (toolType == 0) itemToDrop = new ItemStack(Items.WOODEN_SWORD);
            else if (toolType == 1) itemToDrop = new ItemStack(Items.WOODEN_PICKAXE);
            else itemToDrop = new ItemStack(Items.WOODEN_AXE);
            
            int maxDamage = itemToDrop.getMaxDamage();
            itemToDrop.setDamageValue(maxDamage - 1 - level.random.nextInt(2));
        }

        spawnItemEntity(level, dropPos, itemToDrop);
    }

    public static void spawnItemEntity(ServerLevel level, Vec3 pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, stack);
        itemEntity.setDefaultPickUpDelay(); // スポーン直後は拾えないようにする
        level.addFreshEntity(itemEntity);
    }

    @Override
    protected void stop(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        super.stop(level, owner, gameTime);
        owner.isActionActive = false;
    }
}
