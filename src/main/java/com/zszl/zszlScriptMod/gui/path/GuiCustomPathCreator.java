package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import java.util.List;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.path.PathRecordingManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager;

public class GuiCustomPathCreator extends ThemedGuiScreen {

    private GuiButton startButton;
    private GuiButton cancelButton;
    private GuiButton saveButton;
    private GuiButton backButton;

    private String statusMessage = "";

    @Override
    public void initGui() {
        this.buttonList.clear();
        int buttonWidth = 120;
        int buttonHeight = 20;

        if (PathRecordingManager.getRecordedSteps().isEmpty()) {
            statusMessage = I18n.format("gui.path.creator.status.start");
            startButton = new GuiButton(0, this.width / 2 - buttonWidth - 5, this.height - 40, buttonWidth,
                    buttonHeight,
                    "§a" + I18n.format("gui.path.creator.start"));
            backButton = new GuiButton(3, this.width / 2 + 5, this.height - 40, buttonWidth, buttonHeight,
                    I18n.format("gui.path.creator.back_main"));
            this.buttonList.add(startButton);
            this.buttonList.add(backButton);
        } else {
            statusMessage = I18n.format("gui.path.creator.status.recorded",
                    PathRecordingManager.getRecordedSteps().size());
            saveButton = new GuiButton(1, this.width / 2 - buttonWidth - 5, this.height - 40, buttonWidth, buttonHeight,
                    "§b" + I18n.format("gui.path.creator.save"));
            cancelButton = new GuiButton(2, this.width / 2 + 5, this.height - 40, buttonWidth, buttonHeight,
                    "§c" + I18n.format("gui.path.creator.cancel_discard"));
            this.buttonList.add(saveButton);
            this.buttonList.add(cancelButton);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        drawCenteredString(fontRenderer, I18n.format("gui.path.creator.title"), this.width / 2, 20, 0xFFFFFFFF);
        drawCenteredString(fontRenderer, statusMessage, this.width / 2, 40, 0xFFDDDDDD);

        List<PathRecordingManager.RecordedStep> steps = PathRecordingManager.getRecordedSteps();
        if (!steps.isEmpty()) {
            int listY = 60;
            int listX = this.width / 2 - 150;
            drawRect(listX, listY, listX + 300, listY + (steps.size() * 12) + 10, 0x80000000);
            for (int i = 0; i < steps.size(); i++) {
                String stepInfo = I18n.format("gui.path.creator.step_info", i + 1, steps.get(i).toString());
                drawString(fontRenderer, stepInfo, listX + 5, listY + 5 + (i * 12), 0xFFFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                PathRecordingManager.startRecording();
                mc.setScreen(null);
                if (mc.player != null) {
                    mc.player.sendSystemMessage(
                            new TextComponentString(ChatFormatting.GREEN + I18n.format("msg.path.record_started")));
                }
                break;
            case 1:
                if (PathRecordingManager.getRecordedSteps().isEmpty()) {
                    statusMessage = I18n.format("gui.path.creator.error.no_steps");
                    return;
                }
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.path.creator.input_name"), (pathName) -> {
                    if (pathName != null && !pathName.trim().isEmpty()) {
                        mc.setScreen(new GuiCategorySelect(this, null, category -> {
                            PathSequenceManager.createSequenceFromRecording(pathName.trim(),
                                    PathRecordingManager.getRecordedSteps(), category);
                            PathRecordingManager.stopAndClearRecording();
                            if (mc.player != null) {
                                mc.player.sendSystemMessage(new TextComponentString(
                                        ChatFormatting.AQUA + I18n.format("msg.path.saved_to_category", pathName.trim(),
                                                category)));
                            }
                            mc.setScreen(new GuiPathManager());
                        }));
                    } else {
                        mc.setScreen(this);
                    }
                }));
                break;
            case 2:
                PathRecordingManager.stopAndClearRecording();
                mc.setScreen(new GuiPathManager());
                break;
            case 3:
                mc.setScreen(new GuiPathManager());
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}







