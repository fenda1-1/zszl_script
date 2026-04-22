package com.zszl.zszlScriptMod.utils.guiinspect;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GuiElementInspector {

    private GuiElementInspector() {
    }

    public enum ElementType {
        TITLE,
        BUTTON,
        SLOT,
        CUSTOM
    }

    public static final class GuiElementInfo {
        private final ElementType type;
        private final String path;
        private final String text;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int buttonId;
        private final int slotIndex;

        public GuiElementInfo(ElementType type, String path, String text, int x, int y, int width, int height,
                int buttonId, int slotIndex) {
            this.type = type;
            this.path = path == null ? "" : path;
            this.text = text == null ? "" : text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.buttonId = buttonId;
            this.slotIndex = slotIndex;
        }

        public ElementType getType() {
            return type;
        }

        public String getPath() {
            return path;
        }

        public String getText() {
            return text;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getButtonId() {
            return buttonId;
        }

        public int getSlotIndex() {
            return slotIndex;
        }
    }

    public static final class GuiSnapshot {
        private final String screenClassName;
        private final String screenSimpleName;
        private final String title;
        private final List<GuiElementInfo> elements;

        private GuiSnapshot(String screenClassName, String screenSimpleName, String title,
                List<GuiElementInfo> elements) {
            this.screenClassName = screenClassName == null ? "" : screenClassName;
            this.screenSimpleName = screenSimpleName == null ? "" : screenSimpleName;
            this.title = title == null ? "" : title;
            this.elements = elements == null ? Collections.emptyList() : elements;
        }

        public String getScreenClassName() {
            return screenClassName;
        }

        public String getScreenSimpleName() {
            return screenSimpleName;
        }

        public String getTitle() {
            return title;
        }

        public List<GuiElementInfo> getElements() {
            return elements;
        }
    }

    private static final class CustomElementCandidate {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int id;
        private final String text;
        private final String sourceField;
        private final String className;

        private CustomElementCandidate(int x, int y, int width, int height, int id, String text, String sourceField,
                String className) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.id = id;
            this.text = text == null ? "" : text;
            this.sourceField = sourceField == null ? "" : sourceField;
            this.className = className == null ? "" : className;
        }
    }

    public static GuiSnapshot captureCurrentSnapshot() {
        Minecraft mc = Minecraft.getInstance();
        Screen rawScreen = mc == null ? null : mc.screen;
        if (rawScreen == null) {
            return new GuiSnapshot("", "", "", Collections.emptyList());
        }

        String className = rawScreen.getClass().getName();
        String simpleName = rawScreen.getClass().getSimpleName();
        String title = getCurrentGuiTitle(mc);
        List<GuiElementInfo> elements = new ArrayList<>();

        elements.add(new GuiElementInfo(ElementType.TITLE,
                "screen/" + simpleName + "/title",
                title,
                0,
                0,
                0,
                0,
                Integer.MIN_VALUE,
                -1));

        collectButtonElements(rawScreen, simpleName, elements);
        collectContainerSlotElements(rawScreen, simpleName, elements);
        collectCustomElements(rawScreen, simpleName, elements);

        return new GuiSnapshot(className, simpleName, title, elements);
    }

    public static GuiElementInfo findFirstByPath(String pathQuery, String matchMode, ElementType... allowedTypes) {
        String normalizedQuery = normalize(pathQuery);
        if (normalizedQuery.isEmpty()) {
            return null;
        }
        GuiSnapshot snapshot = captureCurrentSnapshot();
        for (GuiElementInfo element : snapshot.getElements()) {
            if (element == null || !isAllowed(element.getType(), allowedTypes)) {
                continue;
            }
            if (matches(normalize(element.getPath()), normalizedQuery, matchMode)) {
                return element;
            }
        }
        return null;
    }

    public static String getCurrentGuiTitle(Minecraft mc) {
        if (mc == null || mc.screen == null || mc.screen.getTitle() == null) {
            return "";
        }
        return stripFormatting(mc.screen.getTitle().getString());
    }

    private static void collectButtonElements(Screen rawScreen, String simpleName, List<GuiElementInfo> elements) {
        if (rawScreen instanceof GuiScreen) {
            GuiScreen screen = (GuiScreen) rawScreen;
            List<GuiButton> buttons = screen.buttonList == null ? Collections.emptyList() : screen.buttonList;
            for (int i = 0; i < buttons.size(); i++) {
                GuiButton button = buttons.get(i);
                if (button == null) {
                    continue;
                }
                String path = "screen/" + simpleName + "/button[" + i + "]";
                elements.add(new GuiElementInfo(ElementType.BUTTON, path, stripFormatting(button.displayString),
                        button.x, button.y, button.width, button.height, button.id, -1));
            }
            return;
        }

        int index = 0;
        for (Object child : rawScreen.children()) {
            if (!(child instanceof AbstractWidget)) {
                continue;
            }
            AbstractWidget widget = (AbstractWidget) child;
            String path = "screen/" + simpleName + "/button[" + index + "]";
            elements.add(new GuiElementInfo(ElementType.BUTTON, path,
                    stripFormatting(widget.getMessage().getString()),
                    widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), index, -1));
            index++;
        }
    }

    private static void collectContainerSlotElements(Screen rawScreen, String simpleName, List<GuiElementInfo> elements) {
        if (!(rawScreen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) rawScreen;
        AbstractContainerMenu menu = screen.getMenu();
        int leftPos = readInt(screen, "leftPos", safeInvokeInt(screen, "getGuiLeft", 0));
        int topPos = readInt(screen, "topPos", safeInvokeInt(screen, "getGuiTop", 0));
        int containerSlots = resolvePrimaryContainerSize(menu);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot == null) {
                continue;
            }
            String pathPrefix = i < containerSlots ? "container_slot" : "player_slot";
            String path = "screen/" + simpleName + "/" + pathPrefix + "[" + i + "]";
            String text = slot.hasItem() ? stripFormatting(slot.getItem().getHoverName().getString()) : "";
            elements.add(new GuiElementInfo(ElementType.SLOT, path, text,
                    leftPos + slot.x,
                    topPos + slot.y,
                    16,
                    16,
                    Integer.MIN_VALUE,
                    i));
        }
    }

    private static void collectCustomElements(Screen rawScreen, String simpleName, List<GuiElementInfo> elements) {
        Map<Object, Boolean> visited = new IdentityHashMap<>();
        Class<?> current = rawScreen.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field == null || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(rawScreen);
                    collectCustomValue(simpleName, elements, visited, field.getName(), value);
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }
    }

    private static void collectCustomValue(String simpleName, List<GuiElementInfo> elements,
            Map<Object, Boolean> visited, String fieldName, Object value) {
        if (value == null || visited.put(value, Boolean.TRUE) != null) {
            return;
        }
        if (value instanceof Iterable<?>) {
            for (Object entry : (Iterable<?>) value) {
                collectCustomValue(simpleName, elements, visited, fieldName, entry);
            }
            return;
        }

        CustomElementCandidate candidate = toCustomCandidate(fieldName, value);
        if (candidate == null || candidate.width <= 0 || candidate.height <= 0) {
            return;
        }
        String path = "screen/" + simpleName + "/custom/" + candidate.sourceField;
        elements.add(new GuiElementInfo(ElementType.CUSTOM, path, candidate.text, candidate.x, candidate.y,
                candidate.width, candidate.height, candidate.id, -1));
    }

    private static CustomElementCandidate toCustomCandidate(String sourceField, Object value) {
        if (value instanceof GuiButton) {
            GuiButton button = (GuiButton) value;
            return new CustomElementCandidate(button.x, button.y, button.width, button.height, button.id,
                    stripFormatting(button.displayString), sourceField, value.getClass().getSimpleName());
        }

        int x = readInt(value, "x", Integer.MIN_VALUE);
        int y = readInt(value, "y", Integer.MIN_VALUE);
        int width = readInt(value, "width", safeInvokeInt(value, "getWidth", 0));
        int height = readInt(value, "height", safeInvokeInt(value, "getHeight", 0));
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || width <= 0 || height <= 0) {
            return null;
        }

        int id = readInt(value, "id", Integer.MIN_VALUE);
        String text = stripFormatting(readString(value, "displayString", "message", "text", "value"));
        return new CustomElementCandidate(x, y, width, height, id, text, sourceField,
                value.getClass().getSimpleName());
    }

    private static int resolvePrimaryContainerSize(AbstractContainerMenu menu) {
        if (menu == null || menu.slots == null) {
            return 0;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            int count = 0;
            for (Slot slot : menu.slots) {
                if (slot != null && slot.container != mc.player.getInventory()) {
                    count++;
                }
            }
            return count;
        }
        return menu.slots.size();
    }

    private static boolean isAllowed(ElementType type, ElementType... allowedTypes) {
        if (allowedTypes == null || allowedTypes.length == 0) {
            return true;
        }
        for (ElementType allowed : allowedTypes) {
            if (allowed == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String source, String query, String matchMode) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        if ("EXACT".equalsIgnoreCase(matchMode)) {
            return source.equals(query);
        }
        return source.contains(query);
    }

    private static String normalize(String value) {
        return stripFormatting(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String stripFormatting(String value) {
        String stripped = ChatFormatting.stripFormatting(value);
        return stripped == null ? (value == null ? "" : value) : stripped;
    }

    private static int safeInvokeInt(Object target, String methodName, int fallback) {
        if (target == null || methodName == null || methodName.isEmpty()) {
            return fallback;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Number ? ((Number) result).intValue() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int readInt(Object target, String fieldName, int fallback) {
        if (target == null || fieldName == null || fieldName.isEmpty()) {
            return fallback;
        }
        Class<?> current = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        Object instance = target instanceof Class<?> ? null : target;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(instance);
                return value instanceof Number ? ((Number) value).intValue() : fallback;
            } catch (Exception ignored) {
            }
            current = current.getSuperclass();
        }
        return fallback;
    }

    private static String readString(Object target, String... fieldOrMethodNames) {
        if (target == null || fieldOrMethodNames == null) {
            return "";
        }
        for (String name : fieldOrMethodNames) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            String fieldValue = readStringField(target, name);
            if (!fieldValue.isEmpty()) {
                return fieldValue;
            }
            String methodValue = invokeStringMethod(target, name);
            if (!methodValue.isEmpty()) {
                return methodValue;
            }
        }
        return "";
    }

    private static String readStringField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value == null) {
                    return "";
                }
                if (value instanceof net.minecraft.network.chat.Component) {
                    return ((net.minecraft.network.chat.Component) value).getString();
                }
                return String.valueOf(value);
            } catch (Exception ignored) {
            }
            current = current.getSuperclass();
        }
        return "";
    }

    private static String invokeStringMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value == null) {
                return "";
            }
            if (value instanceof net.minecraft.network.chat.Component) {
                return ((net.minecraft.network.chat.Component) value).getString();
            }
            return String.valueOf(value);
        } catch (Exception ignored) {
            return "";
        }
    }
}
