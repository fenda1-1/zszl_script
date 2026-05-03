package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.feedback;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;

import java.util.Locale;

public final class ActionEditorFeedbackSupport {
    private ActionEditorFeedbackSupport() {
    }

    public static String buildActionEffectHint(ActionData draft) {
        if (draft == null) {
            return "保存后将按当前参数执行此动作。";
        }
        JsonObject params = draft.params == null ? new JsonObject() : draft.params;
        String type = safe(draft.type).trim().toLowerCase(Locale.ROOT);
        switch (type) {
            case "hunt":
                return String.format(Locale.ROOT,
                        "会在触发坐标 %.1f 格范围内临时运行区域版杀戮光环，战斗/索敌/转向/追击使用杀戮光环当前配置。",
                        getDraftDouble(params, "radius", 3.0D));
            case "run_sequence":
                String sequenceName = getDraftString(params, "sequenceName", "").trim();
                boolean background = getDraftBoolean(params, "backgroundExecution", false);
                String runMode = "interval".equalsIgnoreCase(getDraftString(params, "executeMode", "always"))
                        ? "按间隔触发"
                        : "每次触发都执行";
                return "会" + (background ? "在后台并行" : "在前台") + "执行序列 "
                        + (sequenceName.isEmpty() ? "（未选择）" : sequenceName) + "，当前为" + runMode + "。";
            case "sequence_control":
                boolean resume = "resume".equalsIgnoreCase(getDraftString(params, "operation", "pause"));
                boolean controlBackground = "background".equalsIgnoreCase(getDraftString(params, "targetScope", "foreground"));
                return "会" + (resume ? "恢复" : "暂停") + (controlBackground ? "后台" : "前台")
                        + "当前正在执行的序列；前台暂停时会一并暂停当前寻路。";
            case "disconnect":
                return "会立即断开当前连接，并触发断线后的状态清理与联动逻辑。";
            default:
                if (isWaitActionType(type)) {
                    return "会在条件满足前保持等待，满足后继续执行后续动作。";
                }
                if (isCaptureActionType(type)) {
                    return "会把当前捕获结果写入变量，供后续动作直接使用。";
                }
                return "保存后将按当前参数执行此动作。";
        }
    }

    public static String buildActionRiskHint(ActionData draft, int liveValidationErrorCount) {
        if (draft == null) {
            return "当前草稿尚未形成完整动作。";
        }
        JsonObject params = draft.params == null ? new JsonObject() : draft.params;
        String type = safe(draft.type).trim().toLowerCase(Locale.ROOT);
        if ("hunt".equals(type)) {
            if (getDraftInt(params, "noTargetSkipCount", 0) > 0) {
                return "无目标时会直接跳过 " + Math.max(0, getDraftInt(params, "noTargetSkipCount", 0)) + " 个动作。";
            }
            return "中心搜怪只限制固定范围和动作名单；攻击细节会读取杀戮光环当前配置。";
        }
        if ("run_sequence".equals(type)) {
            if (getDraftString(params, "sequenceName", "").trim().isEmpty()) {
                return "还没有选择要执行的序列。";
            }
            if (getDraftBoolean(params, "backgroundExecution", false)) {
                return "后台执行会与当前序列并行运行，适合持续性逻辑。";
            }
            if ("interval".equalsIgnoreCase(getDraftString(params, "executeMode", "always"))) {
                return "当前不是每次都触发，而是每 "
                        + Math.max(1, getDraftInt(params, "executeEveryCount", 1)) + " 次执行一次。";
            }
            return "风险较低，建议确认目标序列本身不会递归或抢占关键资源。";
        }
        if ("sequence_control".equals(type)) {
            if ("resume".equalsIgnoreCase(getDraftString(params, "operation", "pause"))) {
                return "如果目标序列当前未运行，或本来就不是暂停状态，这次恢复不会产生实际效果。";
            }
            return "暂停后需要在别处补一个“恢复”动作，否则目标序列会一直挂起。";
        }
        if ("disconnect".equals(type)) {
            return "执行后当前会话会立刻断开；若启用了自动重连等功能，会继续按其现有配置处理。";
        }
        if (isWaitActionType(type)) {
            if (getDraftInt(params, "timeoutSkipCount", 0) > 0) {
                return "超时后会跳过 " + Math.max(0, getDraftInt(params, "timeoutSkipCount", 0)) + " 个动作。";
            }
            if (getDraftInt(params, "preExecuteCount", 0) > 0) {
                return "等待前会先放行 " + Math.max(0, getDraftInt(params, "preExecuteCount", 0)) + " 个动作。";
            }
            return "如果条件长期不满足，后续动作会一直被等待逻辑阻塞。";
        }
        if (isCaptureActionType(type)) {
            String varName = getDraftString(params, "varName", "").trim();
            return varName.isEmpty()
                    ? "还没有明确的变量名，建议保存前确认捕获结果写入位置。"
                    : "捕获结果会覆盖变量 " + varName + "，后续动作会读取这个最新值。";
        }
        return liveValidationErrorCount > 0
                ? "当前仍有实时校验错误，建议先修正后再保存。"
                : "未发现明显风险，建议结合摘要再检查一次关键参数。";
    }

    private static int getDraftInt(JsonObject params, String key, int defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsInt();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static double getDraftDouble(JsonObject params, String key, double defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsDouble();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static boolean getDraftBoolean(JsonObject params, String key, boolean defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String getDraftString(JsonObject params, String key, String defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            String value = params.get(key).getAsString();
            return value == null ? defaultValue : value;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static boolean isWaitActionType(String actionType) {
        return actionType != null && (actionType.startsWith("wait_until_") || "wait_combined".equalsIgnoreCase(actionType));
    }

    private static boolean isCaptureActionType(String actionType) {
        return actionType != null && actionType.startsWith("capture_");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
