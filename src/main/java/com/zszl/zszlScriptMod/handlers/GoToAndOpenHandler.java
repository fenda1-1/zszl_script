package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.util.Collection;

public final class GoToAndOpenHandler {

    public static final GoToAndOpenHandler INSTANCE = new GoToAndOpenHandler();
    private static final Minecraft MC = Minecraft.getInstance();

    private enum State {
        IDLE,
        MOVING,
        OPENING
    }

    private State currentState = State.IDLE;
    private BlockPos targetChestPos;
    private BlockPos targetStandPos;
    private int timeoutTicks;
    private Collection<EventListener> registeredListeners;

    private GoToAndOpenHandler() {
    }

    public static void start(BlockPos pos) {
        INSTANCE.startInternal(pos);
    }

    private void startInternal(BlockPos chestPos) {
        if (chestPos == null || MC.player == null || MC.level == null) {
            return;
        }
        if (currentState != State.IDLE) {
            MC.player.displayClientMessage(new TextComponentString("§c[仓库] 另一个前往开箱任务仍在进行中。"), false);
            return;
        }

        targetChestPos = chestPos.immutable();
        targetStandPos = findBestStandPosition(chestPos);
        currentState = State.MOVING;
        timeoutTicks = 20 * 30;
        registeredListeners = MinecraftForge.EVENT_BUS.register(this);

        if (targetStandPos != null) {
            MC.player.displayClientMessage(new TextComponentString("§b[仓库] §a开始前往箱子可交互点 @" + targetStandPos), false);
            EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.GO_TO_AND_OPEN,
                    targetStandPos.getX() + 0.5D, targetStandPos.getY(),
                    targetStandPos.getZ() + 0.5D, true, "已找到箱子可交互落脚点，开始前往");
        } else {
            MC.player.displayClientMessage(new TextComponentString("§b[仓库] §e未找到理想落脚点，回退到箱子坐标导航 @" + chestPos), false);
            EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.GO_TO_AND_OPEN,
                    chestPos.getX() + 0.5D, chestPos.getY(),
                    chestPos.getZ() + 0.5D, true, "未找到理想落脚点，回退到箱子坐标导航");
        }
    }

    private void stop() {
        currentState = State.IDLE;
        targetChestPos = null;
        targetStandPos = null;
        timeoutTicks = 0;
        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.GO_TO_AND_OPEN,
                "前往开箱任务结束，停止专属导航");
        try {
            if (registeredListeners != null) {
                MinecraftForge.EVENT_BUS.unregister(registeredListeners);
                registeredListeners = null;
            }
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MC.player == null || currentState == State.IDLE) {
            return;
        }

        timeoutTicks--;
        if (timeoutTicks <= 0) {
            MC.player.displayClientMessage(new TextComponentString("§c[仓库] 前往箱子超时！"), false);
            stop();
            return;
        }

        if (currentState != State.MOVING) {
            return;
        }

        BlockPos arrivePos = targetStandPos != null ? targetStandPos : targetChestPos;
        if (arrivePos == null) {
            stop();
            return;
        }

        double distanceSq = MC.player.distanceToSqr(arrivePos.getX() + 0.5D, arrivePos.getY() + 0.5D, arrivePos.getZ() + 0.5D);
        if (distanceSq > 4.0D) {
            return;
        }

        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.GO_TO_AND_OPEN,
                "已到达箱子附近，停止前往开箱导航");
        currentState = State.OPENING;
        MC.player.displayClientMessage(new TextComponentString("§b[仓库] §a已到达箱子附近，正在尝试打开..."), false);
        ModUtils.DelayScheduler.init();
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (currentState == State.OPENING && targetChestPos != null && MC.player != null) {
                ModUtils.rightClickOnBlock(MC.player, targetChestPos);
            }
        }, 5);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (currentState != State.OPENING || event == null || event.getGui() == null) {
            return;
        }
        if (MC.player == null || !(MC.player.containerMenu instanceof ChestMenu)) {
            return;
        }
        MC.player.displayClientMessage(new TextComponentString("§b[仓库] §a已成功打开目标箱子。"), false);
        stop();
    }

    private BlockPos findBestStandPosition(BlockPos chestPos) {
        if (MC.level == null || chestPos == null) {
            return null;
        }
        BlockPos[] candidates = new BlockPos[] {
                chestPos.north(),
                chestPos.south(),
                chestPos.west(),
                chestPos.east(),
                chestPos
        };

        BlockPos best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (BlockPos candidate : candidates) {
            if (!isStandable(candidate)) {
                continue;
            }
            double distanceSq = MC.player == null ? 0.0D
                    : MC.player.distanceToSqr(candidate.getX() + 0.5D, candidate.getY() + 0.5D,
                            candidate.getZ() + 0.5D);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private boolean isStandable(BlockPos pos) {
        if (MC.level == null || pos == null) {
            return false;
        }
        return MC.level.getBlockState(pos).canBeReplaced()
                && MC.level.getBlockState(pos.above()).canBeReplaced()
                && MC.level.getBlockState(pos.below()).isSolid();
    }

    public static boolean isBusy() {
        return INSTANCE.currentState != State.IDLE;
    }

    public static boolean isTargetChest(BlockPos pos) {
        return INSTANCE.targetChestPos != null && INSTANCE.targetChestPos.equals(pos);
    }

    public static BlockPos getTargetChestPos() {
        return INSTANCE.targetChestPos == null ? null : INSTANCE.targetChestPos.immutable();
    }
}
