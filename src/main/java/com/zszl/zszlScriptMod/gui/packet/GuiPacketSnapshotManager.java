package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNo;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNoCallback;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiPacketSnapshotManager extends ThemedGuiScreen implements GuiYesNoCallback {

    private final GuiScreen parentScreen;
    private List<PacketSnapshotManager.SnapshotMeta> snapshots = new ArrayList<>();
    private List<PacketSnapshotManager.SnapshotMeta> filteredSnapshots = new ArrayList<>();
    private int selectedIndex = -1; // index in filteredSnapshots
    private String selectedSnapshotName = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean sortDesc = true;
    private int lastClickListIndex = -1;
    private long lastClickTimeMs = 0L;

    private GuiButton btnOpen;
    private GuiButton btnRename;
    private GuiButton btnDelete;
    private GuiButton btnSort;
    private GuiTextField filterField;

    public GuiPacketSnapshotManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.snapshots = PacketSnapshotManager.listSnapshots();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelWidth = 420;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 250) / 2;

        String oldFilter = filterField == null ? "" : filterField.getText();
        filterField = new GuiTextField(10, fontRenderer, panelX + 10, panelY + 12, panelWidth - 20, 18);
        filterField.setMaxStringLength(Integer.MAX_VALUE);
        filterField.setFocused(true);
        filterField.setText(oldFilter);

        applyFilter();

        btnOpen = new ThemedButton(0, panelX + 10, panelY + 195, 120, 20,
                I18n.format("gui.packet.snapshot.open"));
        btnRename = new ThemedButton(3, panelX + 140, panelY + 195, 120, 20,
                I18n.format("gui.path.rename"));
        btnDelete = new ThemedButton(4, panelX + 270, panelY + 195, 140, 20,
                I18n.format("gui.common.delete"));
        btnSort = new ThemedButton(5, panelX + 140, panelY + 220, 120, 20,
                I18n.format(sortDesc ? "gui.packet.snapshot.sort_desc" : "gui.packet.snapshot.sort_asc"));

        this.buttonList.add(btnOpen);
        this.buttonList.add(btnRename);
        this.buttonList.add(btnDelete);
        this.buttonList.add(new ThemedButton(1, panelX + 10, panelY + 220, 120, 20,
                I18n.format("gui.common.reload")));
        this.buttonList.add(btnSort);
        this.buttonList.add(new ThemedButton(2, panelX + 270, panelY + 220, 140, 20,
                I18n.format("gui.common.back")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedIndex >= 0 && selectedIndex < filteredSnapshots.size();
        btnOpen.enabled = hasSelection;
        btnRename.enabled = hasSelection;
        btnDelete.enabled = hasSelection;
    }

    private void applyFilter() {
        String keyword = PinyinSearchHelper.normalizeQuery(filterField == null ? "" : filterField.getText());
        if (keyword.isEmpty()) {
            filteredSnapshots = new ArrayList<>(snapshots);
        } else {
            filteredSnapshots = new ArrayList<>();
            for (PacketSnapshotManager.SnapshotMeta meta : snapshots) {
                String searchText = (meta.name == null ? "" : meta.name) + " "
                        + (meta.captureMode == null ? "" : meta.captureMode) + " "
                        + (meta.getDisplayTime() == null ? "" : meta.getDisplayTime());
                if (PinyinSearchHelper.matchesNormalized(searchText, keyword)) {
                    filteredSnapshots.add(meta);
                }
            }
        }

        if (!sortDesc) {
            java.util.Collections.reverse(filteredSnapshots);
        }

        if (selectedSnapshotName != null) {
            selectedIndex = -1;
            for (int i = 0; i < filteredSnapshots.size(); i++) {
                if (selectedSnapshotName.equals(filteredSnapshots.get(i).name)) {
                    selectedIndex = i;
                    break;
                }
            }
        } else {
            selectedIndex = -1;
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredSnapshots.size() - 1)));
    }

    private void openSelectedSnapshot() {
        if (selectedIndex < 0 || selectedIndex >= filteredSnapshots.size()) {
            return;
        }
        PacketSnapshotManager.SnapshotMeta meta = filteredSnapshots.get(selectedIndex);
        PacketSnapshotManager.SavedSnapshot snapshot = PacketSnapshotManager.loadSnapshot(meta.name);
        if (snapshot != null && snapshot.packets != null) {
            List<PacketCaptureHandler.CapturedPacketData> packets = snapshot.packets.stream()
                    .map(PacketSnapshotManager.SavedPacket::toCaptured)
                    .collect(Collectors.toList());
            String title = I18n.format("gui.packet.snapshot.viewer_title", snapshot.name,
                    packets.size(), snapshot.captureMode);
            mc.setScreen(new GuiPacketViewer(this, packets, title, true));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                openSelectedSnapshot();
                break;
            case 1:
                initGui();
                break;
            case 2:
                mc.setScreen(parentScreen);
                break;
            case 3:
                if (selectedIndex >= 0 && selectedIndex < filteredSnapshots.size()) {
                    String oldName = filteredSnapshots.get(selectedIndex).name;
                    mc.setScreen(new GuiTextInput(this, I18n.format("gui.path.manager.input_new_name"), oldName,
                            newName -> {
                                if (newName != null && !newName.trim().isEmpty()) {
                                    boolean ok = PacketSnapshotManager.renameSnapshot(oldName, newName.trim());
                                    if (mc.player != null) {
                                        mc.player.sendSystemMessage(new TextComponentString(
                                                (ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                                                        + (ok ? I18n.format("msg.packet.snapshot.rename_success")
                                                                : I18n.format("msg.packet.snapshot.rename_failed"))));
                                    }
                                }
                                this.initGui();
                                mc.setScreen(this);
                            }));
                }
                break;
            case 4:
                if (selectedIndex >= 0 && selectedIndex < filteredSnapshots.size()) {
                    String name = filteredSnapshots.get(selectedIndex).name;
                    mc.setScreen(new GuiYesNo((GuiYesNoCallback) this,
                            I18n.format("gui.common.confirm_delete"),
                            I18n.format("gui.packet.snapshot.delete_confirm", name), 99));
                }
                break;
            case 5:
                sortDesc = !sortDesc;
                if (btnSort != null) {
                    btnSort.displayString = I18n.format(sortDesc ? "gui.packet.snapshot.sort_desc"
                            : "gui.packet.snapshot.sort_asc");
                }
                applyFilter();
                updateButtonStates();
                break;
            default:
                break;
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id == 99 && result && selectedIndex >= 0 && selectedIndex < filteredSnapshots.size()) {
            String name = filteredSnapshots.get(selectedIndex).name;
            boolean ok = PacketSnapshotManager.deleteSnapshot(name);
            if (mc.player != null) {
                mc.player.sendSystemMessage(new TextComponentString(
                        (ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                                + (ok ? I18n.format("msg.packet.snapshot.delete_success")
                                        : I18n.format("msg.packet.snapshot.delete_failed"))));
            }
            if (ok) {
                selectedSnapshotName = null;
                selectedIndex = -1;
                initGui();
            }
        }
        mc.setScreen(this);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (filterField.textboxKeyTyped(typedChar, keyCode)) {
            applyFilter();
            updateButtonStates();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        filterField.mouseClicked(mouseX, mouseY, mouseButton);

        int panelWidth = 420;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 250) / 2;
        int listY = panelY + 35;
        int listHeight = 150;
        int itemHeight = 24;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clicked = (mouseY - listY) / itemHeight + scrollOffset;
            if (clicked >= 0 && clicked < filteredSnapshots.size()) {
                selectedIndex = clicked;
                selectedSnapshotName = filteredSnapshots.get(clicked).name;
                updateButtonStates();

                long now = System.currentTimeMillis();
                if (lastClickListIndex == clicked && mouseButton == 0 && (now - lastClickTimeMs) <= 350L) {
                    openSelectedSnapshot();
                }
                lastClickListIndex = clicked;
                lastClickTimeMs = now;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 420;
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.packet.snapshot.history_title"),
                this.fontRenderer);
        drawThemedTextField(filterField);
        if (filterField.getText().isEmpty() && !filterField.isFocused()) {
            drawString(fontRenderer, "§8" + I18n.format("gui.packet.snapshot.filter_hint"), panelX + 14, panelY + 17,
                    0xFFFFFF);
        }

        int listX = panelX + 10;
        int listY = panelY + 35;
        int listW = panelWidth - 20;
        int listH = 150;
        int itemHeight = 24;
        int visible = Math.max(1, listH / itemHeight);
        maxScroll = Math.max(0, filteredSnapshots.size() - visible);

        if (filteredSnapshots.isEmpty()) {
            drawCenteredString(this.fontRenderer, "§7" + I18n.format("gui.packet.snapshot.empty"), this.width / 2,
                    listY + 60, 0xAAAAAA);
        } else {
            for (int i = 0; i < visible; i++) {
                int index = i + scrollOffset;
                if (index >= filteredSnapshots.size()) {
                    break;
                }
                PacketSnapshotManager.SnapshotMeta meta = filteredSnapshots.get(index);
                int y = listY + i * itemHeight;
                int bg = (index == selectedIndex) ? 0xFF0066AA : 0x80444444;
                drawRect(listX, y, listX + listW, y + itemHeight - 1, bg);

                String line = String.format("%s | %s | %d | %s", meta.name, meta.getDisplayTime(), meta.packetCount,
                        meta.captureMode);
                drawString(fontRenderer, line, listX + 6, y + 8, 0xFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawSnapshotTooltip(mouseX, mouseY, panelX, panelY, panelWidth);
    }

    private void drawSnapshotTooltip(int mouseX, int mouseY, int panelX, int panelY, int panelWidth) {
        String tooltip = null;
        int listY = panelY + 35;
        int listHeight = 150;
        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            tooltip = "抓包快照列表。\n双击可直接打开快照，或先选中再使用下方按钮操作。";
        } else if (isMouseOverField(mouseX, mouseY, filterField)) {
            tooltip = "按快照名称、时间或模式过滤历史快照。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case 0:
                        tooltip = "打开当前选中的快照，进入只读抓包查看器。";
                        break;
                    case 1:
                        tooltip = "重新扫描并载入快照文件列表。";
                        break;
                    case 2:
                        tooltip = "返回上一页。";
                        break;
                    case 3:
                        tooltip = "重命名当前选中的快照。";
                        break;
                    case 4:
                        tooltip = "删除当前选中的快照文件。";
                        break;
                    case 5:
                        tooltip = "切换快照列表排序方向。\n可在新到旧和旧到新之间切换。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }
}


