package com.zszl.zszlScriptMod.gui.nbt;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.renderer.RenderItem;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NBTListElement extends Gui {

    protected String key;
    protected Tag tag;
    protected ItemStack icon;
    protected NBTListCompound parent;
    protected int x;
    protected int y;

    public NBTListElement(String key, Tag tag, ItemStack iconStack, int x, int y) {
        this.key = key == null ? "" : key;
        this.tag = tag;
        this.icon = iconStack == null ? ItemStack.EMPTY : iconStack;
        this.x = x;
        this.y = y;
    }

    public String getKey() {
        return this.key;
    }

    public Tag getTag() {
        return this.tag;
    }

    public ItemStack getIconStack() {
        return this.icon;
    }

    public String getText() {
        String valueStr = GuiNBTAdvanced.tagToString(this.tag);
        if (valueStr.length() > 40) {
            valueStr = valueStr.substring(0, 37) + "...";
        }
        return getKey() + " : " + valueStr;
    }

    public String getTypeName() {
        if (this.tag == null) {
            return "Unknown";
        }
        switch (this.tag.getId()) {
        case Tag.TAG_BYTE:
            return "Byte";
        case Tag.TAG_SHORT:
            return "Short";
        case Tag.TAG_INT:
            return "Int";
        case Tag.TAG_LONG:
            return "Long";
        case Tag.TAG_FLOAT:
            return "Float";
        case Tag.TAG_DOUBLE:
            return "Double";
        case Tag.TAG_BYTE_ARRAY:
            return "Byte Array";
        case Tag.TAG_STRING:
            return "String";
        case Tag.TAG_LIST:
            return "List";
        case Tag.TAG_COMPOUND:
            return "Compound";
        case Tag.TAG_INT_ARRAY:
            return "Int Array";
        case Tag.TAG_LONG_ARRAY:
            return "Long Array";
        default:
            return "Unknown";
        }
    }

    public void drawIcon(RenderItem itemRender) {
        if (this.icon.isEmpty()) {
            return;
        }
        GuiGraphics graphics = GuiCompatContext.current();
        Minecraft mc = Minecraft.getInstance();
        if (graphics == null || mc == null) {
            return;
        }
        graphics.renderItem(this.icon, this.x - 8, this.y - 9);
        graphics.renderItemDecorations(mc.font, this.icon, this.x - 8, this.y - 9);
    }

    public void draw(Minecraft mc, int mouseX, int mouseY) {
        FontRenderer fontRenderer = new FontRenderer(mc.font);
        boolean over = isMouseOver(mouseX, mouseY);
        String displayText = fontRenderer.trimStringToWidth(getText(), 300);
        fontRenderer.drawString(displayText, this.x + 15, this.y - 5, over ? 0xFFE67E22 : 0xFFFFFFFF);
    }

    protected static void drawVerticalStructureLine(int x, int y, int length) {
        drawRect(x - 1, y - 1, x + 1, y + length + 1, 0xFF2C3E50);
    }

    protected static void drawHorizontalStructureLine(int x, int y, int length) {
        drawRect(x - 1, y - 1, x + length + 1, y + 1, 0xFF34495E);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        NBTListRoot root = getRootAsRoot();
        if (root != null && root.getSelected() != null) {
            return false;
        }
        FontRenderer fontRenderer = new FontRenderer(Minecraft.getInstance().font);
        int left = this.x - 9;
        int textWidth = Math.min(300, fontRenderer.getStringWidth(getText()));
        int right = left + 25 + textWidth;
        int top = this.y - 8;
        int bottom = this.y + 7;
        return mouseX > left && mouseX < right && mouseY > top && mouseY < bottom;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isMouseOver(mouseX, mouseY)) {
            return;
        }

        NBTListRoot root = getRootAsRoot();
        if (root == null) {
            return;
        }
        if (mouseButton == 1) {
            root.setSelected(this, mouseX, mouseY);
        } else {
            root.setFocus(this);
        }
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public NBTListElement getRoot() {
        return this.parent != null ? this.parent.getRoot() : this;
    }

    public NBTListRoot getRootAsRoot() {
        NBTListElement root = getRoot();
        return root instanceof NBTListRoot ? (NBTListRoot) root : null;
    }

    public NBTOption[] getOptions() {
        List<NBTOption> options = new ArrayList<>();

        options.add(new NBTOption() {
            @Override
            public String getText() {
                return I18n.format("gui.nbt.change_value");
            }

            @Override
            public void action(GuiScreen currentScreen) {
                Minecraft.getInstance().setScreen(new GuiTextInput(currentScreen,
                        I18n.format("gui.nbt.edit_value", getKey()),
                        GuiNBTAdvanced.tagToString(tag),
                        (newValue) -> {
                            if (parent != null) {
                                CompoundTag compound = parent.getTagCompound();
                                compound.put(getKey(), GuiNBTAdvanced.coerceTagToOriginalType(newValue, tag));
                            }
                            Minecraft.getInstance().setScreen(currentScreen);
                            NBTListRoot root = getRootAsRoot();
                            if (root != null) {
                                root.refresh();
                            }
                        }));
            }
        });

        options.add(new NBTOption() {
            @Override
            public String getText() {
                return I18n.format("gui.common.delete");
            }

            @Override
            public void action(GuiScreen currentScreen) {
                if (parent != null) {
                    parent.getTagCompound().remove(getKey());
                }
                NBTListRoot root = getRootAsRoot();
                if (root != null) {
                    root.clearSelected();
                    root.refresh();
                }
            }
        });

        return options.toArray(new NBTOption[0]);
    }
}
