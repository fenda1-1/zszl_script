package com.zszl.zszlScriptMod.shadowbaritone.utils.schematic.schematica;

import com.zszl.zszlScriptMod.shadowbaritone.api.schematic.IStaticSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;

import java.util.Optional;

public final class SchematicaHelper {
    private SchematicaHelper() {
    }

    public static boolean isSchematicaPresent() {
        return false;
    }

    public static Optional<Tuple<IStaticSchematic, BlockPos>> getOpenSchematic() {
        return Optional.empty();
    }
}

