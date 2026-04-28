package com.zszl.zszlScriptMod.utils;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MerchantExchangeManager {

    private static volatile List<MerchantDef> merchants = new ArrayList<>();

    private MerchantExchangeManager() {
    }

    public static class ExchangeDef {
        public final ItemStack leftItem;
        public final ItemStack middleItem;
        public final ItemStack rightItem;
        public final ItemStack resultItem;

        public ExchangeDef(ItemStack leftItem, ItemStack middleItem, ItemStack rightItem, ItemStack resultItem) {
            this.leftItem = leftItem == null ? ItemStack.EMPTY : leftItem;
            this.middleItem = middleItem == null ? ItemStack.EMPTY : middleItem;
            this.rightItem = rightItem == null ? ItemStack.EMPTY : rightItem;
            this.resultItem = resultItem == null ? ItemStack.EMPTY : resultItem;
        }
    }

    public static class CategoryDef {
        public final String name;
        public final int startIndex;
        public final int endIndex;

        public CategoryDef(String name, int startIndex, int endIndex) {
            this.name = name == null ? "" : name;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    public static class MerchantDef {
        public final String id;
        public final String name;
        public final List<ExchangeDef> exchanges;
        public final List<CategoryDef> categories;

        public MerchantDef(String id, String name, List<ExchangeDef> exchanges, List<CategoryDef> categories) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.exchanges = exchanges == null ? new ArrayList<>() : exchanges;
            this.categories = categories == null ? new ArrayList<>() : categories;
        }
    }

    public static void reload() {
        merchants = new ArrayList<>();
    }

    public static List<MerchantDef> getMerchants() {
        return merchants == null ? Collections.emptyList() : merchants;
    }
}
