package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.config.AbstractThreePaneRuleManager;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseManager;
import com.zszl.zszlScriptMod.system.dungeon.Warehouse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiWarehouseManager extends AbstractThreePaneRuleManager<Warehouse> {

    private static final int BTN_GET_POINT1 = 2301;
    private static final int BTN_GET_POINT2 = 2302;
    private static final int BTN_TOGGLE_ACTIVE = 2303;
    private static final int BTN_SCAN_CHESTS = 2304;
    private static final int BTN_INFO = 2305;
    private static final int BTN_DEPOSIT_MODE = 2306;
    private static final int BTN_AUTO_DEPOSIT = 2307;

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField x1Field;
    private GuiTextField z1Field;
    private GuiTextField x2Field;
    private GuiTextField z2Field;

    private GuiButton btnGetPoint1;
    private GuiButton btnGetPoint2;
    private GuiButton btnToggleActive;
    private GuiButton btnScanChests;
    private GuiButton btnInfo;
    private GuiButton btnDepositMode;
    private GuiButton btnAutoDeposit;

    private Warehouse workingWarehouse;
    private boolean editorActive;

    public GuiWarehouseManager(GuiScreen parent) {
        super(parent);
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("gui.warehouse.manager.title");
    }

    @Override
    protected String getGuideText() {
        return "§7左键筛选/折叠分组，右键分组或卡片打开菜单，右侧可编辑区域并管理扫描/自动存入。";
    }

    @Override
    protected String getEntityDisplayName() {
        return "仓库";
    }

    @Override
    protected String getAllItemsLabel() {
        return "全部仓库";
    }

    @Override
    protected String getEmptyTreeText() {
        return I18n.format("gui.warehouse.manager.empty");
    }

    @Override
    protected String getEmptyListText() {
        return "该分组下暂无仓库";
    }

    @Override
    protected List<Warehouse> getSourceItems() {
        return WarehouseManager.warehouses;
    }

    @Override
    protected List<String> getSourceCategories() {
        return WarehouseManager.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return WarehouseManager.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return WarehouseManager.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return WarehouseManager.deleteCategory(category);
    }

    @Override
    protected void persistChanges() {
        WarehouseManager.saveWarehouses();
    }

    @Override
    protected void reloadSource() {
        WarehouseManager.loadWarehouses();
    }

    @Override
    protected Warehouse createNewItem() {
        Warehouse warehouse = new Warehouse();
        warehouse.name = I18n.format("gui.warehouse.edit.new_name");
        warehouse.category = CATEGORY_DEFAULT;
        warehouse.chests = new CopyOnWriteArrayList<>();
        warehouse.updateBounds();
        return warehouse;
    }

    @Override
    protected Warehouse copyItem(Warehouse source) {
        Warehouse copy = new Warehouse();
        if (source == null) {
            copy.name = I18n.format("gui.warehouse.edit.new_name");
            copy.category = CATEGORY_DEFAULT;
            copy.chests = new CopyOnWriteArrayList<>();
            copy.updateBounds();
            return copy;
        }

        copy.name = source.name;
        copy.category = normalizeWarehouseCategory(source.category);
        copy.isActive = source.isActive;
        copy.x1 = source.x1;
        copy.z1 = source.z1;
        copy.x2 = source.x2;
        copy.z2 = source.z2;
        copy.chests = source.chests == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(source.chests);
        copy.updateBounds();
        return copy;
    }

    @Override
    protected void addItemToSource(Warehouse item) {
        WarehouseManager.warehouses.add(item);
    }

    @Override
    protected void removeItemFromSource(Warehouse item) {
        WarehouseManager.warehouses.remove(item);
    }

    @Override
    protected String getItemName(Warehouse item) {
        return item == null ? "" : item.name;
    }

    @Override
    protected void setItemName(Warehouse item, String name) {
        if (item != null) {
            item.name = name;
        }
    }

    @Override
    protected String getItemCategory(Warehouse item) {
        return item == null ? CATEGORY_DEFAULT : item.category;
    }

    @Override
    protected void setItemCategory(Warehouse item, String category) {
        if (item != null) {
            item.category = normalizeWarehouseCategory(category);
        }
    }

    @Override
    protected void loadEditor(Warehouse item) {
        workingWarehouse = copyItem(item == null ? createNewItem() : item);
        setText(nameField, safe(workingWarehouse.name));
        setText(categoryField, normalizeWarehouseCategory(workingWarehouse.category));
        setText(x1Field, formatDouble(workingWarehouse.x1));
        setText(z1Field, formatDouble(workingWarehouse.z1));
        setText(x2Field, formatDouble(workingWarehouse.x2));
        setText(z2Field, formatDouble(workingWarehouse.z2));
        editorActive = workingWarehouse.isActive;
        layoutAllWidgets();
    }

    @Override
    protected Warehouse buildItemFromEditor(boolean creatingNew, Warehouse selectedItem) {
        syncFieldsToWorkingWarehouse();
        return copyItem(workingWarehouse);
    }

    @Override
    protected void applyItemValues(Warehouse target, Warehouse source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = normalizeWarehouseCategory(source.category);
        target.isActive = source.isActive;
        target.x1 = source.x1;
        target.z1 = source.z1;
        target.x2 = source.x2;
        target.z2 = source.z2;
        target.chests = source.chests == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(source.chests);
        target.updateBounds();
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createWarehouseField(3301);
        categoryField = createWarehouseField(3302);
        x1Field = createWarehouseField(3303);
        z1Field = createWarehouseField(3304);
        x2Field = createWarehouseField(3305);
        z2Field = createWarehouseField(3306);
    }

    @Override
    protected void initAdditionalButtons() {
        btnGetPoint1 = new ThemedButton(BTN_GET_POINT1, 0, 0, 100, 20, I18n.format("gui.warehouse.edit.get_point1"));
        btnGetPoint2 = new ThemedButton(BTN_GET_POINT2, 0, 0, 100, 20, I18n.format("gui.warehouse.edit.get_point2"));
        btnToggleActive = new ThemedButton(BTN_TOGGLE_ACTIVE, 0, 0, 100, 20, "");
        btnScanChests = new ThemedButton(BTN_SCAN_CHESTS, 0, 0, 100, 20, I18n.format("gui.warehouse.edit.scan_chests"));
        btnInfo = new ThemedButton(BTN_INFO, 0, 0, 100, 20, I18n.format("gui.warehouse.manager.info"));
        btnDepositMode = new ThemedButton(BTN_DEPOSIT_MODE, 0, 0, 100, 20, "");
        btnAutoDeposit = new ThemedButton(BTN_AUTO_DEPOSIT, 0, 0, 100, 20, "自动存入");

        buttonList.add(btnGetPoint1);
        buttonList.add(btnGetPoint2);
        buttonList.add(btnToggleActive);
        buttonList.add(btnScanChests);
        buttonList.add(btnInfo);
        buttonList.add(btnDepositMode);
        buttonList.add(btnAutoDeposit);
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullWidth = Math.max(120, right - editorFieldX);
        int halfWidth = Math.max(70, (fullWidth - 10) / 2);

        placeField(nameField, 0, editorFieldX, fullWidth);
        placeField(categoryField, 1, editorFieldX, fullWidth);
        placeField(x1Field, 2, editorFieldX, halfWidth);
        placeField(z1Field, 2, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetPoint1, 3, editorFieldX, fullWidth, 20);
        placeField(x2Field, 4, editorFieldX, halfWidth);
        placeField(z2Field, 4, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetPoint2, 5, editorFieldX, fullWidth, 20);
        placeButton(btnToggleActive, 6, editorFieldX, fullWidth, 20);
        placeButton(btnScanChests, 7, editorFieldX, halfWidth, 20);
        placeButton(btnInfo, 7, editorFieldX + halfWidth + 10, halfWidth, 20);
        placeButton(btnDepositMode, 8, editorFieldX, fullWidth, 20);
        placeButton(btnAutoDeposit, 9, editorFieldX, fullWidth, 20);
    }

    @Override
    protected void layoutAdditionalButtons() {
        updateEditorButtonStates();
    }

    @Override
    protected int getEditorTotalRows() {
        return 11;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        switch (row) {
            case 0:
                return I18n.format("gui.warehouse.edit.name");
            case 1:
                return "所属分组";
            case 2:
                return I18n.format("gui.warehouse.edit.point1");
            case 3:
                return "获取点1";
            case 4:
                return I18n.format("gui.warehouse.edit.point2");
            case 5:
                return "获取点2";
            case 6:
                return "激活状态";
            case 7:
                return "扫描 / 信息";
            case 8:
                return I18n.format("gui.warehouse.manager.deposit_mode",
                        WarehouseEventHandler.oneClickDepositMode
                                ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"));
            case 9:
                return "自动存入";
            case 10:
                return "概要";
            default:
                return "";
        }
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(x1Field);
        fields.add(z1Field);
        fields.add(x2Field);
        fields.add(z2Field);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorFields();
        drawWarehouseSummary();
    }

    @Override
    protected void drawCard(Warehouse item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered) {
        int background = selected ? 0xFF234A6A : hovered ? 0xFF1C2733 : 0xFF141B22;
        drawRect(x, y, x + width, y + height, background);
        drawRect(x, y, x + width, y + 1, 0xFF33485C);
        drawRect(x, y + height - 1, x + width, y + height, 0xFF0C1014);

        drawString(fontRenderer, trimToWidth(safe(item.name), width - 12), x + 6, y + 6, 0xFFFFFFFF);
        drawString(fontRenderer, trimToWidth("分组: " + normalizeWarehouseCategory(item.category), width - 12),
                x + 6, y + 20, 0xFFB8C7D9);

        String stateText = (item.isActive ? "§a启用" : "§7停用")
                + "  箱子 " + (item.chests == null ? 0 : item.chests.size());
        drawString(fontRenderer, trimToWidth(stateText, width - 12), x + 6, y + 34, 0xFF9FB0C4);

        String areaText = String.format("范围: %.1f, %.1f -> %.1f, %.1f", item.x1, item.z1, item.x2, item.z2);
        drawString(fontRenderer, trimToWidth(areaText, width - 12), x + 6, y + 48, 0xFF8FA1B5);
    }

    @Override
    protected String validateItem(Warehouse item) {
        if (item == null) {
            return "仓库为空";
        }
        if (isBlank(item.name)) {
            return "仓库名称不能为空";
        }
        return null;
    }

    @Override
    protected void onSelectionChanged(Warehouse item) {
        updateEditorButtonStates();
    }

    @Override
    protected void updateEditorButtonStates() {
        boolean hasEditor = workingWarehouse != null;
        btnGetPoint1.visible = true;
        btnGetPoint2.visible = true;
        btnToggleActive.visible = true;
        btnScanChests.visible = true;
        btnInfo.visible = true;
        btnDepositMode.visible = true;
        btnAutoDeposit.visible = true;

        btnGetPoint1.enabled = hasEditor;
        btnGetPoint2.enabled = hasEditor;
        btnToggleActive.enabled = hasEditor;
        btnScanChests.enabled = hasEditor && !creatingNew && getSelectedItemOrNull() != null;
        btnInfo.enabled = !creatingNew && getSelectedItemOrNull() != null;
        btnDepositMode.enabled = true;
        btnAutoDeposit.enabled = WarehouseManager.currentWarehouse != null;

        btnToggleActive.displayString = editorActive
                ? I18n.format("gui.warehouse.manager.deactivate")
                : I18n.format("gui.warehouse.manager.activate");
        btnDepositMode.displayString = I18n.format("gui.warehouse.manager.deposit_mode",
                WarehouseEventHandler.oneClickDepositMode
                        ? I18n.format("gui.common.enabled")
                        : I18n.format("gui.common.disabled"));
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button == null) {
            return false;
        }

        if (button.id == BTN_GET_POINT1) {
            if (mc.player != null) {
                setText(x1Field, formatDouble(mc.player.getX()));
                setText(z1Field, formatDouble(mc.player.getZ()));
            }
            return true;
        }

        if (button.id == BTN_GET_POINT2) {
            if (mc.player != null) {
                setText(x2Field, formatDouble(mc.player.getX()));
                setText(z2Field, formatDouble(mc.player.getZ()));
            }
            return true;
        }

        if (button.id == BTN_TOGGLE_ACTIVE) {
            editorActive = !editorActive;
            updateEditorButtonStates();
            return true;
        }

        if (button.id == BTN_INFO) {
            Warehouse selected = getSelectedItemOrNull();
            if (selected != null && !creatingNew) {
                mc.setScreen(new GuiWarehouseInfo(this, selected));
            }
            return true;
        }

        if (button.id == BTN_SCAN_CHESTS) {
            if (creatingNew) {
                setStatus("§c请先保存仓库后再扫描箱子", 0xFFFF8E8E);
                return true;
            }

            Warehouse selected = getSelectedItemOrNull();
            if (selected == null) {
                setStatus("§c请先选择一个仓库", 0xFFFF8E8E);
                return true;
            }

            syncFieldsToWorkingWarehouse();
            applyItemValues(selected, workingWarehouse);
            WarehouseManager.scanForChestsInWarehouse(selected);
            persistChanges();
            refreshData(true);
            selectByItemName(selected.name);
            setStatus("§a已扫描仓库内箱子", 0xFF8CFF9E);
            return true;
        }

        if (button.id == BTN_DEPOSIT_MODE) {
            WarehouseEventHandler.oneClickDepositMode = !WarehouseEventHandler.oneClickDepositMode;
            updateEditorButtonStates();
            return true;
        }

        if (button.id == BTN_AUTO_DEPOSIT) {
            WarehouseEventHandler.startAutoDepositByHighlights();
            setStatus("§a已触发自动存入", 0xFF8CFF9E);
            return true;
        }

        return false;
    }

    @Override
    protected void afterItemSaved(Warehouse item, boolean createdNow) {
        if (item != null && item.isActive) {
            for (Warehouse warehouse : WarehouseManager.warehouses) {
                if (warehouse != null && warehouse != item) {
                    warehouse.isActive = false;
                }
            }
        }
        persistChanges();
        WarehouseManager.updateCurrentWarehouse();
    }

    private void syncFieldsToWorkingWarehouse() {
        if (workingWarehouse == null) {
            workingWarehouse = createNewItem();
        }

        workingWarehouse.name = safe(nameField.getText()).trim();
        workingWarehouse.category = normalizeWarehouseCategory(categoryField.getText());
        workingWarehouse.isActive = editorActive;
        workingWarehouse.x1 = parseDouble(x1Field.getText(), workingWarehouse.x1);
        workingWarehouse.z1 = parseDouble(z1Field.getText(), workingWarehouse.z1);
        workingWarehouse.x2 = parseDouble(x2Field.getText(), workingWarehouse.x2);
        workingWarehouse.z2 = parseDouble(z2Field.getText(), workingWarehouse.z2);
        if (workingWarehouse.chests == null) {
            workingWarehouse.chests = new CopyOnWriteArrayList<>();
        }
        workingWarehouse.updateBounds();
    }

    private void drawWarehouseSummary() {
        int y = getEditorRowY(10);
        if (y < 0 || workingWarehouse == null) {
            return;
        }

        syncFieldsToWorkingWarehouse();
        int chestCount = workingWarehouse.chests == null ? 0 : workingWarehouse.chests.size();
        String currentName = WarehouseManager.currentWarehouse == null
                ? "无"
                : safe(WarehouseManager.currentWarehouse.name);
        String summary = "箱子记录: " + chestCount
                + "  当前仓库: " + currentName
                + "  边界: (" + formatDouble(workingWarehouse.x1) + ", " + formatDouble(workingWarehouse.z1)
                + ") ~ (" + formatDouble(workingWarehouse.x2) + ", " + formatDouble(workingWarehouse.z2) + ")";
        drawString(fontRenderer, trimToWidth(summary, editorWidth - getEditorLabelWidth() - 24),
                editorFieldX, y + 4, 0xFFB8C7D9);
    }

    private GuiTextField createWarehouseField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(safe(text).trim().replace(',', '.'));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    protected String normalizeWarehouseCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private String formatDouble(double value) {
        return COORD_FORMAT.format(value);
    }
}
