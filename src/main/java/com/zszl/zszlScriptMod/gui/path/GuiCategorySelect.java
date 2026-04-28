// 文件: src/main/java/com/keycommand2/zszlScriptMod/gui/path/GuiCategorySelect.java
// (这是一个新文件)

package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.path.PathSequenceManager;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiCategorySelect extends ThemedGuiScreen {

    private static String builtinCategory() {
        return I18n.format("path.category.builtin");
    }

    private final GuiScreen parentScreen;
    private final Consumer<String> onSelect;
    private final List<String> categories;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public GuiCategorySelect(GuiScreen parent, String currentCategory, Consumer<String> onSelect) {
        this.parentScreen = parent;
        this.onSelect = onSelect;
        // 过滤掉当前分类和内置分类
        this.categories = PathSequenceManager.getAllCategories().stream()
                .filter(c -> !c.equals(currentCategory) && !builtinCategory().equals(c))
                .collect(Collectors.toList());
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(
                new GuiButton(0, (this.width - 100) / 2, this.height - 30, 100, 20, I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 200;
        int panelHeight = 200;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.path.select_target_category"),
                this.fontRenderer);

        int listY = panelY + 40;
        int listHeight = 140;
        int itemHeight = 20;
        int visibleItems = listHeight / itemHeight;
        maxScroll = Math.max(0, categories.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= categories.size())
                break;

            String category = categories.get(index);
            int itemY = listY + i * itemHeight;
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            drawRect(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight,
                    isHovered ? 0xFF666666 : 0xFF444444);
            this.drawCenteredString(fontRenderer, category, this.width / 2, itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int panelWidth = 200;
        int panelX = (this.width - panelWidth) / 2;
        int listY = (this.height - 200) / 2 + 40;
        int listHeight = 140;
        int itemHeight = 20;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < categories.size()) {
                onSelect.accept(categories.get(clickedIndex));
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}






