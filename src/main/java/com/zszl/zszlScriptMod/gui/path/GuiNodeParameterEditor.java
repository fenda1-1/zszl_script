package com.zszl.zszlScriptMod.gui.path;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.path.node.NodeNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.MathHelper;

public class GuiNodeParameterEditor extends ThemedGuiScreen {

    private static final int BTN_SAVE = 1;
    private static final int BTN_CANCEL = 2;
    private static final int BTN_BASE_SELECT_START = 1000;
    private static final int BTN_PARAMS_SELECT_START = 2000;
    private static final int BTN_BASE_TOGGLE_START = 3000;
    private static final int BTN_PARAMS_TOGGLE_START = 4000;

    private final GuiScreen parent;
    private final String nodeType;
    private final JsonObject originalData;
    private final List<String> graphNames;
    private final Consumer<JsonObject> onSave;

    private final List<NodeParameterSchemaRegistry.FieldSchema> baseSchemas = new ArrayList<>();
    private final List<NodeParameterSchemaRegistry.FieldSchema> paramSchemas = new ArrayList<>();

    private final Map<String, GuiTextField> baseTextFields = new LinkedHashMap<>();
    private final Map<String, GuiTextField> paramTextFields = new LinkedHashMap<>();
    private final Map<String, GuiButton> baseSelectButtons = new LinkedHashMap<>();
    private final Map<String, GuiButton> paramSelectButtons = new LinkedHashMap<>();
    private final Map<String, ToggleGuiButton> baseToggleButtons = new LinkedHashMap<>();
    private final Map<String, ToggleGuiButton> paramToggleButtons = new LinkedHashMap<>();
    private final Map<String, String> baseSelectValues = new LinkedHashMap<>();
    private final Map<String, String> paramSelectValues = new LinkedHashMap<>();
    private final Map<String, String> baseValidationMessages = new LinkedHashMap<>();
    private final Map<String, String> paramValidationMessages = new LinkedHashMap<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentTop;
    private int contentBottom;
    private int contentHeight;
    private int scrollOffset;
    private String hoverTooltip = "";
    private String statusMessage = "";
    private int statusColor = 0xFFCCCCCC;
    private String openDropdownKey = null;
    private boolean openDropdownParamsField = false;
    private String dropdownSearchText = "";

