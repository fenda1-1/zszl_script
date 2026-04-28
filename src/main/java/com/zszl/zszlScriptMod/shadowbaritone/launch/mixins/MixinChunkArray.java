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

import com.zszl.zszlScriptMod.shadowbaritone.launch.util.ReflectionHelper;
import com.zszl.zszlScriptMod.shadowbaritone.utils.accessor.IChunkArray;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public abstract class MixinChunkArray implements IChunkArray {
    private AtomicReferenceArray<LevelChunk> chunks() {
        return ReflectionHelper.getField(this, "chunks", "f_104466_");
    }

    private int chunkRadius() {
        return ReflectionHelper.getField(this, "chunkRadius", "f_104467_");
    }

    private int viewCenterX() {
        return ReflectionHelper.getField(this, "viewCenterX", "f_104469_");
    }

    private void setViewCenterX(int value) {
        ReflectionHelper.setField(this, value, "viewCenterX", "f_104469_");
    }

    private int viewCenterZ() {
        return ReflectionHelper.getField(this, "viewCenterZ", "f_104470_");
    }

    private void setViewCenterZ(int value) {
        ReflectionHelper.setField(this, value, "viewCenterZ", "f_104470_");
    }

    private boolean inRangeCompat(int x, int z) {
        return ReflectionHelper.invoke(this, new Class<?>[]{int.class, int.class}, new Object[]{x, z}, "inRange", "m_104500_");
    }

    private int getIndexCompat(int x, int z) {
        return ReflectionHelper.invoke(this, new Class<?>[]{int.class, int.class}, new Object[]{x, z}, "getIndex", "m_104481_");
    }

    private void replaceCompat(int index, LevelChunk chunk) {
        ReflectionHelper.invoke(this, new Class<?>[]{int.class, LevelChunk.class}, new Object[]{index, chunk}, "replace", "m_104484_");
    }

    @Override
    public int centerX() {
        return viewCenterX();
    }

    @Override
    public int centerZ() {
        return viewCenterZ();
    }

    @Override
    public int viewDistance() {
        return chunkRadius();
    }

    @Override
    public AtomicReferenceArray<LevelChunk> getChunks() {
        return chunks();
    }

    @Override
    public void copyFrom(IChunkArray other) {
        setViewCenterX(other.centerX());
        setViewCenterZ(other.centerZ());

        AtomicReferenceArray<LevelChunk> copyingFrom = other.getChunks();
        for (int k = 0; k < copyingFrom.length(); ++k) {
            LevelChunk chunk = copyingFrom.get(k);
            if (chunk != null) {
                ChunkPos chunkpos = chunk.getPos();
                if (inRangeCompat(chunkpos.x, chunkpos.z)) {
                    int index = getIndexCompat(chunkpos.x, chunkpos.z);
                    if (chunks().get(index) != null) {
                        throw new IllegalStateException("Doing this would mutate the client's REAL loaded chunks?!");
                    }
                    replaceCompat(index, chunk);
                }
            }
        }
    }
}

