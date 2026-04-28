package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.event.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.event.MutableEvent;

import java.util.ArrayList;
import java.util.List;

public class GetCollisionBoxesEvent extends MutableEvent {

    private final Level world;
    private final Entity entity;
    private final AABB aabb;
    private final List<AABB> collisionBoxesList = new ArrayList<>();
    private final BlockPos pos;

    public GetCollisionBoxesEvent(Level world, Entity entity, AABB aabb, BlockPos pos) {
        this.world = world;
        this.entity = entity;
        this.aabb = aabb;
        this.pos = pos;
    }

    public Level getWorld() {
        return world;
    }

    public Entity getEntity() {
        return entity;
    }

    public AABB getAabb() {
        return aabb;
    }

    public List<AABB> getCollisionBoxesList() {
        return collisionBoxesList;
    }

    public BlockPos getPos() {
        return pos;
    }
}