    public GuiNodeParameterEditor(GuiScreen parent, String nodeType, JsonObject currentData, List<String> graphNames,
            Consumer<JsonObject> onSave) {
        this.parent = parent;
        this.nodeType = nodeType == null ? "" : nodeType;
        this.originalData = currentData == null ? new JsonObject()
                : new JsonParser().parse(currentData.toString()).getAsJsonObject();
        this.graphNames = graphNames == null ? new ArrayList<String>() : new ArrayList<String>(graphNames);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.baseTextFields.clear();
        this.paramTextFields.clear();
        this.baseSelectButtons.clear();
        this.paramSelectButtons.clear();
        this.baseToggleButtons.clear();
        this.paramToggleButtons.clear();
        this.baseSelectValues.clear();
        this.paramSelectValues.clear();
        this.baseValidationMessages.clear();
        this.paramValidationMessages.clear();
        this.dropdownSearchText = "";

        panelW = Math.min(520, this.width - 30);
        panelH = Math.min(360, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        contentTop = panelY + 28;
        contentBottom = panelY + panelH - 52;

        rebuildSchemasAndControls();

        this.buttonList.add(new ThemedButton(BTN_SAVE, panelX + panelW - 156, panelY + panelH - 24, 70, 18, "§a保存"));
        this.buttonList.add(new ThemedButton(BTN_CANCEL, panelX + panelW - 80, panelY + panelH - 24, 70, 18, "§7取消"));
    }

    private void rebuildSchemasAndControls() {
        baseSchemas.clear();
        paramSchemas.clear();
        scrollOffset = 0;
        closeDropdown();

        List<NodeParameterSchemaRegistry.FieldSchema> allSchemas = NodeParameterSchemaRegistry.getSchemas(nodeType, originalData,
                graphNames);
        for (NodeParameterSchemaRegistry.FieldSchema schema : allSchemas) {
            if (schema.isParamsField()) {
                paramSchemas.add(schema);
            } else {
                baseSchemas.add(schema);
            }
        }

        createControlsForSchemaList(baseSchemas, originalData, false);
        JsonObject params = originalData.has("params") && originalData.get("params").isJsonObject()
                ? originalData.getAsJsonObject("params")
                : new JsonObject();
        createControlsForSchemaList(paramSchemas, params, true);
    }

    private void createControlsForSchemaList(List<NodeParameterSchemaRegistry.FieldSchema> schemas, JsonObject data,
            boolean paramsField) {
        for (int i = 0; i < schemas.size(); i++) {
            NodeParameterSchemaRegistry.FieldSchema schema = schemas.get(i);
            String value = readFieldValue(data, schema);
            switch (schema.getType()) {
                case BOOLEAN: {
                    int id = (paramsField ? BTN_PARAMS_TOGGLE_START : BTN_BASE_TOGGLE_START) + i;
                    ToggleGuiButton button = new ToggleGuiButton(id, 0, 0, 10, 18, buildToggleText(schema, value),
                            parseBoolean(value, "true".equalsIgnoreCase(schema.getDefaultValue())));
                    if (paramsField) {
                        paramToggleButtons.put(schema.getKey(), button);
                    } else {
                        baseToggleButtons.put(schema.getKey(), button);
                    }
                    this.buttonList.add(button);
                    break;
                }
                case SELECT:
                case GRAPH_REF: {
                    int id = (paramsField ? BTN_PARAMS_SELECT_START : BTN_BASE_SELECT_START) + i;
                    GuiButton button = new ThemedButton(id, 0, 0, 10, 18, "");
                    if (paramsField) {
                        paramSelectButtons.put(schema.getKey(), button);
                        paramSelectValues.put(schema.getKey(), value.isEmpty() ? firstOption(schema) : value);
                    } else {
                        baseSelectButtons.put(schema.getKey(), button);
                        baseSelectValues.put(schema.getKey(), value.isEmpty() ? firstOption(schema) : value);
                    }
                    updateSelectButtonText(schema, paramsField);
                    this.buttonList.add(button);
                    break;
                }
                default: {
                    GuiTextField field = new GuiTextField(paramsField ? 20000 + i : 10000 + i, this.fontRenderer, 0, 0,
                            Math.max(80, panelW - 140), 18);
                    field.setMaxStringLength(Integer.MAX_VALUE);
                    field.setText(value == null ? "" : value);
                    field.setCursorPositionZero();
                    if (paramsField) {
                        paramTextFields.put(schema.getKey(), field);
                    } else {
                        baseTextFields.put(schema.getKey(), field);
                    }
                    break;
                }
            }
        }
    }

    private String readFieldValue(JsonObject data, NodeParameterSchemaRegistry.FieldSchema schema) {
        if (data != null && data.has(schema.getKey()) && !data.get(schema.getKey()).isJsonNull()) {
            JsonElement element = data.get(schema.getKey());
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
            return element.toString();
        }
        return schema.getDefaultValue();
    }

    private String buildToggleText(NodeParameterSchemaRegistry.FieldSchema schema, String value) {
        boolean enabled = parseBoolean(value, "true".equalsIgnoreCase(schema.getDefaultValue()));
        return schema.getLabel() + ": " + (enabled ? "§a开" : "§c关");
    }

    private void updateSelectButtonText(NodeParameterSchemaRegistry.FieldSchema schema, boolean paramsField) {
        GuiButton button = paramsField ? paramSelectButtons.get(schema.getKey()) : baseSelectButtons.get(schema.getKey());
        if (button == null) {
            return;
        }
        String value = paramsField ? paramSelectValues.get(schema.getKey()) : baseSelectValues.get(schema.getKey());
        String displayValue = value == null || value.trim().isEmpty() ? "(空)" : getDisplayOptionText(schema, value);
        button.displayString = schema.getLabel() + ": §b" + displayValue + " §7▼";
    }

    private String getDisplayOptionText(NodeParameterSchemaRegistry.FieldSchema schema, String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        if (schema == null) {
            return value;
        }
        if (schema.getType() == NodeParameterSchemaRegistry.FieldType.GRAPH_REF) {
            return value;
        }
        String key = schema.getKey() == null ? "" : schema.getKey().trim();
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);

        if ("actionType".equals(key)) {
            if ("command".equals(normalized)) return "执行命令";
            if ("system_message".equals(normalized)) return "系统提示";
            if ("delay".equals(normalized)) return "延迟";
            if ("key".equals(normalized)) return "按键";
            if ("jump".equals(normalized)) return "跳跃";
            if ("click".equals(normalized)) return "屏幕点击";
            if ("window_click".equals(normalized)) return "容器内槽位点击";
            if ("conditional_window_click".equals(normalized)) return "容器内槽位点击";
            if ("setview".equals(normalized)) return "设置视角";
            if ("rightclickblock".equals(normalized)) return "右键方块";
            if ("rightclickentity".equals(normalized)) return "右键实体";
            if ("takeallitems".equals(normalized)) return "取出全部物品";
            if ("take_all_items_safe".equals(normalized)) return "安全取出全部物品";
            if ("dropfiltereditems".equals(normalized)) return "丢弃过滤物品";
            if ("autochestclick".equals(normalized)) return "自动点击箱子";
            if ("move_inventory_items_to_chest_slots".equals(normalized)) return "容器物品批量移动";
            if ("transferitemstowarehouse".equals(normalized)) return "转移物品到仓库";
            if ("warehouse_auto_deposit".equals(normalized)) return "仓库管理-自动存入";
            if ("blocknextgui".equals(normalized)) return "屏蔽GUI";
            if ("close_container_window".equals(normalized)) return "关闭当前容器窗口";
            if ("hud_text_check".equals(normalized)) return "GUI/HUD 文本识别";
            if ("autoeat".equals(normalized)) return "自动吃食物";
            if ("autoequip".equals(normalized)) return "自动穿戴";
            if ("autopickup".equals(normalized)) return "自动拾取";
            if ("runlastsequence".equals(normalized)) return "运行上次序列";
            if ("run_sequence".equals(normalized)) return "运行序列";
            if ("stop_current_sequence".equals(normalized)) return "停止当前执行序列";
            if ("restart_sequence".equals(normalized)) return "重头执行序列";
            if ("silentuse".equals(normalized)) return "静默使用物品";
            if ("switch_hotbar_slot".equals(normalized)) return "切换快捷栏";
            if ("use_hotbar_item".equals(normalized)) return "静默使用快捷栏物品";
            if ("move_inventory_item_to_hotbar".equals(normalized)) return "背包物品切到快捷栏";
            if ("use_held_item".equals(normalized)) return "使用手中物品";
            if ("hunt".equals(normalized)) return "中心搜怪击杀";
            if ("use_skill".equals(normalized)) return "施放技能";
            if ("send_packet".equals(normalized)) return "发送数据包";
        }

        if ("state".equals(key)) {
            if ("press".equals(normalized)) return "单击";
            if ("down".equals(normalized)) return "按下";
            if ("up".equals(normalized)) return "抬起";
        }

        if ("triggerType".equals(key)) {
            if ("onchat".equals(normalized)) return "聊天触发";
            if ("onpacket".equals(normalized)) return "数据包触发";
            if ("onguiopen".equals(normalized)) return "界面打开触发";
            if ("onhplow".equals(normalized)) return "低血量触发";
            if ("ontimer".equals(normalized)) return "定时触发";
            if ("oninventoryfull".equals(normalized)) return "背包已满触发";
        }

        if ("mode".equals(key)) {
            if ("simple".equals(normalized)) return "简单模式";
            if ("advanced".equals(normalized)) return "高级模式";
        }

        if ("operator".equals(key)) {
            if ("==".equals(value)) return "等于 (==)";
            if ("!=".equals(value)) return "不等于 (!=)";
            if (">".equals(value)) return "大于 (>)";
            if ("<".equals(value)) return "小于 (<)";
            if (">=".equals(value)) return "大于等于 (>=)";
            if ("<=".equals(value)) return "小于等于 (<=)";
        }

        if ("rightType".equals(key) || "valueType".equals(key)) {
            if ("string".equals(normalized)) return "文本";
            if ("number".equals(normalized)) return "数字";
            if ("boolean".equals(normalized)) return "布尔";
        }

        if ("scope".equals(key)) {
            if ("global".equals(normalized)) return "全局";
            if ("local".equals(normalized)) return "局部";
            if ("graph".equals(normalized)) return "当前图";
        }

        if ("state".equals(key)) {
            if ("press".equals(normalized)) return "按一次";
            if ("down".equals(normalized)) return "按下";
            if ("up".equals(normalized)) return "抬起";
        }

        if ("slotBase".equals(key)) {
            if ("dec".equals(normalized)) return "十进制";
            if ("hex".equals(normalized)) return "十六进制";
        }

        if ("direction".equals(key)) {
            if ("c2s".equals(normalized)) return "客户端到服务端 (C2S)";
            if ("s2c".equals(normalized)) return "服务端到客户端 (S2C)";
        }

        if ("moveDirection".equals(key)) {
            if ("inventory_to_chest".equals(normalized)) return "背包 -> 容器";
            if ("chest_to_inventory".equals(normalized)) return "容器 -> 背包";
        }

        if ("clickType".equals(key)) {
            if ("pickup".equals(normalized)) return "普通点击";
            if ("quick_move".equals(normalized)) return "Shift快速移动";
            if ("swap".equals(normalized)) return "数字键交换";
            if ("throw".equals(normalized)) return "丢弃";
            if ("pickup_all".equals(normalized)) return "收集同类";
            if ("quick_craft".equals(normalized)) return "拖拽分发";
            if ("clone".equals(normalized)) return "创造复制";
        }

        if ("onError".equals(key)) {
            if ("stop".equals(normalized)) return "停止";
            if ("continue".equals(normalized)) return "继续";
            if ("goto".equals(normalized)) return "跳转到节点";
        }

        if ("left".equals(key) || "value".equals(key) || "onlyOnSlotChange".equals(key)) {
            if ("true".equals(normalized)) return "是";
            if ("false".equals(normalized)) return "否";
        }

        return value;
    }

