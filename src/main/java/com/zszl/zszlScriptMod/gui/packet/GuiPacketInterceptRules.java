package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PacketInterceptManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiPacketInterceptRules extends ThemedGuiScreen {

    private final GuiScreen parent;
    private final List<PacketInterceptConfig.InterceptRule> rules = new ArrayList<>();

    private int selected = -1;
    private int scroll = 0;

    private GuiTextField nameField;
    private GuiTextField packetFilterField;
    private GuiTextField channelField;
    private GuiTextField matchField;
    private GuiTextField replaceField;

    private GuiButton toggleGlobalBtn;
    private GuiButton toggleRuleEnabledBtn;
    private GuiButton toggleRegexBtn;
    private GuiButton toggleReplaceAllBtn;

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private static final int ROW_H = 24;

    private boolean editingEnabled = true;
    private boolean editingRegex = false;
    private boolean editingReplaceAll = true;

    public GuiPacketInterceptRules(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        reloadWorkingCopy();

        int panelW = Math.min(930, this.width - 30);
        int panelX = (this.width - panelW) / 2;
        int panelY = 30;

        listX = panelX + 10;
        listY = panelY + 36;
        listW = 310;
        listH = this.height - panelY - 76;

        int rightX = listX + listW + 12;
        int rightW = panelX + panelW - rightX - 10;

        int y = listY;
        toggleGlobalBtn = new ThemedButton(10, rightX, y, rightW, 20, I18n.format("gui.packet.intercept.global",
                formatBool(PacketInterceptConfig.INSTANCE.inboundInterceptEnabled)));
        buttonList.add(toggleGlobalBtn);
        y += 26;

        nameField = new GuiTextField(2001, fontRenderer, rightX, y, rightW, 18);
        nameField.setMaxStringLength(80);
        y += 24;

        packetFilterField = new GuiTextField(2005, fontRenderer, rightX, y, rightW, 18);
        packetFilterField.setMaxStringLength(120);
        y += 24;

        channelField = new GuiTextField(2002, fontRenderer, rightX, y, rightW, 18);
        channelField.setMaxStringLength(120);
        y += 24;

        toggleRuleEnabledBtn = new ThemedButton(11, rightX, y, (rightW - 6) / 2, 20,
                I18n.format("gui.packet.intercept.rule_enabled", formatBool(true)));
        toggleReplaceAllBtn = new ThemedButton(12, rightX + (rightW + 6) / 2, y, (rightW - 6) / 2, 20,
                I18n.format("gui.packet.intercept.replace_all", formatBool(true)));
        buttonList.add(toggleRuleEnabledBtn);
        buttonList.add(toggleReplaceAllBtn);
        y += 26;

        toggleRegexBtn = new ThemedButton(13, rightX, y, rightW, 20,
                I18n.format("gui.packet.intercept.regex", formatBool(false)));
        buttonList.add(toggleRegexBtn);
        y += 26;

        matchField = new GuiTextField(2003, fontRenderer, rightX, y, rightW, 18);
        matchField.setMaxStringLength(Integer.MAX_VALUE);
        y += 24;

        replaceField = new GuiTextField(2004, fontRenderer, rightX, y, rightW, 18);
        replaceField.setMaxStringLength(Integer.MAX_VALUE);

        int btnY = this.height - 32;
        buttonList.add(new ThemedButton(1, listX, btnY, 72, 20, I18n.format("gui.common.new")));
        buttonList.add(new ThemedButton(2, listX + 78, btnY, 72, 20, I18n.format("gui.common.delete")));
        buttonList.add(new ThemedButton(3, listX + 156, btnY, 72, 20, I18n.format("gui.common.reload")));
        buttonList.add(new ThemedButton(4, panelX + panelW - 258, btnY, 78, 20, I18n.format("gui.common.validate")));
        buttonList.add(new ThemedButton(5, panelX + panelW - 174, btnY, 78, 20, I18n.format("gui.common.save")));
        buttonList.add(new ThemedButton(6, panelX + panelW - 90, btnY, 78, 20, I18n.format("gui.common.back")));

        if (selected >= rules.size()) {
            selected = rules.isEmpty() ? -1 : 0;
        }
        if (selected >= 0) {
            loadFromRule(rules.get(selected));
        } else {
            clearEditor();
        }
    }

    private void reloadWorkingCopy() {
        PacketInterceptConfig.load();
        rules.clear();
        for (PacketInterceptConfig.InterceptRule rule : PacketInterceptConfig.INSTANCE.inboundRules) {
            rules.add(rule.copy());
        }
    }

    private String formatBool(boolean v) {
        return v ? "§aON" : "§cOFF";
    }

    private void loadFromRule(PacketInterceptConfig.InterceptRule rule) {
        if (rule == null) {
            clearEditor();
            return;
        }
        nameField.setText(rule.name == null ? "" : rule.name);
        packetFilterField.setText(rule.packetFilter == null ? "" : rule.packetFilter);
        channelField.setText(rule.channel == null ? "" : rule.channel);
        matchField.setText(rule.matchHex == null ? "" : rule.matchHex);
        replaceField.setText(rule.replaceHex == null ? "" : rule.replaceHex);
        editingEnabled = rule.enabled;
        editingRegex = rule.regexEnabled;
        editingReplaceAll = rule.replaceAll;
        refreshToggleText();
    }

    private void clearEditor() {
        nameField.setText("");
        packetFilterField.setText("");
        channelField.setText("");
        matchField.setText("");
        replaceField.setText("");
        editingEnabled = true;
        editingRegex = false;
        editingReplaceAll = true;
        refreshToggleText();
    }

    private void refreshToggleText() {
        if (toggleGlobalBtn != null) {
            toggleGlobalBtn.displayString = I18n.format("gui.packet.intercept.global",
                    formatBool(PacketInterceptConfig.INSTANCE.inboundInterceptEnabled));
        }
        if (toggleRuleEnabledBtn != null) {
            toggleRuleEnabledBtn.displayString = I18n.format("gui.packet.intercept.rule_enabled",
                    formatBool(editingEnabled));
        }
        if (toggleRegexBtn != null) {
            toggleRegexBtn.displayString = I18n.format("gui.packet.intercept.regex", formatBool(editingRegex));
        }
        if (toggleReplaceAllBtn != null) {
            toggleReplaceAllBtn.displayString = I18n.format("gui.packet.intercept.replace_all",
                    formatBool(editingReplaceAll));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case 1: // new
            PacketInterceptConfig.InterceptRule rule = new PacketInterceptConfig.InterceptRule();
            rule.name = "rule_" + (rules.size() + 1);
            rule.channel = "";
            rules.add(rule);
            selected = rules.size() - 1;
            loadFromRule(rule);
            ensureVisible();
            break;
        case 2: // delete
            if (selected >= 0 && selected < rules.size()) {
                rules.remove(selected);
                if (selected >= rules.size()) {
                    selected = rules.size() - 1;
                }
                if (selected >= 0) {
                    loadFromRule(rules.get(selected));
                } else {
                    clearEditor();
                }
            }
            break;
        case 3: // reload
            reloadWorkingCopy();
            selected = rules.isEmpty() ? -1 : 0;
            if (selected >= 0) {
                loadFromRule(rules.get(selected));
            } else {
                clearEditor();
            }
            break;
        case 4: // validate
            flushEditorToSelected();
            List<String> errors = PacketInterceptManager.validateRules(rules);
            if (errors.isEmpty()) {
                toast(ChatFormatting.GREEN + I18n.format("msg.packet.intercept.validate_ok"));
            } else {
                toast(ChatFormatting.RED + I18n.format("msg.packet.intercept.validate_failed", errors.get(0)));
            }
            break;
        case 5: // save
            flushEditorToSelected();
            PacketInterceptConfig.INSTANCE.inboundRules.clear();
            for (PacketInterceptConfig.InterceptRule r : rules) {
                PacketInterceptConfig.normalizeRule(r);
                PacketInterceptConfig.INSTANCE.inboundRules.add(r.copy());
            }
            PacketInterceptConfig.save();
            toast(ChatFormatting.GREEN + I18n.format("msg.common.save_success"));
            break;
        case 6: // back
            mc.setScreen(parent);
            break;
        case 10:
            PacketInterceptConfig.INSTANCE.inboundInterceptEnabled = !PacketInterceptConfig.INSTANCE.inboundInterceptEnabled;
            refreshToggleText();
            break;
        case 11:
            editingEnabled = !editingEnabled;
            refreshToggleText();
            break;
        case 12:
            editingReplaceAll = !editingReplaceAll;
            refreshToggleText();
            break;
        case 13:
            editingRegex = !editingRegex;
            refreshToggleText();
            break;
        default:
            super.actionPerformed(button);
        }
    }

    private void flushEditorToSelected() {
        if (selected < 0 || selected >= rules.size()) {
            return;
        }
        PacketInterceptConfig.InterceptRule rule = rules.get(selected);
        rule.name = nameField.getText().trim();
        rule.packetFilter = packetFilterField.getText().trim();
        rule.channel = channelField.getText().trim();
        rule.matchHex = matchField.getText().trim();
        rule.replaceHex = replaceField.getText().trim();
        rule.enabled = editingEnabled;
        rule.regexEnabled = editingRegex;
        rule.replaceAll = editingReplaceAll;
    }

    private void ensureVisible() {
        int visible = Math.max(1, listH / ROW_H);
        if (selected < scroll) {
            scroll = selected;
        }
        if (selected >= scroll + visible) {
            scroll = selected - visible + 1;
        }
        if (scroll < 0) {
            scroll = 0;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (nameField.textboxKeyTyped(typedChar, keyCode) || packetFilterField.textboxKeyTyped(typedChar, keyCode)
                || channelField.textboxKeyTyped(typedChar, keyCode) || matchField.textboxKeyTyped(typedChar, keyCode)
                || replaceField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        packetFilterField.mouseClicked(mouseX, mouseY, mouseButton);
        channelField.mouseClicked(mouseX, mouseY, mouseButton);
        matchField.mouseClicked(mouseX, mouseY, mouseButton);
        replaceField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int idx = (mouseY - listY) / ROW_H + scroll;
            if (idx >= 0 && idx < rules.size()) {
                flushEditorToSelected();
                selected = idx;
                loadFromRule(rules.get(selected));
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            return;
        }
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, rules.size() - visible);
        if (wheel < 0) {
            scroll = Math.min(maxScroll, scroll + 1);
        } else {
            scroll = Math.max(0, scroll - 1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int panelW = Math.min(930, this.width - 30);
        int panelX = (this.width - panelW) / 2;
        int panelY = 30;
        int panelH = this.height - panelY - 12;
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.packet.intercept.title"), this.fontRenderer);

        drawCenteredString(fontRenderer, I18n.format("gui.packet.intercept.title"), width / 2, 12, 0xFFFFFF);

        drawRect(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, 0x6680A0C0);
        int visible = Math.max(1, listH / ROW_H);
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= rules.size()) {
                break;
            }
            int y = listY + i * ROW_H;
            PacketInterceptConfig.InterceptRule r = rules.get(idx);
            int bg = idx == selected ? 0x664AA3FF : 0x33223344;
            drawRect(listX, y, listX + listW, y + ROW_H - 2, bg);
            String title = (r.enabled ? "§a● " : "§c○ ") + (r.name == null ? "(unnamed)" : r.name);
            drawString(fontRenderer, title, listX + 6, y + 5, 0xFFFFFF);
            String sub = (r.channel == null || r.channel.trim().isEmpty()) ? "*" : r.channel;
            drawString(fontRenderer, "§7" + sub, listX + 6, y + 14, 0xFFFFFF);
        }

        drawString(fontRenderer, I18n.format("gui.packet.intercept.rule_name"), nameField.x, nameField.y - 10,
                0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.intercept.packet_filter"), packetFilterField.x,
                packetFilterField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.intercept.channel"), channelField.x, channelField.y - 10,
                0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.intercept.match_hex"), matchField.x, matchField.y - 10,
                0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.intercept.replace_hex"), replaceField.x, replaceField.y - 10,
                0xFFFFFF);

        drawThemedTextField(nameField);
        drawThemedTextField(packetFilterField);
        drawThemedTextField(channelField);
        drawThemedTextField(matchField);
        drawThemedTextField(replaceField);

        drawString(fontRenderer, "§8" + I18n.format("gui.packet.intercept.hint"), matchField.x, replaceField.y + 24,
                0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawInterceptTooltip(mouseX, mouseY);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void toast(String text) {
        if (mc.player != null) {
            mc.player.displayClientMessage(new TextComponentString(text), false);
        }
    }

    private void drawInterceptTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            tooltip = "拦截规则列表。\n左键切换当前编辑项，保存后会按列表顺序参与匹配。";
        } else if (isMouseOverField(mouseX, mouseY, nameField)) {
            tooltip = "规则名称，用于区分不同拦截逻辑。";
        } else if (isMouseOverField(mouseX, mouseY, packetFilterField)) {
            tooltip = "包过滤关键字。\n可填包名、ID、频道等，用来限制这条规则作用于哪些包。";
        } else if (isMouseOverField(mouseX, mouseY, channelField)) {
            tooltip = "频道过滤。\n主要给 FML/自定义频道包使用，留空表示不过滤频道。";
        } else if (isMouseOverField(mouseX, mouseY, matchField)) {
            tooltip = "要匹配的 HEX / 文本内容。\n配合“启用正则”可写正则表达式。";
        } else if (isMouseOverField(mouseX, mouseY, replaceField)) {
            tooltip = "匹配成功后的替换内容。\n启用替换全部时会替换所有命中，否则只替换第一处。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case 1:
                        tooltip = "新增一条空白拦截规则。";
                        break;
                    case 2:
                        tooltip = "删除当前选中的拦截规则。";
                        break;
                    case 3:
                        tooltip = "重新从配置文件加载规则，放弃未保存修改。";
                        break;
                    case 4:
                        tooltip = "校验当前规则列表是否存在空值、格式错误或无效表达式。";
                        break;
                    case 5:
                        tooltip = "保存当前拦截规则到配置文件。";
                        break;
                    case 6:
                        tooltip = "返回上一页。";
                        break;
                    case 10:
                        tooltip = "总开关。\n控制入站拦截规则是否整体生效。";
                        break;
                    case 11:
                        tooltip = "单条规则开关。\n关闭后该规则保留但不会参与匹配。";
                        break;
                    case 12:
                        tooltip = "替换全部开关。\n开启后会替换所有命中，关闭时只替换第一处。";
                        break;
                    case 13:
                        tooltip = "正则开关。\n开启后“匹配内容”按正则处理，关闭则按普通文本匹配。";
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










