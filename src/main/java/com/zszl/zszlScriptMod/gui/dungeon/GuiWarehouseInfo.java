package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.handlers.GoToAndOpenHandler; // !! 修复：添加导入
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.system.dungeon.ChestData;
import com.zszl.zszlScriptMod.system.dungeon.Warehouse;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos; // !! 修复：添加导入
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiWarehouseInfo extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private final Warehouse warehouse;

    // --- 新增：UI控件和状态 ---
    private GuiTextField searchField;
    private List<ChestData> filteredChests; // 存储筛选后的箱子列表

    private int chestListScrollOffset = 0;
    private int maxChestListScroll = 0;
    private int contentScrollOffset = 0;
    private int maxContentScroll = 0;
    private int selectedChestIndex = -1; // 在 filteredChests 中的索引

    private boolean isDraggingChestScrollbar = false;
    private boolean isDraggingContentScrollbar = false;

    private final Map<java.awt.Rectangle, ItemStack> itemTooltipAreas = new ConcurrentHashMap<>();

    public GuiWarehouseInfo(GuiScreen parent, Warehouse warehouse) {
        this.parentScreen = parent;
        this.warehouse = warehouse;
        // 初始时，显示所有箱子
        this.filteredChests = new ArrayList<>(warehouse.chests);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int panelWidth = 450;
        int panelX = (this.width - panelWidth) / 2;

        // 搜索框
        searchField = new GuiTextField(1, this.fontRenderer, panelX + 10, 35, 180, 20);
        searchField.setFocused(true);

        this.buttonList.add(
                new GuiButton(0, (this.width - 100) / 2, this.height - 30, 100, 20, I18n.format("gui.common.back")));
        int chestListX = panelX + 10;
        int chestListWidth = 180;
        // 在搜索框下方添加按钮
        this.buttonList
                .add(new GuiButton(2, chestListX, 60, chestListWidth, 20, I18n.format("gui.warehouse.info.go_open")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        }
        // !! 新增代码 !!
        else if (button.id == 2) {
            if (selectedChestIndex != -1 && selectedChestIndex < filteredChests.size()) {
                BlockPos targetPos = filteredChests.get(selectedChestIndex).pos;
                mc.setScreen(null); // 关闭所有GUI
                GoToAndOpenHandler.start(targetPos);
            }
        }
    }

    // --- 新增：筛选逻辑 ---
    private void filterChests() {
        String searchText = PinyinSearchHelper.normalizeQuery(searchField == null ? "" : searchField.getText());
        if (searchText.isEmpty()) {
            filteredChests = new ArrayList<>(warehouse.chests);
        } else {
            filteredChests = warehouse.chests.stream()
                    .filter(chest -> {
                        if (!chest.hasBeenScanned)
                            return false;
                        for (ItemStack stack : chest.getSnapshotContents(54)) {
                            if (!stack.isEmpty()
                                    && PinyinSearchHelper.matchesNormalized(stack.getHoverName().getString(), searchText)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }
        // 重置选择和滚动
        selectedChestIndex = -1;
        chestListScrollOffset = 0;
        contentScrollOffset = 0;
    }

    private String getUniqueItemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        // 使用“物品注册名@元数据”作为基础键
        String key = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())) + "@" + stack.getDamageValue();
        // 如果有NBT，附加NBT的字符串表示，以区分附魔、命名等不同的物品
        if (stack.hasTag()) {
            key += ":" + stack.getTag();
        }
        return key;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        itemTooltipAreas.clear();

        int panelWidth = 450;
        int panelHeight = this.height - 60;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 20;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.warehouse.info.title", warehouse.name),
                this.width / 2, panelY + 10, 0xFFFFFF);

        // --- 绘制搜索框 (保持不变) ---
        drawThemedTextField(searchField);
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            drawString(fontRenderer, I18n.format("gui.warehouse.info.search_placeholder"), searchField.x + 5,
                    searchField.y + (searchField.height - 8) / 2,
                    0xFFFFFF);
        }

        // --- 绘制左侧箱子列表 (保持不变) ---
        int chestListX = panelX + 10;
        int listY = panelY + 60;
        int chestListWidth = 180;
        int listHeight = panelHeight - 70;

        drawRect(chestListX, listY, chestListX + chestListWidth, listY + listHeight, 0x80000000);

        int itemHeight = 20;
        int visibleChests = listHeight / itemHeight;
        maxChestListScroll = Math.max(0, filteredChests.size() - visibleChests);
        chestListScrollOffset = Math.max(0, Math.min(chestListScrollOffset, maxChestListScroll));

        for (int i = 0; i < visibleChests; i++) {
            int index = i + chestListScrollOffset;
            if (index >= filteredChests.size())
                break;

            ChestData chest = filteredChests.get(index);
            int itemY = listY + i * itemHeight;

            int bgColor = (index == selectedChestIndex) ? 0xFF0066AA : 0xFF444444;
            boolean isHovered = mouseX >= chestListX && mouseX <= chestListX + chestListWidth && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            if (isHovered && index != selectedChestIndex)
                bgColor = 0xFF666666;

            drawRect(chestListX + 1, itemY + 1, chestListX + chestListWidth - 1, itemY + itemHeight, bgColor);

            String info = I18n.format("gui.warehouse.info.chest_pos", chest.pos.getX(), chest.pos.getY(),
                    chest.pos.getZ());
            this.drawString(fontRenderer, info, chestListX + 5, itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }

        if (maxChestListScroll > 0) {
            int scrollbarX = chestListX + chestListWidth - 5;
            drawRect(scrollbarX, listY, scrollbarX + 4, listY + listHeight, 0xFF101010);
            int thumbHeight = Math.max(10, (int) ((float) visibleChests / filteredChests.size() * listHeight));
            int thumbY = listY
                    + (int) ((float) chestListScrollOffset / maxChestListScroll * (listHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF888888);
        }

        // --- 核心修改：重构右侧内容区域的绘制逻辑 ---
        int contentX = chestListX + chestListWidth + 10;
        int contentWidth = panelWidth - chestListWidth - 30;
        drawRect(contentX, listY, contentX + contentWidth, listY + listHeight, 0x80000000);

        if (selectedChestIndex != -1 && selectedChestIndex < filteredChests.size()) {
            ChestData selectedChest = filteredChests.get(selectedChestIndex);

            if (selectedChest.hasBeenScanned) {
                // 1. 数据聚合
                Map<String, Integer> itemCounts = new LinkedHashMap<>();
                Map<String, ItemStack> itemSamples = new LinkedHashMap<>(); // 存储每种物品的一个样本，用于渲染Tooltip

                NonNullList<ItemStack> items = selectedChest.getSnapshotContents(54);
                for (ItemStack stack : items) {
                    if (!stack.isEmpty()) {
                        String uniqueKey = getUniqueItemKey(stack); // 使用一个唯一键来区分带NBT的物品
                        itemCounts.put(uniqueKey, itemCounts.getOrDefault(uniqueKey, 0) + stack.getCount());
                        itemSamples.putIfAbsent(uniqueKey, stack); // 只保存第一个遇到的样本
                    }
                }

                if (itemCounts.isEmpty()) {
                    drawCenteredString(fontRenderer, I18n.format("gui.warehouse.info.empty"),
                            contentX + contentWidth / 2, listY + listHeight / 2,
                            0xAAAAAA);
                } else {
                    // 2. 将Map转换为可排序和绘制的列表
                    List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(itemCounts.entrySet());
                    // 按物品名称排序
                    sortedItems.sort(Comparator.comparing(entry -> itemSamples.get(entry.getKey()).getHoverName().getString()));

                    // 3. 绘制列表
                    int contentListY = listY + 5;
                    int contentListHeight = listHeight - 10;
                    int visibleContentLines = contentListHeight / itemHeight;
                    maxContentScroll = Math.max(0, sortedItems.size() - visibleContentLines);
                    contentScrollOffset = Math.max(0, Math.min(contentScrollOffset, maxContentScroll));

                    for (int i = 0; i < visibleContentLines; i++) {
                        int index = i + contentScrollOffset;
                        if (index >= sortedItems.size())
                            break;

                        Map.Entry<String, Integer> entry = sortedItems.get(index);
                        ItemStack sampleStack = itemSamples.get(entry.getKey());
                        int totalCount = entry.getValue();

                        String line = String.format("  §f- %s §7* %d", sampleStack.getHoverName().getString(), totalCount);
                        int itemY = contentListY + i * itemHeight;

                        drawString(fontRenderer, line, contentX + 5, itemY + 4, 0xFFFFFF);

                        int textWidth = fontRenderer.getStringWidth(line);
                        java.awt.Rectangle area = new java.awt.Rectangle(contentX + 5, itemY, textWidth, itemHeight);
                        itemTooltipAreas.put(area, sampleStack); // 悬浮提示使用样本物品
                    }

                    // 4. 绘制滚动条
                    if (maxContentScroll > 0) {
                        int scrollbarX = contentX + contentWidth - 5;
                        drawRect(scrollbarX, contentListY, scrollbarX + 4, contentListY + contentListHeight,
                                0xFF101010);
                        int thumbHeight = Math.max(10,
                                (int) ((float) visibleContentLines / sortedItems.size() * contentListHeight));
                        int thumbY = contentListY + (int) ((float) contentScrollOffset / maxContentScroll
                                * (contentListHeight - thumbHeight));
                        drawRect(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF888888);
                    }
                }
            } else {
                drawCenteredString(fontRenderer, I18n.format("gui.warehouse.info.unscanned"),
                        contentX + contentWidth / 2, listY + listHeight / 2,
                        0xAAAAAA);
            }
        } else {
            drawCenteredString(fontRenderer, I18n.format("gui.warehouse.info.select_left"), contentX + contentWidth / 2,
                    listY + listHeight / 2,
                    0xAAAAAA);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 悬浮提示渲染 (保持不变)
        for (Map.Entry<java.awt.Rectangle, ItemStack> entry : itemTooltipAreas.entrySet()) {
            if (entry.getKey().contains(mouseX, mouseY)) {
                if (GuiCompatContext.current() != null) {
                    GuiCompatContext.current().renderTooltip(this.font, entry.getValue(), mouseX, mouseY);
                }
                break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            filterChests();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            int panelX = (this.width - 450) / 2;
            int listY = 20 + 60;
            int listHeight = this.height - 60 - 70;

            // 左侧列表点击
            int chestListX = panelX + 10;
            int chestListWidth = 180;
            if (mouseX >= chestListX && mouseX <= chestListX + chestListWidth && mouseY >= listY
                    && mouseY <= listY + listHeight) {
                int clickedIndex = (mouseY - listY) / 20 + chestListScrollOffset;
                if (clickedIndex >= 0 && clickedIndex < filteredChests.size()) {
                    selectedChestIndex = clickedIndex;
                    contentScrollOffset = 0; // 切换箱子时重置内容滚动条
                }
            }

            // 左侧滚动条点击
            int chestScrollbarX = chestListX + chestListWidth - 5;
            if (mouseX >= chestScrollbarX && mouseX <= chestScrollbarX + 4 && mouseY >= listY
                    && mouseY <= listY + listHeight) {
                isDraggingChestScrollbar = true;
            }

            // 右侧滚动条点击
            int contentX = chestListX + chestListWidth + 10;
            int contentWidth = 450 - chestListWidth - 30;
            int contentScrollbarX = contentX + contentWidth - 5;
            if (mouseX >= contentScrollbarX && mouseX <= contentScrollbarX + 4 && mouseY >= listY + 20
                    && mouseY <= listY + listHeight - 5) {
                isDraggingContentScrollbar = true;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        int listY = 20 + 60;
        int listHeight = this.height - 60 - 70;

        if (isDraggingChestScrollbar) {
            float percent = (float) (mouseY - listY) / listHeight;
            chestListScrollOffset = (int) (percent * maxChestListScroll);
            chestListScrollOffset = Math.max(0, Math.min(maxChestListScroll, chestListScrollOffset));
        }
        if (isDraggingContentScrollbar) {
            int contentListY = listY + 20;
            int contentListHeight = listHeight - 25;
            float percent = (float) (mouseY - contentListY) / contentListHeight;
            contentScrollOffset = (int) (percent * maxContentScroll);
            contentScrollOffset = Math.max(0, Math.min(maxContentScroll, contentScrollOffset));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingChestScrollbar = false;
        isDraggingContentScrollbar = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getX() * this.width / this.mc.getWindow().getWidth();
            int panelX = (this.width - 450) / 2;
            int chestListX = panelX + 10;
            int chestListWidth = 180;

            if (mouseX >= chestListX && mouseX <= chestListX + chestListWidth) {
                if (dWheel > 0)
                    chestListScrollOffset = Math.max(0, chestListScrollOffset - 1);
                else
                    chestListScrollOffset = Math.min(maxChestListScroll, chestListScrollOffset + 1);
            } else {
                if (dWheel > 0)
                    contentScrollOffset = Math.max(0, contentScrollOffset - 1);
                else
                    contentScrollOffset = Math.min(maxContentScroll, contentScrollOffset + 1);
            }
        }
    }
}