    private String firstOption(NodeParameterSchemaRegistry.FieldSchema schema) {
        String[] options = schema.getOptions();
        return options != null && options.length > 0 ? options[0] : schema.getDefaultValue();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CANCEL) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BTN_SAVE) {
            saveAndClose();
            return;
        }

        handleSchemaButtonClick(button.id, false, BTN_BASE_SELECT_START, baseSchemas, baseSelectButtons, baseSelectValues);
        handleSchemaButtonClick(button.id, true, BTN_PARAMS_SELECT_START, paramSchemas, paramSelectButtons, paramSelectValues);
        handleToggleButtonClick(button.id, false, BTN_BASE_TOGGLE_START, baseSchemas, baseToggleButtons);
        handleToggleButtonClick(button.id, true, BTN_PARAMS_TOGGLE_START, paramSchemas, paramToggleButtons);
    }

    private void handleSchemaButtonClick(int buttonId, boolean paramsField, int startId,
            List<NodeParameterSchemaRegistry.FieldSchema> schemas, Map<String, GuiButton> buttons, Map<String, String> values) {
        if (buttonId < startId || buttonId >= startId + schemas.size()) {
            return;
        }
        int index = buttonId - startId;
        if (index < 0 || index >= schemas.size()) {
            return;
        }
        NodeParameterSchemaRegistry.FieldSchema schema = schemas.get(index);
        if (schema.getType() != NodeParameterSchemaRegistry.FieldType.SELECT
                && schema.getType() != NodeParameterSchemaRegistry.FieldType.GRAPH_REF) {
            return;
        }
        String[] options = schema.getOptions();
        if (options == null || options.length == 0) {
            return;
        }
        if (schema.getKey().equals(openDropdownKey) && paramsField == openDropdownParamsField) {
            closeDropdown();
        } else {
            openDropdownKey = schema.getKey();
            openDropdownParamsField = paramsField;
            dropdownSearchText = "";
        }
    }

    private void handleToggleButtonClick(int buttonId, boolean paramsField, int startId,
            List<NodeParameterSchemaRegistry.FieldSchema> schemas, Map<String, ToggleGuiButton> buttons) {
        if (buttonId < startId || buttonId >= startId + schemas.size()) {
            return;
        }
        int index = buttonId - startId;
        if (index < 0 || index >= schemas.size()) {
            return;
        }
        NodeParameterSchemaRegistry.FieldSchema schema = schemas.get(index);
        if (schema.getType() != NodeParameterSchemaRegistry.FieldType.BOOLEAN) {
            return;
        }
        ToggleGuiButton toggle = buttons.get(schema.getKey());
        if (toggle == null) {
            return;
        }
        toggle.setEnabledState(!toggle.getEnabledState());
        toggle.displayString = buildToggleText(schema, String.valueOf(toggle.getEnabledState()));
    }

    private void rebuildActionParamControls(String actionType) {
        removeDynamicParamButtons();
        paramSchemas.clear();
        closeDropdown();
        scrollOffset = 0;
        JsonObject currentParams = collectParamsObject();
        paramSchemas.addAll(NodeParameterSchemaRegistry.getSchemas(NodeNode.TYPE_ACTION, wrapActionType(actionType), graphNames));
        List<NodeParameterSchemaRegistry.FieldSchema> onlyParams = new ArrayList<>();
        for (NodeParameterSchemaRegistry.FieldSchema schema : paramSchemas) {
            if (schema.isParamsField()) {
                onlyParams.add(schema);
            }
        }
        paramSchemas.clear();
        paramSchemas.addAll(onlyParams);

        paramTextFields.clear();
        paramSelectButtons.clear();
        paramToggleButtons.clear();
        paramSelectValues.clear();

        createControlsForSchemaList(paramSchemas, currentParams, true);
    }

    private JsonObject wrapActionType(String actionType) {
        JsonObject data = new JsonObject();
        data.addProperty("actionType", actionType == null ? "" : actionType);
        JsonObject params = collectParamsObject();
        data.add("params", params);
        return data;
    }

    private void removeDynamicParamButtons() {
        List<GuiButton> removeList = new ArrayList<>();
        for (GuiButton button : this.buttonList) {
            if (button == null) {
                continue;
            }
            if ((button.id >= BTN_PARAMS_SELECT_START && button.id < BTN_PARAMS_SELECT_START + 500)
                    || (button.id >= BTN_PARAMS_TOGGLE_START && button.id < BTN_PARAMS_TOGGLE_START + 500)) {
                removeList.add(button);
            }
        }
        this.buttonList.removeAll(removeList);
    }

    private void saveAndClose() {
        try {
            JsonObject result = new JsonObject();

            for (NodeParameterSchemaRegistry.FieldSchema schema : baseSchemas) {
                applySchemaValue(result, schema, false);
            }

            JsonObject params = collectParamsObject();
            if (params.entrySet().size() > 0) {
                result.add("params", params);
            }

            normalizeSpecialData(result);

            if (onSave != null) {
                onSave.accept(result);
            }
            closeDropdown();
            this.mc.displayGuiScreen(parent);
        } catch (Exception e) {
            statusMessage = "保存失败: " + e.getMessage();
            statusColor = 0xFFFF8888;
        }
    }

    private JsonObject collectParamsObject() {
        JsonObject params = new JsonObject();
        for (NodeParameterSchemaRegistry.FieldSchema schema : paramSchemas) {
            applySchemaValue(params, schema, true);
        }
        return params;
    }

    private void applySchemaValue(JsonObject target, NodeParameterSchemaRegistry.FieldSchema schema, boolean paramsField) {
        String key = schema.getKey();
        switch (schema.getType()) {
            case BOOLEAN: {
                ToggleGuiButton toggle = paramsField ? paramToggleButtons.get(key) : baseToggleButtons.get(key);
                if (toggle != null) {
                    target.addProperty(key, toggle.getEnabledState());
                }
                break;
            }
            case NUMBER: {
                GuiTextField field = paramsField ? paramTextFields.get(key) : baseTextFields.get(key);
                String text = field == null ? "" : field.getText().trim();
                if (text.isEmpty()) {
                    return;
                }
                if (text.contains(".")) {
                    target.addProperty(key, Double.parseDouble(text));
                } else {
                    target.addProperty(key, Long.parseLong(text));
                }
                break;
            }
            case SELECT:
            case GRAPH_REF: {
                String value = paramsField ? paramSelectValues.get(key) : baseSelectValues.get(key);
                if (value != null && !value.trim().isEmpty()) {
                    target.addProperty(key, value);
                }
                break;
            }
            case KV_LINES: {
                GuiTextField field = paramsField ? paramTextFields.get(key) : baseTextFields.get(key);
                String text = field == null ? "" : field.getText();
                target.addProperty(key, text == null ? "" : text);
                break;
            }
            default: {
                GuiTextField field = paramsField ? paramTextFields.get(key) : baseTextFields.get(key);
                String text = field == null ? "" : field.getText();
                if (text != null && !text.isEmpty()) {
                    target.addProperty(key, text);
                }
                break;
            }
        }
    }

    private void normalizeSpecialData(JsonObject result) {
        String type = NodeNode.normalizeType(nodeType);

        if (NodeNode.TYPE_ACTION.equals(type)) {
            String actionType = readString(result, "actionType");
            if (actionType.isEmpty()) {
                result.addProperty("actionType", "command");
            }
            normalizeActionSpecificData(result, actionType);
        }

        if (NodeNode.TYPE_SET_VAR.equals(type)) {
            String scope = readString(result, "scope");
            if (scope.isEmpty()) {
                result.addProperty("scope", "global");
            }
        }

        if (NodeNode.TYPE_RUN_SEQUENCE.equals(type) || NodeNode.TYPE_SUBGRAPH.equals(type)) {
            String argsText = readString(result, "argsText");
            result.add("args", parseKeyValueLines(argsText));
        }

        if (NodeNode.TYPE_PARALLEL.equals(type)) {
            String branchesText = readString(result, "branchesText");
            result.add("branches", parseKeyValueLines(branchesText));
        }

        if (NodeNode.TYPE_IF.equals(type) || NodeNode.TYPE_WAIT_UNTIL.equals(type)) {
            String mode = readString(result, "mode");
            if ("advanced".equalsIgnoreCase(mode)) {
                if (result.has("leftVar")) {
                    result.remove("leftVar");
                }
                if (result.has("operator")) {
                    result.remove("operator");
                }
                if (result.has("right")) {
                    result.remove("right");
                }
                if (result.has("rightType")) {
                    result.remove("rightType");
                }
            } else {
                if (result.has("expression") && readString(result, "expression").trim().isEmpty()) {
                    result.remove("expression");
                }
            }
        }
    }

    private void normalizeActionSpecificData(JsonObject result, String actionType) {
        String normalizedAction = actionType == null ? "" : actionType.trim().toLowerCase(java.util.Locale.ROOT);
        if ("rightclickblock".equals(normalizedAction) || "rightclickentity".equals(normalizedAction)) {
            String posText = readString(result, "pos");
            JsonArray posArray = parsePositionArray(posText);
            if (posArray != null) {
                result.add("pos", posArray);
            }
        }
    }

    private JsonArray parsePositionArray(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return null;
        }
        JsonArray array = new JsonArray();
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                array.add(Double.parseDouble(trimmed));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return array;
    }

    private JsonObject parseKeyValueLines(String text) {
        JsonObject object = new JsonObject();
        if (text == null || text.trim().isEmpty()) {
            return object;
        }
        String[] lines = text.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (!key.isEmpty()) {
                object.addProperty(key, value);
            }
        }
        return object;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "节点参数表单 - " + NodeNode.getDisplayName(nodeType), this.fontRenderer);

        Gui.drawRect(panelX + 8, contentTop, panelX + panelW - 8, contentBottom, 0x3318202E);
        Gui.drawRect(panelX + 8, contentBottom + 2, panelX + panelW - 8, panelY + panelH - 28, 0x44212B38);
        Gui.drawRect(panelX + 8, contentBottom + 2, panelX + panelW - 8, contentBottom + 3, 0x665B87B0);

        refreshValidationMessages();
        hoverTooltip = "";
        int viewportHeight = Math.max(1, contentBottom - contentTop);
        contentHeight = computeContentHeightEstimate();
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        int contentY = contentTop + 6 - scrollOffset;
        int cursor = 0;

        cursor = drawSchemaSection("基础字段", baseSchemas, false, contentY, cursor, mouseX, mouseY);
        if (!paramSchemas.isEmpty()) {
            cursor += 6;
            cursor = drawSchemaSection("动作参数", paramSchemas, true, contentY, cursor, mouseX, mouseY);
        }

        contentHeight = cursor + 8;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
        if (maxScroll > 0) {
            int trackX = panelX + panelW - 10;
            int trackY = contentTop;
            int trackH = contentBottom - contentTop;
            int thumbH = Math.max(16, (int) (trackH * (trackH / (float) Math.max(trackH, contentHeight))));
            int thumbY = trackY + (int) ((trackH - thumbH) * (scrollOffset / (float) maxScroll));
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackH, thumbY, thumbH);
        }

        drawString(this.fontRenderer, statusMessage, panelX + 10, panelY + panelH - 20, statusColor);
        if (!hoverTooltip.isEmpty()) {
            this.fontRenderer.drawSplitString(hoverTooltip, panelX + 10, panelY + panelH - 44, panelW - 180, 0xFFBBBBBB);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiTextField field : baseTextFields.values()) {
            if (field != null && field.getVisible()) {
                drawThemedTextField(field);
            }
        }
        for (GuiTextField field : paramTextFields.values()) {
            if (field != null && field.getVisible()) {
                drawThemedTextField(field);
            }
        }

        drawValidationOverlays();
        drawOpenDropdown(mouseX, mouseY);
    }

    private int drawSchemaSection(String title, List<NodeParameterSchemaRegistry.FieldSchema> schemas, boolean paramsField,
            int contentY, int cursor, int mouseX, int mouseY) {
        int x = panelX + 12;
        int width = panelW - 28;

        int titleY = contentY + cursor;
        if (isRowVisible(titleY, 16)) {
            drawString(this.fontRenderer, "§e" + title, x, titleY, 0xFFFFFFFF);
        }
        cursor += 18;

        for (NodeParameterSchemaRegistry.FieldSchema schema : schemas) {
            int rowY = contentY + cursor;
            int rowH = schema.getType() == NodeParameterSchemaRegistry.FieldType.TEXTAREA
                    || schema.getType() == NodeParameterSchemaRegistry.FieldType.KV_LINES ? 42 : 24;

            layoutFieldControl(schema, paramsField, x + 88, rowY + 2, width - 92, rowH - 4);

            String validationMessage = getValidationMessage(schema, paramsField);
            if (isRowVisible(rowY, rowH)) {
                Gui.drawRect(x, rowY, x + width, rowY + rowH, validationMessage.isEmpty() ? 0x22273344 : 0x332E1C1C);
                String label = schema.getLabel() + (schema.isRequired() ? " §c*" : "");
                drawString(this.fontRenderer, label, x + 4, rowY + 7, validationMessage.isEmpty() ? 0xFFEDEDED : 0xFFFFA8A8);
                if (!validationMessage.isEmpty()) {
                    drawString(this.fontRenderer, "!", x + width - 8, rowY + 7, 0xFFFF6666);
                }
                if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + rowH) {
                    hoverTooltip = schema.getTooltip() + (schema.getValidationRule().isEmpty() ? ""
                            : "  规则: " + schema.getValidationRule())
                            + (validationMessage.isEmpty() ? "" : "  当前错误: " + validationMessage);
                }
            }
            cursor += rowH + 4;
        }

        return cursor;
    }

    private void layoutFieldControl(NodeParameterSchemaRegistry.FieldSchema schema, boolean paramsField,
            int x, int y, int width, int height) {
        switch (schema.getType()) {
            case BOOLEAN: {
                ToggleGuiButton button = paramsField ? paramToggleButtons.get(schema.getKey()) : baseToggleButtons.get(schema.getKey());
                if (button != null) {
                    button.x = x;
                    button.y = y;
                    button.width = width;
                    button.height = height;
                    button.visible = isControlFullyVisible(y, height);
                    button.enabled = button.visible;
                    button.visible = isControlFullyVisible(y, height);
                    button.enabled = button.visible;
                }
                break;
            }
            case SELECT:
            case GRAPH_REF: {
                GuiButton button = paramsField ? paramSelectButtons.get(schema.getKey()) : baseSelectButtons.get(schema.getKey());
                if (button != null) {
                    button.x = x;
                    button.y = y;
                    button.width = width;
                    button.height = height;
                    button.visible = isControlFullyVisible(y, height);
                    button.enabled = button.visible;
                }
                break;
            }
            default: {
                GuiTextField field = paramsField ? paramTextFields.get(schema.getKey()) : baseTextFields.get(schema.getKey());
                if (field != null) {
                    field.x = x;
                    field.y = y;
                    field.width = width;
                    field.height = height;
                    boolean visible = isControlFullyVisible(y, height);
                    field.setVisible(visible);
                    if (!visible && field.isFocused()) {
                        field.setFocused(false);
                    }
                    if (visible && !field.isFocused()) {
                        field.setCursorPositionZero();
                    }
                }
                break;
            }
        }
    }

    private boolean isRowVisible(int y, int h) {
        return y >= contentTop && y + h <= contentBottom;
    }

    private boolean isControlFullyVisible(int y, int h) {
        return y >= contentTop && y + h <= contentBottom;
    }

    private int computeContentHeightEstimate() {
        int cursor = 0;
        cursor = accumulateSectionHeight(baseSchemas, cursor);
        if (!paramSchemas.isEmpty()) {
            cursor += 6;
            cursor = accumulateSectionHeight(paramSchemas, cursor);
        }
        return cursor + 8;
    }

    private int accumulateSectionHeight(List<NodeParameterSchemaRegistry.FieldSchema> schemas, int cursor) {
        cursor += 18;
        for (NodeParameterSchemaRegistry.FieldSchema schema : schemas) {
            int rowH = schema.getType() == NodeParameterSchemaRegistry.FieldType.TEXTAREA
                    || schema.getType() == NodeParameterSchemaRegistry.FieldType.KV_LINES ? 42 : 24;
            cursor += rowH + 4;
        }
        return cursor;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (openDropdownKey != null) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                closeDropdown();
                return;
            }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN || keyCode == org.lwjgl.input.Keyboard.KEY_NUMPADENTER) {
                DropdownContext ctx = getOpenDropdownContext();
                List<String> filtered = getFilteredOptions(ctx);
                if (!filtered.isEmpty()) {
                    applyDropdownSelection(ctx, filtered.get(0));
                }
                return;
            }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_BACK) {
                if (!dropdownSearchText.isEmpty()) {
                    dropdownSearchText = dropdownSearchText.substring(0, dropdownSearchText.length() - 1);
                }
                return;
            }
            if (!GuiScreen.isCtrlKeyDown() && !GuiScreen.isAltKeyDown() && typedChar >= 32 && typedChar != 127) {
                dropdownSearchText += typedChar;
                return;
            }
        }

        for (GuiTextField field : baseTextFields.values()) {
            if (field != null && field.getVisible() && field.isFocused()) {
                field.textboxKeyTyped(typedChar, keyCode);
                return;
            }
        }
        for (GuiTextField field : paramTextFields.values()) {
            if (field != null && field.getVisible() && field.isFocused()) {
                field.textboxKeyTyped(typedChar, keyCode);
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && handleDropdownOptionClick(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : baseTextFields.values()) {
            if (field != null && field.getVisible()) {
                field.mouseClicked(mouseX, mouseY, mouseButton);
            } else if (field != null) {
                field.setFocused(false);
            }
        }
        for (GuiTextField field : paramTextFields.values()) {
            if (field != null && field.getVisible()) {
                field.mouseClicked(mouseX, mouseY, mouseButton);
            } else if (field != null) {
                field.setFocused(false);
            }
        }
        if (mouseButton == 0 && openDropdownKey != null && !isMouseOverOpenDropdown(mouseX, mouseY)
                && !isMouseOverOpenDropdownButton(mouseX, mouseY)) {
            closeDropdown();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = org.lwjgl.input.Mouse.getX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - org.lwjgl.input.Mouse.getY() * this.height / this.mc.displayHeight - 1;
        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < contentTop || mouseY > contentBottom) {
            return;
        }
        if (dWheel > 0) {
            scrollOffset -= 16;
        } else {
            scrollOffset += 16;
        }
        scrollOffset = MathHelper.clamp(scrollOffset, 0,
                Math.max(0, contentHeight - Math.max(1, contentBottom - contentTop)));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : baseTextFields.values()) {
            field.updateCursorCounter();
        }
        for (GuiTextField field : paramTextFields.values()) {
            field.updateCursorCounter();
        }
    }

    private void drawOpenDropdown(int mouseX, int mouseY) {
        DropdownContext ctx = getOpenDropdownContext();
        if (ctx == null || ctx.button == null || ctx.options.length == 0 || !ctx.button.visible) {
            return;
        }
        List<String> filtered = getFilteredOptions(ctx);
        int[] bounds = getDropdownBounds(ctx, filtered.size());
        int itemHeight = 18;
        int searchHeight = 18;
        int listX = bounds[0];
        int listY = bounds[1];
        int listW = bounds[2];
        int visibleCount = bounds[3];
        int listH = searchHeight + visibleCount * itemHeight;

        Gui.drawRect(listX, listY, listX + listW, listY + listH, 0xEE1E2633);
        Gui.drawRect(listX, listY, listX + listW, listY + 1, 0xFF5B87B0);
        Gui.drawRect(listX, listY + listH - 1, listX + listW, listY + listH, 0xFF5B87B0);
        Gui.drawRect(listX, listY, listX + 1, listY + listH, 0xFF5B87B0);
        Gui.drawRect(listX + listW - 1, listY, listX + listW, listY + listH, 0xFF5B87B0);

        Gui.drawRect(listX + 1, listY + 1, listX + listW - 1, listY + searchHeight, 0x4430475F);
        String searchLabel = dropdownSearchText.isEmpty() ? "搜索: (输入筛选)" : "搜索: " + dropdownSearchText;
        drawString(this.fontRenderer, searchLabel, listX + 6, listY + 5, 0xFFE6EEF8);

        if (filtered.isEmpty()) {
            int rowY = listY + searchHeight;
            if (visibleCount > 0) {
                Gui.drawRect(listX + 1, rowY, listX + listW - 1, rowY + itemHeight, 0x22111111);
                drawString(this.fontRenderer, "(无匹配项)", listX + 6, rowY + 5, 0xFFAAAAAA);
            }
            return;
        }

        for (int i = 0; i < visibleCount; i++) {
            int rowY = listY + searchHeight + i * itemHeight;
            String option = filtered.get(i);
            String displayOption = getDisplayOptionText(ctx.schema, option);
            boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= rowY && mouseY < rowY + itemHeight;
            boolean selected = option.equalsIgnoreCase(ctx.currentValue);
            int bg = selected ? 0xAA2D6A9F : (hovered ? 0x88405A78 : 0x00000000);
            if (bg != 0) {
                Gui.drawRect(listX + 1, rowY, listX + listW - 1, rowY + itemHeight, bg);
            }
            drawString(this.fontRenderer, displayOption, listX + 6, rowY + 5, selected ? 0xFFFFFFFF : 0xFFDDDDDD);
        }
    }

    private boolean handleDropdownOptionClick(int mouseX, int mouseY) {
        DropdownContext ctx = getOpenDropdownContext();
        if (ctx == null || ctx.button == null || ctx.options.length == 0 || !ctx.button.visible) {
            return false;
        }
        List<String> filtered = getFilteredOptions(ctx);
        int[] bounds = getDropdownBounds(ctx, filtered.size());
        int itemHeight = 18;
        int searchHeight = 18;
        int listX = bounds[0];
        int listY = bounds[1];
        int listW = bounds[2];
        int visibleCount = bounds[3];
        int listH = searchHeight + visibleCount * itemHeight;
        if (mouseX < listX || mouseX >= listX + listW || mouseY < listY || mouseY >= listY + listH) {
            return false;
        }
        if (mouseY < listY + searchHeight) {
            return true;
        }
        int index = (mouseY - listY - searchHeight) / itemHeight;
        if (index < 0 || index >= visibleCount || index >= filtered.size()) {
            return false;
        }
        applyDropdownSelection(ctx, filtered.get(index));
        return true;
    }

    private boolean isMouseOverOpenDropdown(int mouseX, int mouseY) {
        DropdownContext ctx = getOpenDropdownContext();
        if (ctx == null || ctx.button == null || ctx.options.length == 0 || !ctx.button.visible) {
            return false;
        }
        List<String> filtered = getFilteredOptions(ctx);
        int[] bounds = getDropdownBounds(ctx, filtered.size());
        int itemHeight = 18;
        int searchHeight = 18;
        int listX = bounds[0];
        int listY = bounds[1];
        int listW = bounds[2];
        int listH = searchHeight + bounds[3] * itemHeight;
        return mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH;
    }

    private int[] getDropdownBounds(DropdownContext ctx, int optionCount) {
        int itemHeight = 18;
        int searchHeight = 18;
        int listX = ctx.button.x;
        int listW = ctx.button.width;

        int belowY = ctx.button.y + ctx.button.height + 1;
        int belowSpace = Math.max(0, contentBottom - belowY);
        int aboveSpace = Math.max(0, ctx.button.y - contentTop - 1);

        boolean preferBelow = belowSpace >= searchHeight + itemHeight || belowSpace >= aboveSpace;
        int availableSpace = preferBelow ? belowSpace : aboveSpace;
        int maxVisibleBySpace = Math.max(0, (availableSpace - searchHeight) / itemHeight);
        int visibleCount = Math.min(optionCount, 8);
        if (maxVisibleBySpace > 0) {
            visibleCount = Math.min(visibleCount, maxVisibleBySpace);
        } else {
            visibleCount = Math.min(visibleCount, 1);
        }

        int listH = searchHeight + visibleCount * itemHeight;
        int listY = preferBelow ? belowY : (ctx.button.y - listH - 1);
        if (listY + listH > contentBottom) {
            listY = Math.max(contentTop, contentBottom - listH);
        }
        if (listY < contentTop) {
            listY = contentTop;
        }
        return new int[] { listX, listY, listW, visibleCount };
    }

    private boolean isMouseOverOpenDropdownButton(int mouseX, int mouseY) {
        DropdownContext ctx = getOpenDropdownContext();
        if (ctx == null || ctx.button == null || !ctx.button.visible) {
            return false;
        }
        return mouseX >= ctx.button.x && mouseX < ctx.button.x + ctx.button.width
                && mouseY >= ctx.button.y && mouseY < ctx.button.y + ctx.button.height;
    }

    private DropdownContext getOpenDropdownContext() {
        if (openDropdownKey == null || openDropdownKey.trim().isEmpty()) {
            return null;
        }
        List<NodeParameterSchemaRegistry.FieldSchema> schemas = openDropdownParamsField ? paramSchemas : baseSchemas;
        Map<String, GuiButton> buttons = openDropdownParamsField ? paramSelectButtons : baseSelectButtons;
        Map<String, String> values = openDropdownParamsField ? paramSelectValues : baseSelectValues;
        for (NodeParameterSchemaRegistry.FieldSchema schema : schemas) {
            if (schema != null && openDropdownKey.equals(schema.getKey())
                    && (schema.getType() == NodeParameterSchemaRegistry.FieldType.SELECT
                            || schema.getType() == NodeParameterSchemaRegistry.FieldType.GRAPH_REF)) {
                DropdownContext ctx = new DropdownContext();
                ctx.schema = schema;
                ctx.button = buttons.get(schema.getKey());
                ctx.values = values;
                ctx.currentValue = values.get(schema.getKey());
                ctx.options = schema.getOptions() == null ? new String[0] : schema.getOptions();
                ctx.paramsField = openDropdownParamsField;
                return ctx;
            }
        }
        return null;
    }

    private List<String> getFilteredOptions(DropdownContext ctx) {
        List<String> filtered = new ArrayList<>();
        if (ctx == null || ctx.options == null) {
            return filtered;
        }
        String keyword = dropdownSearchText == null ? "" : dropdownSearchText.trim().toLowerCase(java.util.Locale.ROOT);
        for (String option : ctx.options) {
            String text = option == null ? "" : option;
            String displayText = getDisplayOptionText(ctx.schema, text);
            String searchText = (text + " " + displayText).toLowerCase(java.util.Locale.ROOT);
            if (keyword.isEmpty() || searchText.contains(keyword)) {
                filtered.add(text);
            }
        }
        return filtered;
    }

    private void applyDropdownSelection(DropdownContext ctx, String selected) {
        if (ctx == null) {
            return;
        }
        ctx.values.put(ctx.schema.getKey(), selected);
        updateSelectButtonText(ctx.schema, ctx.paramsField);
        closeDropdown();

        if (!ctx.paramsField && "actionType".equals(ctx.schema.getKey())) {
            rebuildActionParamControls(selected);
        }
    }

    private void closeDropdown() {
        openDropdownKey = null;
        dropdownSearchText = "";
    }

    private void refreshValidationMessages() {
        baseValidationMessages.clear();
        paramValidationMessages.clear();
        collectValidationMessages(baseSchemas, false, baseValidationMessages);
        collectValidationMessages(paramSchemas, true, paramValidationMessages);
    }

    private void collectValidationMessages(List<NodeParameterSchemaRegistry.FieldSchema> schemas, boolean paramsField,
            Map<String, String> target) {
        for (NodeParameterSchemaRegistry.FieldSchema schema : schemas) {
            if (schema == null) {
                continue;
            }
            String message = validateSchemaValue(schema, paramsField);
            if (!message.isEmpty()) {
                target.put(schema.getKey(), message);
            }
        }
    }

    private String validateSchemaValue(NodeParameterSchemaRegistry.FieldSchema schema, boolean paramsField) {
        if (schema == null) {
            return "";
        }
        String key = schema.getKey();
        switch (schema.getType()) {
            case NUMBER: {
                GuiTextField field = paramsField ? paramTextFields.get(key) : baseTextFields.get(key);
                String text = field == null ? "" : field.getText().trim();
                if (text.isEmpty()) {
                    return schema.isRequired() ? "该字段不能为空" : "";
                }
                try {
                    Double.parseDouble(text);
                    return "";
                } catch (NumberFormatException ex) {
                    return "必须为数字";
                }
            }
            case SELECT:
            case GRAPH_REF: {
                String value = paramsField ? paramSelectValues.get(key) : baseSelectValues.get(key);
                return schema.isRequired() && (value == null || value.trim().isEmpty()) ? "该字段不能为空" : "";
            }
            case BOOLEAN:
                return "";
            default: {
                GuiTextField field = paramsField ? paramTextFields.get(key) : baseTextFields.get(key);
                String text = field == null ? "" : field.getText().trim();
                return schema.isRequired() && text.isEmpty() ? "该字段不能为空" : "";
            }
        }
    }

    private String getValidationMessage(NodeParameterSchemaRegistry.FieldSchema schema, boolean paramsField) {
        if (schema == null) {
            return "";
        }
        Map<String, String> messages = paramsField ? paramValidationMessages : baseValidationMessages;
        String message = messages.get(schema.getKey());
        return message == null ? "" : message;
    }

    private void drawValidationOverlays() {
        drawValidationOverlayForSchemas(baseSchemas, false);
        drawValidationOverlayForSchemas(paramSchemas, true);
    }

    private void drawValidationOverlayForSchemas(List<NodeParameterSchemaRegistry.FieldSchema> schemas, boolean paramsField) {
        for (NodeParameterSchemaRegistry.FieldSchema schema : schemas) {
            String message = getValidationMessage(schema, paramsField);
            if (message.isEmpty()) {
                continue;
            }
            drawValidationOverlay(schema, paramsField);
        }
    }

    private void drawValidationOverlay(NodeParameterSchemaRegistry.FieldSchema schema, boolean paramsField) {
        if (schema == null) {
            return;
        }
        String key = schema.getKey();
        int x = -1;
        int y = -1;
        int w = 0;
        int h = 0;

        switch (schema.getType()) {
            case BOOLEAN: {
                ToggleGuiButton button = paramsField ? paramToggleButtons.get(key) : baseToggleButtons.get(key);
                if (button == null || !button.visible) {
                    return;
                }
                x = button.x;
                y = button.y;
                w = button.width;
                h = button.height;
                break;
            }
            case SELECT:
            case GRAPH_REF: {
                GuiButton button = paramsField ? paramSelectButtons.get(key) : baseSelectButtons.get(key);
                if (button == null || !button.visible) {
                    return;
                }
                x = button.x;
                y = button.y;
                w = button.width;
                h = button.height;
                break;
            }
            default: {
                GuiTextField field = paramsField ? paramTextFields.get(key) : baseTextFields.get(key);
                if (field == null || !field.getVisible()) {
                    return;
                }
                x = field.x;
                y = field.y;
                w = field.width;
                h = field.height;
                break;
            }
        }

        Gui.drawRect(x - 2, y - 2, x + w + 2, y - 1, 0xCCFF6666);
        Gui.drawRect(x - 2, y + h + 1, x + w + 2, y + h + 2, 0xCCFF6666);
        Gui.drawRect(x - 2, y - 1, x - 1, y + h + 1, 0xCCFF6666);
        Gui.drawRect(x + w + 1, y - 1, x + w + 2, y + h + 1, 0xCCFF6666);
    }

    private boolean parseBoolean(String text, boolean fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        return Boolean.parseBoolean(text.trim());
    }

    private String readString(JsonObject data, String key) {
        if (data != null && key != null && data.has(key) && !data.get(key).isJsonNull()) {
            return data.get(key).isJsonPrimitive() ? data.get(key).getAsString() : data.get(key).toString();
        }
        return "";
    }

    private static final class DropdownContext {
        private NodeParameterSchemaRegistry.FieldSchema schema;
        private GuiButton button;
        private Map<String, String> values;
        private String currentValue;
        private String[] options;
        private boolean paramsField;
    }
}
