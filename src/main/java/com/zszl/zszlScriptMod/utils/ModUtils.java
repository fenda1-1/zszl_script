package com.zszl.zszlScriptMod.utils;

import com.google.common.primitives.Ints;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.gui.GuiInventoryOverlayScreen;
import com.zszl.zszlScriptMod.handlers.EmbeddedNavigationHandler;
import com.zszl.zszlScriptMod.system.AutoUseItemRule;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.Robot;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModUtils {
    public static final String CLICK_COORDINATE_MODE_RAW = "RAW";
    public static final String CLICK_COORDINATE_MODE_SCALED = "SCALED";
    public static final String CLICK_MOUSE_MOVE_MODE_SILENT = "SILENT";
    public static final String CLICK_MOUSE_MOVE_MODE_MOVE = "MOVE";

    private static final Pattern HEX_PLACEHOLDER_PATTERN = Pattern
            .compile("\\{\\s*([a-zA-Z0-9_]+)\\s*(?:([+-])\\s*([0-9A-Fa-f]+))?\\s*\\}");
    private static final String LEGACY_CHANNEL_NAMESPACE = "legacy";
    private static final String DEFAULT_CUSTOM_PAYLOAD_PATH = "unnamed";
    private static volatile boolean disconnectScheduled = false;

    private ModUtils() {
    }

    public static void disconnectFromCurrentWorld() {
        if (disconnectScheduled) {
            return;
        }
        disconnectScheduled = true;
        Runnable task = () -> {
            disconnectScheduled = false;
            performDisconnectFromCurrentWorld();
        };
        DelayScheduler.init();
        if (DelayScheduler.instance != null) {
            DelayScheduler.instance.schedule(task, 1, "disconnect_world");
        } else {
            task.run();
        }
    }

    private static void performDisconnectFromCurrentWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (mc.level != null) {
            mc.disconnectFromWorld(Component.literal("Disconnected"));
        }
        mc.setScreen(new TitleScreen());
    }

    public static class DelayScheduler {
        public static DelayScheduler instance;
        private final Queue<DelayTask> tasks = new ConcurrentLinkedQueue<>();

        static {
            init();
        }

        private DelayScheduler() {
            TickEvent.ClientTickEvent.BUS.addListener(this::onClientTick);
        }

        public static void init() {
            if (instance == null) {
                instance = new DelayScheduler();
            }
        }

        public void schedule(Runnable action, int delayTicks) {
            schedule(action, delayTicks, null);
        }

        public void schedule(Runnable action, int delayTicks, String tag) {
            if (action != null) {
                tasks.add(new DelayTask(action, Math.max(0, delayTicks), tag));
            }
        }

        public void cancelTasks(java.util.function.Predicate<DelayTask> predicate) {
            if (predicate != null) {
                tasks.removeIf(predicate);
            }
        }

        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }
            Iterator<DelayTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                DelayTask task = iterator.next();
                task.tick();
                if (task.isFinished()) {
                    try {
                        task.run();
                    } finally {
                        iterator.remove();
                    }
                }
            }
        }

        public static class DelayTask {
            private final Runnable action;
            private int remainingTicks;
            private final String tag;

            public DelayTask(Runnable action, int delayTicks, String tag) {
                this.action = action;
                this.remainingTicks = delayTicks;
                this.tag = tag;
            }

            public void tick() {
                remainingTicks--;
            }

            public boolean isFinished() {
                return remainingTicks <= 0;
            }

            public void run() {
                action.run();
            }

            public String getTag() {
                return tag;
            }
        }
    }

    public static class Click {
        public final int slot;
        public final int button;
        public final ClickType type;

        public Click(int slot, int button, ClickType type) {
            this.slot = slot;
            this.button = button;
            this.type = type;
        }
    }

    public static class DelayAction implements Consumer<LocalPlayer> {
        private final int delayTicks;
        private final Runnable action;

        public DelayAction(int delayTicks) {
            this(delayTicks, null);
        }

        public DelayAction(int delayTicks, Runnable action) {
            this.delayTicks = delayTicks;
            this.action = action;
        }

        public int getDelayTicks() {
            return delayTicks;
        }

        @Override
        public void accept(LocalPlayer player) {
            DelayScheduler.init();
            if (action != null) {
                DelayScheduler.instance.schedule(action, delayTicks);
            }
        }
    }

    public static Vec3 worldToScreenPos(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || mc.getWindow() == null || pos == null) {
            return null;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return null;
        }

        Vec3 cameraPos = camera.position();
        Vector3f relative = new Vector3f(
                (float) (pos.x - cameraPos.x),
                (float) (pos.y - cameraPos.y),
                (float) (pos.z - cameraPos.z));
        Quaternionf inverseRotation = new Quaternionf(camera.rotation()).conjugate();
        relative.rotate(inverseRotation);
        if (relative.z() <= 0.0F) {
            return null;
        }

        Matrix4f projection = new Matrix4f(
                mc.gameRenderer.getProjectionMatrix(mc.options.fov().get().intValue()));
        Vector4f clip = new Vector4f(relative.x(), relative.y(), relative.z(), 1.0F).mul(projection);
        if (clip.w() == 0.0F || clip.z() <= 0.0F) {
            return null;
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        float ndcZ = clip.z() / clip.w();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double screenX = (ndcX + 1.0D) * 0.5D * screenWidth;
        double screenY = (1.0D - ndcY) * 0.5D * screenHeight;
        return new Vec3(screenX, screenY, ndcZ);
    }

    public static void setPlayerViewAngles(LocalPlayer player, float yaw, float pitch) {
        if (player == null) {
            return;
        }
        float normalizedYaw = yaw % 360.0F;
        float normalizedPitch = Math.max(-90.0F, Math.min(90.0F, pitch));
        player.setYRot(normalizedYaw);
        player.setXRot(normalizedPitch);
        player.setYHeadRot(normalizedYaw);
        player.setYBodyRot(normalizedYaw);
        player.yRotO = normalizedYaw;
        player.xRotO = normalizedPitch;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundMovePlayerPacket.Rot(normalizedYaw, normalizedPitch, player.onGround(), false));
        }
    }

    public static void sendJumpPacket(LocalPlayer player) {
        if (player == null) {
            return;
        }
        if (!player.onGround() && !player.getAbilities().flying) {
            return;
        }
        player.jumpFromGround();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundMovePlayerPacket.Pos(player.position(), false, false));
        }
    }

    public static void sendChatCommand(String command) {
        String text = command == null ? "" : command.trim();
        if (text.isEmpty()) {
            return;
        }
        if (EmbeddedNavigationHandler.INSTANCE.handleInternalCommand(text)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return;
        }
        if (text.startsWith("/")) {
            connection.sendCommand(text.substring(1));
        } else {
            connection.sendChat(text);
        }
    }

    public static void rightClickOnBlock(LocalPlayer player, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null || mc.gameMode == null || pos == null) {
            return;
        }

        Vec3 blockCenter = Vec3.atCenterOf(pos);
        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = blockCenter.subtract(eyePos);
        if (diff.lengthSqr() > 36.0D) {
            return;
        }

        Direction direction = Direction.getApproximateNearest(diff.x, diff.y, diff.z).getOpposite();
        Vec3 hitVec = blockCenter.add(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(0.5D));
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y,
                Math.max(0.0001D, Math.sqrt(diff.x * diff.x + diff.z * diff.z)))));
        setPlayerViewAngles(player, yaw, pitch);

        scheduleDelayedAction(() -> {
            BlockHitResult hitResult = new BlockHitResult(hitVec, direction, pos, false);
            InteractionResult result = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
            }
        }, 2);
    }

    public static void rightClickOnNearestEntity(LocalPlayer player, BlockPos pos, double range) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null || mc.gameMode == null || pos == null) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        double maxRange = Math.max(1.0D, range);
        EntityHitResult bestHit = null;
        double bestDistSq = maxRange * maxRange;
        for (net.minecraft.world.entity.Entity entity : mc.level.getEntities(player,
                new AABB(center, center).inflate(maxRange))) {
            if (entity == null || entity == player) {
                continue;
            }
            double distSq = entity.distanceToSqr(center);
            if (distSq > bestDistSq) {
                continue;
            }
            bestDistSq = distSq;
            bestHit = new EntityHitResult(entity, entity.position());
        }
        if (bestHit == null) {
            return;
        }

        net.minecraft.world.entity.Entity target = bestHit.getEntity();
        Vec3 diff = target.position().subtract(player.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y,
                Math.max(0.0001D, Math.sqrt(diff.x * diff.x + diff.z * diff.z)))));
        setPlayerViewAngles(player, yaw, pitch);

        EntityHitResult finalHit = bestHit;
        scheduleDelayedAction(() -> {
            InteractionResult result = mc.gameMode.interact(player, target, InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) {
                result = mc.gameMode.interactAt(player, target, finalHit, InteractionHand.MAIN_HAND);
            }
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
            }
        }, 2);
    }

    public static void autoChestClick(LocalPlayer player, int chestSlotIndex) {
        autoChestClick(player, chestSlotIndex, 1);
    }

    public static void autoChestClick(LocalPlayer player, int chestSlotIndex, int delayTicks) {
        autoChestClick(player, chestSlotIndex, delayTicks, "PICKUP");
    }

    public static void autoChestClick(LocalPlayer player, int chestSlotIndex, int delayTicks, String clickTypeName) {
        DelayScheduler.init();
        DelayScheduler.instance.schedule(() -> clickChestSlotNow(chestSlotIndex, clickTypeName), delayTicks);
    }

    public static void clickChestSlotNow(int chestSlotIndex, String clickTypeName) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || player.containerMenu == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (chestSlotIndex < 0 || chestSlotIndex >= menu.slots.size()) {
            return;
        }
        mc.gameMode.handleInventoryMouseClick(menu.containerId, chestSlotIndex, 0, resolveClickType(clickTypeName),
                player);
    }

    public static int resolveLwjglKeyCode(String keyName) {
        if (keyName == null || keyName.trim().isEmpty()) {
            return Keyboard.KEY_NONE;
        }
        String normalized = keyName.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "CTRL":
            case "CONTROL":
                return Keyboard.KEY_LCONTROL;
            case "SHIFT":
                return Keyboard.KEY_LSHIFT;
            case "ALT":
                return Keyboard.KEY_LMENU;
            case "ENTER":
                return Keyboard.KEY_RETURN;
            case "ESC":
            case "ESCAPE":
                return Keyboard.KEY_ESCAPE;
            case "BACKSPACE":
            case "BACK":
                return Keyboard.KEY_BACK;
            case "DELETE":
            case "DEL":
                return Keyboard.KEY_DELETE;
            case "CAPSLOCK":
            case "CAPS":
                return Keyboard.KEY_CAPITAL;
            case "PAGEUP":
            case "PGUP":
                return Keyboard.KEY_PRIOR;
            case "PAGEDOWN":
            case "PGDN":
                return Keyboard.KEY_NEXT;
            case "INS":
                return Keyboard.KEY_INSERT;
            case "UPARROW":
                return Keyboard.KEY_UP;
            case "DOWNARROW":
                return Keyboard.KEY_DOWN;
            case "LEFTARROW":
                return Keyboard.KEY_LEFT;
            case "RIGHTARROW":
                return Keyboard.KEY_RIGHT;
            case "SEMICOLON":
                return Keyboard.KEY_SEMICOLON;
            case "QUOTE":
                return Keyboard.KEY_APOSTROPHE;
            case "BACKTICK":
                return Keyboard.KEY_GRAVE;
            case "LEFTBRACKET":
                return Keyboard.KEY_LBRACKET;
            case "RIGHTBRACKET":
                return Keyboard.KEY_RBRACKET;
            case "NUMPADDECIMAL":
            case "NUMPAD.":
                return Keyboard.KEY_DECIMAL;
            case "NUMPADENTER":
                return Keyboard.KEY_NUMPADENTER;
            case "NUMPADPLUS":
            case "NUMPAD+":
                return Keyboard.KEY_ADD;
            case "NUMPADMINUS":
            case "NUMPAD-":
                return Keyboard.KEY_SUBTRACT;
            case "NUMPADMULTIPLY":
            case "NUMPAD*":
                return Keyboard.KEY_MULTIPLY;
            case "NUMPADDIVIDE":
            case "NUMPAD/":
                return Keyboard.KEY_DIVIDE;
            default:
                return Keyboard.getKeyIndex(normalized);
        }
    }

    public static int parseWindowIdSpec(String windowIdSpec) {
        if (windowIdSpec == null || windowIdSpec.trim().isEmpty()) {
            return -1;
        }

        String spec = windowIdSpec.trim();
        if (spec.contains("{")) {
            spec = resolvePacketHexPlaceholders(spec).trim();
        }

        if (spec.matches("[-+]?\\d+")) {
            return Integer.parseInt(spec);
        }

        String hex = spec.replaceAll("\\s+", "");
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (!hex.matches("[0-9A-Fa-f]+")) {
            throw new IllegalArgumentException("非法窗口ID: " + windowIdSpec);
        }
        if (hex.length() <= 2) {
            int value = Integer.parseInt(hex, 16) & 0xFF;
            if (value >= 0x30 && value <= 0x39) {
                return value - 0x30;
            }
            return value;
        }
        return Integer.parseInt(hex, 16);
    }

    public static int parseNumericSpec(String valueSpec, String baseHint) {
        if (valueSpec == null || valueSpec.trim().isEmpty()) {
            return -1;
        }

        String spec = valueSpec.trim();
        if (spec.contains("{")) {
            spec = resolvePacketHexPlaceholders(spec).trim();
        }

        if ("DEC".equalsIgnoreCase(baseHint)) {
            return Integer.parseInt(spec);
        }

        String hex = spec.replaceAll("\\s+", "");
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if ("HEX".equalsIgnoreCase(baseHint) || hex.matches("[0-9A-Fa-f]+")) {
            return Integer.parseInt(hex, 16);
        }
        if (spec.matches("[-+]?\\d+")) {
            return Integer.parseInt(spec);
        }
        throw new IllegalArgumentException("非法数值: " + valueSpec);
    }

    public static void performWindowClick(String windowIdSpec, int slot, int button, String clickTypeName) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || player.containerMenu == null) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        int expectedWindowId = -1;
        try {
            expectedWindowId = parseWindowIdSpec(windowIdSpec);
        } catch (Exception ignored) {
        }
        if (expectedWindowId >= 0 && expectedWindowId != menu.containerId) {
            return;
        }
        if (slot < 0 || slot >= menu.slots.size()) {
            return;
        }

        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, resolveClickType(clickTypeName), player);
    }

    public static String normalizeClickTypeName(String clickTypeName) {
        return clickTypeName == null || clickTypeName.trim().isEmpty()
                ? "PICKUP"
                : clickTypeName.trim().toUpperCase(Locale.ROOT);
    }

    public static String clickTypeToDisplayName(String clickTypeName) {
        return normalizeClickTypeName(clickTypeName);
    }

    public static ClickType resolveClickType(String clickTypeName) {
        try {
            return ClickType.valueOf(normalizeClickTypeName(clickTypeName));
        } catch (IllegalArgumentException ignored) {
            return ClickType.PICKUP;
        }
    }

    public static void takeAllItemsFromChest(boolean shiftQuickMove) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || player.containerMenu == null) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        List<Integer> containerSlots = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot == null || slot.container == player.getInventory() || !slot.hasItem()) {
                continue;
            }
            containerSlots.add(i);
        }
        for (Integer slotIndex : containerSlots) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 0,
                    shiftQuickMove ? ClickType.QUICK_MOVE : ClickType.PICKUP, player);
        }
    }

    public static void takeAllItemsFromChest() {
        takeAllItemsFromChest(true);
    }

    public static String normalizeSimulatedKeyState(String state) {
        String normalized = state == null ? "" : state.trim().toLowerCase(Locale.ROOT);
        if ("down".equals(normalized) || "robotdown".equals(normalized)) {
            return "Down";
        }
        if ("up".equals(normalized) || "robotup".equals(normalized)) {
            return "Up";
        }
        return "Tap";
    }

    public static void simulateKey(String key, String state) {
        SimulatedKeyInputManager.simulateKey(key, state);
        if (ModConfig.isDebugFlagEnabled(DebugModule.AHK_EXECUTION) && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("simulateKey " + key + " " + state), false);
        }
    }

    public static String normalizeClickCoordinateMode(String coordinateMode) {
        String mode = coordinateMode == null ? "" : coordinateMode.trim().toUpperCase(Locale.ROOT);
        if (CLICK_COORDINATE_MODE_SCALED.equals(mode)) {
            return CLICK_COORDINATE_MODE_SCALED;
        }
        return CLICK_COORDINATE_MODE_RAW;
    }

    public static String inferLegacyClickCoordinateMode(int originalWidth, int originalHeight) {
        if (originalWidth > 0 && originalHeight > 0
                && originalWidth <= 1100
                && originalHeight <= 700) {
            return CLICK_COORDINATE_MODE_SCALED;
        }
        return CLICK_COORDINATE_MODE_RAW;
    }

    public static String normalizeClickCoordinateMode(String coordinateMode, int originalWidth, int originalHeight) {
        if (coordinateMode == null || coordinateMode.trim().isEmpty()) {
            return inferLegacyClickCoordinateMode(originalWidth, originalHeight);
        }
        return normalizeClickCoordinateMode(coordinateMode);
    }

    public static String normalizeClickMouseMoveMode(String mouseMoveMode) {
        String mode = mouseMoveMode == null ? "" : mouseMoveMode.trim().toUpperCase(Locale.ROOT);
        if (CLICK_MOUSE_MOVE_MODE_MOVE.equals(mode)) {
            return CLICK_MOUSE_MOVE_MODE_MOVE;
        }
        return CLICK_MOUSE_MOVE_MODE_SILENT;
    }

    public static void simulateMouseClick(int x, int y, boolean isLeftClick, int originalWidth, int originalHeight,
            String coordinateMode, String mouseMoveMode) {
        simulateMouseClick(x, y, isLeftClick ? "left" : "right", originalWidth, originalHeight, coordinateMode,
                mouseMoveMode);
    }

    public static void simulateMouseClick(int x, int y, String mouseButton, int originalWidth, int originalHeight,
            String coordinateMode, String mouseMoveMode) {
        runOnClientThread(() -> {
            Minecraft mc = Minecraft.getInstance();
            int button = "middle".equalsIgnoreCase(mouseButton)
                    ? 2
                    : ("right".equalsIgnoreCase(mouseButton) ? 1 : 0);
            Screen screen = mc.screen;
            int screenWidth = Math.max(1, mc.getWindow().getScreenWidth());
            int screenHeight = Math.max(1, mc.getWindow().getScreenHeight());
            int scaledWidth = Math.max(1, screen != null && screen.width > 0
                    ? screen.width
                    : mc.getWindow().getGuiScaledWidth());
            int scaledHeight = Math.max(1, screen != null && screen.height > 0
                    ? screen.height
                    : mc.getWindow().getGuiScaledHeight());
            String normalizedCoordinateMode = normalizeClickCoordinateMode(coordinateMode, originalWidth,
                    originalHeight);
            String normalizedMouseMoveMode = normalizeClickMouseMoveMode(mouseMoveMode);

            int rawX;
            int rawYTop;
            int scaledX;
            int scaledY;
            if (CLICK_COORDINATE_MODE_SCALED.equals(normalizedCoordinateMode)) {
                scaledX = scaleCoordinate(x, originalWidth, scaledWidth);
                scaledY = scaleCoordinate(y, originalHeight, scaledHeight);
                rawX = scaleCoordinate(scaledX, scaledWidth, screenWidth);
                rawYTop = scaleCoordinate(scaledY, scaledHeight, screenHeight);
            } else {
                rawX = scaleCoordinate(x, originalWidth, screenWidth);
                rawYTop = scaleCoordinate(y, originalHeight, screenHeight);
                scaledX = scaleCoordinate(rawX, screenWidth, scaledWidth);
                scaledY = scaleCoordinate(rawYTop, screenHeight, scaledHeight);
            }

            if (CLICK_MOUSE_MOVE_MODE_MOVE.equals(normalizedMouseMoveMode)) {
                syncMouseCursor(mc, rawX, rawYTop);
            }

            if (screen instanceof GuiInventoryOverlayScreen) {
                int overlayRawY = toLegacyRawMouseY(rawYTop, mc);
                try {
                    GuiInventory.handleMouseClick(rawX, overlayRawY, button);
                    GuiInventory.handleMouseRelease(rawX, overlayRawY, button);
                    return;
                } catch (IOException e) {
                    zszlScriptMod.LOGGER.warn("Overlay simulated mouse click failed: ({}, {})", rawX, rawYTop, e);
                }
            }

            if (screen != null) {
                try {
                    MouseButtonEvent event = new MouseButtonEvent(scaledX, scaledY, new MouseButtonInfo(button, 0));
                    screen.mouseClicked(event, false);
                    screen.mouseReleased(event);
                    return;
                } catch (Throwable t) {
                    zszlScriptMod.LOGGER.warn("Screen simulated mouse click failed: raw=({}, {}), scaled=({}, {})",
                            rawX, rawYTop, scaledX, scaledY, t);
                }
            }

            if (CLICK_MOUSE_MOVE_MODE_MOVE.equals(normalizedMouseMoveMode)
                    && performDetachedWindowClick(rawX, rawYTop, mouseButton)) {
                return;
            }

            invokeMinecraftClick(mouseButton);
        });
    }

    public static void simulateMouseClick(int x, int y, boolean isLeftClick, int originalWidth, int originalHeight) {
        simulateMouseClick(x, y, isLeftClick, originalWidth, originalHeight,
                CLICK_COORDINATE_MODE_RAW, CLICK_MOUSE_MOVE_MODE_SILENT);
    }

    public static void simulateMouseClick(int x, int y, String mouseButton, int originalWidth, int originalHeight) {
        simulateMouseClick(x, y, mouseButton, originalWidth, originalHeight,
                CLICK_COORDINATE_MODE_RAW, CLICK_MOUSE_MOVE_MODE_SILENT);
    }

    public static boolean moveInventoryItemToHotbar(String itemName, AutoUseItemRule.MatchMode matchMode,
            int targetHotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || player.containerMenu == null) {
            return false;
        }

        String normalizedItemName = normalizeDisplayName(itemName);
        if (normalizedItemName.isEmpty()) {
            return false;
        }

        int targetSlot = Math.max(0, Math.min(8, targetHotbarSlot - 1));
        AbstractContainerMenu menu = player.containerMenu;
        int sourceContainerSlot = -1;
        for (int menuSlotIndex = 0; menuSlotIndex < menu.slots.size(); menuSlotIndex++) {
            Slot slot = menu.slots.get(menuSlotIndex);
            if (slot == null || slot.container != player.getInventory() || !slot.hasItem()) {
                continue;
            }
            if (!matchesDisplayName(normalizedItemName, slot.getItem().getHoverName().getString(), matchMode)) {
                continue;
            }
            sourceContainerSlot = menuSlotIndex;
            break;
        }
        if (sourceContainerSlot < 0) {
            return false;
        }

        mc.gameMode.handleInventoryMouseClick(menu.containerId, sourceContainerSlot, targetSlot, ClickType.SWAP,
                player);
        return true;
    }

    public static boolean switchToHotbarSlot(int targetHotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener connection = mc.getConnection();
        if (player == null || connection == null) {
            return false;
        }
        int slot = Math.max(0, Math.min(8, targetHotbarSlot - 1));
        player.getInventory().setSelectedSlot(slot);
        connection.send(new ServerboundSetCarriedItemPacket(slot));
        return true;
    }

    public static boolean useHeldItemNow() {
        return triggerHeldItemUse(Minecraft.getInstance().player);
    }

    public static void useHeldItem(int delayTicks) {
        useHeldItem(Minecraft.getInstance().player, delayTicks);
    }

    public static void useHeldItem(LocalPlayer player, int delayTicks) {
        scheduleDelayedAction(() -> triggerHeldItemUse(player), delayTicks);
    }

    public static void useItemFromInventory(String itemName, int tempHotbarSlot) {
        useItemFromInventory(itemName, tempHotbarSlot, -1, -1, -1);
    }

    public static void useItemFromInventory(String itemName, int tempHotbarSlot, int switchDelayTicks,
            int useDelayTicks, int switchBackDelayTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            return;
        }

        String normalizedItemName = normalizeDisplayName(itemName);
        if (normalizedItemName.isEmpty()) {
            return;
        }

        int inventorySlot = -1;
        ItemStack matchedStack = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack stack = player.getInventory().getNonEquipmentItems().get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (matchesDisplayName(normalizedItemName, stack.getHoverName().getString(),
                    AutoUseItemRule.MatchMode.CONTAINS)) {
                inventorySlot = i;
                matchedStack = stack.copy();
                break;
            }
        }
        if (inventorySlot < 0) {
            return;
        }

        final int sourceInventorySlot = inventorySlot;
        final int originalHotbarSlot = player.getInventory().getSelectedSlot();
        final boolean alreadyInHotbar = sourceInventorySlot >= 0 && sourceInventorySlot < 9;
        final int safeTempHotbarSlot = Math.max(0, Math.min(8, tempHotbarSlot));
        final int useHotbarSlot = alreadyInHotbar ? sourceInventorySlot : safeTempHotbarSlot;
        final int safeSwitchDelayTicks = switchDelayTicks >= 0 ? Math.max(0, switchDelayTicks)
                : (alreadyInHotbar ? 0 : 1);
        final int safeUseDelayTicks = useDelayTicks >= 0 ? Math.max(0, useDelayTicks) : 1;
        final int safeRestoreDelayTicks = switchBackDelayTicks >= 0 ? Math.max(0, switchBackDelayTicks)
                : getSilentUseAutoRestoreDelayTicks(matchedStack);

        if (!alreadyInHotbar) {
            AbstractContainerMenu menu = player.containerMenu == null ? player.inventoryMenu : player.containerMenu;
            int sourceContainerSlot = findContainerSlotForPlayerInventoryIndex(menu, player, sourceInventorySlot);
            if (sourceContainerSlot < 0) {
                return;
            }
            mc.gameMode.handleInventoryMouseClick(menu.containerId, sourceContainerSlot, safeTempHotbarSlot,
                    ClickType.SWAP, player);
        }

        scheduleDelayedAction(() -> {
            if (!switchToHotbarSlot(useHotbarSlot + 1)) {
                return;
            }
            scheduleDelayedAction(() -> {
                useHeldItem(player, 0);
                scheduleDelayedAction(() -> {
                    switchToHotbarSlot(originalHotbarSlot + 1);
                    if (!alreadyInHotbar) {
                        AbstractContainerMenu menu = player.containerMenu == null ? player.inventoryMenu
                                : player.containerMenu;
                        int sourceContainerSlot = findContainerSlotForPlayerInventoryIndex(menu, player,
                                sourceInventorySlot);
                        if (sourceContainerSlot >= 0) {
                            mc.gameMode.handleInventoryMouseClick(menu.containerId, sourceContainerSlot,
                                    safeTempHotbarSlot, ClickType.SWAP, player);
                        }
                    }
                }, safeRestoreDelayTicks);
            }, safeUseDelayTicks);
        }, safeSwitchDelayTicks);
    }

    public static void sendFmlPacket(String channel, String hexData) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            return;
        }
        try {
            String finalHex = resolvePacketHexPlaceholders(hexData);
            byte[] data = parseHexToBytes(finalHex);
            Identifier identifier = ensureCustomPayloadIdentifier(resolveCustomPayloadIdentifier(channel), channel);
            listener.send(PacketCodecCompat.serverboundCustomPayload(identifier, data));
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("发送频道包失败: " + e.getMessage()), false);
            }
        }
    }

    public static void sendStandardPacketById(int packetId, String hexData) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            return;
        }
        try {
            String finalHex = resolvePacketHexPlaceholders(hexData);
            byte[] data = parseHexToBytes(finalHex);
            Packet<?> packet = PacketCodecCompat.decodeStandardPacket(packetId, data, true);
            if (packet == null) {
                throw new IllegalArgumentException("未知标准包 ID: " + String.format("0x%02X", packetId));
            }
            listener.send(packet);
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("发送标准包失败: " + e.getMessage()), false);
            }
        }
    }

    public static void mockReceiveFmlPacket(String channel, String hexData) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            return;
        }
        runOnClientThread(() -> {
            try {
                String finalHex = resolvePacketHexPlaceholders(hexData);
                byte[] data = parseHexToBytes(finalHex);
                Identifier identifier = ensureCustomPayloadIdentifier(resolveCustomPayloadIdentifier(channel), channel);
                ClientboundCustomPayloadPacket packet = PacketCodecCompat.clientboundCustomPayload(identifier, data);
                PacketCaptureHandler.recordSyntheticPacket(false, packet.getClass().getSimpleName(), true, null,
                        channel, data);
                packet.handle(listener);
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("模拟接收频道包失败: " + e.getMessage()), false);
                }
            }
        });
    }

    public static void mockReceiveStandardPacketById(int packetId, String hexData) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            return;
        }
        runOnClientThread(() -> {
            try {
                String finalHex = resolvePacketHexPlaceholders(hexData);
                byte[] data = parseHexToBytes(finalHex);
                Packet<?> packet = PacketCodecCompat.decodeStandardPacket(packetId, data, false);
                if (packet == null) {
                    throw new IllegalArgumentException("未知标准包 ID: " + String.format("0x%02X", packetId));
                }
                PacketCaptureHandler.recordSyntheticPacket(false, packet.getClass().getSimpleName(), false, packetId,
                        "", data);
                handleClientboundPacket(packet, listener);
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("模拟接收标准包失败: " + e.getMessage()), false);
                }
            }
        });
    }

    private static void scheduleDelayedAction(Runnable action, int delayTicks) {
        DelayScheduler.init();
        int safeDelayTicks = Math.max(0, delayTicks);
        if (safeDelayTicks <= 0 || DelayScheduler.instance == null) {
            runOnClientThread(action);
            return;
        }
        DelayScheduler.instance.schedule(() -> runOnClientThread(action), safeDelayTicks);
    }

    private static boolean triggerHeldItemUse(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.gameMode == null) {
            return false;
        }
        InteractionResult result = mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return result != InteractionResult.FAIL;
    }

    private static int getSilentUseAutoRestoreDelayTicks(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        ItemUseAnimation useAnim = stack.getUseAnimation();
        if (useAnim == ItemUseAnimation.EAT || useAnim == ItemUseAnimation.DRINK) {
            return Math.min(40, Math.max(2, stack.getUseDuration(Minecraft.getInstance().player) + 2));
        }
        if (useAnim == ItemUseAnimation.BOW || useAnim == ItemUseAnimation.BLOCK) {
            return 8;
        }
        return 1;
    }

    private static int findContainerSlotForPlayerInventoryIndex(AbstractContainerMenu menu, LocalPlayer player,
            int inventoryIndex) {
        if (menu == null || player == null) {
            return -1;
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot != null && slot.container == player.getInventory() && slot.getSlotIndex() == inventoryIndex) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeDisplayName(String name) {
        String stripped = ChatFormatting.stripFormatting(name);
        if (stripped == null) {
            stripped = name == null ? "" : name;
        }
        return stripped.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesDisplayName(String normalizedExpectedName, String actualDisplayName,
            AutoUseItemRule.MatchMode matchMode) {
        String normalizedActual = normalizeDisplayName(actualDisplayName);
        if (normalizedExpectedName.isEmpty() || normalizedActual.isEmpty()) {
            return false;
        }
        return matchMode == AutoUseItemRule.MatchMode.EXACT
                ? normalizedActual.equals(normalizedExpectedName)
                : normalizedActual.contains(normalizedExpectedName);
    }

    private static void invokeMinecraftClick(boolean isLeftClick) {
        invokeMinecraftClick(isLeftClick ? "left" : "right");
    }

    private static void invokeMinecraftClick(String mouseButton) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if ("middle".equalsIgnoreCase(mouseButton)) {
                return;
            }
            Method method = Minecraft.class.getDeclaredMethod(
                    "right".equalsIgnoreCase(mouseButton) ? "startUseItem" : "startAttack");
            method.setAccessible(true);
            method.invoke(mc);
        } catch (Exception ignored) {
        }
    }

    private static int scaleCoordinate(int value, int originalSize, int currentSize) {
        int sourceSize = originalSize > 0 ? originalSize : currentSize;
        if (sourceSize <= 0 || currentSize <= 0) {
            return value;
        }
        return Ints.constrainToRange((int) Math.round((value / (double) sourceSize) * currentSize), 0,
                Math.max(0, currentSize - 1));
    }

    private static void syncMouseCursor(Minecraft mc, int rawX, int rawYTop) {
        if (mc == null || mc.mouseHandler == null || mc.mouseHandler.isMouseGrabbed()) {
            return;
        }
        try {
            GLFW.glfwSetCursorPos(mc.getWindow().handle(), rawX, rawYTop);
        } catch (Throwable ignored) {
        }
    }

    private static int toLegacyRawMouseY(int rawYTop, Minecraft mc) {
        if (mc == null) {
            return rawYTop;
        }
        int screenHeight = Math.max(1, mc.getWindow().getScreenHeight());
        return Ints.constrainToRange(screenHeight - rawYTop - 1, 0, screenHeight - 1);
    }

    private static boolean performDetachedWindowClick(int rawX, int rawYTop, boolean isLeftClick) {
        return performDetachedWindowClick(rawX, rawYTop, isLeftClick ? "left" : "right");
    }

    private static boolean performDetachedWindowClick(int rawX, int rawYTop, String mouseButton) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !ModConfig.isMouseDetached) {
            return false;
        }

        try {
            long window = mc.getWindow().handle();
            int[] windowX = new int[1];
            int[] windowY = new int[1];
            GLFW.glfwGetWindowPos(window, windowX, windowY);

            Robot robot = new Robot();
            robot.mouseMove(windowX[0] + rawX, windowY[0] + rawYTop);
            int mask = "middle".equalsIgnoreCase(mouseButton)
                    ? java.awt.event.InputEvent.BUTTON2_DOWN_MASK
                    : ("right".equalsIgnoreCase(mouseButton)
                            ? java.awt.event.InputEvent.BUTTON3_DOWN_MASK
                            : java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.mousePress(mask);
            robot.mouseRelease(mask);
            return true;
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.warn("Detached window mouse click failed at ({}, {})", rawX, rawYTop, t);
            return false;
        }
    }

    private static void runOnClientThread(Runnable runnable) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleClientboundPacket(Packet<?> packet, ClientPacketListener listener) {
        if (packet == null || listener == null) {
            return;
        }
        ((Packet) packet).handle(listener);
    }

    private static byte[] getPlaceholderBytes(String placeholderName) {
        return CapturedIdRuleManager.getCapturedIdBytes(placeholderName);
    }

    private static String bytesToHexWithSpaces(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            builder.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private static byte[] applyOffsetToBytes(byte[] baseBytes, String sign, String offsetHex) {
        BigInteger base = new BigInteger(1, baseBytes);
        BigInteger offset = new BigInteger(offsetHex, 16);
        BigInteger mod = BigInteger.ONE.shiftLeft(baseBytes.length * 8);
        BigInteger result = "+".equals(sign) ? base.add(offset) : base.subtract(offset);
        result = result.mod(mod);

        byte[] raw = result.toByteArray();
        byte[] fixed = new byte[baseBytes.length];
        int copyLength = Math.min(raw.length, fixed.length);
        System.arraycopy(raw, raw.length - copyLength, fixed, fixed.length - copyLength, copyLength);
        return fixed;
    }

    private static String resolvePacketHexPlaceholders(String hexData) {
        String finalHexData = hexData == null ? "" : hexData;
        Matcher matcher = HEX_PLACEHOLDER_PATTERN.matcher(finalHexData);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String sign = matcher.group(2);
            String offsetHex = matcher.group(3);
            byte[] baseBytes = getPlaceholderBytes(name);
            if (baseBytes == null || baseBytes.length == 0) {
                throw new IllegalStateException("HEX中需要 {" + name + "}，但未捕获到对应值。");
            }

            byte[] valueBytes = sign != null && offsetHex != null && !offsetHex.isEmpty()
                    ? applyOffsetToBytes(baseBytes, sign, offsetHex)
                    : baseBytes;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(bytesToHexWithSpaces(valueBytes)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static byte[] parseHexToBytes(String hexData) {
        String cleanHex = hexData == null ? "" : hexData.replaceAll("\\s+", "");
        if (cleanHex.isEmpty()) {
            return new byte[0];
        }
        if ((cleanHex.length() & 1) != 0) {
            cleanHex = "0" + cleanHex;
        }
        byte[] data = new byte[cleanHex.length() / 2];
        for (int i = 0; i < data.length; i++) {
            int index = i * 2;
            data[i] = (byte) Integer.parseInt(cleanHex.substring(index, index + 2), 16);
        }
        return data;
    }

    static Identifier resolveCustomPayloadIdentifier(String channel) {
        String raw = channel == null ? "" : channel.trim();
        if (raw.isEmpty()) {
            return defaultCustomPayloadIdentifier();
        }
        try {
            Identifier direct = Identifier.tryParse(raw);
            if (direct != null) {
                return direct;
            }
        } catch (Exception ignored) {
        }
        Identifier legacy = Identifier.tryParse(LEGACY_CHANNEL_NAMESPACE + ":" + sanitizeLegacyChannel(raw));
        return legacy != null ? legacy : defaultCustomPayloadIdentifier();
    }

    static Identifier ensureCustomPayloadIdentifier(Identifier identifier, String channelHint) {
        if (identifier != null) {
            return identifier;
        }
        String safeChannel = channelHint == null ? "" : channelHint.trim();
        zszlScriptMod.LOGGER.warn("检测到空的自定义包 identifier，已回退为 legacy:unnamed，channel={}", safeChannel);
        return defaultCustomPayloadIdentifier();
    }

    private static Identifier defaultCustomPayloadIdentifier() {
        return Identifier.fromNamespaceAndPath(LEGACY_CHANNEL_NAMESPACE, DEFAULT_CUSTOM_PAYLOAD_PATH);
    }

    private static String sanitizeLegacyChannel(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "unnamed";
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '/'
                    || ch == '.') {
                builder.append(ch);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }
}
