package com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Mouse {

    private static int eventX;
    private static int eventY;
    private static int eventButton = -1;
    private static boolean eventButtonState;
    private static int eventDWheel;

    private Mouse() {
    }

    public static int getX() {
        double[] x = new double[1];
        double[] y = new double[1];
        long window = Minecraft.getInstance().getWindow().handle();
        GLFW.glfwGetCursorPos(window, x, y);
        return (int) x[0];
    }

    public static int getY() {
        double[] x = new double[1];
        double[] y = new double[1];
        long window = Minecraft.getInstance().getWindow().handle();
        GLFW.glfwGetCursorPos(window, x, y);
        return (int) y[0];
    }

    public static boolean isButtonDown(int button) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    public static int getDWheel() {
        return eventDWheel;
    }

    public static int getEventX() {
        return eventX;
    }

    public static int getEventY() {
        return eventY;
    }

    public static int getEventButton() {
        return eventButton;
    }

    public static boolean getEventButtonState() {
        return eventButtonState;
    }

    public static int getEventDWheel() {
        return eventDWheel;
    }

    public static boolean isGrabbed() {
        return Minecraft.getInstance().mouseHandler.isMouseGrabbed();
    }

    public static void setGrabbed(boolean grabbed) {
        if (grabbed) {
            Minecraft.getInstance().mouseHandler.grabMouse();
        } else {
            Minecraft.getInstance().mouseHandler.releaseMouse();
        }
    }

    public static void setButtonEvent(int x, int y, int button, boolean state) {
        eventX = x;
        eventY = y;
        eventButton = button;
        eventButtonState = state;
    }

    public static void setScrollEvent(int x, int y, int dWheel) {
        eventX = x;
        eventY = y;
        eventDWheel = dWheel;
    }

    public static void clearEventState() {
        eventButton = -1;
        eventButtonState = false;
        eventDWheel = 0;
    }
}

