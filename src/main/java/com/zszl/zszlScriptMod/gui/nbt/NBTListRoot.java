package com.zszl.zszlScriptMod.gui.nbt;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.renderer.RenderItem;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.ItemComponentCompat;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class NBTListRoot extends NBTListCompound {

    private NBTListCompound tagElement;
    private NBTListElement focus;
    private NBTListElement selected;
    private int selX;
    private int selY;
    private int selWidth;
    private int selHeight;
    private NBTOption[] options;
    private final GuiScreen hostScreen;

    public NBTListRoot(ItemStack stack, GuiScreen parentGui) {
        super(stack.getHoverName().getString(), null, stack, 30, 50);
        this.hostScreen = parentGui;
        refresh();
    }

    public NBTListElement getSelected() {
        return this.selected;
    }

    public void setSelected(NBTListElement element, int mouseX, int mouseY) {
        this.selected = element;
        this.options = element == null ? null : element.getOptions();
        this.selWidth = 0;
        if (this.options != null) {
            FontRenderer fontRenderer = new FontRenderer(Minecraft.getInstance().font);
            for (NBTOption option : this.options) {
                this.selWidth = Math.max(fontRenderer.getStringWidth(option.getText()), this.selWidth);
            }
        }
        this.selWidth += 10;
        this.selHeight = this.options == null ? 15 : 12 * this.options.length + 15;

        int maxX = Math.max(6, this.hostScreen.width - (this.selWidth + 6));
        int maxY = Math.max(6, this.hostScreen.height - this.selHeight);
        this.selX = Math.max(6, Math.min(mouseX, maxX));
        this.selY = Math.max(6, Math.min(mouseY, maxY));
    }

    public void clearSelected() {
        this.selected = null;
        this.selX = 0;
        this.selY = 0;
        this.selWidth = 0;
        this.selHeight = 0;
        this.options = null;
    }

    public void setFocus(NBTListElement element) {
        if (element != this) {
            this.focus = element;
        }
    }

    public void refresh() {
        clearSelected();
        if (ItemComponentCompat.hasCustomDataTag(this.icon)) {
            boolean keepClosed = this.tagElement != null && this.tagElement.closed;
            this.tagElement = new NBTListCompound("tag", ItemComponentCompat.getCustomDataTag(this.icon), keepClosed,
                    getX() + 15, getY() + 20);
            this.tagElement.parent = this;
            this.tagElement.redoPositions();
        } else {
            this.tagElement = null;
        }
    }

    @Override
    public void redoPositions() {
        if (this.tagElement != null) {
            this.tagElement.redoPositions();
        }
    }

    @Override
    public void drawIcon(RenderItem itemRender) {
        super.drawIcon(itemRender);
        if (this.tagElement != null) {
            this.tagElement.drawIcon(itemRender);
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        FontRenderer fontRenderer = new FontRenderer(mc.font);
        fontRenderer.drawString(getText(), getX() + 15, getY() - 5, 0xFFFFFFFF);
        if (this.tagElement != null) {
            drawVerticalStructureLine(getX(), getY(), 20);
            drawHorizontalStructureLine(getX() + 2, getY() + 20, 12);
            this.tagElement.draw(mc, mouseX, mouseY);
        }

        if (this.selected != null && this.options != null) {
            drawRect(this.selX, this.selY, this.selX + this.selWidth + 6, this.selY + 13, 0xCC007788);
            String selectedType = this.selected.getTypeName();
            fontRenderer.drawString(selectedType, this.selX + 3, this.selY + 3,
                    GuiTheme.resolveTextColor(selectedType, 0xFFCCCCCC));
            for (int i = 0; i < this.options.length; i++) {
                int optionTop = this.selY + 13 * (i + 1);
                boolean over = mouseX >= this.selX && mouseX <= this.selX + this.selWidth + 6
                        && mouseY >= optionTop && mouseY <= optionTop + 13;
                drawRect(this.selX, optionTop, this.selX + this.selWidth + 6, optionTop + 13, 0xCC333333);
                String optionText = this.options[i].getText();
                fontRenderer.drawString(optionText, this.selX + 3, this.selY + 3 + (i + 1) * 13,
                        GuiTheme.resolveTextColor(optionText, over ? 0xFFE67E22 : 0xFFCCCCCC));
            }
        }
    }

    public boolean mouseOverSelected(int mouseX, int mouseY) {
        return this.selected != null
                && mouseX >= this.selX
                && mouseX <= this.selX + this.selWidth + 6
                && mouseY >= this.selY
                && mouseY <= this.selY + this.selHeight;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.selected != null) {
            if (mouseOverSelected(mouseX, mouseY)) {
                if (mouseButton == 0) {
                    int y = mouseY - (this.selY + 13);
                    if (y >= 0) {
                        int optionIndex = y / 13;
                        if (optionIndex >= 0 && optionIndex < this.options.length) {
                            this.options[optionIndex].action(this.hostScreen);
                        }
                    }
                }
            } else {
                clearSelected();
            }
            return;
        }

        if (this.tagElement != null) {
            this.tagElement.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public NBTOption[] getOptions() {
        return new NBTOption[] {
                ItemComponentCompat.hasCustomDataTag(this.icon) ? new NBTOption() {
                    @Override
                    public String getText() {
                        return I18n.format("gui.nbt.clear_all_tags");
                    }

                    @Override
                    public void action(GuiScreen currentScreen) {
                        ItemComponentCompat.setCustomDataTag(icon, null);
                        refresh();
                    }
                } : new NBTOption() {
                    @Override
                    public String getText() {
                        return I18n.format("gui.nbt.create_root_tag");
                    }

                    @Override
                    public void action(GuiScreen currentScreen) {
                        ItemComponentCompat.setCustomDataTag(icon, new CompoundTag());
                        refresh();
                    }
                }
        };
    }
}
