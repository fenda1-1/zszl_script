package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text;

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
            return net.minecraft.network.chat.Component.Serializer.fromJson(json);
        }
    }
}

