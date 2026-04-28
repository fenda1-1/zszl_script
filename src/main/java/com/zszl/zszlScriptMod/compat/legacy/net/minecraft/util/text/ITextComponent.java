package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface ITextComponent extends Component {

    default String getFormattedText() {
        return getString();
    }

    default String getUnformattedText() {
        return getString();
    }

    default String getUnformattedComponentText() {
        return getString();
    }

    final class Serializer {
        private Serializer() {
        }

        public static MutableComponent jsonToComponent(String json) {
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                    .result()
                    .map(Component::copy)
                    .orElseGet(() -> Component.literal(json == null ? "" : json));
        }
    }
}

