package com.zszl.zszlScriptMod.otherfeatures.handler.misc.runtime;

import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.util.Map;

public final class AutoReconnectRuntime {

    private ServerData pendingReconnectServer;
    private int reconnectDelayTicks;
    private int reconnectKeepAliveTicks;
    private int reconnectAttemptCount;

    public void onClientDisconnected(Minecraft mc, boolean featureEnabled, int retryDelayTicks) {
        if (!featureEnabled || mc == null || mc.isSingleplayer()) {
            clearState();
            return;
        }

        ServerData current = mc.getCurrentServer();
        if (current == null || current.ip == null || current.ip.trim().isEmpty()) {
            clearState();
            return;
        }

        this.pendingReconnectServer = new ServerData(current.name, current.ip, ServerData.Type.OTHER);
        this.reconnectDelayTicks = Math.max(5, retryDelayTicks);
        this.reconnectKeepAliveTicks = 200;
        this.reconnectAttemptCount = 0;
    }

    public void tick(Minecraft mc, boolean featureEnabled, int retryDelayTicks, int maxAttempts, boolean infiniteAttempts) {
        if (!featureEnabled || this.pendingReconnectServer == null || mc == null) {
            return;
        }
        if (mc.player != null && mc.level != null) {
            clearState();
            return;
        }
        if (mc.screen instanceof ConnectScreen) {
            return;
        }
        if (mc.screen instanceof DisconnectedScreen) {
            if (this.reconnectDelayTicks > 0) {
                this.reconnectDelayTicks--;
                return;
            }
            if (!infiniteAttempts && this.reconnectAttemptCount >= Math.max(1, maxAttempts)) {
                clearState();
                return;
            }
            try {
                ConnectScreen.startConnecting(new TitleScreen(), mc,
                        ServerAddress.parseString(this.pendingReconnectServer.ip),
                        this.pendingReconnectServer, false, new TransferState(Map.of(), Map.of(), false));
                this.reconnectAttemptCount++;
                this.reconnectDelayTicks = Math.max(5, retryDelayTicks);
            } catch (Exception e) {
                zszlScriptMod.LOGGER.debug("自动重连执行失败", e);
                clearState();
            }
            return;
        }
        if (this.reconnectKeepAliveTicks > 0) {
            this.reconnectKeepAliveTicks--;
            return;
        }
        clearState();
    }

    public MiscFeatureManager.ServerReconnectState snapshot() {
        return new MiscFeatureManager.ServerReconnectState(this.pendingReconnectServer != null,
                this.reconnectDelayTicks, this.reconnectAttemptCount);
    }

    public void clearState() {
        this.pendingReconnectServer = null;
        this.reconnectDelayTicks = 0;
        this.reconnectKeepAliveTicks = 0;
        this.reconnectAttemptCount = 0;
    }
}
