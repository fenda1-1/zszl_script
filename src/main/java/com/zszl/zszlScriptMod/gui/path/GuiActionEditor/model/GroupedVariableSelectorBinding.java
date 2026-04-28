package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

import java.util.LinkedHashMap;

public class GroupedVariableSelectorBinding {
    public final String actualParamKey;
    public final String groupDropdownKey;
    public final String valueDropdownKey;
    public final LinkedHashMap<String, LinkedHashMap<String, String>> groupValueMap;
    public final boolean allowEmptySelection;

    public GroupedVariableSelectorBinding(String actualParamKey, String groupDropdownKey, String valueDropdownKey,
            LinkedHashMap<String, LinkedHashMap<String, String>> groupValueMap, boolean allowEmptySelection) {
        this.actualParamKey = actualParamKey;
        this.groupDropdownKey = groupDropdownKey;
        this.valueDropdownKey = valueDropdownKey;
        this.groupValueMap = groupValueMap == null ? new LinkedHashMap<String, LinkedHashMap<String, String>>()
                : groupValueMap;
        this.allowEmptySelection = allowEmptySelection;
    }
}
