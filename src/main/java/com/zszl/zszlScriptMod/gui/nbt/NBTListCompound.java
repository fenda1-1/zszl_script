package com.zszl.zszlScriptMod.gui.nbt;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.renderer.RenderItem;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NBTListCompound extends NBTListElement {

    protected List<NBTListElement> children;
    protected boolean closed;

    public static final ItemStack OPEN_ICON = new ItemStack(Blocks.CHEST);
    public static final ItemStack CLOSED_ICON = new ItemStack(Blocks.ENDER_CHEST);

    public NBTListCompound(String key, CompoundTag tag, boolean closed, int x, int y) {
        this(key, tag, closed ? CLOSED_ICON : OPEN_ICON, x, y);
        this.closed = closed;
    }

    public NBTListCompound(String key, CompoundTag tag, ItemStack iconStack, int x, int y) {
        super(key, tag, iconStack, x, y);
        this.closed = true;
        rebuildChildren();
    }

    public void rebuildChildren() {
        this.children = new ArrayList<>();
        CompoundTag compound = getTagCompound();
        if (compound == null) {
            return;
        }

        int length = 20;
        for (String childKey : compound.keySet()) {
            Tag child = compound.get(childKey);
            NBTListElement childElement;
            if (child instanceof CompoundTag childCompound) {
                childElement = new NBTListCompound(childKey, childCompound, true, this.x + 15, this.y + length);
            } else {
                childElement = new NBTListElement(childKey, child, new ItemStack(Items.PAPER), this.x + 15,
                        this.y + length);
            }
            length += addChild(childElement);
        }
    }

    public int addChild(NBTListElement child) {
        child.parent = this;
        this.children.add(child);
        return child instanceof NBTListCompound ? ((NBTListCompound) child).getLength() + 20 : 20;
    }

    public CompoundTag getTagCompound() {
        return this.tag instanceof CompoundTag ? (CompoundTag) this.tag : new CompoundTag();
    }

    @Override
    public String getText() {
        return this.children != null ? getKey() + " (" + this.children.size() + ")" : getKey();
    }

    @Override
    public String getTypeName() {
        return "Compound Tag";
    }

    @Override
    public void drawIcon(RenderItem itemRender) {
        super.drawIcon(itemRender);
        if (this.closed || this.children == null) {
            return;
        }
        for (NBTListElement element : this.children) {
            element.drawIcon(itemRender);
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        super.draw(mc, mouseX, mouseY);
        if (this.closed || this.children == null) {
            return;
        }
        for (NBTListElement element : this.children) {
            drawHorizontalStructureLine(element.getX() - 13, element.getY(), 11);
            element.draw(mc, mouseX, mouseY);
        }
        int length = getLength();
        if (length > 0) {
            drawVerticalStructureLine(getX(), getY(), length);
        }
    }

    public int getLength() {
        if (this.closed || this.children == null || getTagCompound() == null) {
            return 0;
        }

        int length = this.children.size() * 20;
        for (NBTListElement child : this.children) {
            if (child instanceof NBTListCompound compound) {
                length += compound.getLength();
            }
        }
        return length;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this instanceof NBTListRoot) {
            return;
        }
        if (isMouseOver(mouseX, mouseY) && mouseButton == 0) {
            this.closed = !this.closed;
            this.icon = this.closed ? CLOSED_ICON : OPEN_ICON;
            NBTListRoot root = getRootAsRoot();
            if (root != null) {
                root.redoPositions();
            }
            return;
        }
        if (!this.closed && this.children != null) {
            for (NBTListElement element : this.children) {
                element.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void redoPositions() {
        if (this.children == null) {
            return;
        }
        int length = 20;
        for (NBTListElement element : this.children) {
            element.setY(this.getY() + length);
            length += 20;
            if (element instanceof NBTListCompound compound) {
                length += compound.getLength();
                compound.redoPositions();
            }
        }
    }

    @Override
    public NBTOption[] getOptions() {
        List<NBTOption> options = new ArrayList<>(Arrays.asList(super.getOptions()));

        options.add(0, new NBTOption() {
            @Override
            public String getText() {
                return I18n.format("gui.nbt.add_tag");
            }

            @Override
            public void action(GuiScreen currentScreen) {
                Minecraft.getInstance().setScreen(new GuiTextInput(currentScreen,
                        I18n.format("gui.nbt.input_new_key"),
                        (newKey) -> {
                            String safeKey = newKey == null ? "" : newKey.trim();
                            if (!safeKey.isEmpty() && !getTagCompound().contains(safeKey)) {
                                getTagCompound().put(safeKey,
                                        StringTag.valueOf(I18n.format("gui.nbt.default_new_value")));
                            }
                            Minecraft.getInstance().setScreen(currentScreen);
                            NBTListRoot root = getRootAsRoot();
                            if (root != null) {
                                root.refresh();
                            }
                        }));
            }
        });

        return options.toArray(new NBTOption[0]);
    }
}
