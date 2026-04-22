package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class AutoEquipHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_EQUIP_INTERVAL_TICKS = 3;
    private static final int MIN_EQUIP_INTERVAL_TICKS = 3;
    private static final int DEFAULT_SMART_RANGE = 5;

    public static final AutoEquipHandler INSTANCE = new AutoEquipHandler();

    public static boolean masterSwitchEnabled = true;
    public static boolean enabled = false;
    public static boolean smartActivationEnabled = false;
    public static int smartActivationRange = DEFAULT_SMART_RANGE;
    public static int equipIntervalTicks = DEFAULT_EQUIP_INTERVAL_TICKS;
    public static Map<String, EquipmentSet> equipmentSets = new ConcurrentHashMap<>();
    public static String activeSetName = "";

    public enum ArmorSlot {
        HELMET(EquipmentSlot.HEAD, 5),
        CHESTPLATE(EquipmentSlot.CHEST, 6),
        LEGGINGS(EquipmentSlot.LEGS, 7),
        BOOTS(EquipmentSlot.FEET, 8);

        public final EquipmentSlot equipmentSlot;
        public final int containerSlotIndex;

        ArmorSlot(EquipmentSlot equipmentSlot, int containerSlotIndex) {
            this.equipmentSlot = equipmentSlot;
            this.containerSlotIndex = containerSlotIndex;
        }
    }

    public static class SlotConfig {
        public String itemName = "";
        public boolean enabled = false;
        public boolean leaveOne = false;
    }

    public static class EquipmentSet {
        public Map<ArmorSlot, SlotConfig> slots = new EnumMap<>(ArmorSlot.class);
        public boolean sequentialEquip = false;

        public EquipmentSet() {
            ensureComplete();
        }

        public void ensureComplete() {
            if (slots == null) {
                slots = new EnumMap<>(ArmorSlot.class);
            }
            for (ArmorSlot slot : ArmorSlot.values()) {
                slots.computeIfAbsent(slot, ignored -> new SlotConfig());
            }
        }
    }

    private static class ConfigWrapper {
        String activeSetName;
        Map<String, EquipmentSet> sets;
        boolean smartActivationEnabled;
        int smartActivationRange;
        int equipIntervalTicks;
    }

    static {
        loadConfig();
    }

    private AutoEquipHandler() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_equip_sets_v5.json");
    }

    public static synchronized void loadConfig() {
        equipmentSets = new ConcurrentHashMap<>();
        activeSetName = "";
        enabled = false;
        smartActivationEnabled = false;
        smartActivationRange = DEFAULT_SMART_RANGE;
        equipIntervalTicks = DEFAULT_EQUIP_INTERVAL_TICKS;

        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<ConfigWrapper>() {
                }.getType();
                ConfigWrapper wrapper = GSON.fromJson(reader, type);
                if (wrapper != null) {
                    if (wrapper.sets != null) {
                        for (Map.Entry<String, EquipmentSet> entry : wrapper.sets.entrySet()) {
                            String name = normalizeName(entry.getKey());
                            if (name.isEmpty()) {
                                continue;
                            }
                            EquipmentSet set = entry.getValue() == null ? new EquipmentSet() : entry.getValue();
                            set.ensureComplete();
                            equipmentSets.put(name, set);
                        }
                    }
                    activeSetName = wrapper.activeSetName == null ? "" : wrapper.activeSetName.trim();
                    smartActivationEnabled = wrapper.smartActivationEnabled;
                    smartActivationRange = wrapper.smartActivationRange > 0 ? wrapper.smartActivationRange
                            : DEFAULT_SMART_RANGE;
                    equipIntervalTicks = wrapper.equipIntervalTicks > 0
                            ? Math.max(MIN_EQUIP_INTERVAL_TICKS, wrapper.equipIntervalTicks)
                            : DEFAULT_EQUIP_INTERVAL_TICKS;
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("加载自动穿戴配置失败", e);
            }
        }

        ensureStateConsistency();
    }

    public static synchronized void saveConfig() {
        ensureStateConsistency();

        ConfigWrapper wrapper = new ConfigWrapper();
        wrapper.activeSetName = activeSetName;
        wrapper.sets = new ConcurrentHashMap<>(equipmentSets);
        wrapper.smartActivationEnabled = smartActivationEnabled;
        wrapper.smartActivationRange = smartActivationRange;
        wrapper.equipIntervalTicks = equipIntervalTicks;

        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(wrapper, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动穿戴配置失败", e);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!masterSwitchEnabled) {
            enabled = false;
            return;
        }
        if (activeSetName == null || activeSetName.isEmpty()) {
            enabled = false;
        }
    }

    public static synchronized Set<String> getAllSetNames() {
        ensureStateConsistency();
        return new TreeSet<>(equipmentSets.keySet());
    }

    public static synchronized EquipmentSet getSet(String name) {
        ensureStateConsistency();
        return equipmentSets.get(normalizeName(name));
    }

    public static synchronized void addSet(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty() || equipmentSets.containsKey(normalized)) {
            return;
        }
        equipmentSets.put(normalized, new EquipmentSet());
        saveConfig();
    }

    public static synchronized void deleteSet(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty() || !equipmentSets.containsKey(normalized)) {
            return;
        }
        equipmentSets.remove(normalized);
        if (normalized.equals(activeSetName)) {
            activeSetName = "";
            smartActivationEnabled = false;
            enabled = false;
        }
        saveConfig();
    }

    public static synchronized void setActiveSet(String name, boolean isSmart) {
        ensureStateConsistency();

        String normalized = normalizeName(name);
        if (normalized.isEmpty() || !equipmentSets.containsKey(normalized)) {
            activeSetName = "";
            smartActivationEnabled = false;
            enabled = false;
            saveConfig();
            return;
        }

        if (normalized.equals(activeSetName) && smartActivationEnabled == isSmart) {
            activeSetName = "";
            smartActivationEnabled = false;
            enabled = false;
        } else {
            activeSetName = normalized;
            smartActivationEnabled = isSmart;
            enabled = masterSwitchEnabled && !isSmart;
        }
        saveConfig();
    }

    private static void ensureStateConsistency() {
        if (equipmentSets == null) {
            equipmentSets = new ConcurrentHashMap<>();
        }

        Map<String, EquipmentSet> normalizedSets = new ConcurrentHashMap<>();
        for (Map.Entry<String, EquipmentSet> entry : equipmentSets.entrySet()) {
            String name = normalizeName(entry.getKey());
            if (name.isEmpty()) {
                continue;
            }
            EquipmentSet set = entry.getValue() == null ? new EquipmentSet() : entry.getValue();
            set.ensureComplete();
            normalizedSets.put(name, set);
        }
        equipmentSets = normalizedSets;

        if (equipmentSets.isEmpty()) {
            equipmentSets.put("默认配置", new EquipmentSet());
        }

        activeSetName = activeSetName == null ? "" : activeSetName.trim();
        if (!equipmentSets.containsKey(activeSetName)) {
            activeSetName = "";
            smartActivationEnabled = false;
            enabled = false;
        }

        smartActivationRange = smartActivationRange > 0 ? smartActivationRange : DEFAULT_SMART_RANGE;
        equipIntervalTicks = equipIntervalTicks > 0 ? Math.max(MIN_EQUIP_INTERVAL_TICKS, equipIntervalTicks)
                : DEFAULT_EQUIP_INTERVAL_TICKS;

        if (!masterSwitchEnabled) {
            enabled = false;
        }
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}

