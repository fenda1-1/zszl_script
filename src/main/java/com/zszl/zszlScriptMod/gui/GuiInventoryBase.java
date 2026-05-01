package com.zszl.zszlScriptMod.gui;

/**
 * 1.21.11 迁移后，原先拆分在 Base/CustomSupport/FeatureScreens/RenderInput
 * 的主界面逻辑已合并到 {@link GuiInventory} 中统一维护。
 *
 * 这个基类保留参考工程的结构锚点，避免后续继续拆分或对照迁移时缺少对应文件。
 */
abstract class GuiInventoryBase {

    protected GuiInventoryBase() {
    }
}
