package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.runtime.safety.PathSafetyManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class GuiPathSafetySettings extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private PathSafetyManager.ConfigSnapshot snapshot;

    public GuiPathSafetySettings(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.snapshot = PathSafetyManager.getSnapshot();

        int panelWidth = 420;
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int rowY = panelY + 40;
        int rowGap = 28;
        int buttonWidth = panelWidth - 40;

        this.buttonList.add(new ThemedButton(10, panelX + 20, rowY, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(11, panelX + 20, rowY + rowGap, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(12, panelX + 20, rowY + rowGap * 2, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(13, panelX + 20, rowY + rowGap * 3, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(14, panelX + 20, rowY + rowGap * 4, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(15, panelX + 20, rowY + rowGap * 5, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(0, panelX + 20, panelY + panelHeight - 34, 90, 20, "返回"));
        this.buttonList.add(new ThemedButton(1, panelX + panelWidth - 110, panelY + panelHeight - 34, 90, 20, "保存"));

        refreshLabels();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                this.mc.setScreen(parentScreen);
                return;
            case 1:
                PathSafetyManager.save(snapshot);
                this.mc.setScreen(parentScreen);
                return;
            case 10:
                snapshot = new PathSafetyManager.ConfigSnapshotBuilder(snapshot).setSafeModeEnabled(!snapshot.isSafeModeEnabled()).build();
                break;
            case 11:
                snapshot = new PathSafetyManager.ConfigSnapshotBuilder(snapshot).setDryRunDangerousActions(!snapshot.isDryRunDangerousActions()).build();
                break;
            case 12:
                snapshot = new PathSafetyManager.ConfigSnapshotBuilder(snapshot).setAllowPacketActions(!snapshot.isAllowPacketActions()).build();
                break;
            case 13:
                snapshot = new PathSafetyManager.ConfigSnapshotBuilder(snapshot).setAllowInventoryWriteActions(!snapshot.isAllowInventoryWriteActions()).build();
                break;
            case 14:
                snapshot = new PathSafetyManager.ConfigSnapshotBuilder(snapshot).setAllowItemDropActions(!snapshot.isAllowItemDropActions()).build();
                break;
            case 15:
                snapshot = new PathSafetyManager.ConfigSnapshotBuilder(snapshot).setAllowBackgroundSequences(!snapshot.isAllowBackgroundSequences()).build();
                break;
            default:
                break;
        }
        refreshLabels();
    }

    private void refreshLabels() {
        for (GuiButton button : this.buttonList) {
            if (button == null) {
                continue;
            }
            switch (button.id) {
                case 10:
                    button.displayString = label("安全模式总开关", snapshot.isSafeModeEnabled());
                    break;
                case 11:
                    button.displayString = label("干跑危险动作", snapshot.isDryRunDangerousActions());
                    break;
                case 12:
                    button.displayString = label("允许发包动作", snapshot.isAllowPacketActions());
                    break;
                case 13:
                    button.displayString = label("允许背包/容器写动作", snapshot.isAllowInventoryWriteActions());
                    break;
                case 14:
                    button.displayString = label("允许丢弃物品动作", snapshot.isAllowItemDropActions());
                    break;
                case 15:
                    button.displayString = label("允许后台序列", snapshot.isAllowBackgroundSequences());
                    break;
                default:
                    break;
            }
        }
    }

    private String label(String title, boolean enabled) {
        return title + ": " + (enabled ? "§a开" : "§c关");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 420;
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "安全模式 / 模拟执行", this.fontRenderer);
        this.drawString(this.fontRenderer,
                "危险动作包括发包、背包写入、丢弃物品、后台子序列等。", panelX + 20, panelY + 22, 0xFFD9E3F0);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}




