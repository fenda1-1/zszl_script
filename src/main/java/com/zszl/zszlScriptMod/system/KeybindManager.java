// 文件路径: src/main/java/com/zszl/zszlScriptMod/system/KeybindManager.java
package com.zszl.zszlScriptMod.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.GuiHandler;
import com.zszl.zszlScriptMod.gui.config.GuiLoopCountInput;
import com.zszl.zszlScriptMod.gui.packet.GuiPacketSequenceEditor;
import com.zszl.zszlScriptMod.gui.packet.PacketSequence;
import com.zszl.zszlScriptMod.gui.packet.PacketSequenceManager;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.handlers.AdExpPanelHandler;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler;
import com.zszl.zszlScriptMod.handlers.AutoSigninOnlineHandler;
import com.zszl.zszlScriptMod.handlers.AutoSkillHandler;
import com.zszl.zszlScriptMod.handlers.DeathAutoRejoinHandler;
import com.zszl.zszlScriptMod.handlers.FlyHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.KillTimerHandler;
import com.zszl.zszlScriptMod.handlers.ShulkerBoxStackingHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 快捷键系统的核心管理器。
 * 负责加载、保存、执行和管理所有自定义快捷键。
 */
public class KeybindManager {

    private static class KeybindConfig {
        Map<String, Keybind> actions = new HashMap<>();
        Map<String, Keybind> pathSequences = new LinkedHashMap<>();
    }

    public static class Keybind {
        private int keyCode;
        private Set<Integer> modifiers;
        // --- 核心修改：为快捷键添加一个额外参数 ---
        private String parameter;

        public Keybind(int keyCode, Set<Integer> modifiers) {
            this.keyCode = keyCode;
            this.modifiers = modifiers != null ? new HashSet<>(modifiers) : new HashSet<>();
            this.parameter = null;
        }

        public Keybind() {
            this.keyCode = Keyboard.KEY_NONE;
            this.modifiers = new HashSet<>();
            this.parameter = null;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public Set<Integer> getModifiers() {
            return modifiers;
        }

        public String getParameter() {
            return parameter;
        } // 新增Getter

        public void setParameter(String parameter) {
            this.parameter = parameter;
        } // 新增Setter

        @Override
        public String toString() {
            if (keyCode == Keyboard.KEY_NONE) {
                return I18n.format("gui.keybind.unbound");
            }
            String mods = Arrays
                    .asList(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT,
                            Keyboard.KEY_LMENU, Keyboard.KEY_RMENU)
                    .stream()
                    .filter(mod -> modifiers.contains(mod))
                    .map(mod -> {
                        if (mod == Keyboard.KEY_LCONTROL || mod == Keyboard.KEY_RCONTROL)
                            return "Ctrl";
                        if (mod == Keyboard.KEY_LSHIFT || mod == Keyboard.KEY_RSHIFT)
                            return "Shift";
                        if (mod == Keyboard.KEY_LMENU || mod == Keyboard.KEY_RMENU)
                            return "Alt";
                        return "";
                    })
                    .distinct()
                    .collect(Collectors.joining(" + "));

            String keyName = Keyboard.getKeyName(keyCode);
            return mods.isEmpty() ? "[" + keyName + "]" : "[" + mods + " + " + keyName + "]";
        }
    }

    public static final Map<BindableAction, Keybind> keybinds = new EnumMap<>(BindableAction.class);
    public static final Map<String, Keybind> pathSequenceKeybinds = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void executeAction(BindableAction action) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null)
            return;
        if (action != null && action.isFeatureToggle()) {
            toggleManagedFeature(mc, action);
            return;
        }

