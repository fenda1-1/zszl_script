package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.template.LegacyActionTemplateManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiLegacyActionTemplateManager extends ThemedGuiScreen {

    private final GuiScreen parent;
    private final List<LegacyActionTemplateManager.TemplateEditModel> templates = new ArrayList<>();

    private int selected = -1;
    private int scroll = 0;
    private static final int ROW_H = 28;

    private GuiTextField nameField;
    private GuiTextField sequenceField;
    private GuiTextField defaultsField;
    private GuiTextField noteField;
    private GuiButton selectSequenceBtn;

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int editorX;
    private int editorW;
    private boolean workingCopyInitialized = false;

    public GuiLegacyActionTemplateManager(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        if (!workingCopyInitialized) {
            reloadWorkingCopy();
            workingCopyInitialized = true;
        }

        panelW = Math.min(980, this.width - 30);
        panelX = (this.width - panelW) / 2;
        panelY = 30;
        panelH = this.height - 52;
        listX = panelX + 10;
        listY = panelY + 36;
        listW = 320;
        listH = this.height - panelY - 92;

        editorX = listX + listW + 12;
        editorW = panelX + panelW - editorX - 10;
        int y = listY;
        nameField = createField(5001, editorX, y, editorW, 18);
        y += 24;
        int buttonW = Math.max(80, Math.min(96, editorW / 3));
        if (editorW < buttonW + 6 + 110) {
            sequenceField = createField(5002, editorX, y, editorW, 18);
            selectSequenceBtn = new ThemedButton(20, editorX, y + 23, editorW, 20, editorW < 108 ? "选择" : "选择序列");
            y += 48;
        } else {
            int inputW = Math.max(1, editorW - buttonW - 6);
            sequenceField = createField(5002, editorX, y, inputW, 18);
            selectSequenceBtn = new ThemedButton(20, editorX + inputW + 6, y - 1, buttonW, 20,
                    buttonW < 108 ? "选择" : "选择序列");
            y += 24;
        }
        buttonList.add(selectSequenceBtn);
        defaultsField = createField(5003, editorX, y, editorW, 18);
        defaultsField.setMaxStringLength(Integer.MAX_VALUE);
        y += 24;
        noteField = createField(5004, editorX, y, editorW, 18);
        noteField.setMaxStringLength(Integer.MAX_VALUE);

        int btnY = this.height - 32;
        buttonList.add(new ThemedButton(1, listX, btnY, 72, 20, "新增"));
        buttonList.add(new ThemedButton(2, listX + 78, btnY, 72, 20, "删除"));
        buttonList.add(new ThemedButton(3, listX + 156, btnY, 72, 20, "重载"));
        buttonList.add(new ThemedButton(7, listX + 234, btnY, 72, 20, "导出"));
        buttonList.add(new ThemedButton(4, panelX + panelW - 258, btnY, 78, 20, "校验"));
        buttonList.add(new ThemedButton(8, panelX + panelW - 344, btnY, 78, 20, "导入"));
        buttonList.add(new ThemedButton(5, panelX + panelW - 174, btnY, 78, 20, "保存"));
        buttonList.add(new ThemedButton(6, panelX + panelW - 90, btnY, 78, 20, "返回"));

        if (selected >= templates.size()) {
            selected = templates.isEmpty() ? -1 : 0;
        }
        if (selected >= 0) {
            loadFromTemplate(templates.get(selected));
        } else {
            clearEditor();
        }
    }

    private GuiTextField createField(int id, int x, int y, int width, int height) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, width, height);
        field.setMaxStringLength(256);
        return field;
    }

    private void reloadWorkingCopy() {
        templates.clear();
        templates.addAll(LegacyActionTemplateManager.getTemplateModels());
    }

    private void loadFromTemplate(LegacyActionTemplateManager.TemplateEditModel model) {
        if (model == null) {
            clearEditor();
            return;
        }
        nameField.setText(safe(model.name));
        sequenceField.setText(safe(model.sequenceName));
        defaultsField.setText(safe(model.defaultsText));
        noteField.setText(safe(model.note));
    }

    private void clearEditor() {
        nameField.setText("");
        sequenceField.setText("");
        defaultsField.setText("");
        noteField.setText("");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                LegacyActionTemplateManager.TemplateEditModel created = new LegacyActionTemplateManager.TemplateEditModel();
                created.name = "template_" + (templates.size() + 1);
                templates.add(created);
                selected = templates.size() - 1;
                loadFromTemplate(created);
                break;
            case 2:
                if (selected >= 0 && selected < templates.size()) {
                    templates.remove(selected);
                    selected = templates.isEmpty() ? -1 : Math.min(selected, templates.size() - 1);
                    if (selected >= 0) {
                        loadFromTemplate(templates.get(selected));
                    } else {
                        clearEditor();
                    }
                }
                break;
            case 3:
                reloadWorkingCopy();
                selected = templates.isEmpty() ? -1 : 0;
                if (selected >= 0) {
                    loadFromTemplate(templates.get(selected));
                } else {
                    clearEditor();
                }
                break;
            case 4:
                flushEditorToSelected();
                String error = validateTemplates();
                toast(error == null ? ChatFormatting.GREEN + "模板校验通过" : ChatFormatting.RED + "校验失败: " + error);
                break;
            case 7:
                flushEditorToSelected();
                if (selected < 0 || selected >= templates.size()) {
                    toast(ChatFormatting.RED + "请先选择一个模板");
                    break;
                }
                String exported = LegacyActionTemplateManager.exportTemplate(templates.get(selected).name);
                if (exported == null || exported.trim().isEmpty()) {
                    toast(ChatFormatting.RED + "导出失败");
                    break;
                }
                GuiScreen.setClipboardString(exported);
                toast(ChatFormatting.GREEN + "模板已复制到剪贴板");
                break;
            case 8:
                flushEditorToSelected();
                mc.setScreen(new GuiTextInput(this, "粘贴模板文本", value -> {
                    String importedName = LegacyActionTemplateManager.importTemplate(value);
                    reloadWorkingCopy();
                    selected = templates.isEmpty() ? -1 : 0;
                    if (!importedName.isEmpty()) {
                        for (int i = 0; i < templates.size(); i++) {
                            LegacyActionTemplateManager.TemplateEditModel model = templates.get(i);
                            if (model != null && importedName.equalsIgnoreCase(safe(model.name))) {
                                selected = i;
                                break;
                            }
                        }
                    }
                    if (selected >= 0 && selected < templates.size()) {
                        loadFromTemplate(templates.get(selected));
                    } else {
                        clearEditor();
                    }
                    toast(importedName.isEmpty() ? ChatFormatting.RED + "导入失败" : ChatFormatting.GREEN + "导入模板: " + importedName);
                    mc.setScreen(this);
                }));
                break;
            case 5:
                flushEditorToSelected();
                String validation = validateTemplates();
                if (validation != null) {
                    toast(ChatFormatting.RED + "保存失败: " + validation);
                    break;
                }
                LegacyActionTemplateManager.saveTemplateModels(templates);
                toast(ChatFormatting.GREEN + "模板已保存");
                break;
            case 6:
                mc.setScreen(parent);
                break;
            case 20:
                flushEditorToSelected();
                mc.setScreen(new GuiSequenceSelector(this, seq -> {
                    String selectedSequence = seq == null ? "" : seq;
                    sequenceField.setText(selectedSequence);
                    if (selected >= 0 && selected < templates.size()) {
                        templates.get(selected).sequenceName = selectedSequence;
                    }
                    mc.setScreen(this);
                }));
                break;
            default:
                break;
        }
    }

    private String validateTemplates() {
        for (LegacyActionTemplateManager.TemplateEditModel model : templates) {
            if (model == null) {
                continue;
            }
            if (safe(model.name).trim().isEmpty()) {
                return "存在未命名模板";
            }
            if (safe(model.sequenceName).trim().isEmpty()) {
                return "模板未绑定序列: " + model.name;
            }
            if (PathSequenceManager.getSequence(model.sequenceName) == null) {
                return "模板目标序列不存在: " + model.sequenceName;
            }
        }
        return null;
    }

    private void flushEditorToSelected() {
        if (selected < 0 || selected >= templates.size()) {
            return;
        }
        LegacyActionTemplateManager.TemplateEditModel model = templates.get(selected);
        model.name = safe(nameField.getText()).trim();
        model.sequenceName = safe(sequenceField.getText()).trim();
        model.defaultsText = safe(defaultsField.getText());
        model.note = safe(noteField.getText());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "动作模板库", fontRenderer);
        drawTemplateList(mouseX, mouseY);
        drawEditor();
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawThemedTextField(nameField);
        drawThemedTextField(sequenceField);
        drawThemedTextField(defaultsField);
        drawThemedTextField(noteField);
    }

    private void drawTemplateList(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(listX, listY, listW, listH, false, true);
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, templates.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < visible; i++) {
            int actual = i + scroll;
            if (actual >= templates.size()) {
                break;
            }
            int rowY = listY + i * ROW_H;
            LegacyActionTemplateManager.TemplateEditModel model = templates.get(actual);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + ROW_H;
            boolean selectedRow = actual == selected;
            GuiTheme.drawButtonFrameSafe(listX + 2, rowY + 1, listW - 4, ROW_H - 2,
                    selectedRow ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            drawString(fontRenderer, safe(model.name).trim().isEmpty() ? "(未命名模板)" : model.name, listX + 8, rowY + 6,
                    0xFFEAF7FF);
            drawString(fontRenderer, safe(model.sequenceName), listX + 8, rowY + 18, 0xFF97ACC0);
        }
    }

    private void drawEditor() {
        drawString(fontRenderer, "模板名", nameField.x, nameField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "目标序列", sequenceField.x, sequenceField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "默认参数(key=value)", defaultsField.x, defaultsField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "备注", noteField.x, noteField.y - 10, 0xFFFFFF);
        fontRenderer.drawSplitString("默认参数支持换行或分号分隔，例如: item=以太龙珠; count=3; scope=global",
                editorX, noteField.y + 28, editorW, 0xFFB8C7D9);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        sequenceField.mouseClicked(mouseX, mouseY, mouseButton);
        defaultsField.mouseClicked(mouseX, mouseY, mouseButton);
        noteField.mouseClicked(mouseX, mouseY, mouseButton);
        int visible = Math.max(1, listH / ROW_H);
        for (int i = 0; i < visible; i++) {
            int actual = i + scroll;
            if (actual >= templates.size()) {
                break;
            }
            int rowY = listY + i * ROW_H;
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + ROW_H) {
                flushEditorToSelected();
                selected = actual;
                loadFromTemplate(templates.get(selected));
                break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        nameField.textboxKeyTyped(typedChar, keyCode);
        sequenceField.textboxKeyTyped(typedChar, keyCode);
        defaultsField.textboxKeyTyped(typedChar, keyCode);
        noteField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            return;
        }
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, templates.size() - visible);
        if (dWheel > 0) {
            scroll = Math.max(0, scroll - 1);
        } else {
            scroll = Math.min(maxScroll, scroll + 1);
        }
    }

    private void toast(String message) {
        if (mc != null && mc.player != null && message != null && !message.trim().isEmpty()) {
            mc.player.displayClientMessage(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString(message), false);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}








