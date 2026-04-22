package com.zszl.zszlScriptMod.path;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

import com.zszl.zszlScriptMod.zszlScriptMod;

/**
 * 管理自定义路径录制的状态和数据
 */
public class PathRecordingManager {

    // 内部类，用于存储每一步录制的详细信息
    public static class RecordedStep {
        public final Vec3 playerPos;
        public final float playerYaw;
        public final float playerPitch;
        public final BlockPos chestPos;

        public RecordedStep(Vec3 playerPos, float playerYaw, float playerPitch, BlockPos chestPos) {
            this.playerPos = playerPos;
            this.playerYaw = playerYaw;
            this.playerPitch = playerPitch;
            this.chestPos = chestPos;
        }

        @Override
        public String toString() {
            return I18n.format("path.record.step.desc",
                    playerPos.x, playerPos.y, playerPos.z,
                    chestPos.getX(), chestPos.getY(), chestPos.getZ());
        }
    }

    private static boolean isRecording = false;
    private static final List<RecordedStep> recordedSteps = new ArrayList<>();
    private static final RecordingEventListener eventListener = new RecordingEventListener();

    /**
     * 开始录制
     */
    public static void startRecording() {
        if (isRecording)
            return;
        isRecording = true;
        recordedSteps.clear();
        MinecraftForge.EVENT_BUS.register(eventListener);
        zszlScriptMod.LOGGER.info(I18n.format("log.path.recording_started"));
    }

    /**
     * 停止并放弃录制
     */
    public static void stopAndClearRecording() {
        if (!isRecording)
            return;
        isRecording = false;
        recordedSteps.clear();
        MinecraftForge.EVENT_BUS.unregister(eventListener);
        zszlScriptMod.LOGGER.info(I18n.format("log.path.recording_stopped_cleared"));
    }

    /**
     * 完成录制（不清空数据，等待保存）
     */
    public static void finishRecording() {
        if (!isRecording)
            return;
        isRecording = false;
        MinecraftForge.EVENT_BUS.unregister(eventListener);
        zszlScriptMod.LOGGER.info(I18n.format("log.path.recording_finished"));
    }

    /**
     * 添加一个录制步骤
     * 
     * @param step 录制的步骤数据
     */
    public static void addStep(RecordedStep step) {
        if (isRecording) {
            recordedSteps.add(step);
        }
    }

    public static boolean isRecording() {
        return isRecording;
    }

    public static List<RecordedStep> getRecordedSteps() {
        return recordedSteps;
    }
}




