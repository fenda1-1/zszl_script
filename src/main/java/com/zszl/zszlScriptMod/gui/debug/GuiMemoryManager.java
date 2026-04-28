package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.MemoryManager;
import com.zszl.zszlScriptMod.system.MemorySnapshot;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuiMemoryManager extends ThemedGuiScreen {

    private List<String> snapshotNames;
    private final Set<Integer> selectedIndices = new HashSet<>();
    private int scrollOffset;
    private int maxScroll;

    private GuiButton btnCompare;
    private GuiButton btnDelete;

    public GuiMemoryManager() {
        this.snapshotNames = new ArrayList<>(MemoryManager.snapshots.keySet());
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 240) / 2;
        int buttonWidth = (panelWidth - 50) / 4;
        int buttonY = panelY + 185;

        this.buttonList.add(new ThemedButton(0, panelX + 10, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.create_snapshot")));
        btnCompare = new ThemedButton(1, panelX + 20 + buttonWidth, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.compare_selected"));
        btnDelete = new ThemedButton(2, panelX + 30 + 2 * buttonWidth, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.delete_selected"));
        this.buttonList.add(btnCompare);
        this.buttonList.add(btnDelete);
        this.buttonList.add(new ThemedButton(3, panelX + 40 + 3 * buttonWidth, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.clear_all")));

        int bottomButtonY = panelY + 215;
        this.buttonList.add(new ThemedButton(4, panelX + 10, bottomButtonY, (panelWidth - 30) / 2, 20,
                I18n.format("gui.memory.manager.advanced_tools")));
        this.buttonList.add(new ThemedButton(5, panelX + 20 + (panelWidth - 30) / 2, bottomButtonY,
                (panelWidth - 30) / 2, 20, I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        if (btnCompare != null) {
            btnCompare.enabled = selectedIndices.size() == 2;
        }
        if (btnDelete != null) {
            btnDelete.enabled = !selectedIndices.isEmpty();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) {
            return;
        }

        switch (button.id) {
            case 0:
                String defaultName = I18n.format("gui.memory.manager.snapshot_prefix")
                        + new SimpleDateFormat("HH-mm-ss").format(new Date());
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.memory.manager.input_snapshot_name"),
                        defaultName, name -> {
                            if (name != null && !name.trim().isEmpty()) {
                                MemoryManager.takeSnapshot(name.trim());
                                this.snapshotNames = new ArrayList<>(MemoryManager.snapshots.keySet());
                                selectedIndices.clear();
                                updateButtonStates();
                            }
                            mc.setScreen(this);
                        }));
                return;
            case 1:
                if (selectedIndices.size() == 2) {
                    List<Integer> indices = new ArrayList<>(selectedIndices);
                    MemorySnapshot before = MemoryManager.snapshots.get(snapshotNames.get(indices.get(0)));
                    MemorySnapshot after = MemoryManager.snapshots.get(snapshotNames.get(indices.get(1)));
                    if (before != null && after != null) {
                        if (before.timestamp > after.timestamp) {
                            MemorySnapshot temp = before;
                            before = after;
                            after = temp;
                        }
                        mc.setScreen(new GuiMemoryComparison(this, MemoryManager.compare(before, after)));
                    }
                }
                return;
            case 2:
                List<String> toDelete = new ArrayList<>();
                for (int index : selectedIndices) {
                    if (index >= 0 && index < snapshotNames.size()) {
                        toDelete.add(snapshotNames.get(index));
                    }
                }
                toDelete.forEach(MemoryManager::deleteSnapshot);
                this.snapshotNames = new ArrayList<>(MemoryManager.snapshots.keySet());
                selectedIndices.clear();
                updateButtonStates();
                return;
            case 3:
                MemoryManager.clearSnapshots();
                this.snapshotNames.clear();
                selectedIndices.clear();
                updateButtonStates();
                return;
            case 4:
                mc.setScreen(new GuiMemoryTools(this));
                return;
            case 5:
                mc.setScreen(null);
                return;
            default:
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 400;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.memory.manager.title"), this.fontRenderer);

        int listY = panelY + 40;
        int listHeight = 140;
        int itemHeight = 20;
        int visibleItems = Math.max(1, listHeight / itemHeight);
        maxScroll = Math.max(0, snapshotNames.size() - visibleItems);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= snapshotNames.size()) {
                break;
            }

            String name = snapshotNames.get(index);
            MemorySnapshot snapshot = MemoryManager.snapshots.get(name);
            if (snapshot == null) {
                continue;
            }

            int itemY = listY + i * itemHeight;
            int bgColor = selectedIndices.contains(index) ? 0xFF0066AA : 0xFF444444;
            boolean hovered = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10
                    && mouseY >= itemY && mouseY <= itemY + itemHeight;
            if (hovered && !selectedIndices.contains(index)) {
                bgColor = 0xFF666666;
            }

            drawRect(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight, bgColor);
            String info = String.format("§f%s §7(%s, %d MB)", snapshot.name, snapshot.getFormattedTimestamp(),
                    snapshot.usedMemory / 1024 / 1024);
            drawString(fontRenderer, info, panelX + 15, itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int listY = (this.height - 240) / 2 + 40;
        int listHeight = 140;
        int itemHeight = 20;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10
                && mouseY >= listY && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < snapshotNames.size()) {
                if (isCtrlKeyDown()) {
                    if (selectedIndices.contains(clickedIndex)) {
                        selectedIndices.remove(clickedIndex);
                    } else {
                        selectedIndices.add(clickedIndex);
                    }
                } else {
                    selectedIndices.clear();
                    selectedIndices.add(clickedIndex);
                }
                updateButtonStates();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (dWheel < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}
