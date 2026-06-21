package com.mittel.ssttaallkkeerr.entity.action;

import com.mittel.ssttaallkkeerr.entity.HorrorSteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class InFogAction extends Behavior<HorrorSteveEntity> {
    private int waitTicks = 0;
    private int currentTicks = 0;

    public InFogAction() {
        super(java.util.Map.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, HorrorSteveEntity owner) {
        return owner.isActionActive;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, HorrorSteveEntity owner, long gameTime) {
        return owner.isActionActive;
    }

    @Override
    protected void start(ServerLevel level, HorrorSteveEntity entity, long gameTime) {
        Optional<List<Player>> players = entity.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (players.isPresent() && !players.get().isEmpty()) {
            Player target = players.get().get(0);
            Vec3 look = target.getLookAngle();
            double distance = 5.0 + level.random.nextDouble() * 1.0;
            double targetX = target.getX() + look.x * distance;
            double targetZ = target.getZ() + look.z * distance;
            
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) targetX, (int) targetZ);
            BlockPos spawnPos = new BlockPos((int)targetX, y, (int)targetZ);

            int rnLevel = com.mittel.ssttaallkkeerr.world.RedNightState.get(level).weaknessLevel;
            waitTicks = Math.max(1, 15 - rnLevel) * 20;
            currentTicks = 0;
            
            // tp
            entity.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        } else {
            entity.isActionActive = false;
        }
    }

    @Override
    protected void tick(ServerLevel level, HorrorSteveEntity entity, long gameTime) {
        super.tick(level, entity, gameTime);
        
        Optional<List<Player>> players = entity.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (players.isPresent() && !players.get().isEmpty()) {
            Player target = players.get().get(0);
            entity.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
        }

        currentTicks++;
        if (currentTicks >= waitTicks) {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, false));
            double ang = level.random.nextDouble() * Math.PI * 2;
            double dist = 80.0 + level.random.nextDouble() * 40.0;
            double fx = entity.getX() + Math.cos(ang) * dist;
            double fz = entity.getZ() + Math.sin(ang) * dist;
            double fy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)fx, (int)fz);
            entity.teleportTo(fx, fy, fz);
            entity.isActionActive = false;
        }
    }
}
