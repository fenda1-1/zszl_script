package com.zszl.zszlScriptMod.handlers;

import java.util.ArrayList;
import java.util.List;

public final class RefineHelper {

    public static final RefineHelper INSTANCE = new RefineHelper();

    private final List<String> refineSlotIdLines = new ArrayList<>();

    private RefineHelper() {
    }

    public void onRefineBlueprintButtonCreated(int componentId, int parentId, String name) {
    }

    public void onRefineViewOpenStateDetected(int mainComponentId, int openFlag, int extraFlag) {
    }

    public void onRefineViewCreated(int mainComponentId, String viewName) {
    }

    public List<String> getRefineSlotIdLines() {
        return new ArrayList<>(refineSlotIdLines);
    }
}
