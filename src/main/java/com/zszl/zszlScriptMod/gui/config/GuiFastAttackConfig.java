package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.FreecamHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.Arrays;

public class GuiFastAttackConfig extends ThemedGuiScreen {
    private final GuiScreen parentScreen;

    private ToggleGuiButton noCollisionButton;
    private ToggleGuiButton antiKnockbackButton;
    private ToggleGuiButton ghostEntityButton;
    private GuiButton ghostSoulCountButton;

    public GuiFastAttackConfig(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 360;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        int y = panelY + 35;
        noCollisionButton = new ToggleGuiButton(1, panelX + 20, y, panelWidth - 40, 20,
                I18n.format("gui.fastattack.no_collision", stateText(FreecamHandler.enableNoCollision)),
                FreecamHandler.enableNoCollision);
        this.buttonList.add(noCollisionButton);

        y += 30;
        antiKnockbackButton = new ToggleGuiButton(2, panelX + 20, y, panelWidth - 40, 20,
                I18n.format("gui.fastattack.anti_knockback", stateText(FreecamHandler.enableAntiKnockback)),
                FreecamHandler.enableAntiKnockback);
        this.buttonList.add(antiKnockbackButton);

        y += 30;
        ghostEntityButton = new ToggleGuiButton(3, panelX + 20, y, panelWidth - 40, 20,
                I18n.format("gui.fastattack.ghost_entity", stateText(FreecamHandler.enableGhostEntity)),
                FreecamHandler.enableGhostEntity);
        this.buttonList.add(ghostEntityButton);

        y += 30;
        ghostSoulCountButton = new ThemedButton(4, panelX + 20, y, panelWidth - 40, 20,
                I18n.format("gui.fastattack.soul_count", Math.max(1, FreecamHandler.ghostSoulCount)));
        this.buttonList.add(ghostSoulCountButton);

        y += 42;
        this.buttonList.add(new ThemedButton(100, panelX + 20, y, (panelWidth - 50) / 2, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList
                .add(new ThemedButton(101, panelX + 30 + (panelWidth - 50) / 2, y, (panelWidth - 50) / 2, 20,
                        I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                FreecamHandler.enableNoCollision = !FreecamHandler.enableNoCollision;
                noCollisionButton.setEnabledState(FreecamHandler.enableNoCollision);
                noCollisionButton.displayString = I18n.format("gui.fastattack.no_collision",
                        stateText(FreecamHandler.enableNoCollision));
                break;
            case 2:
                FreecamHandler.enableAntiKnockback = !FreecamHandler.enableAntiKnockback;
                antiKnockbackButton.setEnabledState(FreecamHandler.enableAntiKnockback);
                antiKnockbackButton.displayString = I18n.format("gui.fastattack.anti_knockback",
                        stateText(FreecamHandler.enableAntiKnockback));
                break;
            case 3:
                FreecamHandler.enableGhostEntity = !FreecamHandler.enableGhostEntity;
                ghostEntityButton.setEnabledState(FreecamHandler.enableGhostEntity);
                ghostEntityButton.displayString = I18n.format("gui.fastattack.ghost_entity",
                        stateText(FreecamHandler.enableGhostEntity));
                break;
            case 4:
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.fastattack.input_soul"),
                        String.valueOf(Math.max(1, FreecamHandler.ghostSoulCount)), value -> {
                            int count = FreecamHandler.ghostSoulCount;
                            try {
                                count = Integer.parseInt(value.trim());
                            } catch (Exception ignored) {
                            }
                            FreecamHandler.ghostSoulCount = Math.max(1, Math.min(20, count));
                            if (ghostSoulCountButton != null) {
                                ghostSoulCountButton.displayString = I18n.format("gui.fastattack.soul_count",
                                        FreecamHandler.ghostSoulCount);
                            }
                            mc.setScreen(this);
                        }));
                break;
            case 100:
                FreecamHandler.saveConfig();
                mc.setScreen(parentScreen);
                break;
            case 101:
                FreecamHandler.loadConfig();
                mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 360;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.fastattack.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.fastattack.tip"), panelX + 20, panelY + 24,
                GuiTheme.SUB_TEXT);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (mouseX >= ghostEntityButton.x && mouseX <= ghostEntityButton.x + ghostEntityButton.width
                && mouseY >= ghostEntityButton.y && mouseY <= ghostEntityButton.y + ghostEntityButton.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.fastattack.ghost_tip.title"),
                    I18n.format("gui.fastattack.ghost_tip.line1"),
                    I18n.format("gui.fastattack.ghost_tip.line2")), mouseX, mouseY);
        } else if (ghostSoulCountButton != null
                && mouseX >= ghostSoulCountButton.x && mouseX <= ghostSoulCountButton.x + ghostSoulCountButton.width
                && mouseY >= ghostSoulCountButton.y && mouseY <= ghostSoulCountButton.y + ghostSoulCountButton.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.fastattack.soul_tip.title"),
                    I18n.format("gui.fastattack.soul_tip.line1"),
                    I18n.format("gui.fastattack.soul_tip.line2")), mouseX, mouseY);
        }
    }

    private String stateText(boolean enabled) {
        return I18n.format(enabled ? "gui.common.enabled" : "gui.common.disabled");
    }
}




