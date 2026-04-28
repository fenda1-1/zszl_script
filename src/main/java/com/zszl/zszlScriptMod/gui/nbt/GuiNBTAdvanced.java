package com.zszl.zszlScriptMod.gui.nbt;

import com.mojang.brigadier.StringReader;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Iterator;

public class GuiNBTAdvanced extends ThemedGuiScreen {

    private final ItemStack stack;
    private final GuiScreen lastScreen;
    private NBTListRoot rootElement;

    public GuiNBTAdvanced(GuiScreen lastScreen, ItemStack stack) {
        this.lastScreen = lastScreen;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.rootElement = new NBTListRoot(this.stack, this);
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 35, 200, 20,
                I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button != null && button.enabled && button.id == 200) {
            this.mc.setScreen(this.lastScreen);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E) {
            this.mc.setScreen(this.lastScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.rootElement != null) {
            this.rootElement.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        drawRect(20, 40, this.width - 20, this.height - 40, 0xDD222222);
        drawRect(18, 38, this.width - 18, 40, 0xFF007788);
        drawRect(18, 40, 20, this.height - 40, 0xFF007788);
        drawRect(this.width - 20, 40, this.width - 18, this.height - 40, 0xFF007788);
        drawRect(18, this.height - 40, this.width - 18, this.height - 38, 0xFF007788);

        if (this.rootElement != null) {
            this.rootElement.drawIcon(null);
            this.rootElement.draw(this.mc, mouseX, mouseY);
        }

        this.drawCenteredString(this.fontRenderer, I18n.format("gui.nbt_advanced.title"), this.width / 2, 20,
                0xFFFFFFFF);
        this.drawCenteredString(this.fontRenderer, "左键展开复合标签，右键打开操作菜单。", this.width / 2, this.height - 52,
                0xFFCCCCCC);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static String tagToString(Tag tag) {
        return tag == null ? "" : nbtBaseToString(tag);
    }

    public static Tag parseTagValue(String text) throws Exception {
        String safe = text == null ? "" : text.trim();
        return TagParser.create(net.minecraft.nbt.NbtOps.INSTANCE).parseFully(new StringReader(safe));
    }

    public static Tag coerceTagToOriginalType(String text, Tag originalTag) {
        if (originalTag == null) {
            return StringTag.valueOf(text == null ? "" : text);
        }

        String safe = text == null ? "" : text.trim();
        try {
            Tag parsed = parseTagValue(safe);
            if (parsed.getId() == originalTag.getId()) {
                return parsed;
            }
        } catch (Exception ignored) {
        }

        try {
            switch (originalTag.getId()) {
            case Tag.TAG_BYTE:
                if ("true".equalsIgnoreCase(safe)) {
                    return ByteTag.valueOf((byte) 1);
                }
                if ("false".equalsIgnoreCase(safe)) {
                    return ByteTag.valueOf((byte) 0);
                }
                return ByteTag.valueOf(Byte.parseByte(stripNumericSuffix(safe)));
            case Tag.TAG_SHORT:
                return ShortTag.valueOf(Short.parseShort(stripNumericSuffix(safe)));
            case Tag.TAG_INT:
                return IntTag.valueOf(Integer.parseInt(stripNumericSuffix(safe)));
            case Tag.TAG_LONG:
                return LongTag.valueOf(Long.parseLong(stripNumericSuffix(safe)));
            case Tag.TAG_FLOAT:
                return FloatTag.valueOf(Float.parseFloat(stripNumericSuffix(safe)));
            case Tag.TAG_DOUBLE:
                return DoubleTag.valueOf(Double.parseDouble(stripNumericSuffix(safe)));
            case Tag.TAG_STRING:
                return StringTag.valueOf(stripOptionalQuotes(safe));
            default:
                break;
            }
        } catch (Exception ignored) {
        }
        return StringTag.valueOf(stripOptionalQuotes(safe));
    }

    private static String nbtBaseToString(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            StringBuilder sb = new StringBuilder("{");
            Iterator<String> iterator = compound.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                sb.append(key).append(":").append(nbtBaseToString(compound.get(key)));
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(nbtBaseToString(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return tag.toString();
    }

    private static String stripOptionalQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value == null ? "" : value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String stripNumericSuffix(String value) {
        if (value == null) {
            return "";
        }
        String safe = value.trim();
        if (safe.isEmpty()) {
            return safe;
        }
        char last = safe.charAt(safe.length() - 1);
        if (last == 'b' || last == 'B' || last == 's' || last == 'S'
                || last == 'l' || last == 'L' || last == 'f' || last == 'F'
                || last == 'd' || last == 'D') {
            return safe.substring(0, safe.length() - 1);
        }
        return safe;
    }
}
