package com.zszl.zszlScriptMod.shadowbaritone.utils.schematic.format.defaults;

import com.zszl.zszlScriptMod.shadowbaritone.api.schematic.IStaticSchematic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SpongeSchematic implements IStaticSchematic {
    public SpongeSchematic(CompoundTag tag) {
    }

    @Override
    public BlockState getDirect(int x, int y, int z) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public int widthX() { return 0; }
    @Override
    public int heightY() { return 0; }
    @Override
    public int lengthZ() { return 0; }
}

