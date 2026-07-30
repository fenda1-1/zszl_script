// 文件路径: src/main/java/com/zszl/zszlScriptMod/handlers/FreecamHandler.java
// (这是只保留极限攻速功能的最终版本)
package com.zszl.zszlScriptMod.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.PerformanceMonitor;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class FreecamHandler {

    public static final FreecamHandler INSTANCE = new FreecamHandler();

    public boolean isFastAttackEnabled = false;
    public int ghostPlayerCount = 0;
    public static int ghostSoulCount = 1;

    public static boolean enableNoCollision = true;
    public static boolean enableAntiKnockback = true;
    public static boolean enableGhostEntity = true;

    private final List<GhostPlayer> ghostEntities = new ArrayList<>();
    private Vec3d lastTickPos = null;
    private static final double TELEPORT_THRESHOLD = 50.0;
    private double lastSafeMotionX = 0.0D;
    private double lastSafeMotionY = 0.0D;
    private double lastSafeMotionZ = 0.0D;

    private FreecamHandler() {
    }

    static {
        loadConfig();
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_fastattack.json").toFile();
    }

    public static void loadConfig() {
        enableNoCollision = true;
        enableAntiKnockback = true;
        enableGhostEntity = true;
        ghostSoulCount = 1;

        try {
            File configFile = getConfigFile();
            if (!configFile.exists()) {
                return;
            }
            JsonObject json = new JsonParser().parse(new FileReader(configFile)).getAsJsonObject();
            enableNoCollision = !json.has("enableNoCollision") || json.get("enableNoCollision").getAsBoolean();
            enableAntiKnockback = !json.has("enableAntiKnockback") || json.get("enableAntiKnockback").getAsBoolean();
            enableGhostEntity = !json.has("enableGhostEntity") || json.get("enableGhostEntity").getAsBoolean();
            ghostSoulCount = json.has("ghostSoulCount") ? json.get("ghostSoulCount").getAsInt() : 1;
            ghostSoulCount = Math.max(1, Math.min(20, ghostSoulCount));
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.fast_attack.load_failed"), e);
        }
    }

    public static void saveConfig() {
        try {
            File configFile = getConfigFile();
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            JsonObject json = new JsonObject();
            json.addProperty("enableNoCollision", enableNoCollision);
            json.addProperty("enableAntiKnockback", enableAntiKnockback);
            json.addProperty("enableGhostEntity", enableGhostEntity);
            json.addProperty("ghostSoulCount", Math.max(1, Math.min(20, ghostSoulCount)));

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.fast_attack.save_failed"), e);
        }
    }

    public void toggleFastAttack() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null)
            return;

        isFastAttackEnabled = !isFastAttackEnabled;
        // overrideIsSpectator = isFastAttackEnabled; // <--- 已删除，这是解除与飞行冲突的关键

        if (isFastAttackEnabled) {
            ghostPlayerCount = enableGhostEntity ? Math.max(1, ghostSoulCount) : 0;
            lastTickPos = mc.player.getPositionVector();
            lastSafeMotionX = 0.0D;
            lastSafeMotionY = 0.0D;
            lastSafeMotionZ = 0.0D;
            mc.player.sendMessage(
                    new TextComponentString(I18n.format("msg.fast_attack.enabled")));
        } else {
            ghostPlayerCount = 0;
            lastTickPos = null;
            lastSafeMotionX = 0.0D;
            lastSafeMotionY = 0.0D;
            lastSafeMotionZ = 0.0D;
            mc.player.sendMessage(
                    new TextComponentString(I18n.format("msg.fast_attack.disabled")));
        }
    }

    private void reloadGhosts() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null)
            return;

        zszlScriptMod.LOGGER.info(I18n.format("log.fast_attack.teleport_reload"));
        mc.player.sendMessage(
                new TextComponentString(I18n.format("msg.fast_attack.teleport_refreshed")));

        for (GhostPlayer ghost : ghostEntities) {
            mc.world.removeEntityFromWorld(ghost.getEntityId());
        }
        ghostEntities.clear();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!PerformanceMonitor.isFeatureEnabled("freecam")) {
            return;
        }
        PerformanceMonitor.PerformanceTimer timer = PerformanceMonitor.startTimer("freecam");
        try {
        if (event.phase != TickEvent.Phase.START || event.player != Minecraft.getMinecraft().player) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null)
            return;

        if (isFastAttackEnabled) {
            ghostPlayerCount = enableGhostEntity ? Math.max(1, ghostSoulCount) : 0;
            KillAuraHandler.INSTANCE.applyMovementProtection(mc.player, true, enableNoCollision, enableAntiKnockback);

            Vec3d currentPos = mc.player.getPositionVector();
            if (lastTickPos != null) {
                double distanceSq = lastTickPos.squareDistanceTo(currentPos.x, lastTickPos.y, currentPos.z);
                if (distanceSq > TELEPORT_THRESHOLD * TELEPORT_THRESHOLD) {
                    reloadGhosts();
                }
            }
            lastTickPos = currentPos;
        } else if (!KillAuraHandler.enabled) {
            KillAuraHandler.INSTANCE.applyMovementProtection(mc.player, false, false, false);
        }

        while (ghostEntities.size() < ghostPlayerCount) {
            GhostPlayer ghost = new GhostPlayer(mc.world, mc.player.getGameProfile());
            ghost.copyLocationAndAnglesFrom(mc.player);
            ghost.setInvisible(true);
            mc.world.addEntityToWorld(-100 - ghostEntities.size(), ghost);
            ghostEntities.add(ghost);
        }

        while (ghostEntities.size() > ghostPlayerCount) {
            GhostPlayer ghostToRemove = ghostEntities.remove(ghostEntities.size() - 1);
            mc.world.removeEntityFromWorld(ghostToRemove.getEntityId());
        }

        if (!ghostEntities.isEmpty()) {
            for (GhostPlayer ghost : ghostEntities) {
                ghost.setPositionAndRotation(mc.player.posX, mc.player.posY, mc.player.posZ, mc.player.rotationYaw,
                        mc.player.rotationPitch);
                ghost.rotationYawHead = mc.player.rotationYawHead;
            }
        }
        // 此处不再有其他代码
        } finally {
            timer.stop();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!PerformanceMonitor.isFeatureEnabled("freecam")) {
            return;
        }
        PerformanceMonitor.PerformanceTimer timer = PerformanceMonitor.startTimer("freecam");
        try {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }

        if (!isFastAttackEnabled) {
            return;
        }

        KillAuraHandler.INSTANCE.applyMovementProtection(mc.player, true, enableNoCollision, enableAntiKnockback);
        } finally {
            timer.stop();
        }
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        if (!event.getEntityPlayer().world.isRemote || ghostEntities.isEmpty()) {
            return;
        }

        for (GhostPlayer ghost : ghostEntities) {
            ghost.swingArm(EnumHand.MAIN_HAND);
            ghost.performHurtAnimation();
        }
    }

    public void onClientDisconnect() {
        // 只重置与极限攻速相关的状态
        isFastAttackEnabled = false;
        ghostPlayerCount = 0;
        ghostEntities.clear();
        lastTickPos = null;
        lastSafeMotionX = 0.0D;
        lastSafeMotionY = 0.0D;
        lastSafeMotionZ = 0.0D;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && !KillAuraHandler.enabled) {
            KillAuraHandler.INSTANCE.applyMovementProtection(mc.player, false, false, false);
        }
    }
}
