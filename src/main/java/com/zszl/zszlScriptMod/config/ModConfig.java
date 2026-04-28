// --- Full Java Content (src/main/java/com/zszl/zszlScriptMod/config/ModConfig.java) ---
package com.zszl.zszlScriptMod.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.gui.config.GuiAntiStuckConfig;
import com.zszl.zszlScriptMod.handlers.*;
import com.zszl.zszlScriptMod.handlers.BlockReplacementHandler;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.system.DebugKeybindManager;
import com.zszl.zszlScriptMod.system.KeybindManager;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.PerformanceMonitor;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import java.util.EnumMap;
import java.util.Map;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler; // 新增导入

public class ModConfig {

    public static boolean showMouseCoordinates = false;
    public static boolean showHoverInfo = false;
    public static final String CONFIG_DIR = "config/我的世界脚本";
    public static boolean ahkMoveMouseMode = false;

    public static boolean enableGuiListener = false;
    public static boolean enableGhostItemCopy = false;

    // !! 核心修改：新增自动暂停开关 !!
    public static boolean autoPauseOnMenuOpen = true;

    public static boolean isDebugModeEnabled = false; // 全局调试模式开关
    public static final Map<DebugModule, Boolean> debugFlags = new EnumMap<>(DebugModule.class);
    public static boolean isMouseDetached = false;
    static {
        for (DebugModule module : DebugModule.values()) {
            debugFlags.put(module, true); // 默认开启所有模块的调试，由总开关控制
        }
    }

    /**
     * 新的调试检查方法。 只有当总开关和对应模块的开关都打开时，才返回true。
     * 
     * @param module 要检查的调试模块
     * @return 是否应为该模块输出调试日志
     */
    public static boolean isDebugFlagEnabled(DebugModule module) {
        if (!isDebugModeEnabled) {
            return false;
        }
        return debugFlags.getOrDefault(module, false);
    }

    /**
     * 向游戏内聊天框打印调试信息的方法。
     * 
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugPrint(DebugModule module, String message) {
        if (isDebugFlagEnabled(module) && Minecraft.getInstance().player != null) {
            // 使用 addScheduledTask 确保消息在主线程发送，避免多线程问题
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().player.displayClientMessage(new TextComponentString(ChatFormatting.AQUA
                        + "[DEBUG: " + module.getDisplayName() + "] " + ChatFormatting.GRAY + message), false);
            });
        }
    }

    /**
     * 强制向游戏内聊天框打印调试信息，无视任何开关。
     * 
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugPrintForce(DebugModule module, String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().player.displayClientMessage(new TextComponentString(ChatFormatting.RED
                        + "[FORCE DEBUG: " + module.getDisplayName() + "] " + ChatFormatting.YELLOW + message), false);
            });
        }
    }

    /**
     * 向日志文件打印调试信息，不输出到聊天框。
     *
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugLog(DebugModule module, String message) {
        if (isDebugFlagEnabled(module)) {
            zszlScriptMod.LOGGER.info("[DEBUG:{}|{}] {}", getDebugModuleName(module), getDebugModuleDisplayName(module),
                    safeDebugMessage(message));
        }
    }

    /**
     * 强制向日志文件打印调试信息，无视任何开关。
     *
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugLogForce(DebugModule module, String message) {
        zszlScriptMod.LOGGER.info("[FORCE DEBUG:{}|{}] {}", getDebugModuleName(module),
                getDebugModuleDisplayName(module), safeDebugMessage(message));
    }

    /**
     * 加载所有配置
     */
    public static void loadAllConfigs() {
        AutoEatHandler.loadAutoEatConfig();
        ItemFilterHandler.loadFilterConfig();
        AutoFollowHandler.loadFollowConfig();
        KillTimerHandler.loadConfig();
        ConditionalExecutionHandler.loadConfig();
        AutoPickupHandler.loadConfig();
        AutoUseItemHandler.loadConfig();
        BlockReplacementHandler.loadConfig();
        PerformanceMonitor.loadConfig();
        WarehouseManager.loadWarehouses();
        PathSequenceManager.initializePathSequences();
        KeybindManager.loadConfig();
        DebugKeybindManager.loadConfig();
        CapturedIdRuleManager.reloadRules();
    }

    /**
     * 检查是否有GUI打开
     * 
     * @return true if a GUI is open, false otherwise.
     */
    public static boolean isGuiOpen() {
        return Minecraft.getInstance().screen != null;
    }

    private static String getDebugModuleName(DebugModule module) {
        return module == null ? "UNKNOWN" : module.name();
    }

    private static String getDebugModuleDisplayName(DebugModule module) {
        return module == null ? "未知模块" : module.getDisplayName();
    }

    private static String safeDebugMessage(String message) {
        return message == null ? "" : message;
    }
}