        switch (action) {
            case STOP_SEQUENCE:
                PathSequenceEventListener.instance.stopTracking();
                mc.player
                        .sendMessage(new TextComponentString(TextFormatting.RED + I18n.format("msg.keybind.stop_all")));
                break;
            case SET_LOOP_COUNT:
                mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiLoopCountInput(null)));
                break;
            case TOGGLE_MOUSE_DETACH:
                ModConfig.isMouseDetached = !ModConfig.isMouseDetached;
                String mouseStatus = ModConfig.isMouseDetached ? I18n.format("gui.inventory.mouse.detached")
                        : I18n.format("gui.inventory.mouse.reattached");
                mc.player.sendMessage(
                        new TextComponentString(
                                TextFormatting.AQUA + I18n.format("msg.keybind.mouse_status", mouseStatus)));
                if (!ModConfig.isMouseDetached && mc.currentScreen == null) {
                    mc.mouseHelper.grabMouseCursor();
                }
                break;
            case OPEN_INVENTORY_VIEWER:
                mc.addScheduledTask(() -> mc.player.openGui(zszlScriptMod.instance, GuiHandler.INVENTORY_VIEWER,
                        mc.world, 0, 0, 0));
                mc.player.sendMessage(
                        new TextComponentString(
                                TextFormatting.AQUA + I18n.format("msg.keybind.open_inventory_viewer")));
                break;
            case TOGGLE_AUTO_EAT:
                AutoEatHandler.autoEatEnabled = !AutoEatHandler.autoEatEnabled;
                AutoEatHandler.saveAutoEatConfig();
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_auto_eat.name"),
                        AutoEatHandler.autoEatEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case TOGGLE_AUTO_FISHING:
                AutoFishingHandler.INSTANCE.toggleEnabled();
                break;
            case TOGGLE_FLY:
                FlyHandler.INSTANCE.toggleEnabled();
                break;
            case TOGGLE_KILL_AURA:
                KillAuraHandler.INSTANCE.toggleEnabled();
                break;
            case TOGGLE_AUTO_SKILL:
                AutoSkillHandler.autoSkillEnabled = !AutoSkillHandler.autoSkillEnabled;
                AutoSkillHandler.saveSkillConfig();
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_auto_skill.name"),
                        AutoSkillHandler.autoSkillEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case TOGGLE_SIGNIN_ONLINE:
                AutoSigninOnlineHandler.enabled = !AutoSigninOnlineHandler.enabled;
                AutoSigninOnlineHandler.saveConfig();
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_signin_online.name"),
                        AutoSigninOnlineHandler.enabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case TOGGLE_AUTO_PICKUP:
                AutoPickupHandler.globalEnabled = !AutoPickupHandler.globalEnabled;
                AutoPickupHandler.saveConfig();
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_auto_pickup.name"),
                        AutoPickupHandler.globalEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case TOGGLE_PACKET_CAPTURE:
                PacketCaptureHandler.isCapturing = !PacketCaptureHandler.isCapturing;
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_packet_capture.name"),
                        PacketCaptureHandler.isCapturing ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case TOGGLE_DEATH_AUTO_REJOIN:
                DeathAutoRejoinHandler.deathAutoRejoinEnabled = !DeathAutoRejoinHandler.deathAutoRejoinEnabled;
                DeathAutoRejoinHandler.saveConfig();
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_death_auto_rejoin.name"),
                        DeathAutoRejoinHandler.deathAutoRejoinEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case TOGGLE_KILL_TIMER:
                KillTimerHandler.toggleEnabled();
                break;
            case TOGGLE_AD_EXP_PANEL:
                AdExpPanelHandler.toggleEnabled();
                break;
            case TOGGLE_AUTO_STACK_SHULKER:
                ShulkerBoxStackingHandler.autoStackingEnabled = !ShulkerBoxStackingHandler.autoStackingEnabled;
                ShulkerBoxStackingHandler.saveConfig();
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                        I18n.format("keybind.action.toggle_auto_stack_shulker.name"),
                        ShulkerBoxStackingHandler.autoStackingEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"))));
                break;
            case EXECUTE_SPECIFIC_PACKET_SEQUENCE:
                Keybind keybind = keybinds.get(action);
                if (keybind != null && keybind.getParameter() != null && !keybind.getParameter().isEmpty()) {
                    String sequenceName = keybind.getParameter();
                    PacketSequence seq = PacketSequenceManager.loadSequence(sequenceName);
                    if (seq != null) {
                        GuiPacketSequenceEditor sender = new GuiPacketSequenceEditor(null, seq);
                        sender.mc = mc;
                        sender.sendSequence();
                    } else {
                        mc.player.sendMessage(new TextComponentString(
                                TextFormatting.RED
                                        + I18n.format("msg.keybind.packet_sequence_not_found", sequenceName)));
                    }
                } else {
                    mc.player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + I18n.format("msg.keybind.packet_sequence_not_bound")));
                }
                break;
            default:
                // Feature toggles are handled above by toggleManagedFeature; all other unhandled
                // enum values intentionally no-op here to keep the switch exhaustive for IDEs.
                break;
        }
    }

    public static void executePathSequenceByName(String sequenceName) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || sequenceName == null || sequenceName.trim().isEmpty()) {
            return;
        }
        if (!PathSequenceManager.hasSequence(sequenceName)) {
            mc.player.sendMessage(new TextComponentString(TextFormatting.RED
                    + I18n.format("msg.keybind.path_sequence_not_found", sequenceName)));
            return;
        }
        PathSequenceManager.runPathSequence(sequenceName);
    }

    private static void toggleManagedFeature(Minecraft mc, BindableAction action) {
        if (mc == null || mc.player == null || action == null) {
            return;
        }

        String featureId = action.getFeatureId();
        if (featureId == null || featureId.trim().isEmpty()) {
            return;
        }

        boolean targetEnabled;
        switch (action.getFeatureGroup()) {
            case MOVEMENT:
                if ("speed".equalsIgnoreCase(featureId)) {
                    SpeedHandler.INSTANCE.toggleEnabled();
                    return;
                }
                if (!MovementFeatureManager.isManagedFeature(featureId)) {
                    sendMissingManagedFeatureMessage(mc, featureId);
                    return;
                }
                targetEnabled = !MovementFeatureManager.isEnabled(featureId);
                MovementFeatureManager.setEnabled(featureId, targetEnabled);
                break;
            case BLOCK:
                if (!BlockFeatureManager.isManagedFeature(featureId)) {
                    sendMissingManagedFeatureMessage(mc, featureId);
                    return;
                }
                targetEnabled = !BlockFeatureManager.isEnabled(featureId);
                BlockFeatureManager.setEnabled(featureId, targetEnabled);
                break;
            case ITEM:
                if (!ItemFeatureManager.isManagedFeature(featureId)) {
                    sendMissingManagedFeatureMessage(mc, featureId);
                    return;
                }
                targetEnabled = !ItemFeatureManager.isEnabled(featureId);
                ItemFeatureManager.setEnabled(featureId, targetEnabled);
                break;
            case RENDER:
                if (!RenderFeatureManager.isManagedFeature(featureId)) {
                    sendMissingManagedFeatureMessage(mc, featureId);
                    return;
                }
                targetEnabled = !RenderFeatureManager.isEnabled(featureId);
                RenderFeatureManager.setEnabled(featureId, targetEnabled);
                break;
            case WORLD:
                if (!WorldFeatureManager.isManagedFeature(featureId)) {
                    sendMissingManagedFeatureMessage(mc, featureId);
                    return;
                }
                targetEnabled = !WorldFeatureManager.isEnabled(featureId);
                WorldFeatureManager.setEnabled(featureId, targetEnabled);
                break;
            case MISC:
                if (!MiscFeatureManager.isManagedFeature(featureId)) {
                    sendMissingManagedFeatureMessage(mc, featureId);
                    return;
                }
                targetEnabled = !MiscFeatureManager.isEnabled(featureId);
                MiscFeatureManager.setEnabled(featureId, targetEnabled);
                break;
            default:
                return;
        }

        mc.player.sendMessage(new TextComponentString(I18n.format("msg.keybind.common_toggle_status",
                action.getDisplayName(),
                targetEnabled ? I18n.format("gui.common.enabled") : I18n.format("gui.common.disabled"))));
    }

    private static void sendMissingManagedFeatureMessage(Minecraft mc, String featureId) {
        if (mc == null || mc.player == null) {
            return;
        }
        mc.player.sendMessage(new TextComponentString(
                TextFormatting.RED + "[快捷键] 未找到其他功能: " + featureId));
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keybinds_v2.json").toFile();
    }

    public static void loadConfig() {
        keybinds.clear();
        pathSequenceKeybinds.clear();
        File configFile = getConfigFile();
        if (!configFile.exists())
            return;

        try (BufferedReader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement rootEl = new JsonParser().parse(reader);
            if (rootEl != null && rootEl.isJsonObject()
                    && (rootEl.getAsJsonObject().has("actions") || rootEl.getAsJsonObject().has("pathSequences"))) {
                KeybindConfig config = GSON.fromJson(rootEl, KeybindConfig.class);
                if (config != null && config.actions != null) {
                    for (Map.Entry<String, Keybind> entry : config.actions.entrySet()) {
                        try {
                            BindableAction action = BindableAction.valueOf(entry.getKey());
                            keybinds.put(action, entry.getValue());
                        } catch (IllegalArgumentException e) {
                            zszlScriptMod.LOGGER.warn("Unknown action found in keybind config: {}", entry.getKey());
                        }
                    }
                }
                if (config != null && config.pathSequences != null) {
                    pathSequenceKeybinds.putAll(config.pathSequences);
                }
            } else {
                Type legacyType = new TypeToken<Map<String, Keybind>>() {
                }.getType();
                Map<String, Keybind> loadedMap = GSON.fromJson(rootEl, legacyType);
                if (loadedMap != null) {
                    for (Map.Entry<String, Keybind> entry : loadedMap.entrySet()) {
                        try {
                            BindableAction action = BindableAction.valueOf(entry.getKey());
                            keybinds.put(action, entry.getValue());
                        } catch (IllegalArgumentException e) {
                            zszlScriptMod.LOGGER.warn("Unknown action found in keybind config: {}", entry.getKey());
                        }
                    }
                }
            }
            syncPathSequenceKeybinds();
            zszlScriptMod.LOGGER.info("Loaded {} custom keybind(s)", keybinds.size());
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to load keybind config", e);
        }
    }

    public static void saveConfig() {
        try {
            File configFile = getConfigFile();
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
                syncPathSequenceKeybinds();
                KeybindConfig config = new KeybindConfig();
                keybinds.forEach((action, keybind) -> config.actions.put(action.name(), keybind));
                config.pathSequences.putAll(pathSequenceKeybinds);
                GSON.toJson(config, writer);
                zszlScriptMod.LOGGER.info("Keybind config saved");
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to save keybind config", e);
        }
    }

    public static void syncPathSequenceKeybinds() {
        Set<String> names = PathSequenceManager.getAllVisibleSequences().stream()
                .map(seq -> seq.getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        pathSequenceKeybinds.entrySet().removeIf(e -> !names.contains(e.getKey()));
        for (String name : names) {
            pathSequenceKeybinds.putIfAbsent(name, new Keybind());
        }
    }
}

