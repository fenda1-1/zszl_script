/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.shadowbaritone.utils.accessor.IFireworkRocketEntity;
import com.zszl.zszlScriptMod.shadowbaritone.launch.util.ReflectionHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import java.util.OptionalInt;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity extends Entity implements IFireworkRocketEntity {

    private MixinFireworkRocketEntity(Level level) {
        super(EntityType.FIREWORK_ROCKET, level);
    }

    @Override
    public LivingEntity getBoostedEntity() {
        LivingEntity attachedToEntity = ReflectionHelper.getField(this, "attachedToEntity", "f_37024_");
        boolean isAttachedToEntity =
                ReflectionHelper.invoke(this, new Class<?>[0], new Object[0], "isAttachedToEntity", "m_37088_");
        if (isAttachedToEntity && attachedToEntity == null) { // isAttachedToEntity checks if the optional is present
            EntityDataAccessor<OptionalInt> targetAccessor =
                    ReflectionHelper.getStaticField(FireworkRocketEntity.class, "DATA_ATTACHED_TO_TARGET", "f_37020_");
            final Entity entity = this.level().getEntity(this.entityData.get(targetAccessor).getAsInt());
            if (entity instanceof LivingEntity) {
                attachedToEntity = (LivingEntity) entity;
                ReflectionHelper.setField(this, attachedToEntity, "attachedToEntity", "f_37024_");
            }
        }
        return attachedToEntity;
    }
}

