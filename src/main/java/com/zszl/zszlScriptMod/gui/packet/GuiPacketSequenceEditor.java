// File path: src/main/java/com/zszl/zszlScriptMod/gui/packet/GuiPacketSequenceEditor.java
// Supports {id} placeholder
package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.utils.ModUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiPacketSequenceEditor extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private PacketSequence sequence;

    private List<GuiButton> scrolledButtons = new ArrayList<>();
    private List<GuiTextField> scrolledTextFields = new ArrayList<>();

    private List<GuiTextField> idOrChannelFields = new ArrayList<>();
    private List<DirectionDropdown> directionDropdowns = new ArrayList<>();
    private List<GuiTextField> dataFields = new ArrayList<>();
    private List<GuiTextField> delayFields = new ArrayList<>();
    private List<Integer> visiblePacketIndices = new ArrayList<>();
    private GuiTextField defaultDelayField;

    private static String lastDefaultDelay = "20";

    private int leftPanelScrollOffset = 0;
    private int maxLeftPanelScroll = 0;
    private boolean isDraggingLeftScrollbar = false;
    private int leftPanelContentHeight = 0;

    private int packetListScrollOffset = 0;
    private int maxPacketScroll = 0;
    private boolean isDraggingPacketScrollbar = false;

    private final int itemHeight = 100;
    private int leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight;
    private int rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight;

    public GuiPacketSequenceEditor(GuiScreen parent, List<PacketCaptureHandler.CapturedPacketData> initialPackets) {
        this.parentScreen = parent;
        this.sequence = new PacketSequence(I18n.format("gui.packet.seq_editor.unnamed_sequence"));

        int defaultDelay;
        try {
            defaultDelay = Integer.parseInt(lastDefaultDelay);
        } catch (NumberFormatException e) {
            defaultDelay = 20;
        }

        for (PacketCaptureHandler.CapturedPacketData data : initialPackets) {
            if (data.isFmlPacket) {
                this.sequence.packets
                        .add(new PacketSequence.PacketToSend(data.channel, data.getHexData(), defaultDelay));
            } else if (data.packetId != null) {
                this.sequence.packets
                        .add(new PacketSequence.PacketToSend(data.packetId, data.getHexData(), defaultDelay));
            }
        }
    }

    public GuiPacketSequenceEditor(GuiScreen parent, PacketSequence sequenceToLoad) {
        this.parentScreen = parent;
        this.sequence = sequenceToLoad;
    }

    // initGui
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.scrolledButtons.clear();
        this.scrolledTextFields.clear();
        this.idOrChannelFields.clear();
        this.directionDropdowns.clear();
        this.dataFields.clear();
        this.delayFields.clear();
        this.visiblePacketIndices.clear();

        int margin = 20;
        int spacing = 10;
        leftPanelWidth = 220;

        leftPanelX = margin;
        leftPanelY = 30;
        leftPanelHeight = this.height - 40 - leftPanelY;

        rightPanelX = leftPanelX + leftPanelWidth + spacing;
        rightPanelY = 30;
        rightPanelWidth = this.width - rightPanelX - margin;
        rightPanelHeight = this.height - 40 - rightPanelY;

        int currentY = 10;
        int controlWidth = leftPanelWidth - 20;

        int halfWidth = (controlWidth - 5) / 2;
        scrolledButtons.add(new ThemedButton(100, 10, currentY, controlWidth, 20,
                "§c§l" + I18n.format("gui.packet.seq_editor.send_sequence")));
        currentY += 25;
        scrolledButtons.add(new ThemedButton(101, 10, currentY, controlWidth, 20,
                "§a" + I18n.format("gui.packet.seq_editor.save_sequence")));
        currentY += 25;
        scrolledButtons.add(new ThemedButton(104, 10, currentY, halfWidth, 20,
                "§b" + I18n.format("gui.packet.seq_editor.add_id")));
        scrolledButtons.add(new ThemedButton(106, 10 + halfWidth + 5, currentY, halfWidth, 20,
                "§b" + I18n.format("gui.packet.seq_editor.add_channel")));
        currentY += 25;
        scrolledButtons.add(new ThemedButton(102, 10, currentY, controlWidth, 20,
                "§e" + I18n.format("gui.packet.seq_editor.clear_list")));
        currentY += 40;

        defaultDelayField = new GuiTextField(105, fontRenderer, 10 + 80, currentY, 60, 20);
        defaultDelayField.setMaxStringLength(Integer.MAX_VALUE);
        defaultDelayField.setText(lastDefaultDelay);
        scrolledTextFields.add(defaultDelayField);
        currentY += 40;

        scrolledButtons.add(new ThemedButton(103, 10, currentY, controlWidth, 20, I18n.format("gui.common.back")));
        currentY += 25;

        leftPanelContentHeight = currentY;
        maxLeftPanelScroll = Math.max(0, leftPanelContentHeight - leftPanelHeight);
        leftPanelScrollOffset = Math.min(leftPanelScrollOffset, maxLeftPanelScroll);

        int visibleItems = rightPanelHeight / itemHeight;
        maxPacketScroll = Math.max(0, sequence.packets.size() - visibleItems);
        packetListScrollOffset = Math.min(packetListScrollOffset, maxPacketScroll);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + packetListScrollOffset;
            if (index >= sequence.packets.size())
                break;

            PacketSequence.PacketToSend packet = sequence.packets.get(index);
            int cardY = rightPanelY + i * itemHeight;
            visiblePacketIndices.add(index);

            if (packet.direction == null || packet.direction.trim().isEmpty()) {
                packet.direction = "C2S";
            }

            DirectionDropdown dirDropdown = new DirectionDropdown(
                    rightPanelX + rightPanelWidth - 200,
                    cardY + 5,
                    65,
                    20,
                    new String[] { "C->S", "S->C" });
            dirDropdown.setValue("S2C".equalsIgnoreCase(packet.direction) ? "S->C" : "C->S");
            directionDropdowns.add(dirDropdown);

            String idOrChannelText = packet.isFmlPacket ? packet.channel
                    : (packet.packetId != null ? String.format("0x%02X", packet.packetId)
                            : I18n.format("gui.packet.seq_editor.invalid"));
            GuiTextField idcField = new GuiTextField(i * 10, fontRenderer, rightPanelX + 80, cardY + 5,
                    rightPanelWidth - 210, 20);
            idcField.setMaxStringLength(Integer.MAX_VALUE);
            idcField.setText(idOrChannelText);
            idOrChannelFields.add(idcField);

            GuiTextField dField = new GuiTextField(i * 10 + 1, fontRenderer, rightPanelX + 5, cardY + 30,
                    rightPanelWidth - 15, 20);
            dField.setMaxStringLength(Integer.MAX_VALUE);
            dField.setText(packet.hexData);
            dataFields.add(dField);

            GuiTextField dlField = new GuiTextField(i * 10 + 2, fontRenderer, rightPanelX + 50, cardY + 55, 60, 20);
            dlField.setMaxStringLength(Integer.MAX_VALUE);
            dlField.setText(String.valueOf(packet.delayTicks));
            delayFields.add(dlField);

            this.buttonList.add(new ThemedButton(200 + i, rightPanelX + rightPanelWidth - 125, cardY + 5, 20, 20, "↑"));
            this.buttonList.add(new ThemedButton(300 + i, rightPanelX + rightPanelWidth - 100, cardY + 5, 20, 20, "↓"));
            this.buttonList.add(new ThemedButton(400 + i, rightPanelX + rightPanelWidth - 75, cardY + 5, 65, 20,
                    "§c" + I18n.format("gui.common.delete")));
        }
    }

    // actionPerformed() 方法保持不变
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        syncFieldsToSequence();

        if (button.id >= 200 && button.id < 300) { // 上移
            int index = getPacketIndexForVisibleRow(button.id - 200);
            if (index > 0 && index < sequence.packets.size()) {
                Collections.swap(sequence.packets, index, index - 1);
                initGui();
            }
        } else if (button.id >= 300 && button.id < 400) { // move down
            int index = getPacketIndexForVisibleRow(button.id - 300);
            if (index >= 0 && index < sequence.packets.size() - 1) {
                Collections.swap(sequence.packets, index, index + 1);
                initGui();
            }
        } else if (button.id >= 400 && button.id < 500) { // delete
            int index = getPacketIndexForVisibleRow(button.id - 400);
            if (index >= 0 && index < sequence.packets.size()) {
                sequence.packets.remove(index);
                initGui();
            }
        }
    }

    public void sendSequence() {
        syncFieldsToSequence();
        if (mc.player == null)
            return;

        mc.player.displayClientMessage(
                new TextComponentString(ChatFormatting.AQUA + I18n.format("msg.packet.seq_editor.send_start",
                        sequence.packets.size())), false);
        mc.setScreen(null);

        ModUtils.DelayScheduler.init();
        final ModUtils.DelayScheduler scheduler = ModUtils.DelayScheduler.instance;
        AtomicInteger totalDelay = new AtomicInteger(0);
        for (int i = 0; i < sequence.packets.size(); i++) {
            final PacketSequence.PacketToSend packetToSend = sequence.packets.get(i);
            final int packetIndex = i + 1;

            totalDelay.addAndGet(packetToSend.delayTicks);

            Runnable sendTask = () -> {
                try {
                    sendSinglePacket(packetToSend);
                    String idString = packetToSend.isFmlPacket ? packetToSend.channel
                            : String.format("ID 0x%02X", packetToSend.packetId);
                    String dirString = "C2S".equalsIgnoreCase(packetToSend.direction) ? "C->S" : "S->C";
                    if (mc.player != null) {
                        mc.player.displayClientMessage(new TextComponentString(ChatFormatting.GREEN
                                + I18n.format("msg.packet.seq_editor.sent", packetIndex, sequence.packets.size(),
                                        idString + " " + dirString)), false);
                    }
                } catch (Exception e) {
                    if (mc.player != null) {
                        mc.player.displayClientMessage(new TextComponentString(ChatFormatting.RED
                                + I18n.format("msg.packet.seq_editor.send_failed", packetIndex,
                                        sequence.packets.size(), e.getMessage())), false);
                        zszlScriptMod.LOGGER.error("Failed to send packet", e);
                    }
                }
            };

            if (scheduler != null) {
                scheduler.schedule(sendTask, totalDelay.get());
            } else {
                zszlScriptMod.LOGGER.warn("DelayScheduler 未初始化成功，数据包序列将立即发送: packetIndex={}/{}",
                        packetIndex, sequence.packets.size());
                mc.execute(sendTask);
            }
        }
    }

    // --- 核心修改：在发送前替换 {id} 占位符 ---
    private void sendSinglePacket(PacketSequence.PacketToSend packetInfo) throws Exception {
        String direction = normalizeDirection(packetInfo.direction);
        boolean inbound = "S2C".equals(direction);

        if (inbound) {
            if (packetInfo.isFmlPacket) {
                ModUtils.mockReceiveFmlPacket(packetInfo.channel, packetInfo.hexData);
            } else if (packetInfo.packetId != null) {
                ModUtils.mockReceiveStandardPacketById(packetInfo.packetId, packetInfo.hexData);
            }
            return;
        }

        // --- 核心修改：统一调用 ModUtils 的方法来处理FML包 ---
        if (packetInfo.isFmlPacket) {
            ModUtils.sendFmlPacket(packetInfo.channel, packetInfo.hexData);
            return; // 直接返回，因为 ModUtils 已经处理了所有逻辑
        }
        // --- 修改结束 ---

        // 非FML包也统一交由 ModUtils 处理，保持占位符解析逻辑一致
        ModUtils.sendStandardPacketById(packetInfo.packetId, packetInfo.hexData);
    }
    // --- 修改结束 ---

    // syncFieldsToSequence(), onGuiClosed(), drawScreen() 等其他方法保持不变
    private void syncFieldsToSequence() {
        for (int i = 0; i < idOrChannelFields.size(); i++) {
            if (i >= visiblePacketIndices.size()) {
                continue;
            }
            int index = visiblePacketIndices.get(i);
            if (index < sequence.packets.size()) {
                PacketSequence.PacketToSend packet = sequence.packets.get(index);

                String idOrChannelText = idOrChannelFields.get(i).getText().trim();

                if (packet.isFmlPacket) {
                    packet.channel = idOrChannelText;
                } else {
                    try {
                        packet.packetId = Integer.decode(idOrChannelText);
                    } catch (NumberFormatException e) {
                        packet.packetId = 0;
                        zszlScriptMod.LOGGER.warn("Invalid Packet ID: '{}', reset to 0", idOrChannelText);
                    }
                }

                String directionText = directionDropdowns.get(i).getValue();
                packet.direction = normalizeDirection(directionText);

                packet.hexData = dataFields.get(i).getText();
                try {
                    packet.delayTicks = Integer.parseInt(delayFields.get(i).getText());
                } catch (NumberFormatException e) {
                    packet.delayTicks = 1;
                }
            }
        }
    }

    private int getPacketIndexForVisibleRow(int visibleRow) {
        if (visibleRow >= 0 && visibleRow < visiblePacketIndices.size()) {
            return visiblePacketIndices.get(visibleRow);
        }
        return -1;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        lastDefaultDelay = defaultDelayField.getText();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRenderer,
                I18n.format("gui.packet.seq_editor.title", sequence.name, sequence.packets.size()),
                this.width / 2, 15, 0xFFFFFF);

        GuiTheme.drawPanel(leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GL11.glScissor(leftPanelX * scaleFactor, mc.getWindow().getScreenHeight() - ((leftPanelY + leftPanelHeight) * scaleFactor),
                leftPanelWidth * scaleFactor, leftPanelHeight * scaleFactor);

        GuiCompatContext.current().pose().pushMatrix();
        GuiCompatContext.current().pose().translate(leftPanelX, leftPanelY - leftPanelScrollOffset);

        drawString(fontRenderer, "§l" + I18n.format("gui.packet.seq_editor.operations"), 10, 0, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.seq_editor.default_delay"), 10, defaultDelayField.y + 4,
                0xFFFFFF);

        int translatedMouseY = mouseY - (leftPanelY - leftPanelScrollOffset);
        for (GuiButton btn : scrolledButtons) {
            btn.drawButton(mc, mouseX - leftPanelX, translatedMouseY, partialTicks);
        }
        for (GuiTextField tf : scrolledTextFields) {
            drawThemedTextField(tf);
        }

        GuiCompatContext.current().pose().popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GuiTheme.drawPanel(rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight);

        for (int i = 0; i < idOrChannelFields.size(); i++) {
            if (i >= visiblePacketIndices.size())
                break;
            int index = visiblePacketIndices.get(i);
            if (index >= sequence.packets.size())
                break;
            PacketSequence.PacketToSend packet = sequence.packets.get(index);
            int cardY = rightPanelY + i * itemHeight;

            boolean hovered = mouseX >= rightPanelX + 2 && mouseX <= rightPanelX + rightPanelWidth - 8
                    && mouseY >= cardY + 2 && mouseY <= cardY + itemHeight;
            UiState cardState = hovered ? UiState.HOVER : UiState.NORMAL;
            GuiTheme.drawButtonFrame(rightPanelX + 2, cardY + 2, rightPanelWidth - 10, itemHeight - 2, cardState);

            String label = packet.isFmlPacket ? "§6" + I18n.format("gui.packet.seq_editor.channel") + ":"
                    : "§b" + I18n.format("gui.packet.seq_editor.packet_id") + ":";
            drawString(fontRenderer, "§7#" + (index + 1) + " " + label, rightPanelX + 5, cardY + 10, 0xFFFFFF);
            drawThemedTextField(idOrChannelFields.get(i));

            drawString(fontRenderer, "§d" + I18n.format("gui.packet.seq_editor.direction") + ":",
                    rightPanelX + rightPanelWidth - 270,
                    cardY + 10, 0xFFFFFF);
            directionDropdowns.get(i).drawMain(mouseX, mouseY);

            drawString(fontRenderer, "§6" + I18n.format("gui.packet.seq_editor.data_hex") + ":", rightPanelX + 5,
                    cardY + 35, 0xFFFFFF);
            drawThemedTextField(dataFields.get(i));

            drawString(fontRenderer, "§6" + I18n.format("gui.packet.seq_editor.delay") + ":", rightPanelX + 5,
                    cardY + 60, 0xFFFFFF);
            drawThemedTextField(delayFields.get(i));
            drawString(fontRenderer, "§7ticks", rightPanelX + 50 + 60 + 5, cardY + 60, 0xAAAAAA);
        }

        if (maxLeftPanelScroll > 0) {
            int scrollbarX = leftPanelX + leftPanelWidth - 6;
            drawRect(scrollbarX, leftPanelY, scrollbarX + 5, leftPanelY + leftPanelHeight, 0xFF202020);
            int thumbHeight = Math.max(10, (int) ((float) leftPanelHeight / leftPanelContentHeight * leftPanelHeight));
            int thumbY = leftPanelY
                    + (int) ((float) leftPanelScrollOffset / maxLeftPanelScroll * (leftPanelHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF888888);
        }
        if (maxPacketScroll > 0) {
            int scrollbarX = rightPanelX + rightPanelWidth - 6;
            drawRect(scrollbarX, rightPanelY, scrollbarX + 5, rightPanelY + rightPanelHeight, 0xFF202020);
            int thumbHeight = Math.max(10,
                    (int) ((float) (rightPanelHeight / itemHeight) / sequence.packets.size() * rightPanelHeight));
            int thumbY = rightPanelY
                    + (int) ((float) packetListScrollOffset / maxPacketScroll * (rightPanelHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (DirectionDropdown dropdown : directionDropdowns) {
            dropdown.drawExpanded(mouseX, mouseY);
        }
        drawSequenceEditorTooltip(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        scrolledTextFields.forEach(f -> f.textboxKeyTyped(typedChar, keyCode));
        idOrChannelFields.forEach(f -> f.textboxKeyTyped(typedChar, keyCode));
        dataFields.forEach(f -> f.textboxKeyTyped(typedChar, keyCode));
        delayFields.forEach(f -> f.textboxKeyTyped(typedChar, keyCode));
        lastDefaultDelay = defaultDelayField.getText();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        syncFieldsToSequence();
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int scrollbarXLeft = leftPanelX + leftPanelWidth - 6;
        if (maxLeftPanelScroll > 0 && mouseX >= scrollbarXLeft && mouseX <= scrollbarXLeft + 5 && mouseY >= leftPanelY
                && mouseY <= leftPanelY + leftPanelHeight) {
            isDraggingLeftScrollbar = true;
            return;
        }

        int scrollbarXRight = rightPanelX + rightPanelWidth - 6;
        if (maxPacketScroll > 0 && mouseX >= scrollbarXRight && mouseX <= scrollbarXRight + 5 && mouseY >= rightPanelY
                && mouseY <= rightPanelY + rightPanelHeight) {
            isDraggingPacketScrollbar = true;
            return;
        }

        if (mouseX >= leftPanelX && mouseX < scrollbarXLeft && mouseY >= leftPanelY
                && mouseY <= leftPanelY + leftPanelHeight) {
            int translatedY = mouseY - (leftPanelY - leftPanelScrollOffset);
            for (GuiButton btn : scrolledButtons) {
                if (btn.mousePressed(mc, mouseX - leftPanelX, translatedY)) {
                    btn.playPressSound(mc.getSoundManager());
                    if (btn.id == 100) {
                        sendSequence();
                    } else if (btn.id == 101) {
                        mc.setScreen(new GuiTextInput(this, I18n.format("gui.packet.seq_editor.input_name"),
                                sequence.name, (newName) -> {
                                    if (newName != null && !newName.trim().isEmpty()) {
                                        sequence.name = newName.trim();
                                        if (PacketSequenceManager.saveSequence(sequence))
                                            mc.player.displayClientMessage(new TextComponentString(
                                                    ChatFormatting.GREEN + I18n.format("msg.packet.seq_editor.saved",
                                                            sequence.name)), false);
                                        else
                                            mc.player.displayClientMessage(new TextComponentString(
                                                    ChatFormatting.RED
                                                            + I18n.format("msg.packet.seq_editor.save_failed")), false);
                                    }
                                    mc.setScreen(this);
                                }));
                    } else if (btn.id == 102) {
                        sequence.packets.clear();
                        initGui();
                        return;
                    } else if (btn.id == 103) {
                        mc.setScreen(parentScreen);
                    } else if (btn.id == 104) { // add by ID
                        int defaultDelay;
                        try {
                            defaultDelay = Integer.parseInt(defaultDelayField.getText());
                        } catch (NumberFormatException e) {
                            defaultDelay = 20;
                        }
                        sequence.packets.add(new PacketSequence.PacketToSend(0x00, "", defaultDelay));
                        initGui();
                        return;
                    } else if (btn.id == 106) { // add by channel
                        int defaultDelay;
                        try {
                            defaultDelay = Integer.parseInt(defaultDelayField.getText());
                        } catch (NumberFormatException e) {
                            defaultDelay = 20;
                        }
                        sequence.packets.add(new PacketSequence.PacketToSend("YourChannel", "", defaultDelay));
                        initGui();
                        return;
                    }
                }
            }
            for (GuiTextField tf : scrolledTextFields) {
                tf.mouseClicked(mouseX - leftPanelX, translatedY, mouseButton);
            }
        }

        idOrChannelFields.forEach(f -> f.mouseClicked(mouseX, mouseY, mouseButton));

        boolean dropdownHandled = false;
        for (DirectionDropdown dropdown : directionDropdowns) {
            if (dropdown.handleClick(mouseX, mouseY, mouseButton)) {
                collapseOtherDirectionDropdowns(dropdown);
                dropdownHandled = true;
                break;
            }
        }
        if (dropdownHandled) {
            return;
        }
        collapseAllDirectionDropdowns();

        dataFields.forEach(f -> f.mouseClicked(mouseX, mouseY, mouseButton));
        delayFields.forEach(f -> f.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingLeftScrollbar) {
            float percent = (float) (mouseY - leftPanelY) / leftPanelHeight;
            leftPanelScrollOffset = (int) (percent * (maxLeftPanelScroll + 10));
            leftPanelScrollOffset = Math.max(0, Math.min(maxLeftPanelScroll, leftPanelScrollOffset));
        }
        if (isDraggingPacketScrollbar) {
            float percent = (float) (mouseY - rightPanelY) / rightPanelHeight;
            packetListScrollOffset = (int) (percent * (maxPacketScroll + 1));
            packetListScrollOffset = Math.max(0, Math.min(maxPacketScroll, packetListScrollOffset));
            initGui();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingLeftScrollbar = false;
        isDraggingPacketScrollbar = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
            int mouseY = this.height - Mouse.getY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;

            if (mouseX >= leftPanelX && mouseX < leftPanelX + leftPanelWidth) {
                if (dWheel > 0)
                    leftPanelScrollOffset = Math.max(0, leftPanelScrollOffset - 20);
                else
                    leftPanelScrollOffset = Math.min(maxLeftPanelScroll, leftPanelScrollOffset + 20);
            } else if (mouseX >= rightPanelX && mouseX < rightPanelX + rightPanelWidth) {
                syncFieldsToSequence();
                if (dWheel > 0)
                    packetListScrollOffset = Math.max(0, packetListScrollOffset - 1);
                else
                    packetListScrollOffset = Math.min(maxPacketScroll, packetListScrollOffset + 1);
                initGui();
            }
        }
    }

    private String normalizeDirection(String value) {
        if (value == null) {
            return "C2S";
        }
        String v = value.trim().toUpperCase();
        if ("S->C".equals(v) || "S2C".equals(v) || "INBOUND".equals(v) || "RECV".equals(v)
                || "RECEIVE".equals(v) || "接收".equals(value.trim())) {
            return "S2C";
        }
        return "C2S";
    }

    private void collapseOtherDirectionDropdowns(DirectionDropdown keep) {
        for (DirectionDropdown dropdown : directionDropdowns) {
            if (dropdown != keep) {
                dropdown.collapse();
            }
        }
    }

    private void collapseAllDirectionDropdowns() {
        for (DirectionDropdown dropdown : directionDropdowns) {
            dropdown.collapse();
        }
    }

    private class DirectionDropdown {
        private int x;
        private int y;
        private int width;
        private int height;
        private final String[] options;
        private int selectedIndex = 0;
        private boolean expanded = false;

        private DirectionDropdown(int x, int y, int width, int height, String[] options) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.options = options;
        }

        private void drawMain(int mouseX, int mouseY) {
            boolean hoverMain = isMouseInside(mouseX, mouseY, x, y, width, height);
            int bg = hoverMain ? 0xCC203146 : 0xCC152433;
            int border = expanded ? 0xFF76D1FF : (hoverMain ? 0xFF4FA6D9 : 0xFF3F6A8C);

            drawRect(x, y, x + width, y + height, bg);
            drawHorizontalLine(x, x + width, y, border);
            drawHorizontalLine(x, x + width, y + height, border);
            drawVerticalLine(x, y, y + height, border);
            drawVerticalLine(x + width, y, y + height, border);

            String value = getValue();
            fontRenderer.drawString(value, x + 5, y + 6, 0xFFFFFFFF);
            String arrowText = expanded ? "▲" : "▼";
            fontRenderer.drawString(arrowText, x + width - 10, y + 6, 0xFF9FDFFF);
        }

        private void drawExpanded(int mouseX, int mouseY) {
            if (!expanded) {
                return;
            }

            for (int i = 0; i < options.length; i++) {
                int oy = y + height + i * height;
                boolean hoverItem = isMouseInside(mouseX, mouseY, x, oy, width, height);
                boolean selected = i == selectedIndex;
                int itemBg = selected ? 0xEE2B5A7C : (hoverItem ? 0xCC29455E : 0xCC1B2D3D);
                int itemBorder = hoverItem ? 0xFF7ED0FF : 0xFF3B6B8A;

                drawRect(x, oy, x + width, oy + height, itemBg);
                drawHorizontalLine(x, x + width, oy, itemBorder);
                drawHorizontalLine(x, x + width, oy + height, itemBorder);
                drawVerticalLine(x, oy, oy + height, itemBorder);
                drawVerticalLine(x + width, oy, oy + height, itemBorder);
                fontRenderer.drawString(options[i], x + 5, oy + 6, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) {
                return false;
            }

            if (isMouseInside(mouseX, mouseY, x, y, width, height)) {
                expanded = !expanded;
                return true;
            }

            if (!expanded) {
                return false;
            }

            for (int i = 0; i < options.length; i++) {
                int oy = y + height + i * height;
                if (isMouseInside(mouseX, mouseY, x, oy, width, height)) {
                    selectedIndex = i;
                    expanded = false;
                    return true;
                }
            }

            return false;
        }

        private String getValue() {
            if (options == null || options.length == 0) {
                return "C->S";
            }
            if (selectedIndex < 0 || selectedIndex >= options.length) {
                selectedIndex = 0;
            }
            return options[selectedIndex];
        }

        private void setValue(String value) {
            String normalized = value == null ? "" : value.trim();
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(normalized)) {
                    selectedIndex = i;
                    return;
                }
            }
            selectedIndex = 0;
        }

        private void collapse() {
            expanded = false;
        }

        private boolean isMouseInside(int mx, int my, int rx, int ry, int rw, int rh) {
            return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
        }
    }

    private void drawSequenceEditorTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        int translatedMouseY = mouseY - (leftPanelY - leftPanelScrollOffset);
        for (GuiButton btn : scrolledButtons) {
            if (btn != null && btn.visible && isHoverRegion(mouseX - leftPanelX, translatedMouseY, btn.x, btn.y, btn.width, btn.height)) {
                switch (btn.id) {
                    case 100:
                        tooltip = "按当前顺序发送整个数据包序列。";
                        break;
                    case 101:
                        tooltip = "保存当前序列到本地，供以后复用。";
                        break;
                    case 102:
                        tooltip = "清空当前序列中的所有数据包项。";
                        break;
                    case 103:
                        tooltip = "返回上一页。";
                        break;
                    case 104:
                        tooltip = "新增一条按 Packet ID 发送的标准包。";
                        break;
                    case 106:
                        tooltip = "新增一条按频道发送的 FML / 自定义频道包。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }

        if (tooltip == null && isMouseOverField(mouseX, mouseY, defaultDelayField)) {
            tooltip = "默认延迟（tick）。\n新添加的数据包项会默认使用这个发送间隔。";
        }

        if (tooltip == null) {
            for (int i = 0; i < idOrChannelFields.size() && i < visiblePacketIndices.size(); i++) {
                GuiTextField idField = idOrChannelFields.get(i);
                GuiTextField dataField = dataFields.get(i);
                GuiTextField delayField = delayFields.get(i);
                DirectionDropdown dropdown = i < directionDropdowns.size() ? directionDropdowns.get(i) : null;
                if (isMouseOverField(mouseX, mouseY, idField)) {
                    tooltip = "Packet ID 或频道。\n标准包填 ID，自定义/FML 包填频道名。";
                    break;
                }
                if (isMouseOverField(mouseX, mouseY, dataField)) {
                    tooltip = "HEX 数据区。\n填写这条数据包的原始包体内容。";
                    break;
                }
                if (isMouseOverField(mouseX, mouseY, delayField)) {
                    tooltip = "这条数据包发送前的延迟 tick 数。";
                    break;
                }
                if (dropdown != null && isHoverRegion(mouseX, mouseY, dropdown.x, dropdown.y, dropdown.width, dropdown.height)) {
                    tooltip = "方向选择。\nC->S 表示发送给服务器，S->C 表示按入站包在本地处理。";
                    break;
                }
            }
        }

        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }
}



