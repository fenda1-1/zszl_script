package com.zszl.zszlScriptMod.path.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.path.ActionParameterVariableResolver;
import com.zszl.zszlScriptMod.path.InventoryItemFilterExpressionEngine;
import com.zszl.zszlScriptMod.path.LegacyActionRuntime;
import com.zszl.zszlScriptMod.path.ActionVariableRegistry;
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

    private static final class ResolvedParamValue {
        private final Object value;
        private final ActionParameterVariableResolver.ReferenceInfo referenceInfo;

        private ResolvedParamValue(Object value, ActionParameterVariableResolver.ReferenceInfo referenceInfo) {
            this.value = value;
            this.referenceInfo = referenceInfo;
        }

        private boolean isDynamicReference() {
            return referenceInfo != null && referenceInfo.isDynamic();
        }

        private boolean isMissingReference() {
            return referenceInfo != null && referenceInfo.isMissing();
        }

        private boolean isReference() {
            return referenceInfo != null && referenceInfo.isReference();
        }
    }

    private PathConfigValidator() {
    }

    private static Map<String, ActionParameterVariableResolver.Context> buildVariableContexts(
            Collection<PathSequence> sequences) {
        LinkedHashMap<String, ActionParameterVariableResolver.Context> contexts = new LinkedHashMap<>();
        contexts.put("", ActionParameterVariableResolver.buildContext("", sequences));
        if (sequences == null) {
            return contexts;
        }
        for (PathSequence sequence : sequences) {
            if (sequence == null || sequence.getName() == null || sequence.getName().trim().isEmpty()) {
                continue;
            }
            String sequenceName = sequence.getName().trim();
            contexts.put(sequenceName, ActionParameterVariableResolver.buildContext(sequenceName, sequences));
        }
        return contexts;
    }

    private static ActionParameterVariableResolver.Context getVariableContext(
            Map<String, ActionParameterVariableResolver.Context> contexts, String sequenceName) {
        if (contexts == null || contexts.isEmpty()) {
            return ActionParameterVariableResolver.buildContext(sequenceName, Collections.<PathSequence>emptyList());
        }
        String normalizedSequenceName = safe(sequenceName).trim();
        ActionParameterVariableResolver.Context context = contexts.get(normalizedSequenceName);
        if (context != null) {
            return context;
        }
        context = contexts.get("");
        return context != null ? context : ActionParameterVariableResolver.buildContext(sequenceName,
                Collections.<PathSequence>emptyList());
    }

    public static List<Issue> validateSequences(Collection<PathSequence> sequences) {
        List<Issue> issues = new ArrayList<>();
        if (sequences == null || sequences.isEmpty()) {
            return issues;
        }

        List<PathSequence> sequenceList = new ArrayList<>(sequences);
        Set<String> sequenceNames = new LinkedHashSet<>();
        Map<String, PathSequence> sequenceMap = new LinkedHashMap<>();
        Map<String, ActionParameterVariableResolver.Context> variableContexts = buildVariableContexts(sequenceList);
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
                validateStep(sequenceName, stepIndex, steps.size(), step, sequenceNames, issues);
                collectStepGraphEdge(sequenceName, step, graph);
                List<ActionData> actions = step.getActions() == null ? Collections.emptyList() : step.getActions();
                for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
                    ActionData action = actions.get(actionIndex);
                    validateAction(sequenceName, stepIndex, actionIndex, action, actions, sequenceNames,
                            getVariableContext(variableContexts, sequenceName), issues);
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
        validateAction(safe(currentSequenceName).trim(), -1, -1, action, null, sequenceNames,
                ActionParameterVariableResolver.buildContext(currentSequenceName, sequences), issues);
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
            Set<String> sequenceNames, List<Issue> issues) {
        if (step.getGotoPoint() == null || step.getGotoPoint().length < 3) {
            issues.add(new Issue(Severity.WARNING, "step_goto_point", sequenceName, stepIndex, -1,
                    "步骤坐标不完整", "GOTO 坐标建议始终保持 3 个分量。"));
        }
        validateRetryExhaustedRunSequence(sequenceName, stepIndex, step, sequenceNames, issues);
    }

    private static void validateRetryExhaustedRunSequence(String sequenceName, int stepIndex, PathStep step,
            Set<String> sequenceNames, List<Issue> issues) {
        if (step == null || !"RUN_SEQUENCE".equalsIgnoreCase(step.getRetryExhaustedPolicy())) {
            return;
        }
        String target = safe(step.getRetryExhaustedSequenceName()).trim();
        if (target.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "step_retry_run_sequence_missing", sequenceName, stepIndex, -1,
                    "失败后执行序列未设置", "重试耗尽策略为执行序列时，必须选择一个目标序列。"));
            return;
        }
        if (sequenceNames == null || !sequenceNames.contains(target)) {
            issues.add(new Issue(Severity.ERROR, "step_retry_run_sequence_not_found", sequenceName, stepIndex, -1,
                    "失败后执行序列不存在", "目标序列 " + target + " 不存在，运行时无法执行。"));
            return;
        }
        if (target.equalsIgnoreCase(sequenceName)) {
            issues.add(new Issue(Severity.WARNING, "step_retry_run_sequence_self", sequenceName, stepIndex, -1,
                    "失败后执行序列指向当前序列", "当前序列失败后会重新启动自己，建议确认不会形成无限失败重试。"));
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

    private static void validateParameterVariableReferences(String sequenceName, int stepIndex, int actionIndex,
            ActionData action, JsonObject params, ActionParameterVariableResolver.Context variableContext,
            List<Issue> issues) {
        if (params == null || variableContext == null) {
            return;
        }
        LinkedHashSet<String> reportedPaths = new LinkedHashSet<>();
        String targetVariableKey = ActionVariableRegistry.resolveVariableParamKey(action);
        validateVariableReferencesInElement(sequenceName, stepIndex, actionIndex, params, "", targetVariableKey,
                variableContext, issues, reportedPaths);
    }

    private static void validateVariableReferencesInElement(String sequenceName, int stepIndex, int actionIndex,
            JsonElement element, String path, String targetVariableKey,
            ActionParameterVariableResolver.Context variableContext, List<Issue> issues, Set<String> reportedPaths) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String childPath = path.isEmpty() ? safe(entry.getKey()) : path + "." + safe(entry.getKey());
                validateVariableReferencesInElement(sequenceName, stepIndex, actionIndex, entry.getValue(), childPath,
                        targetVariableKey, variableContext, issues, reportedPaths);
            }
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                String childPath = path + "[" + i + "]";
                validateVariableReferencesInElement(sequenceName, stepIndex, actionIndex, array.get(i), childPath,
                        targetVariableKey, variableContext, issues, reportedPaths);
            }
            return;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return;
        }

        String currentKey = extractLeafKey(path);
        if (shouldSkipVariableReferenceValidation(currentKey, path, targetVariableKey)) {
            return;
        }

        ActionParameterVariableResolver.ReferenceInfo info = ActionParameterVariableResolver.inspect(variableContext,
                element.getAsString());
        if (!info.isMissing()) {
            return;
        }

        String signature = path + "|" + info.getNormalizedReference();
        if (!reportedPaths.add(signature)) {
            return;
        }
        issues.add(new Issue(Severity.ERROR, "param_variable_missing", sequenceName, stepIndex, actionIndex,
                "参数变量未定义",
                (path.isEmpty() ? "当前参数" : "参数 " + path)
                        + " 使用了变量 " + info.getNormalizedReference() + "，但当前没有找到已定义值。"));
    }

    private static boolean shouldSkipVariableReferenceValidation(String currentKey, String path, String targetVariableKey) {
        String normalizedKey = safe(currentKey).trim();
        if (normalizedKey.isEmpty()) {
            return false;
        }
        if (path.indexOf('.') < 0 && path.indexOf('[') < 0
                && normalizedKey.equalsIgnoreCase(safe(targetVariableKey).trim())) {
            return true;
        }
        return "expression".equalsIgnoreCase(normalizedKey)
                || "conditionstext".equalsIgnoreCase(normalizedKey)
                || "cancelexpression".equalsIgnoreCase(normalizedKey)
                || "itemfilterexpression".equalsIgnoreCase(normalizedKey)
                || "itemfilterexpressions".equalsIgnoreCase(normalizedKey)
                || "expressions".equalsIgnoreCase(normalizedKey)
                || "fromvar".equalsIgnoreCase(normalizedKey);
    }

    private static String extractLeafKey(String path) {
        String safePath = safe(path).trim();
        if (safePath.isEmpty()) {
            return "";
        }
        int dotIndex = safePath.lastIndexOf('.');
        int bracketIndex = safePath.lastIndexOf('[');
        int start = Math.max(dotIndex, bracketIndex);
        return start < 0 ? safePath : safePath.substring(start + 1).replace("]", "");
    }

    private static ResolvedParamValue resolveParamValue(ActionParameterVariableResolver.Context variableContext,
            JsonObject params, String key) {
        if (params == null || key == null || !params.has(key) || params.get(key).isJsonNull()) {
            return new ResolvedParamValue(null, null);
        }
        JsonElement element = params.get(key);
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                String raw = element.getAsString();
                ActionParameterVariableResolver.ReferenceInfo info = ActionParameterVariableResolver.inspect(variableContext,
                        raw);
                if (info.isResolved()) {
                    return new ResolvedParamValue(info.getResolvedValue(), info);
                }
                return new ResolvedParamValue(raw, info.isReference() ? info : null);
            }
            if (element.getAsJsonPrimitive().isNumber()) {
                return new ResolvedParamValue(element.getAsDouble(), null);
            }
            if (element.getAsJsonPrimitive().isBoolean()) {
                return new ResolvedParamValue(element.getAsBoolean(), null);
            }
        }
        return new ResolvedParamValue(element, null);
    }

    private static Integer readResolvedInt(ResolvedParamValue resolvedValue) {
        if (resolvedValue == null || resolvedValue.isDynamicReference() || resolvedValue.isMissingReference()) {
            return null;
        }
        Double numeric = ActionParameterVariableResolver.toNumber(resolvedValue.value);
        if (numeric == null) {
            return null;
        }
        return Integer.valueOf((int) Math.round(numeric.doubleValue()));
    }

    private static Double readResolvedDouble(ResolvedParamValue resolvedValue) {
        if (resolvedValue == null || resolvedValue.isDynamicReference() || resolvedValue.isMissingReference()) {
            return null;
        }
        return ActionParameterVariableResolver.toNumber(resolvedValue.value);
    }

    private static boolean shouldReportNumericParseIssue(ResolvedParamValue resolvedValue) {
        if (resolvedValue == null || resolvedValue.isDynamicReference() || resolvedValue.isMissingReference()) {
            return false;
        }
        return resolvedValue.value != null && !safe(LegacyActionRuntime.stringifyValue(resolvedValue.value)).trim().isEmpty();
    }

    private static void addNumericParseIssue(String sequenceName, int stepIndex, int actionIndex, String code,
            String label, String detail, List<Issue> issues) {
        issues.add(new Issue(Severity.ERROR, code, sequenceName, stepIndex, actionIndex,
                label + "格式无效", detail));
    }

    private static void validateAction(String sequenceName, int stepIndex, int actionIndex, ActionData action,
            List<ActionData> stepActions, Set<String> sequenceNames,
            ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        if (action == null) {
            return;
        }

        String type = safe(action.type).trim().toLowerCase(Locale.ROOT);
        JsonObject params = action.params == null ? new JsonObject() : action.params;
        validateParameterVariableReferences(sequenceName, stepIndex, actionIndex, action, params, variableContext, issues);
        switch (type) {
            case "run_sequence":
                validateRunSequence(sequenceName, stepIndex, actionIndex, params, sequenceNames, variableContext, issues);
                break;
            case "stop_current_sequence":
                validateStopCurrentSequence(sequenceName, stepIndex, actionIndex, params, issues);
                break;
            case "run_template":
                validateRunTemplate(sequenceName, stepIndex, actionIndex, params, sequenceNames, variableContext, issues);
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
                    ResolvedParamValue targetIndexValue = resolveParamValue(variableContext, params, "targetActionIndex");
                    Integer targetIndex = readResolvedInt(targetIndexValue);
                    if (targetIndex == null) {
                        if (shouldReportNumericParseIssue(targetIndexValue)) {
                            addNumericParseIssue(sequenceName, stepIndex, actionIndex, "goto_action_parse",
                                    "跳转动作序号", "目标动作序号需要填写整数。", issues);
                        }
                        break;
                    }
                    if (targetIndex.intValue() < 0 || targetIndex.intValue() >= stepActions.size()) {
                        issues.add(new Issue(Severity.ERROR, "goto_action_range", sequenceName, stepIndex, actionIndex,
                                "跳转动作序号越界", "目标动作序号必须在 0 到 " + Math.max(0, stepActions.size() - 1) + " 之间。"));
                    } else if (targetIndex.intValue() == actionIndex) {
                        issues.add(new Issue(Severity.WARNING, "goto_action_self", sequenceName, stepIndex, actionIndex,
                                "跳转到当前动作", "这会让当前动作原地循环，建议确认是否需要配合条件或计数器使用。"));
                    }
                }
                break;
            case "skip_actions":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "count", "跳过动作数",
                        variableContext, issues);
                if (stepActions != null && !stepActions.isEmpty()) {
                    Integer skipActionCount = readResolvedInt(resolveParamValue(variableContext, params, "count"));
                    if (skipActionCount != null
                            && actionIndex >= 0
                            && actionIndex + Math.max(0, skipActionCount.intValue()) + 1 >= stepActions.size()) {
                        issues.add(new Issue(Severity.WARNING, "skip_actions_reach_end", sequenceName, stepIndex,
                                actionIndex,
                                "跳过动作将直接到步骤末尾", "运行时会直接结束当前步骤并进入后续步骤，请确认这正是你想要的控制流。"));
                    }
                }
                break;
            case "skip_steps":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "count", "跳过步骤数",
                        variableContext, issues);
                break;
            case "repeat_actions":
                ResolvedParamValue bodyCountValue = resolveParamValue(variableContext, params, "bodyCount");
                Integer bodyCount = readResolvedInt(bodyCountValue);
                if (bodyCount == null && shouldReportNumericParseIssue(bodyCountValue)) {
                    addNumericParseIssue(sequenceName, stepIndex, actionIndex, "repeat_actions_body_parse",
                            "循环体动作数", "循环体动作数需要填写整数。", issues);
                }
                if (bodyCount != null && bodyCount.intValue() <= 0) {
                    issues.add(new Issue(Severity.ERROR, "repeat_actions_body", sequenceName, stepIndex, actionIndex,
                            "循环体动作数无效", "循环体动作数必须大于 0。"));
                } else if (bodyCount != null && stepActions != null && actionIndex >= 0
                        && actionIndex + bodyCount.intValue() >= stepActions.size()) {
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
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount",
                        "超时跳过动作数", variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "preExecuteCount", "先执行动作数",
                        variableContext, issues);
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
                            variableContext, issues);
                    validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount",
                            "超时跳过动作数", variableContext, issues);
                } else {
                    validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "skipCount", "失败跳过动作数",
                            variableContext, issues);
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
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "ticks", "延迟",
                        variableContext, issues);
                break;
            case "capture_inventory_slot":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                validateCaptureInventorySlot(sequenceName, stepIndex, actionIndex, params, variableContext, issues);
                break;
            case "capture_nearby_entity":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "radius", "范围", false,
                        variableContext, issues);
                break;
            case "capture_gui_title":
            case "capture_hotbar":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                break;
            case "capture_entity_list":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, "maxCount", "最大采集数量",
                        1, variableContext, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "radius", "范围", false,
                        variableContext, issues);
                break;
            case "capture_packet_field":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                String lookupMode = getString(params, "lookupMode");
                String fieldKey = getString(params, "fieldKey");
                if ("VARIABLE".equalsIgnoreCase(lookupMode) && isBlank(fieldKey)) {
                    issues.add(new Issue(Severity.ERROR, "capture_packet_field_variable_key", sequenceName, stepIndex,
                            actionIndex, "包字段采集缺少变量键", "运行时变量模式下必须填写变量键。"));
                }
                break;
            case "capture_gui_element":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                break;
            case "capture_scoreboard":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                ResolvedParamValue lineIndexValue = resolveParamValue(variableContext, params, "lineIndex");
                Integer lineIndex = readResolvedInt(lineIndexValue);
                if (lineIndex == null && shouldReportNumericParseIssue(lineIndexValue)) {
                    addNumericParseIssue(sequenceName, stepIndex, actionIndex, "capture_scoreboard_line_parse",
                            "指定行索引", "指定行索引需要填写整数。", issues);
                }
                if (lineIndex != null && lineIndex.intValue() < -1) {
                    issues.add(new Issue(Severity.ERROR, "capture_scoreboard_line", sequenceName, stepIndex, actionIndex,
                            "记分板指定行索引无效", "指定行索引只能填写 -1 或更大的值。"));
                }
                break;
            case "capture_block_at":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                validateCoordinateTriplet(sequenceName, stepIndex, actionIndex, params, "pos", "方块坐标",
                        variableContext, issues);
                break;
            case "capture_screen_region":
                validateCaptureVarName(sequenceName, stepIndex, actionIndex, params, issues);
                validateVisionRegionRect(sequenceName, stepIndex, actionIndex, params, variableContext, issues);
                break;
            case "condition_inventory_item":
            case "wait_until_inventory_item":
                List<String> itemFilterExpressions = InventoryItemFilterExpressionEngine.readExpressions(params);
                if (!itemFilterExpressions.isEmpty()) {
                    validateItemFilterExpressionList(sequenceName, stepIndex, actionIndex,
                            itemFilterExpressions, "物品过滤表达式", issues);
                } else if (isBlank(getString(params, "itemName"))
                        && ItemFilterHandler.readTagFilters(params, "requiredNbtTags", "requiredNbtTagsText").isEmpty()) {
                    issues.add(new Issue(Severity.ERROR, "inventory_item_filter_missing", sequenceName, stepIndex,
                            actionIndex, "缺少物品过滤条件", "请至少添加一条物品过滤表达式，或保留旧版物品名/NBT条件。"));
                }
                validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, "count", "最少数量", 1,
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount", "超时跳过动作数",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "skipCount", "失败跳过动作数",
                        variableContext, issues);
                break;
            case "wait_until_gui_title":
            case "wait_until_player_in_area":
            case "wait_until_entity_nearby":
            case "wait_until_hud_text":
            case "wait_until_captured_id":
            case "wait_until_packet_text":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount", "超时跳过动作数",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "preExecuteCount", "先执行动作数",
                        variableContext, issues);
                break;
            case "wait_until_screen_region":
                validateVisionRegionRect(sequenceName, stepIndex, actionIndex, params, variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutTicks", "超时",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "timeoutSkipCount", "超时跳过动作数",
                        variableContext, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "colorTolerance", "颜色容差",
                        false, variableContext, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "similarityThreshold", "相似度阈值",
                        false, variableContext, issues);
                break;
            case "condition_gui_title":
            case "condition_player_in_area":
            case "condition_entity_nearby":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "skipCount", "失败跳过动作数",
                        variableContext, issues);
                break;
            case "move_inventory_item_to_hotbar":
                ResolvedParamValue hotbarSlotValue = resolveParamValue(variableContext, params, "targetHotbarSlot");
                Integer hotbarSlot = readResolvedInt(hotbarSlotValue);
                if (hotbarSlot == null && shouldReportNumericParseIssue(hotbarSlotValue)) {
                    addNumericParseIssue(sequenceName, stepIndex, actionIndex, "hotbar_slot_parse",
                            "目标快捷栏位", "目标快捷栏位需要填写 1-9 的整数。", issues);
                }
                if (hotbarSlot != null && (hotbarSlot.intValue() < 1 || hotbarSlot.intValue() > 9)) {
                    issues.add(new Issue(Severity.ERROR, "hotbar_slot_range", sequenceName, stepIndex, actionIndex,
                            "快捷栏位超出范围", "目标快捷栏位必须填写 1-9。"));
                }
                break;
            case "switch_hotbar_slot":
                ResolvedParamValue switchHotbarSlotValue = resolveParamValue(variableContext, params, "targetHotbarSlot");
                Integer switchHotbarSlot = readResolvedInt(switchHotbarSlotValue);
                if (switchHotbarSlot == null && shouldReportNumericParseIssue(switchHotbarSlotValue)) {
                    addNumericParseIssue(sequenceName, stepIndex, actionIndex, "switch_hotbar_slot_parse",
                            "目标快捷栏位", "目标快捷栏位需要填写 1-9 的整数。", issues);
                }
                if (switchHotbarSlot != null
                        && (switchHotbarSlot.intValue() < 1 || switchHotbarSlot.intValue() > 9)) {
                    issues.add(new Issue(Severity.ERROR, "hotbar_slot_range", sequenceName, stepIndex, actionIndex,
                            "快捷栏位超出范围", "目标快捷栏位必须填写 1-9。"));
                }
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "useAfterSwitchDelayTicks",
                        "切换后使用延迟", variableContext, issues);
                break;
            case "silentuse":
                ResolvedParamValue tempHotbarSlotValue = resolveParamValue(variableContext, params, "tempslot");
                Integer tempHotbarSlot = readResolvedInt(tempHotbarSlotValue);
                if (tempHotbarSlot == null && shouldReportNumericParseIssue(tempHotbarSlotValue)) {
                    addNumericParseIssue(sequenceName, stepIndex, actionIndex, "temp_hotbar_slot_parse",
                            "临时槽位", "临时槽位需要填写 0-8 的整数。", issues);
                }
                if (tempHotbarSlot != null && (tempHotbarSlot.intValue() < 0 || tempHotbarSlot.intValue() > 8)) {
                    issues.add(new Issue(Severity.ERROR, "temp_hotbar_slot_range", sequenceName, stepIndex, actionIndex,
                            "临时槽位超出范围", "临时槽位必须填写 0-8。"));
                }
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchDelayTicks",
                        "切换物品延迟", variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "useDelayTicks",
                        "切后使用延迟", variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchBackDelayTicks",
                        "切回延迟", variableContext, issues);
                break;
            case "use_hotbar_item":
                validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, "count", "使用次数", 1,
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchItemDelayTicks",
                        "切换物品延迟", variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchDelayTicks",
                        "切后使用延迟", variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "switchBackDelayTicks",
                        "切回延迟", variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "intervalTicks", "使用间隔",
                        variableContext, issues);
                break;
            case "use_held_item":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "delayTicks", "延迟",
                        variableContext, issues);
                break;
            case "hunt":
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "radius", "搜索半径", false,
                        variableContext, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "trackingDistance", "追踪距离",
                        false, variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "attackCount", "攻击次数",
                        variableContext, issues);
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "noTargetSkipCount",
                        "无目标时跳过动作数", variableContext, issues);
                validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, "huntChaseIntervalSeconds",
                        "追怪间隔秒数", true, variableContext, issues);
                validateHuntAttackSequence(sequenceName, stepIndex, actionIndex, params, sequenceNames, variableContext,
                        issues);
                break;
            case "autochestclick":
            case "move_inventory_items_to_chest_slots":
                validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, "delayTicks", "延迟",
                        variableContext, issues);
                boolean hasItemExpressions = !InventoryItemFilterExpressionEngine.readExpressions(params).isEmpty();
                if (!hasItemExpressions && ItemFilterHandler.readMoveChestFilterRules(params).isEmpty()) {
                    issues.add(new Issue(Severity.ERROR, "move_chest_filter_missing", sequenceName, stepIndex,
                            actionIndex,
                            "容器物品批量移动缺少过滤条件",
                            "至少需要添加一条有效物品过滤表达式，或保留一条旧版物品名/NBT规则。"));
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
            Set<String> sequenceNames, ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        ResolvedParamValue targetValue = resolveParamValue(variableContext, params, "sequenceName");
        String target = targetValue.isDynamicReference() || targetValue.isMissingReference()
                ? ""
                : safe(LegacyActionRuntime.stringifyValue(targetValue.value)).trim();
        if (isBlank(getString(params, "uuid"))) {
            issues.add(new Issue(Severity.WARNING, "persistent_uuid_missing", sequenceName, stepIndex, actionIndex,
                    "动作 UUID 缺失", "保存后建议重新确认该动作已生成稳定 UUID，避免计数或状态冲突。"));
        }
        if (!targetValue.isDynamicReference() && !targetValue.isMissingReference() && target.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "run_sequence_missing", sequenceName, stepIndex, actionIndex,
                    "执行序列未选择目标", "请先选择要执行的目标序列。"));
            return;
        }
        if (targetValue.isDynamicReference() || targetValue.isMissingReference()) {
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
        Integer executeEveryCount = readResolvedInt(resolveParamValue(variableContext, params, "executeEveryCount"));
        if ("interval".equalsIgnoreCase(getString(params, "executeMode"))
                && executeEveryCount != null
                && executeEveryCount.intValue() <= 1) {
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
            Set<String> sequenceNames, ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        ResolvedParamValue templateNameValue = resolveParamValue(variableContext, params, "templateName");
        String templateName = templateNameValue.isDynamicReference() || templateNameValue.isMissingReference()
                ? ""
                : safe(LegacyActionRuntime.stringifyValue(templateNameValue.value)).trim();
        if (isBlank(getString(params, "uuid"))) {
            issues.add(new Issue(Severity.WARNING, "persistent_uuid_missing", sequenceName, stepIndex, actionIndex,
                    "动作 UUID 缺失", "保存后建议重新确认该动作已生成稳定 UUID，避免计数或状态冲突。"));
        }
        if (!templateNameValue.isDynamicReference() && !templateNameValue.isMissingReference() && templateName.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "run_template_missing", sequenceName, stepIndex, actionIndex,
                    "执行模板未选择目标", "请先选择模板。"));
            return;
        }
        if (templateNameValue.isDynamicReference() || templateNameValue.isMissingReference()) {
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
        Integer executeEveryCount = readResolvedInt(resolveParamValue(variableContext, params, "executeEveryCount"));
        if ("interval".equalsIgnoreCase(getString(params, "executeMode"))
                && executeEveryCount != null
                && executeEveryCount.intValue() <= 1) {
            issues.add(new Issue(Severity.WARNING, "run_template_interval_one", sequenceName, stepIndex, actionIndex,
                    "模板间隔执行次数等于 1", "这与“每次执行”效果一致，建议改回每次执行或把次数调大。"));
        }
    }

    private static void validateHuntAttackSequence(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            Set<String> sequenceNames, ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        if (!KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(getString(params, "attackMode").trim())) {
            return;
        }
        ResolvedParamValue targetValue = resolveParamValue(variableContext, params, "attackSequenceName");
        String target = targetValue.isDynamicReference() || targetValue.isMissingReference()
                ? ""
                : safe(LegacyActionRuntime.stringifyValue(targetValue.value)).trim();
        if (!targetValue.isDynamicReference() && !targetValue.isMissingReference() && target.isEmpty()) {
            issues.add(new Issue(Severity.ERROR, "hunt_attack_sequence_missing", sequenceName, stepIndex, actionIndex,
                    "搜怪击杀未选择攻击序列", "当攻击方式为“执行序列攻击”时，必须选择一条攻击序列。"));
            return;
        }
        if (targetValue.isDynamicReference() || targetValue.isMissingReference()) {
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
            JsonObject params, ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        String area = getString(params, "slotArea").trim().toUpperCase(Locale.ROOT);
        if (area.isEmpty()) {
            area = "MAIN";
        }
        ResolvedParamValue slotIndexValue = resolveParamValue(variableContext, params, "slotIndex");
        Integer slotIndex = readResolvedInt(slotIndexValue);
        if (slotIndex == null) {
            if (shouldReportNumericParseIssue(slotIndexValue)) {
                addNumericParseIssue(sequenceName, stepIndex, actionIndex, "capture_slot_parse",
                        "槽位索引", "请填写合法的整数槽位索引。", issues);
            }
            return;
        }
        if (slotIndex.intValue() < 0) {
            issues.add(new Issue(Severity.ERROR, "capture_slot_negative", sequenceName, stepIndex, actionIndex,
                    "槽位索引不能小于 0", "请填写合法的槽位索引。"));
            return;
        }
        switch (area) {
            case "MAIN":
                if (slotIndex.intValue() > 35) {
                    issues.add(new Issue(Severity.WARNING, "capture_slot_main_range", sequenceName, stepIndex, actionIndex,
                            "主背包槽位超出常规范围", "主背包模式通常建议填写 0-35。"));
                }
                break;
            case "HOTBAR":
                if (slotIndex.intValue() > 8) {
                    issues.add(new Issue(Severity.ERROR, "capture_slot_hotbar_range", sequenceName, stepIndex, actionIndex,
                            "快捷栏槽位超出范围", "快捷栏模式下槽位索引必须是 0-8。"));
                }
                break;
            case "ARMOR":
                if (slotIndex.intValue() > 3) {
                    issues.add(new Issue(Severity.ERROR, "capture_slot_armor_range", sequenceName, stepIndex, actionIndex,
                            "护甲栏槽位超出范围", "护甲栏模式下槽位索引必须是 0-3。"));
                }
                break;
            case "OFFHAND":
                if (slotIndex.intValue() > 0) {
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

    private static void validateCaptureVarName(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            List<Issue> issues) {
        if (isBlank(getString(params, "varName"))) {
            issues.add(new Issue(Severity.WARNING, "capture_var_name_missing", sequenceName, stepIndex, actionIndex,
                    "采集变量名为空", "未填写时会回退到动作默认变量名，建议显式填写，避免和其他采集动作撞名。"));
        }
    }

    private static void validateCoordinateTriplet(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            String key, String label, ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        if (params == null || !params.has(key)) {
            issues.add(new Issue(Severity.ERROR, key + "_missing", sequenceName, stepIndex, actionIndex,
                    label + "未配置", "请按 [x,y,z] 格式填写。"));
            return;
        }
        try {
            double[] values = new double[3];
            ResolvedParamValue resolved = resolveParamValue(variableContext, params, key);
            if (resolved.isDynamicReference() || resolved.isMissingReference()) {
                return;
            }
            if (resolved.value instanceof JsonArray) {
                JsonArray array = (JsonArray) resolved.value;
                if (array.size() < 3) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 3; i++) {
                    values[i] = array.get(i).getAsDouble();
                }
            } else if (resolved.value instanceof JsonElement && ((JsonElement) resolved.value).isJsonArray()) {
                JsonArray array = ((JsonElement) resolved.value).getAsJsonArray();
                if (array.size() < 3) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 3; i++) {
                    values[i] = array.get(i).getAsDouble();
                }
            } else {
                String text = safe(LegacyActionRuntime.stringifyValue(resolved.value)).replace("[", "").replace("]", "");
                String[] parts = text.split(",");
                if (parts.length < 3) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 3; i++) {
                    values[i] = Double.parseDouble(parts[i].trim());
                }
            }
            for (double value : values) {
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    throw new IllegalArgumentException("nan");
                }
            }
        } catch (Exception e) {
            issues.add(new Issue(Severity.ERROR, key + "_parse", sequenceName, stepIndex, actionIndex,
                    label + "格式无效", "请按 [x,y,z] 填写。"));
        }
    }

    private static void validateVisionRegionRect(String sequenceName, int stepIndex, int actionIndex, JsonObject params,
            ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        if (params == null || !params.has("regionRect")) {
            issues.add(new Issue(Severity.ERROR, "vision_region_missing", sequenceName, stepIndex, actionIndex,
                    "视觉区域未配置", "请填写 [x,y,width,height] 区域。"));
            return;
        }
        try {
            int[] values = new int[4];
            ResolvedParamValue resolved = resolveParamValue(variableContext, params, "regionRect");
            if (resolved.isDynamicReference() || resolved.isMissingReference()) {
                return;
            }
            if (resolved.value instanceof JsonArray) {
                JsonArray array = (JsonArray) resolved.value;
                if (array.size() < 4) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 4; i++) {
                    values[i] = (int) Math.round(array.get(i).getAsDouble());
                }
            } else if (resolved.value instanceof JsonElement && ((JsonElement) resolved.value).isJsonArray()) {
                JsonArray array = ((JsonElement) resolved.value).getAsJsonArray();
                if (array.size() < 4) {
                    throw new IllegalArgumentException("size");
                }
                for (int i = 0; i < 4; i++) {
                    values[i] = (int) Math.round(array.get(i).getAsDouble());
                }
            } else {
                String text = safe(LegacyActionRuntime.stringifyValue(resolved.value)).replace("[", "").replace("]", "");
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

    private static void collectStepGraphEdge(String sequenceName, PathStep step, Map<String, List<String>> graph) {
        if (step == null || graph == null || !"RUN_SEQUENCE".equalsIgnoreCase(step.getRetryExhaustedPolicy())) {
            return;
        }
        String target = safe(step.getRetryExhaustedSequenceName()).trim();
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
        validateNonNegativeIntParam(sequenceName, stepIndex, actionIndex, params, key, label,
                ActionParameterVariableResolver.buildContext(sequenceName, Collections.<PathSequence>emptyList()), issues);
    }

    private static void validateNonNegativeIntParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, ActionParameterVariableResolver.Context variableContext,
            List<Issue> issues) {
        if (params == null || key == null || !params.has(key)) {
            return;
        }
        ResolvedParamValue resolvedValue = resolveParamValue(variableContext, params, key);
        Integer value = readResolvedInt(resolvedValue);
        if (value == null) {
            if (shouldReportNumericParseIssue(resolvedValue)) {
                addNumericParseIssue(sequenceName, stepIndex, actionIndex, key + "_parse", label,
                        "参数 " + key + " 需要填写数字。", issues);
            }
            return;
        }
        if (value != null && value.intValue() < 0) {
            issues.add(new Issue(Severity.ERROR, key + "_negative", sequenceName, stepIndex, actionIndex,
                    label + "不能小于 0", "参数 " + key + " 当前值为 " + value.intValue() + "。"));
        }
    }

    private static void validatePositiveIntParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, int minimum, List<Issue> issues) {
        validatePositiveIntParam(sequenceName, stepIndex, actionIndex, params, key, label, minimum,
                ActionParameterVariableResolver.buildContext(sequenceName, Collections.<PathSequence>emptyList()), issues);
    }

    private static void validatePositiveIntParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, int minimum,
            ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        if (params == null || key == null || !params.has(key)) {
            return;
        }
        ResolvedParamValue resolvedValue = resolveParamValue(variableContext, params, key);
        Integer value = readResolvedInt(resolvedValue);
        if (value == null) {
            if (shouldReportNumericParseIssue(resolvedValue)) {
                addNumericParseIssue(sequenceName, stepIndex, actionIndex, key + "_parse", label,
                        "参数 " + key + " 需要填写整数。", issues);
            }
            return;
        }
        if (value != null && value.intValue() < minimum) {
            issues.add(new Issue(Severity.ERROR, key + "_range", sequenceName, stepIndex, actionIndex,
                    label + "过小", "参数 " + key + " 至少应为 " + minimum + "，当前为 " + value.intValue() + "。"));
        }
    }

    private static void validatePositiveDoubleParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, boolean allowZero, List<Issue> issues) {
        validatePositiveDoubleParam(sequenceName, stepIndex, actionIndex, params, key, label, allowZero,
                ActionParameterVariableResolver.buildContext(sequenceName, Collections.<PathSequence>emptyList()), issues);
    }

    private static void validatePositiveDoubleParam(String sequenceName, int stepIndex, int actionIndex,
            JsonObject params, String key, String label, boolean allowZero,
            ActionParameterVariableResolver.Context variableContext, List<Issue> issues) {
        if (params == null || key == null || !params.has(key) || params.get(key).isJsonNull()) {
            return;
        }
        ResolvedParamValue resolvedValue = resolveParamValue(variableContext, params, key);
        Double value = readResolvedDouble(resolvedValue);
        if (value == null) {
            if (shouldReportNumericParseIssue(resolvedValue)) {
                addNumericParseIssue(sequenceName, stepIndex, actionIndex, key + "_parse", label,
                        "参数 " + key + " 需要填写数字。", issues);
            }
            return;
        }
        if (value != null && (allowZero ? value.doubleValue() < 0D : value.doubleValue() <= 0D)) {
            issues.add(new Issue(Severity.ERROR, key + "_range", sequenceName, stepIndex, actionIndex,
                    label + "无效", "参数 " + key + " 当前值为 " + value + "。"));
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

    private static void validateItemFilterExpressionList(String sequenceName, int stepIndex, int actionIndex,
            List<String> expressions, String label, List<Issue> issues) {
        if (expressions == null || expressions.isEmpty()) {
            return;
        }
        int count = 0;
        for (String expression : expressions) {
            String value = safe(expression).trim();
            if (value.isEmpty()) {
                continue;
            }
            count++;
            try {
                InventoryItemFilterExpressionEngine.validate(value);
            } catch (Exception e) {
                issues.add(new Issue(Severity.ERROR, "item_filter_expression_invalid", sequenceName, stepIndex,
                        actionIndex, label + "#" + count + " 无效", safe(e.getMessage())));
            }
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


