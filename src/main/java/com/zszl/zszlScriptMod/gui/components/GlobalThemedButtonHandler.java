package com.zszl.zszlScriptMod.gui.components;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher.Side;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class GlobalThemedButtonHandler {

    private static final int PASSWORD_BTN_MIN = 42001;
    private static final int PASSWORD_BTN_MAX = 42003;
    private static Field guiButtonListField;

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        applyTheme(event.getGui(), event.getButtonList());
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        applyTheme(event.getGui(), getButtonList(event.getGui()));
    }

    private void applyTheme(GuiScreen gui, List<GuiButton> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            return;
        }

        Map<GuiButton, GuiButton> replaced = new IdentityHashMap<>();

        ListIterator<GuiButton> it = buttons.listIterator();
        while (it.hasNext()) {
            GuiButton old = it.next();
            if (!shouldTheme(gui, old)) {
                continue;
            }

            ThemedButton themed = new ThemedButton(old.id, old.x, old.y, old.width, old.height, old.displayString);
            themed.enabled = old.enabled;
            themed.visible = old.visible;
            themed.packedFGColour = old.packedFGColour;
            it.set(themed);
            replaced.put(old, themed);
        }

        if (!replaced.isEmpty()) {
            syncButtonFields(gui, replaced);
        }
    }

    private void syncButtonFields(GuiScreen gui, Map<GuiButton, GuiButton> replaced) {
        if (gui == null || replaced == null || replaced.isEmpty()) {
            return;
        }
        try {
            for (Class<?> c = gui.getClass(); c != null; c = c.getSuperclass()) {
                Field[] fields = c.getDeclaredFields();
                for (Field f : fields) {
                    if (!GuiButton.class.isAssignableFrom(f.getType())) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object value = f.get(gui);
                    if (!(value instanceof GuiButton)) {
                        continue;
                    }
                    GuiButton mapped = replaced.get(value);
                    if (mapped != null) {
                        try {
                            f.set(gui, mapped);
                        } catch (Throwable ignored) {
                            // final 字段或安全限制时忽略
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // 反射同步失败不影响主流程
        }
    }

    private boolean shouldTheme(GuiScreen gui, GuiButton button) {
        if (button == null) {
            return false;
        }
        if (button instanceof ThemedButton) {
            return false;
        }
        // 保留已有自定义按钮子类行为（如下拉、切换按钮等）
        if (button.getClass() != GuiButton.class) {
            return false;
        }

        if (gui != null) {
            String className = gui.getClass().getName();
            if (className.startsWith("com.zszl.zszlScriptMod.")) {
                return true;
            }
        }

        int id = button.id;
        return id >= PASSWORD_BTN_MIN && id <= PASSWORD_BTN_MAX;
    }

    @SuppressWarnings("unchecked")
    private List<GuiButton> getButtonList(GuiScreen gui) {
        if (gui == null) {
            return null;
        }
        try {
            if (guiButtonListField == null) {
                guiButtonListField = GuiScreen.class.getDeclaredField("buttonList");
                guiButtonListField.setAccessible(true);
            }
            return (List<GuiButton>) guiButtonListField.get(gui);
        } catch (Throwable ignored) {
            return null;
        }
    }
}




