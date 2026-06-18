package com.example.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;

public class FinalActionSteveEntity extends PathfinderMob {

    public static final EntityDataAccessor<Float> DATA_WIDTH_SCALE_ID = SynchedEntityData.defineId(FinalActionSteveEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_HEIGHT_SCALE_ID = SynchedEntityData.defineId(FinalActionSteveEntity.class, EntityDataSerializers.FLOAT);

    public FinalActionSteveEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide) {
            // 幅は 0.7 ～ 1.5
            float wScale = 0.7f + this.random.nextFloat() * 0.8f;
            // 高さは 1.0 ～ 1.5
            float hScale = 1.0f + this.random.nextFloat() * 0.5f;
            this.entityData.set(DATA_WIDTH_SCALE_ID, wScale);
            this.entityData.set(DATA_HEIGHT_SCALE_ID, hScale);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WIDTH_SCALE_ID, 1.0f);
        this.entityData.define(DATA_HEIGHT_SCALE_ID, 1.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("WidthScale", this.entityData.get(DATA_WIDTH_SCALE_ID));
        compound.putFloat("HeightScale", this.entityData.get(DATA_HEIGHT_SCALE_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("WidthScale")) {
            this.entityData.set(DATA_WIDTH_SCALE_ID, compound.getFloat("WidthScale"));
        }
        if (compound.contains("HeightScale")) {
            this.entityData.set(DATA_HEIGHT_SCALE_ID, compound.getFloat("HeightScale"));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // 動かない
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // AI目標なし（何もしない）
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        // 自発的な移動処理（歩行・遊泳など）を完全に無効化
    }

    @Override
    public boolean isPushable() {
        return false; // 他のエンティティに押されない
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // 他のエンティティを押さない
    }

    @Override
    public void push(double x, double y, double z) {
        // ノックバックなどの押し出しを無効化
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
