package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextComponentString implements ITextComponent {

    private final MutableComponent delegate;

    public TextComponentString(String text) {
        this(Component.literal(text == null ? "" : text));
    }

    private TextComponentString(MutableComponent delegate) {
        this.delegate = delegate;
    }

    public String getText() { return this.delegate.getString(); }
    public String getFormattedText() { return this.delegate.getString(); }
    public String getUnformattedText() { return this.delegate.getString(); }
    public String getUnformattedComponentText() { return this.delegate.getString(); }

    public TextComponentString appendSibling(Component component) {
        this.delegate.append(component);
        return this;
    }

    public TextComponentString appendText(String text) {
        this.delegate.append(text == null ? "" : text);
        return this;
    }

    public TextComponentString setStyle(Style style) {
        this.delegate.setStyle(style == null ? Style.EMPTY : style);
        return this;
    }

    public TextComponentString createCopy() {
        return new TextComponentString(this.delegate.copy());
    }

    @Override
    public Style getStyle() { return this.delegate.getStyle(); }
    @Override
    public ComponentContents getContents() { return this.delegate.getContents(); }
    @Override
    public List<Component> getSiblings() { return this.delegate.getSiblings(); }
    @Override
    public FormattedCharSequence getVisualOrderText() { return this.delegate.getVisualOrderText(); }
}

