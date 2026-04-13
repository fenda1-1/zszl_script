package com.zszl.zszlScriptMod.path.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.path.LegacyActionRuntime;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.path.runtime.locks.ResourceLockManager;
import com.zszl.zszlScriptMod.path.template.LegacyActionTemplateManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PathConfigValidator {

    public enum Severity {
        ERROR,
        WARNING
    }

    public static final class Issue {
        private final Severity severity;
        private final String code;
        private final String sequenceName;
        private final int stepIndex;
        private final int actionIndex;
        private final String summary;
        private final String detail;

        public Issue(Severity severity, String code, String sequenceName, int stepIndex, int actionIndex,
                String summary, String detail) {
            this.severity = severity == null ? Severity.WARNING : severity;
            this.code = safe(code);
            this.sequenceName = safe(sequenceName);
            this.stepIndex = stepIndex;
            this.actionIndex = actionIndex;
            this.summary = safe(summary);
            this.detail = safe(detail);
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getCode() {
            return code;
        }

        public String getSequenceName() {
            return sequenceName;
        }

        public int getStepIndex() {
            return stepIndex;
        }

        public int getActionIndex() {
            return actionIndex;
        }

        public String getSummary() {
            return summary;
        }

        public String getDetail() {
            return detail;
        }

        public String getLocationText() {
            StringBuilder builder = new StringBuilder();
            if (!sequenceName.isEmpty()) {
                builder.append(sequenceName);
            }
            if (stepIndex >= 0) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append("步骤 ").append(stepIndex);
            }
            if (actionIndex >= 0) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append("动作 ").append(actionIndex);
            }
            return builder.toString();
        }

        public String toCompactText() {
            StringBuilder builder = new StringBuilder();
            builder.append(severity == Severity.ERROR ? "错误" : "警告");
            String location = getLocationText();
            if (!location.isEmpty()) {
                builder.append(" - ").append(location);
            }
            if (!summary.isEmpty()) {
                builder.append(" - ").append(summary);
            }
            if (!detail.isEmpty()) {
                builder.append("：").append(detail);
            }
            return builder.toString();
        }
    }

    private static final class SequenceResourceProfile {
        private final EnumSet<ResourceLockManager.Resource> directResources = EnumSet
                .noneOf(ResourceLockManager.Resource.class);
        private final EnumSet<ResourceLockManager.Resource> effectiveResources = EnumSet
                .noneOf(ResourceLockManager.Resource.class);
        private boolean effectiveResolved = false;
    }

    private PathConfigValidator() {
    }

    public static List<Issue> validateSequences(Collection<PathSequence> sequences) {
        List<Issue> issues = new ArrayList<>();
        if (sequences == null || sequences.isEmpty()) {
            return issues;
        }

        List<PathSequence> sequenceList = new ArrayList<>(sequences);
        Set<String> sequenceNames = new LinkedHashSet<>();
        Map<String, PathSequence> sequenceMap = new LinkedHashMap<>();
        for (PathSequence sequence : sequenceList) {
            if (sequence != null && sequence.getName() != null && !sequence.getName().trim().isEmpty()) {
                String name = sequence.getName().trim();
                sequenceNames.add(name);
                sequenceMap.put(name, sequence);
            }
        }

        Map<String, List<String>> uuidOwners = new LinkedHashMap<>();
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (PathSequence sequence : sequenceList) {
            if (sequence == null) {
                continue;
            }
            String sequenceName = safe(sequence.getName()).trim();
            graph.putIfAbsent(sequenceName, new ArrayList<>());
            validateSequenceLevel(sequenceName, sequence, issues);
            List<PathStep> steps = sequence.getSteps() == null ? Collections.emptyList() : sequence.getSteps();
            for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
                PathStep step = steps.get(stepIndex);
                if (step == null) {
                    continue;
                }
                validateStep(sequenceName, stepIndex, steps.size(), step, issues);
                List<ActionData> actions = step.getActions() == null ? Collections.emptyList() : step.getActions();
                for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
                    ActionData action = actions.get(actionIndex);
                    validateAction(sequenceName, stepIndex, actionIndex, action, actions, sequenceNames, issues);
                    collectPersistentUuidOwner(sequenceName, stepIndex, actionIndex, action, uuidOwners);
                    collectGraphEdge(sequenceName, action, graph);
                }
            }
        }

        addDuplicateUuidIssues(uuidOwners, issues);
        addCallCycleIssues(graph, issues);
        Map<String, SequenceResourceProfile> profiles = buildSequenceResourceProfiles(sequenceMap, graph);
        addBackgroundConflictIssues(sequenceMap, profiles, issues);
        return issues;
    }

    public static List<Issue> validateActionDraft(ActionData action, String currentSequenceName,
            Collection<PathSequence> sequences) {
        List<Issue> issues = new ArrayList<>();
        if (action == null) {
            return issues;
        }
        Set<String> sequenceNames = new LinkedHashSet<>();
        if (sequences != null) {
            for (PathSequence sequence : sequences) {
                if (sequence != null && sequence.getName() != null && !sequence.getName().trim().isEmpty()) {
                    sequenceNames.add(sequence.getName().trim());
                }
            }
        }
        validateAction(safe(currentSequenceName).trim(), -1, -1, action, null, sequenceNames, issues);
        return issues;
    }

    public static String buildIssueSignature(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Issue issue : issues) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(issue.getSeverity().name()).append(':')
                    .append(issue.getCode()).append(':')
                    .append(issue.getLocationText()).append(':')
                    .append(issue.getSummary());
        }
        return builder.toString();
    }

    private static void validateStep(String sequenceName, int stepIndex, int stepCount, PathStep step,
            List<Issue> issues) {
        if (step.getGotoPoint() == null || step.getGotoPoint().length < 3) {
            issues.add(new Issue(Severity.WARNING, "step_goto_point", sequenceName, stepIndex, -1,
                    "步骤坐标不完整", "GOTO 坐标建议始终保持 3 个分量。"));
        }
    }

    private static void validateSequenceLevel(String sequenceName, PathSequence sequence, List<Issue> issues) {
        if (sequence == null) {
            return;
        }
        List<PathStep> steps = sequence.getSteps() == null ? Collections.emptyList() : sequence.getSteps();
        if (steps.isEmpty()) {
            issues.add(new Issue(Severity.WARNING, "sequence_empty", sequenceName, -1, -1,
                    "序列没有任何步骤", "保存后该序列将无法执行有效逻辑。"));
        }
        if (sequence.getLoopDelayTicks() < 0) {
            issues.add(new Issue(Severity.ERROR, "loop_delay_negative", sequenceName, -1, -1,
                    "循环延迟不能小于 0", "请把循环延迟改成 0 或更大的值。"));
        }
        if (sequence.isNonInterruptingExecution()) {
            issues.add(new Issue(Severity.WARNING, "sequence_background_mode", sequenceName, -1, -1,
                    "序列启用了不打断其他序列", "从主页直接运行时会转为后台执行，建议确认资源锁策略是否符合预期。"));
        }
    }

    private static List<Issue> validateStepJumpTarget(String sequenceName, int stepIndex, int target, int currentStep,
            int stepCount) {
        List<Issue> issues = new ArrayList<>();
        if (target < 0) {
            issues.add(new Issue(Severity.ERROR, "failure_jump_missing", sequenceName, stepIndex, -1,
                    "失败跳转目标无效", "失败策略为跳转步骤时，必须填写大于等于 0 的目标步骤。"));
        } else if (target >= stepCount) {
            issues.add(new Issue(Severity.ERROR, "failure_jump_range", sequenceName, stepIndex, -1,
                    "失败跳转目标超出范围", "当前序列只有 " + stepCount + " 个步骤。"));
        } else if (target == currentStep) {
            issues.add(new Issue(Severity.WARNING, "failure_jump_self", sequenceName, stepIndex, -1,
                    "失败跳转回当前步骤", "连续失败时可能反复重试当前步骤，建议确认是否会形成死循环。"));
        }
        return issues;
    }

    private static void validateAction(String sequenceName, int stepIndex, int actionIndex, ActionData action,
            List<ActionData> stepActions, Set<String> sequenceNames, List<Issue> issues) {
        if (action == null) {
            return;
        }

        String type = safe(action.type).trim().toLowerCase(Locale.ROOT);
        JsonObject params = action.params == null ? new JsonObject() : action.params;
        switch (type) {
            case "run_sequence":
                validateRunSequence(sequenceName, stepIndex, actionIndex, params, sequenceNames, issues);
                break;
            case "stop_current_sequence":
                validateStopCurrentSequence(sequenceName, stepIndex, actionIndex, params, issues);
                break;
            case "run_template":
                validateRunTemplate(sequenceName, stepIndex, actionIndex, params, sequenceNames, issues);
                break;
            case "send_packet":
                if (isBlank(getString(params, "hex"))) {
                    issues.add(new Issue(Severity.ERROR, "send_packet_hex", sequenceName, stepIndex, actionIndex,
                            "发送数据包缺少 HEX 数据", "请至少填写 HEX 数据。"));
                }
                if (isBlank(getString(params, "channel")) && !params.has("packetId")) {
                    issues.add(new Issue(Severity.ERROR, "send_packet_target", sequenceName, stepIndex, actionIndex,
                            "发送数据包未指定频道或 Packet ID", "请填写 FML 频道，或填写标准包 Packet ID。"));
                }
                break;
            case "goto_action":
                if (stepActions != null && !stepActions.isEmpty()) {
                    int targetIndex = getInt(params, "targetActionIndex", -1);
                    if (targetIndex < 0 || targetIndex >= stepActions.size()) {
                        issues.add(new Issue(Severity.ERROR, "goto_action_range", sequenceName, stepIndex, actionIndex,
                                "跳转动作序号越界", "目标动作序号必须在 0 到 " + Math.max(0, stepActions.size() - 1) + " 之间。"));
                    } else if (targetIndex == actionIndex) {
                        issues.add(new Issue(Severity.WARNING, "goto_action_self", sequenceName, stepIndex, actionIndex,
                                "跳转到当前动作", "这会让当前动作原地循环，建议确认是否需要配合条件或计数器使用。"));
                    }
                }
                break;
            case "skip_actions":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "count", "跳过动作数", issues);
                if (stepActions != null && !stepActions.isEmpty()) {
                    int skipActionCount = getInt(params, "count", 1);
                    if (actionIndex >= 0 && actionIndex + Math.max(0, skipActionCount) + 1 >= stepActions.size()) {
                        issues.add(new Issue(Severity.WARNING, "skip_actions_reach_end", sequenceName, stepIndex,
                                actionIndex,
                                "跳过动作将直接到步骤末尾", "运行时会直接结束当前步骤并进入后续步骤，请确认这正是你想要的控制流。"));
                    }
                }
                break;
            case "skip_steps":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "count", "跳过步骤数", issues);
                break;
            case "repeat_actions":
                int bodyCount = getInt(params, "bodyCount", 0);
                if (bodyCount <= 0) {
                    issues.add(new Issue(Severity.ERROR, "repeat_actions_body", sequenceName, stepIndex, actionIndex,
                            "循环体动作数无效", "循环体动作数必须大于 0。"));
                } else if (stepActions != null && actionIndex >= 0 && actionIndex + bodyCount >= stepActions.size()) {
                    issues.add(new Issue(Severity.WARNING, "repeat_actions_truncate", sequenceName, stepIndex, actionIndex,
                            "循环体超出剩余动作", "运行时会截断到当前步骤末尾，建议显式调整循环体动作数。"));
                }
                break;
            case "wait_combined":
                if (isBlank(getString(params, "conditionsText"))) {
                    issues.add(new Issue(Severity.ERROR, "wait_combined_empty", sequenceName, stepIndex, actionIndex,
                            "组合等待没有条件表达式", "请至少填写一条条件表达式。"));
                } else {
                    validateBooleanExpressionList(sequenceName, stepIndex, actionIndex,
                            getString(params, "conditionsText"), "组合条件表达式", issues);
                }
                if (!isBlank(getString(params, "cancelExpression"))) {
                    validateBooleanExpression(sequenceName, stepIndex, actionIndex,
                            getString(params, "cancelExpression"), params, "取消表达式", issues);
                }
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount",
                        "超时跳过动作数", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "preExecuteCount", "先执行动作数",
                        issues);
                break;
            case "condition_expression":
            case "wait_until_expression":
                List<String> booleanExpressions = readBooleanExpressionList(params, "expressions", "expression");
                if (booleanExpressions.isEmpty()) {
                    issues.add(new Issue(Severity.ERROR, "expression_empty", sequenceName, stepIndex, actionIndex,
                            "表达式为空", "请至少添加一条条件表达式。"));
                } else {
                    validateBooleanExpressionList(sequenceName, stepIndex, actionIndex,
                            booleanExpressions, params, "条件表达式", issues);
                }
                if ("wait_until_expression".equals(type)) {
                    validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时",
                            issues);
                    validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount",
                            "超时跳过动作数", issues);
                } else {
                    validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "skipCount", "失败跳过动作数",
                        issues);
                }
                break;
            case "set_var":
                if (!isBlank(getString(params, "expression"))) {
                    validateValueExpression(sequenceName, stepIndex, actionIndex,
                            getString(params, "expression"), params, "赋值表达式", issues);
                    addSetVarAutoInitWarning(sequenceName, stepIndex, actionIndex, params, issues);
                }
                break;
            case "delay":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "ticks", "延迟", issues);
                break;
            case "capture_inventory_slot":
                validateCaptureInventorySlot(sequenceName, stepIndex, actionIndex, params, issues);
                break;
            case "capture_entity_list":
                validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, "maxCount", "最大采集数量",
                        1, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "radius", "范围", false,
                        issues);
                break;
            case "capture_packet_field":
                String lookupMode = getString(params, "lookupMode");
                String fieldKey = getString(params, "fieldKey");
                if ("VARIABLE".equalsIgnoreCase(lookupMode) && isBlank(fieldKey)) {
                    issues.add(new Issue(Severity.ERROR, "capture_packet_field_variable_key", sequenceName, stepIndex,
                            actionIndex, "包字段采集缺少变量键", "运行时变量模式下必须填写变量键。"));
                }
                break;
            case "capture_scoreboard":
                int lineIndex = getInt(params, "lineIndex", -1);
                if (lineIndex < -1) {
                    issues.add(new Issue(Severity.ERROR, "capture_scoreboard_line", sequenceName, stepIndex, actionIndex,
                            "记分板指定行索引无效", "指定行索引只能填写 -1 或更大的值。"));
                }
                break;
            case "capture_screen_region":
                validateVisionRegionRect(sequenceName, stepIndex, actionIndex, params, issues);
                break;
            case "condition_inventory_item":
            case "wait_until_inventory_item":
                validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, "count", "最少数量", 1, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount", "超时跳过动作数",
                        issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "skipCount", "失败跳过动作数", issues);
                break;
            case "wait_until_gui_title":
            case "wait_until_player_in_area":
            case "wait_until_entity_nearby":
            case "wait_until_hud_text":
            case "wait_until_captured_id":
            case "wait_until_packet_text":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount", "超时跳过动作数",
                        issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "preExecuteCount", "先执行动作数",
                        issues);
                break;
            case "wait_until_screen_region":
                validateVisionRegionRect(sequenceName, stepIndex, actionIndex, params, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount", "超时跳过动作数",
                        issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "colorTolerance", "颜色容差",
                        false, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "similarityThreshold", "相似度阈值",
                        false, issues);
                break;
            case "condition_gui_title":
            case "condition_player_in_area":
            case "condition_entity_nearby":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "skipCount", "失败跳过动作数", issues);
                break;
            case "move_inventory_item_to_hotbar":
                int hotbarSlot = getInt(params, "targetHotbarSlot", -1);
                if (hotbarSlot < 1 || hotbarSlot > 9) {
                    issues.add(new Issue(Severity.ERROR, "hotbar_slot_range", sequenceName, stepIndex, actionIndex,
                            "快捷栏位超出范围", "目标快捷栏位必须填写 1-9。"));
                }
                break;
            case "switch_hotbar_slot":
                int switchHotbarSlot = getInt(params, "targetHotbarSlot", -1);
                if (switchHotbarSlot < 1 || switchHotbarSlot > 9) {
                    issues.add(new Issue(Severity.ERROR, "hotbar_slot_range", sequenceName, stepIndex, actionIndex,
                            "快捷栏位超出范围", "目标快捷栏位必须填写 1-9。"));
                }
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "useAfterSwitchDelayTicks",
                        "切换后使用延迟", issues);
                break;
            case "silentuse":
                int tempHotbarSlot = getInt(params, "tempslot", 0);
                if (tempHotbarSlot < 0 || tempHotbarSlot > 8) {
                    issues.add(new Issue(Severity.ERROR, "temp_hotbar_slot_range", sequenceName, stepIndex, actionIndex,
                            "临时槽位超出范围", "临时槽位必须填写 0-8。"));
                }
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchDelayTicks",
                        "切换物品延迟", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "useDelayTicks",
                        "切后使用延迟", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchBackDelayTicks",
                        "切回延迟", issues);
                break;
            case "use_hotbar_item":
                validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, "count", "使用次数", 1, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchItemDelayTicks",
                        "切换物品延迟", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchDelayTicks",
                        "切后使用延迟", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchBackDelayTicks",
                        "切回延迟", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "intervalTicks", "使用间隔",
                        issues);
                break;
            case "use_held_item":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "delayTicks", "延迟", issues);
                break;
            case "hunt":
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "radius", "搜索半径", false,
                        issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "trackingDistance", "追踪距离",
                        false, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "attackCount", "攻击次数", issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "noTargetSkipCount",
                        "无目标时跳过动作数", issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "huntChaseIntervalSeconds",
                        "追怪间隔秒数", true, issues);
                validateHuntAttackSequence(sequenceName, stepIndex, actionIndex, params, sequenceNames, issues);
                break;
            case "autochestclick":
            case "move_inventory_items_to_chest_slots":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "delayTicks", "延迟", issues);
                if (ItemFilterHandler.readMoveChestFilterRules(params).isEmpty()) {
                    issues.add(new Issue(Severity.ERROR, "move_chest_filter_missing", sequenceName, stepIndex,
                            actionIndex,
                            "容器物品批量移动缺少过滤条件",
                            "至少需要添加一条有效规则；每条规则可填写物品名、NBT，或两者同时填写。"));
                }
                break;
            case "window_click":
                if (isBlank(getString(params, "uuid"))) {
                    issues.add(new Issue(Severity.WARNING, "persistent_uuid_missing", sequenceName, stepIndex, actionIndex,
                            "动作 UUID 缺失", "保存后建议重新确认该动作已生成稳定 UUID，避免计数或状态冲突。"));
                }
                break;
            default:
                break;
        }
    }

    private static void validateRunSequence(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            Set<String> sequenceNames, List<Issue> issues) {
        String target = getString(params, "sequenceName").trim();
        if (isBlank(getString(params, "uuid"))) {
            issues.add(new Issue(Severity.WARNING, "persistent_uuid_missing", sequenceName, stepIndex, actionIndex,
                    "动作 UUID 缺失", "保存后建议重新确认该动作已生成稳定 UUID，避免计数或状态冲突。"));
        }
        if (target.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "run_sequence_missing", sequenceName, stepIndex, actionIndex,
                    "执行序列未选择目标", "请先选择要执行的目标序列。"));
            return;
        }
        if (target.equalsIgnoreCase(sequenceName)) {
            issues.add(new Issue(Severity.ERROR, "run_sequence_self", sequenceName, stepIndex, actionIndex,
                    "执行序列指向自身", "这会直接形成自调用循环。"));
        }
        if (!sequenceNames.isEmpty() && !sequenceNames.contains(target)) {
            issues.add(new Issue(Severity.ERROR, "run_sequence_missing_target", sequenceName, stepIndex, actionIndex,
                    "执行序列目标不存在", "找不到目标序列: " + target));
        }
        if ("interval".equalsIgnoreCase(getString(params, "executeMode"))
                && getInt(params, "executeEveryCount", 1) <= 1) {
            issues.add(new Issue(Severity.WARNING, "run_sequence_interval_one", sequenceName, stepIndex, actionIndex,
                    "间隔执行次数等于 1", "这与“每次执行”效果一致，建议改回每次执行或把次数调大。"));
        }
    }

    private static void validateStopCurrentSequence(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, List<Issue> issues) {
        String scope = getString(params, "targetScope").trim();
        if (isBlank(scope)) {
            return;
        }
        if (!"foreground".equalsIgnoreCase(scope) && !"background".equalsIgnoreCase(scope)) {
            issues.add(new Issue(Severity.WARNING, "stop_current_sequence_scope_invalid",
                    sequenceName, stepIndex, actionIndex,
                    "停止范围无效", "仅支持 foreground 或 background，留空时默认 foreground。"));
        }
    }

    private static void validateRunTemplate(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            Set<String> sequenceNames, List<Issue> issues) {
        String templateName = getString(params, "templateName").trim();
        if (isBlank(getString(params, "uuid"))) {
            issues.add(new Issue(Severity.WARNING, "persistent_uuid_missing", sequenceName, stepIndex, actionIndex,
                    "动作 UUID 缺失", "保存后建议重新确认该动作已生成稳定 UUID，避免计数或状态冲突。"));
        }
        if (templateName.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "run_template_missing", sequenceName, stepIndex, actionIndex,
                    "执行模板未选择目标", "请先选择模板。"));
            return;
        }
        LegacyActionTemplateManager.TemplateEditModel model = LegacyActionTemplateManager.getTemplate(templateName);
        if (model == null) {
            issues.add(new Issue(Severity.ERROR, "run_template_missing_template", sequenceName, stepIndex, actionIndex,
                    "模板不存在", "找不到模板: " + templateName));
            return;
        }
        String targetSequence = safe(LegacyActionTemplateManager.resolveTemplateTargetSequence(templateName)).trim();
        if (targetSequence.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "run_template_missing_target", sequenceName, stepIndex, actionIndex,
                    "模板未绑定目标序列", "模板 " + templateName + " 还没有绑定可执行的序列。"));
            return;
        }
        if (targetSequence.equalsIgnoreCase(sequenceName)) {
            issues.add(new Issue(Severity.ERROR, "run_template_self", sequenceName, stepIndex, actionIndex,
                    "模板目标指向当前序列", "这会形成直接递归调用。"));
        }
        if (!sequenceNames.isEmpty() && !sequenceNames.contains(targetSequence)) {
            issues.add(new Issue(Severity.ERROR, "run_template_missing_sequence", sequenceName, stepIndex, actionIndex,
                    "模板目标序列不存在", "模板 " + templateName + " 指向的序列不存在: " + targetSequence));
        }
        if ("interval".equalsIgnoreCase(getString(params, "executeMode"))
                && getInt(params, "executeEveryCount", 1) <= 1) {
            issues.add(new Issue(Severity.WARNING, "run_template_interval_one", sequenceName, stepIndex, actionIndex,
                    "模板间隔执行次数等于 1", "这与“每次执行”效果一致，建议改回每次执行或把次数调大。"));
        }
    }

    private static void validateHuntAttackSequence(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            Set<String> sequenceNames, List<Issue> issues) {
        if (!KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(getString(params, "attackMode").trim())) {
            return;
        }
        String target = getString(params, "attackSequenceName").trim();
        if (target.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "hunt_attack_sequence_missing", sequenceName, stepIndex, actionIndex,
                    "搜怪击杀未选择攻击序列", "当攻击方式为“执行序列攻击”时，必须选择一条攻击序列。"));
            return;
        }
        if (target.equalsIgnoreCase(sequenceName)) {
            issues.add(new Issue(Severity.ERROR, "hunt_attack_sequence_self", sequenceName, stepIndex, actionIndex,
                    "攻击序列指向当前序列", "这会让搜怪击杀动作递归调用当前序列。"));
        }
        if (!sequenceNames.isEmpty() && !sequenceNames.contains(target)) {
            issues.add(new Issue(Severity.ERROR, "hunt_attack_sequence_missing_target", sequenceName, stepIndex, actionIndex,
                    "攻击序列目标不存在", "找不到攻击序列: " + target));
        }
    }

    private static void validateCaptureInventorySlot(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, List<Issue> issues) {
        String area = getString(params, "slotArea").trim().toUpperCase(Locale.ROOT);
        if (area.isEmpty()) {
            area = "MAIN";
        }
        int slotIndex = getInt(params, "slotIndex", -1);
        if (slotIndex < 0) {
            issues.add(new Issue(Severity.ERROR, "capture_slot_negative", sequenceName, stepIndex, actionIndex,
                    "槽位索引不能小于 0", "请填写合法的槽位索引。"));
            return;
        }
        switch (area) {
            case "MAIN":
                if (slotIndex > 35) {
                    issues.add(new Issue(Severity.WARNING, "capture_slot_main_range", sequenceName, stepIndex, actionIndex,
                            "主背包槽位超出常规范围", "主背包模式通常建议填写 0-35。"));
                }
                break;
            case "HOTBAR":
                if (slotIndex > 8) {
                    issues.add(new Issue(Severity.ERROR, "capture_slot_hotbar_range", sequenceName, stepIndex, actionIndex,
                            "快捷栏槽位超出范围", "快捷栏模式下槽位索引必须是 0-8。"));
                }
                break;
            case "ARMOR":
                if (slotIndex > 3) {
                    issues.add(new Issue(Severity.ERROR, "capture_slot_armor_range", sequenceName, stepIndex, actionIndex,
                            "护甲栏槽位超出范围", "护甲栏模式下槽位索引必须是 0-3。"));
                }
                break;
            case "OFFHAND":
                if (slotIndex > 0) {
                    issues.add(new Issue(Severity.ERROR, "capture_slot_offhand_range", sequenceName, stepIndex, actionIndex,
                            "副手槽位超出范围", "副手模式下槽位索引只能是 0。"));
                }
                break;
            default:
                issues.add(new Issue(Severity.ERROR, "capture_slot_area_invalid", sequenceName, stepIndex, actionIndex,
                        "槽位区域无效", "支持的槽位区域只有 MAIN/HOTBAR/ARMOR/OFFHAND。"));
                break;
        }
    }

    private static void validateVisionRegionRect(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            List<Issue> issues) {
        if (params == null || !params.has("regionRect")) {
            issues.add(new Issue(Severity.ERROR, "vision_region_missing", sequenceName, stepIndex, actionIndex,
                    "视觉区域未配置", "请填写 [x,y,width,height] 区域。"));
            return;
        }
        try {
            int[] values = new int[4];
            if (params.get("regionRect").isJsonArray()) {
                if (params.getAsJsonArray("regionRect").size() < 4) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 4; i++) {
                    values[i] = (int) Math.round(params.getAsJsonArray("regionRect").get(i).getAsDouble());
                }
            } else {
                String text = getString(params, "regionRect").replace("[", "").replace("]", "");
                String[] parts = text.split(",");
                if (parts.length < 4) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 4; i++) {
                    values[i] = (int) Math.round(Double.parseDouble(parts[i].trim()));
                }
            }
            if (values[2] <= 0 || values[3] <= 0) {
                issues.add(new Issue(Severity.ERROR, "vision_region_size", sequenceName, stepIndex, actionIndex,
                        "视觉区域宽高必须大于 0", "当前区域为 [" + values[0] + "," + values[1] + "," + values[2] + "," + values[3] + "]"));
            }
            if (values[0] < 0 || values[1] < 0) {
                issues.add(new Issue(Severity.ERROR, "vision_region_pos", sequenceName, stepIndex, actionIndex,
                        "视觉区域坐标不能小于 0", "当前区域为 [" + values[0] + "," + values[1] + "," + values[2] + "," + values[3] + "]"));
            }
        } catch (Exception e) {
            issues.add(new Issue(Severity.ERROR, "vision_region_parse", sequenceName, stepIndex, actionIndex,
                    "视觉区域格式无效", "请按 [x,y,width,height] 填写。"));
        }
    }

    private static void collectPersistentUuidOwner(String sequenceName, int stepIndex, int actionIndex, ActionData action,
            Map<String, List<String>> uuidOwners) {
        if (action == null || action.params == null) {
            return;
        }
        String type = safe(action.type).trim().toLowerCase(Locale.ROOT);
        if (!"window_click".equals(type) && !"run_sequence".equals(type) && !"run_template".equals(type)) {
            return;
        }
        String uuid = getString(action.params, "uuid").trim();
        if (uuid.isEmpty()) {
            return;
        }
        uuidOwners.computeIfAbsent(uuid, key -> new ArrayList<>())
                .add(sequenceName + " / 步骤 " + stepIndex + " / 动作 " + actionIndex + " / " + type);
    }

    private static void addDuplicateUuidIssues(Map<String, List<String>> uuidOwners, List<Issue> issues) {
        for (Map.Entry<String, List<String>> entry : uuidOwners.entrySet()) {
            List<String> owners = entry.getValue();
            if (owners == null || owners.size() < 2) {
                continue;
            }
            issues.add(new Issue(Severity.WARNING, "duplicate_uuid", "", -1, -1,
                    "检测到重复动作 UUID",
                    "以下动作共享同一个 UUID，可能导致状态串线: " + String.join("；", owners)));
        }
    }

    private static void collectGraphEdge(String sequenceName, ActionData action, Map<String, List<String>> graph) {
        if (action == null || action.params == null) {
            return;
        }
        String type = safe(action.type).trim().toLowerCase(Locale.ROOT);
        String target = "";
        if ("run_sequence".equals(type)) {
            target = getString(action.params, "sequenceName").trim();
        } else if ("hunt".equals(type)
                && KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(getString(action.params, "attackMode").trim())) {
            target = getString(action.params, "attackSequenceName").trim();
        } else if ("run_template".equals(type)) {
            target = safe(LegacyActionTemplateManager.resolveTemplateTargetSequence(getString(action.params, "templateName"))).trim();
        }
        if (target.isEmpty()) {
            return;
        }
        graph.computeIfAbsent(sequenceName, key -> new ArrayList<>()).add(target);
        graph.putIfAbsent(target, new ArrayList<>());
    }

    private static void addCallCycleIssues(Map<String, List<String>> graph, List<Issue> issues) {
        Set<String> visited = new LinkedHashSet<>();
        Set<String> visiting = new LinkedHashSet<>();
        Deque<String> path = new ArrayDeque<>();
        Set<String> cycleKeys = new LinkedHashSet<>();
        for (String sequenceName : graph.keySet()) {
            dfsSequenceGraph(sequenceName, graph, visited, visiting, path, cycleKeys, issues);
        }
    }

    private static void dfsSequenceGraph(String sequenceName, Map<String, List<String>> graph, Set<String> visited,
            Set<String> visiting, Deque<String> path, Set<String> cycleKeys, List<Issue> issues) {
        if (visited.contains(sequenceName)) {
            return;
        }
        visiting.add(sequenceName);
        path.addLast(sequenceName);
        for (String target : graph.getOrDefault(sequenceName, Collections.emptyList())) {
            if (target == null || target.trim().isEmpty()) {
                continue;
            }
            if (visiting.contains(target)) {
                List<String> cycle = extractCycle(path, target);
                String cycleKey = String.join("->", cycle).toLowerCase(Locale.ROOT);
                if (cycleKeys.add(cycleKey)) {
                    issues.add(new Issue(Severity.WARNING, "sequence_cycle", sequenceName, -1, -1,
                            "检测到序列调用环", "调用链存在循环: " + String.join(" -> ", cycle)));
                }
                continue;
            }
            if (!visited.contains(target)) {
                dfsSequenceGraph(target, graph, visited, visiting, path, cycleKeys, issues);
            }
        }
        path.removeLast();
        visiting.remove(sequenceName);
        visited.add(sequenceName);
    }

    private static List<String> extractCycle(Deque<String> path, String cycleStart) {
        List<String> cycle = new ArrayList<>();
        boolean collecting = false;
        for (String item : path) {
            if (!collecting && item.equalsIgnoreCase(cycleStart)) {
                collecting = true;
            }
            if (collecting) {
                cycle.add(item);
            }
        }
        cycle.add(cycleStart);
        return cycle;
    }

    private static Map<String, SequenceResourceProfile> buildSequenceResourceProfiles(Map<String, PathSequence> sequenceMap,
            Map<String, List<String>> graph) {
        Map<String, SequenceResourceProfile> profiles = new LinkedHashMap<>();
        for (Map.Entry<String, PathSequence> entry : sequenceMap.entrySet()) {
            SequenceResourceProfile profile = new SequenceResourceProfile();
            PathSequence sequence = entry.getValue();
            if (sequence != null && sequence.getSteps() != null) {
                for (PathStep step : sequence.getSteps()) {
                    profile.directResources.addAll(resolveMovementResources(step));
                    if (step == null || step.getActions() == null) {
                        continue;
                    }
                    for (ActionData action : step.getActions()) {
                        profile.directResources.addAll(resolveActionResources(action));
                    }
                }
            }
            profiles.put(entry.getKey(), profile);
        }

        for (String sequenceName : profiles.keySet()) {
            resolveEffectiveResources(sequenceName, profiles, graph, new LinkedHashSet<String>());
        }
        return profiles;
    }

    private static EnumSet<ResourceLockManager.Resource> resolveEffectiveResources(String sequenceName,
            Map<String, SequenceResourceProfile> profiles, Map<String, List<String>> graph, Set<String> visiting) {
        SequenceResourceProfile profile = profiles.get(sequenceName);
        if (profile == null) {
            return EnumSet.noneOf(ResourceLockManager.Resource.class);
        }
        if (profile.effectiveResolved) {
            return profile.effectiveResources.isEmpty()
                    ? EnumSet.noneOf(ResourceLockManager.Resource.class)
                    : EnumSet.copyOf(profile.effectiveResources);
        }
        if (!visiting.add(sequenceName)) {
            return profile.directResources.isEmpty()
                    ? EnumSet.noneOf(ResourceLockManager.Resource.class)
                    : EnumSet.copyOf(profile.directResources);
        }

        profile.effectiveResources.clear();
        profile.effectiveResources.addAll(profile.directResources);
        for (String target : graph.getOrDefault(sequenceName, Collections.<String>emptyList())) {
            profile.effectiveResources.addAll(resolveEffectiveResources(target, profiles, graph, visiting));
        }
        visiting.remove(sequenceName);
        profile.effectiveResolved = true;
        return profile.effectiveResources.isEmpty()
                ? EnumSet.noneOf(ResourceLockManager.Resource.class)
                : EnumSet.copyOf(profile.effectiveResources);
    }

    private static void addBackgroundConflictIssues(Map<String, PathSequence> sequenceMap,
            Map<String, SequenceResourceProfile> profiles, List<Issue> issues) {
        for (Map.Entry<String, PathSequence> entry : sequenceMap.entrySet()) {
            String sequenceName = entry.getKey();
            PathSequence sequence = entry.getValue();
            if (sequence == null || sequence.getSteps() == null) {
                continue;
            }

            List<String> backgroundActionLocations = new ArrayList<>();
            for (int stepIndex = 0; stepIndex < sequence.getSteps().size(); stepIndex++) {
                PathStep step = sequence.getSteps().get(stepIndex);
                if (step == null || step.getActions() == null) {
                    continue;
                }
                for (int actionIndex = 0; actionIndex < step.getActions().size(); actionIndex++) {
                    ActionData action = step.getActions().get(actionIndex);
                    if (!isBackgroundCallAction(action)) {
                        continue;
                    }
                    backgroundActionLocations.add("步骤 " + stepIndex + " / 动作 " + actionIndex);
                    String target = resolveCallTarget(action);
                    if (target.isEmpty() || !sequenceMap.containsKey(target)) {
                        continue;
                    }

                    SequenceResourceProfile callerProfile = profiles.get(sequenceName);
                    SequenceResourceProfile targetProfile = profiles.get(target);
                    if (callerProfile == null || targetProfile == null) {
                        continue;
                    }

                    EnumSet<ResourceLockManager.Resource> overlap = callerProfile.directResources.isEmpty()
                            ? EnumSet.noneOf(ResourceLockManager.Resource.class)
                            : EnumSet.copyOf(callerProfile.directResources);
                    overlap.retainAll(targetProfile.effectiveResources);
                    if (!overlap.isEmpty()) {
                        issues.add(new Issue(Severity.WARNING, "background_resource_overlap", sequenceName, stepIndex,
                                actionIndex, "后台子序列与当前序列存在资源重叠",
                                "目标序列 " + target + " 会与当前序列争用资源: " + formatResources(overlap)
                                        + "。若是有意并发可忽略，否则建议拆分资源或调整锁策略。"));
                    }

                    if ("WAIT".equalsIgnoreCase(sequence.getLockConflictPolicy())
                            && "WAIT".equalsIgnoreCase(sequenceMap.get(target).getLockConflictPolicy())
                            && !overlap.isEmpty()) {
                        issues.add(new Issue(Severity.WARNING, "background_wait_policy", sequenceName, stepIndex,
                                actionIndex, "前后台都设置为等待冲突",
                                "当前序列和目标序列都使用 WAIT，发生冲突时更容易长时间等待。"));
                    }
                }
            }

            if (backgroundActionLocations.size() > 1) {
                issues.add(new Issue(Severity.WARNING, "multiple_background_actions", sequenceName, -1, -1,
                        "同一序列里存在多个后台子序列动作",
                        "新的后台子序列会打断旧的后台子序列，相关位置: " + String.join("；", backgroundActionLocations)));
            }
        }
    }

    private static boolean isBackgroundCallAction(ActionData action) {
        if (action == null || action.params == null) {
            return false;
        }
        String type = safe(action.type).trim().toLowerCase(Locale.ROOT);
        return ("run_sequence".equals(type) || "run_template".equals(type))
                && action.params.has("backgroundExecution")
                && action.params.get("backgroundExecution").getAsBoolean();
    }

    private static String resolveCallTarget(ActionData action) {
        if (action == null || action.params == null) {
            return "";
        }
        String type = safe(action.type).trim().toLowerCase(Locale.ROOT);
        if ("run_sequence".equals(type)) {
            return getString(action.params, "sequenceName").trim();
        }
        if ("run_template".equals(type)) {
            return safe(LegacyActionTemplateManager.resolveTemplateTargetSequence(getString(action.params, "templateName")))
                    .trim();
        }
        return "";
    }

    private static EnumSet<ResourceLockManager.Resource> resolveMovementResources(PathStep step) {
        EnumSet<ResourceLockManager.Resource> resources = EnumSet.noneOf(ResourceLockManager.Resource.class);
        if (step != null && step.getGotoPoint() != null && step.getGotoPoint().length >= 3) {
            resources.add(ResourceLockManager.Resource.MOVE);
        }
        return resources;
    }

    private static EnumSet<ResourceLockManager.Resource> resolveActionResources(ActionData action) {
        EnumSet<ResourceLockManager.Resource> resources = EnumSet.noneOf(ResourceLockManager.Resource.class);
        String type = action == null || action.type == null ? "" : action.type.trim().toLowerCase(Locale.ROOT);
        switch (type) {
            case "setview":
                resources.add(ResourceLockManager.Resource.LOOK);
                break;
            case "click":
            case "rightclickblock":
            case "rightclickentity":
                resources.add(ResourceLockManager.Resource.INTERACT);
                resources.add(ResourceLockManager.Resource.LOOK);
                break;
            case "window_click":
            case "conditional_window_click":
            case "takeallitems":
            case "take_all_items_safe":
            case "dropfiltereditems":
            case "autochestclick":
            case "move_inventory_items_to_chest_slots":
            case "warehouse_auto_deposit":
            case "transferitemstowarehouse":
            case "move_inventory_item_to_hotbar":
            case "switch_hotbar_slot":
            case "silentuse":
            case "use_hotbar_item":
            case "use_held_item":
            case "autoeat":
            case "autoequip":
            case "autopickup":
                resources.add(ResourceLockManager.Resource.INVENTORY);
                break;
            case "send_packet":
                resources.add(ResourceLockManager.Resource.PACKET);
                break;
            case "wait_until_inventory_item":
            case "wait_until_gui_title":
            case "wait_until_player_in_area":
            case "wait_until_entity_nearby":
            case "wait_until_hud_text":
            case "wait_until_expression":
            case "wait_until_captured_id":
            case "wait_until_packet_text":
            case "wait_combined":
                resources.add(ResourceLockManager.Resource.WAIT);
                break;
            case "hunt":
            case "toggle_kill_aura":
                resources.add(ResourceLockManager.Resource.COMBAT);
                resources.add(ResourceLockManager.Resource.LOOK);
                resources.add(ResourceLockManager.Resource.MOVE);
                break;
            default:
                break;
        }
        return resources;
    }

    private static String formatResources(EnumSet<ResourceLockManager.Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return "(无)";
        }
        List<String> names = new ArrayList<>();
        for (ResourceLockManager.Resource resource : resources) {
            names.add(resource.name());
        }
        return String.join("/", names);
    }

    private static void validateNonNegativeIntParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, List<Issue> issues) {
        if (params == null || key == null || !params.has(key)) {
            return;
        }
        int value = getInt(params, key, 0);
        if (value < 0) {
            issues.add(new Issue(Severity.ERROR, key + "_negative", sequenceName, stepIndex, actionIndex,
                    label + "不能小于 0", "参数 " + key + " 当前值为 " + value + "。"));
        }
    }

    private static void validatePositiveIntParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, int minimum, List<Issue> issues) {
        if (params == null || key == null || !params.has(key)) {
            return;
        }
        int value = getInt(params, key, minimum);
        if (value < minimum) {
            issues.add(new Issue(Severity.ERROR, key + "_range", sequenceName, stepIndex, actionIndex,
                    label + "过小", "参数 " + key + " 至少应为 " + minimum + "，当前为 " + value + "。"));
        }
    }

    private static void validatePositiveDoubleParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, boolean allowZero, List<Issue> issues) {
        if (params == null || key == null || !params.has(key) || params.get(key).isJsonNull()) {
            return;
        }
        try {
            double value = params.get(key).getAsDouble();
            if (allowZero ? value < 0D : value <= 0D) {
                issues.add(new Issue(Severity.ERROR, key + "_range", sequenceName, stepIndex, actionIndex,
                        label + "无效", "参数 " + key + " 当前值为 " + value + "。"));
            }
        } catch (Exception ignored) {
        }
    }

    private static void validateValueExpression(String sequenceName, int stepIndex, int actionIndex,
            String expression, JsonObject params, String label, List<Issue> issues) {
        try {
            LegacyActionRuntime.validateValueExpressionSyntax(expression, params);
        } catch (Exception e) {
            issues.add(new Issue(Severity.ERROR, "expression_invalid_value", sequenceName, stepIndex, actionIndex,
                    label + "无效", safe(e.getMessage())));
        }
    }

    private static void addSetVarAutoInitWarning(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, List<Issue> issues) {
        String varName = getString(params, "name").trim();
        String expression = getString(params, "expression").trim();
        if (isBlank(varName) || isBlank(expression)) {
            return;
        }
        if (expression.startsWith("+=") || expression.startsWith("-=")
                || expression.startsWith("*=") || expression.startsWith("/=")
                || expression.startsWith("%=")
                || expression.toLowerCase(Locale.ROOT).contains(varName.toLowerCase(Locale.ROOT))) {
            issues.add(new Issue(Severity.WARNING, "set_var_auto_init", sequenceName, stepIndex, actionIndex,
                    "目标变量会自动初始化",
                    "如果该变量首次执行时尚未存在，运行时会按表达式结果类型自动补默认值：数字 0，文本 待获取文本，布尔 true。"));
        }
    }

    private static void validateBooleanExpression(String sequenceName, int stepIndex, int actionIndex,
            String expression, JsonObject params, String label, List<Issue> issues) {
        try {
            LegacyActionRuntime.validateBooleanExpressionSyntax(expression, params);
        } catch (Exception e) {
            issues.add(new Issue(Severity.ERROR, "expression_invalid_boolean", sequenceName, stepIndex, actionIndex,
                    label + "无效", safe(e.getMessage())));
        }
    }

    private static void validateBooleanExpressionList(String sequenceName, int stepIndex, int actionIndex,
            String text, String label, List<Issue> issues) {
        if (isBlank(text)) {
            return;
        }
        String[] tokens = text.split("\\r?\\n|;|；");
        int count = 0;
        for (String token : tokens) {
            String expression = safe(token).trim();
            if (expression.isEmpty()) {
                continue;
            }
            count++;
            validateBooleanExpression(sequenceName, stepIndex, actionIndex, expression, new JsonObject(),
                    label + "#" + count, issues);
        }
    }

    private static void validateBooleanExpressionList(String sequenceName, int stepIndex, int actionIndex,
            List<String> expressions, JsonObject params, String label, List<Issue> issues) {
        if (expressions == null || expressions.isEmpty()) {
            return;
        }
        int count = 0;
        for (String expression : expressions) {
            String normalized = safe(expression).trim();
            if (normalized.isEmpty()) {
                continue;
            }
            count++;
            validateBooleanExpression(sequenceName, stepIndex, actionIndex, normalized, params,
                    label + "#" + count, issues);
        }
    }

    private static List<String> readBooleanExpressionList(JsonObject params, String arrayKey, String legacyKey) {
        List<String> expressions = new ArrayList<>();
        if (params == null) {
            return expressions;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            JsonArray array = params.getAsJsonArray(arrayKey);
            for (JsonElement element : array) {
                if (element != null && element.isJsonPrimitive()) {
                    String expression = safe(element.getAsString()).trim();
                    if (!expression.isEmpty()) {
                        expressions.add(expression);
                    }
                }
            }
            if (!expressions.isEmpty()) {
                return expressions;
            }
        }
        String legacyExpression = getString(params, legacyKey).trim();
        if (!legacyExpression.isEmpty()) {
            expressions.add(legacyExpression);
        }
        return expressions;
    }

    private static List<String> readSimpleStringList(JsonObject params, String arrayKey, String legacyKey) {
        List<String> values = new ArrayList<>();
        if (params == null) {
            return values;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            JsonArray array = params.getAsJsonArray(arrayKey);
            for (JsonElement element : array) {
                if (element != null && element.isJsonPrimitive()) {
                    String value = safe(element.getAsString()).trim();
                    if (!value.isEmpty()) {
                        values.add(value);
                    }
                }
            }
            if (!values.isEmpty()) {
                return values;
            }
        }

        String text = getString(params, legacyKey);
        if (isBlank(text)) {
            text = getString(params, arrayKey);
        }
        if (isBlank(text)) {
            return values;
        }

        for (String token : text.split("\\r?\\n|,")) {
            String value = safe(token).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String getString(JsonObject params, String key) {
        if (params == null || key == null || !params.has(key) || params.get(key).isJsonNull()) {
            return "";
        }
        try {
            return params.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject params, String key, int defaultValue) {
        if (params == null || key == null || !params.has(key) || params.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsInt();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

