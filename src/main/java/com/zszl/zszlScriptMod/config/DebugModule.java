// --- Full Java Content (src/main/java/com/zszl/zszlScriptMod/config/DebugModule.java) ---
package com.zszl.zszlScriptMod.config;

/**
 * 定义所有可进行独立调试日志开关的模块。
 */
public enum DebugModule {
    PATH_SEQUENCE("路径序列"),
    EVACUATION("撤离逻辑"),
    AUTO_EAT("自动进食"),
    AUTO_SKILL("自动技能"),
    ITEM_FILTER("物品过滤"),
    AHK_EXECUTION("AHK脚本调用"),
    ARENA_HANDLER("竞技场处理"),
    CHEST_ANALYSIS("箱子判断"),
    KILL_TIMER("杀怪计时器"),
    WAREHOUSE_ANALYSIS("仓库判断"),
    CONDITIONAL_EXECUTION("条件执行"),
    AUTO_PICKUP("自动拾取"),
    SHULKER_STACKING("潜影盒叠加"),
    REFINE("精炼设置"),
    AUTO_EQUIP("自动穿戴"),
    TRIGGER_RULES("触发器"),
    MAIL_GUI("邮件GUI调试"),
    PASSWORD_MANAGER("密码管理"),
    BARITONE("调试Baritone"),
    KILL_AURA_ORBIT("杀戮绕圈");

    private final String displayName;

    DebugModule(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
