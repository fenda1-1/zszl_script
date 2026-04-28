package com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class Keyboard {

    public static final int KEY_NONE = 0;
    public static final int KEY_ESCAPE = 1;
    public static final int KEY_1 = 2;
    public static final int KEY_2 = 3;
    public static final int KEY_3 = 4;
    public static final int KEY_4 = 5;
    public static final int KEY_5 = 6;
    public static final int KEY_6 = 7;
    public static final int KEY_7 = 8;
    public static final int KEY_8 = 9;
    public static final int KEY_9 = 10;
    public static final int KEY_0 = 11;
    public static final int KEY_SUBTRACT = 12;
    public static final int KEY_ADD = 13;
    public static final int KEY_BACK = 14;
    public static final int KEY_TAB = 15;
    public static final int KEY_Q = 16;
    public static final int KEY_W = 17;
    public static final int KEY_E = 18;
    public static final int KEY_R = 19;
    public static final int KEY_T = 20;
    public static final int KEY_Y = 21;
    public static final int KEY_U = 22;
    public static final int KEY_I = 23;
    public static final int KEY_O = 24;
    public static final int KEY_P = 25;
    public static final int KEY_LBRACKET = 26;
    public static final int KEY_RBRACKET = 27;
    public static final int KEY_RETURN = 28;
    public static final int KEY_LCONTROL = 29;
    public static final int KEY_A = 30;
    public static final int KEY_S = 31;
    public static final int KEY_D = 32;
    public static final int KEY_F = 33;
    public static final int KEY_G = 34;
    public static final int KEY_H = 35;
    public static final int KEY_J = 36;
    public static final int KEY_K = 37;
    public static final int KEY_L = 38;
    public static final int KEY_SEMICOLON = 39;
    public static final int KEY_APOSTROPHE = 40;
    public static final int KEY_GRAVE = 41;
    public static final int KEY_LSHIFT = 42;
    public static final int KEY_BACKSLASH = 43;
    public static final int KEY_Z = 44;
    public static final int KEY_X = 45;
    public static final int KEY_C = 46;
    public static final int KEY_V = 47;
    public static final int KEY_B = 48;
    public static final int KEY_N = 49;
    public static final int KEY_M = 50;
    public static final int KEY_COMMA = 51;
    public static final int KEY_PERIOD = 52;
    public static final int KEY_DIVIDE = 53;
    public static final int KEY_RSHIFT = 54;
    public static final int KEY_MULTIPLY = 55;
    public static final int KEY_LMENU = 56;
    public static final int KEY_SPACE = 57;
    public static final int KEY_CAPITAL = 58;
    public static final int KEY_F1 = 59;
    public static final int KEY_F2 = 60;
    public static final int KEY_F3 = 61;
    public static final int KEY_F4 = 62;
    public static final int KEY_F5 = 63;
    public static final int KEY_F6 = 64;
    public static final int KEY_F7 = 65;
    public static final int KEY_F8 = 66;
    public static final int KEY_F9 = 67;
    public static final int KEY_F10 = 68;
    public static final int KEY_F11 = 87;
    public static final int KEY_F12 = 88;
    public static final int KEY_HOME = 199;
    public static final int KEY_UP = 200;
    public static final int KEY_PRIOR = 201;
    public static final int KEY_LEFT = 203;
    public static final int KEY_RIGHT = 205;
    public static final int KEY_END = 207;
    public static final int KEY_DOWN = 208;
    public static final int KEY_NEXT = 209;
    public static final int KEY_INSERT = 210;
    public static final int KEY_DELETE = 211;
    public static final int KEY_NUMPADENTER = 156;
    public static final int KEY_DECIMAL = 83;
    public static final int KEY_RMENU = 184;
    public static final int KEY_RCONTROL = 157;

    private static final Map<Integer, Integer> LWJGL_TO_GLFW = new HashMap<>();
    private static final Map<Integer, Integer> GLFW_TO_LWJGL = new HashMap<>();
    private static final Map<Integer, String> KEY_NAMES = new HashMap<>();

    private static int eventKey = KEY_NONE;
    private static char eventCharacter = 0;
    private static boolean eventKeyState = false;

    static {
        map(KEY_1, GLFW.GLFW_KEY_1, "1");
        map(KEY_2, GLFW.GLFW_KEY_2, "2");
        map(KEY_3, GLFW.GLFW_KEY_3, "3");
        map(KEY_4, GLFW.GLFW_KEY_4, "4");
        map(KEY_5, GLFW.GLFW_KEY_5, "5");
        map(KEY_6, GLFW.GLFW_KEY_6, "6");
        map(KEY_7, GLFW.GLFW_KEY_7, "7");
        map(KEY_8, GLFW.GLFW_KEY_8, "8");
        map(KEY_9, GLFW.GLFW_KEY_9, "9");
        map(KEY_0, GLFW.GLFW_KEY_0, "0");
        map(KEY_SUBTRACT, GLFW.GLFW_KEY_MINUS, "SUBTRACT");
        map(KEY_SUBTRACT, GLFW.GLFW_KEY_KP_SUBTRACT, "NUMPADMINUS");
        map(KEY_ADD, GLFW.GLFW_KEY_EQUAL, "ADD");
        map(KEY_ADD, GLFW.GLFW_KEY_KP_ADD, "NUMPADPLUS");
        map(KEY_ESCAPE, GLFW.GLFW_KEY_ESCAPE, "ESCAPE");
        map(KEY_RETURN, GLFW.GLFW_KEY_ENTER, "RETURN");
        map(KEY_NUMPADENTER, GLFW.GLFW_KEY_KP_ENTER, "NUMPADENTER");
        map(KEY_BACK, GLFW.GLFW_KEY_BACKSPACE, "BACK");
        map(KEY_TAB, GLFW.GLFW_KEY_TAB, "TAB");
        map(KEY_Q, GLFW.GLFW_KEY_Q, "Q");
        map(KEY_W, GLFW.GLFW_KEY_W, "W");
        map(KEY_E, GLFW.GLFW_KEY_E, "E");
        map(KEY_R, GLFW.GLFW_KEY_R, "R");
        map(KEY_T, GLFW.GLFW_KEY_T, "T");
        map(KEY_Y, GLFW.GLFW_KEY_Y, "Y");
        map(KEY_U, GLFW.GLFW_KEY_U, "U");
        map(KEY_I, GLFW.GLFW_KEY_I, "I");
        map(KEY_O, GLFW.GLFW_KEY_O, "O");
        map(KEY_P, GLFW.GLFW_KEY_P, "P");
        map(KEY_LBRACKET, GLFW.GLFW_KEY_LEFT_BRACKET, "LBRACKET");
        map(KEY_RBRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET, "RBRACKET");
        map(KEY_DELETE, GLFW.GLFW_KEY_DELETE, "DELETE");
        map(KEY_INSERT, GLFW.GLFW_KEY_INSERT, "INSERT");
        map(KEY_HOME, GLFW.GLFW_KEY_HOME, "HOME");
        map(KEY_END, GLFW.GLFW_KEY_END, "END");
        map(KEY_PRIOR, GLFW.GLFW_KEY_PAGE_UP, "PRIOR");
        map(KEY_NEXT, GLFW.GLFW_KEY_PAGE_DOWN, "NEXT");
        map(KEY_LEFT, GLFW.GLFW_KEY_LEFT, "LEFT");
        map(KEY_RIGHT, GLFW.GLFW_KEY_RIGHT, "RIGHT");
        map(KEY_UP, GLFW.GLFW_KEY_UP, "UP");
        map(KEY_DOWN, GLFW.GLFW_KEY_DOWN, "DOWN");
        map(KEY_LCONTROL, GLFW.GLFW_KEY_LEFT_CONTROL, "LCONTROL");
        map(KEY_RCONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL, "RCONTROL");
        map(KEY_LSHIFT, GLFW.GLFW_KEY_LEFT_SHIFT, "LSHIFT");
        map(KEY_RSHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT, "RSHIFT");
        map(KEY_LMENU, GLFW.GLFW_KEY_LEFT_ALT, "LMENU");
        map(KEY_RMENU, GLFW.GLFW_KEY_RIGHT_ALT, "RMENU");
        map(KEY_A, GLFW.GLFW_KEY_A, "A");
        map(KEY_S, GLFW.GLFW_KEY_S, "S");
        map(KEY_D, GLFW.GLFW_KEY_D, "D");
        map(KEY_F, GLFW.GLFW_KEY_F, "F");
        map(KEY_G, GLFW.GLFW_KEY_G, "G");
        map(KEY_H, GLFW.GLFW_KEY_H, "H");
        map(KEY_J, GLFW.GLFW_KEY_J, "J");
        map(KEY_K, GLFW.GLFW_KEY_K, "K");
        map(KEY_L, GLFW.GLFW_KEY_L, "L");
        map(KEY_SEMICOLON, GLFW.GLFW_KEY_SEMICOLON, "SEMICOLON");
        map(KEY_APOSTROPHE, GLFW.GLFW_KEY_APOSTROPHE, "APOSTROPHE");
        map(KEY_GRAVE, GLFW.GLFW_KEY_GRAVE_ACCENT, "GRAVE");
        map(KEY_BACKSLASH, GLFW.GLFW_KEY_BACKSLASH, "BACKSLASH");
        map(KEY_Z, GLFW.GLFW_KEY_Z, "Z");
        map(KEY_X, GLFW.GLFW_KEY_X, "X");
        map(KEY_C, GLFW.GLFW_KEY_C, "C");
        map(KEY_V, GLFW.GLFW_KEY_V, "V");
        map(KEY_B, GLFW.GLFW_KEY_B, "B");
        map(KEY_N, GLFW.GLFW_KEY_N, "N");
        map(KEY_M, GLFW.GLFW_KEY_M, "M");
        map(KEY_COMMA, GLFW.GLFW_KEY_COMMA, "COMMA");
        map(KEY_PERIOD, GLFW.GLFW_KEY_PERIOD, "PERIOD");
        map(KEY_DIVIDE, GLFW.GLFW_KEY_SLASH, "DIVIDE");
        map(KEY_DIVIDE, GLFW.GLFW_KEY_KP_DIVIDE, "NUMPADDIVIDE");
        map(KEY_MULTIPLY, GLFW.GLFW_KEY_KP_MULTIPLY, "NUMPADMULTIPLY");
        map(KEY_SPACE, GLFW.GLFW_KEY_SPACE, "SPACE");
        map(KEY_CAPITAL, GLFW.GLFW_KEY_CAPS_LOCK, "CAPITAL");
        map(KEY_DECIMAL, GLFW.GLFW_KEY_KP_DECIMAL, "DECIMAL");
        map(KEY_F1, GLFW.GLFW_KEY_F1, "F1");
        map(KEY_F2, GLFW.GLFW_KEY_F2, "F2");
        map(KEY_F3, GLFW.GLFW_KEY_F3, "F3");
        map(KEY_F4, GLFW.GLFW_KEY_F4, "F4");
        map(KEY_F5, GLFW.GLFW_KEY_F5, "F5");
        map(KEY_F6, GLFW.GLFW_KEY_F6, "F6");
        map(KEY_F7, GLFW.GLFW_KEY_F7, "F7");
        map(KEY_F8, GLFW.GLFW_KEY_F8, "F8");
        map(KEY_F9, GLFW.GLFW_KEY_F9, "F9");
        map(KEY_F10, GLFW.GLFW_KEY_F10, "F10");
        map(KEY_F11, GLFW.GLFW_KEY_F11, "F11");
        map(KEY_F12, GLFW.GLFW_KEY_F12, "F12");
    }

    private Keyboard() {
    }

    private static void map(int lwjgl, int glfw, String name) {
        LWJGL_TO_GLFW.put(lwjgl, glfw);
        GLFW_TO_LWJGL.put(glfw, lwjgl);
        KEY_NAMES.put(lwjgl, name);
    }

    public static int fromGlfwKey(int glfwKey) {
        return GLFW_TO_LWJGL.getOrDefault(glfwKey, KEY_NONE);
    }

    public static int toGlfwKey(int lwjglKey) {
        return LWJGL_TO_GLFW.getOrDefault(lwjglKey, lwjglKey);
    }

    public static void enableRepeatEvents(boolean enabled) {
    }

    public static boolean isKeyDown(int key) {
        Integer glfwKey = LWJGL_TO_GLFW.get(key);
        if (glfwKey == null) {
            return false;
        }
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;
    }

    public static int getEventKey() {
        return eventKey;
    }

    public static char getEventCharacter() {
        return eventCharacter;
    }

    public static boolean getEventKeyState() {
        return eventKeyState;
    }

    public static String getKeyName(int key) {
        return KEY_NAMES.getOrDefault(key, Integer.toString(key));
    }

    public static int getKeyIndex(String name) {
        return KEY_NAMES.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(KEY_NONE);
    }

    public static void setEvent(int key, char character, boolean state) {
        eventKey = key;
        eventCharacter = character;
        eventKeyState = state;
    }
}

