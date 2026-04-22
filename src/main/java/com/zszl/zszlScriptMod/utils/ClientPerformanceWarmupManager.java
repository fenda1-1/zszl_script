package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager;
import com.zszl.zszlScriptMod.path.node.NodeTriggerManager;
import com.zszl.zszlScriptMod.path.runtime.safety.PathSafetyManager;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientPerformanceWarmupManager {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable,
                "zszl-warmup-" + THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    private ClientPerformanceWarmupManager() {
    }

    public static void warmupCurrentProfileAsync(String reason) {
        final String safeReason = reason == null ? "" : reason.trim();
        EXECUTOR.execute(() -> performWarmup(safeReason));
    }

    private static void performWarmup(String reason) {
        long startNanos = System.nanoTime();
        try {
            OtherFeatureGroupManager.reload();
        } catch (Throwable throwable) {
            zszlScriptMod.LOGGER.warn("[Warmup] 加载其他功能分组失败", throwable);
        }
        try {
            LegacySequenceTriggerManager.reloadRules();
        } catch (Throwable throwable) {
            zszlScriptMod.LOGGER.warn("[Warmup] 预热旧版触发规则失败", throwable);
        }
        try {
            PacketFieldRuleManager.reloadRules();
        } catch (Throwable throwable) {
            zszlScriptMod.LOGGER.warn("[Warmup] 预热数据包字段规则失败", throwable);
        }
        try {
            PathSafetyManager.reload();
        } catch (Throwable throwable) {
            zszlScriptMod.LOGGER.warn("[Warmup] 预热路径安全配置失败", throwable);
        }
        try {
            NodeTriggerManager.preload();
        } catch (Throwable throwable) {
            zszlScriptMod.LOGGER.warn("[Warmup] 预热节点触发图失败", throwable);
        }

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        zszlScriptMod.LOGGER.info("[Warmup] 异步预热完成，reason='{}'，耗时 {} ms", reason, elapsedMillis);
    }
}
