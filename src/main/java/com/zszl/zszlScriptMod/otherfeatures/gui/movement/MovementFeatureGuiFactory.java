package com.zszl.zszlScriptMod.otherfeatures.gui.movement;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public final class MovementFeatureGuiFactory {

    private MovementFeatureGuiFactory() {
    }

    public static GuiScreen create(GuiScreen parent, String featureId) {
        String normalized = featureId == null ? "" : featureId.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
        case "speed":
            return new GuiSpeedConfig(parent);
        case "no_slow":
            return new GuiNoSlowConfig(parent);
        case "force_sprint":
            return new GuiForceSprintConfig(parent);
        case "anti_knockback":
            return new GuiAntiKnockbackConfig(parent);
        case "gui_move":
            return new GuiGuiMoveConfig(parent);
        case "auto_step":
            return new GuiAutoStepConfig(parent);
        case "block_phase":
            return new GuiBlockPhaseConfig(parent);
        case "no_collision":
            return new GuiNoEntityPushConfig(parent);
        case "long_jump":
            return new GuiLongJumpConfig(parent);
        case "timer_accel":
            return new GuiTimerAccelConfig(parent);
        case "blink_move":
            return new GuiBlinkMoveConfig(parent);
        case "safe_walk":
            return new GuiSafeWalkConfig(parent);
        case "scaffold":
            return new GuiScaffoldConfig(parent);
        case "low_gravity":
            return new GuiLowGravityConfig(parent);
        case "ice_boost":
            return new GuiIceBoostConfig(parent);
        case "lava_walk":
            return new GuiLavaWalkConfig(parent);
        case "auto_obstacle_avoid":
            return new GuiAutoObstacleAvoidConfig(parent);
        case "hover_mode":
            return new GuiHoverModeConfig(parent);
        case "fall_cushion":
            return new GuiFallCushionConfig(parent);
        case "no_fall":
            return new GuiNoFallConfig(parent);
        case "anti_arrow_knockback":
            return new GuiAntiArrowKnockbackConfig(parent);
        default:
            return null;
        }
    }
}



