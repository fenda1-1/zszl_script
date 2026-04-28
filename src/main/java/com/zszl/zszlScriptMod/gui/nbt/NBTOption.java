// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/nbt/NBTOption.java
package com.zszl.zszlScriptMod.gui.nbt;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public interface NBTOption {
    String getText();
    // !! 核心修改：让action可以接收当前GUI屏幕 !!
    void action(GuiScreen currentScreen);
}



