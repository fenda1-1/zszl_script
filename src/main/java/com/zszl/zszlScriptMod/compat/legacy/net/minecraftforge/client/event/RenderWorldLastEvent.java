package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderWorldLastEvent extends MutableEvent {
    public static final EventBus<RenderWorldLastEvent> BUS = EventBus.create(RenderWorldLastEvent.class);

    private final float partialTicks;
    private final PoseStack poseStack;
    private final Matrix4f projectionMatrix;
    private final Camera camera;
    private WorldRenderContext worldRenderContext;
    private boolean worldRenderContextResolved;

    public RenderWorldLastEvent(float partialTicks) {
        this(partialTicks, null, null, null);
    }

    public RenderWorldLastEvent(float partialTicks, PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        this.partialTicks = partialTicks;
        this.poseStack = poseStack;
        this.projectionMatrix = projectionMatrix;
        this.camera = camera;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Camera getCamera() {
        return camera;
    }

    public Vec3 getCameraPosition() {
        WorldRenderContext context = getWorldRenderContext();
        return context == null ? null : context.getCameraPosition();
    }

    public WorldRenderContext getWorldRenderContext() {
        if (!worldRenderContextResolved) {
            worldRenderContext = WorldRenderContext.create(camera);
            worldRenderContextResolved = true;
        }
        return worldRenderContext;
    }

    public PoseStack createWorldPoseStack() {
        WorldRenderContext context = getWorldRenderContext();
        return context == null ? null : context.createWorldPoseStack();
    }

    public Vec3 toCameraSpace(Vec3 worldPos) {
        WorldRenderContext context = getWorldRenderContext();
        return context == null || worldPos == null ? null : context.toCameraSpace(worldPos);
    }

    public AABB toCameraSpace(AABB worldBox) {
        WorldRenderContext context = getWorldRenderContext();
        return context == null || worldBox == null ? null : context.toCameraSpace(worldBox);
    }

    private static Matrix4f matrixFromRotation(Matrix3f rotation) {
        Matrix4f matrix = new Matrix4f().identity();
        matrix.m00(rotation.m00());
        matrix.m01(rotation.m01());
        matrix.m02(rotation.m02());
        matrix.m10(rotation.m10());
        matrix.m11(rotation.m11());
        matrix.m12(rotation.m12());
        matrix.m20(rotation.m20());
        matrix.m21(rotation.m21());
        matrix.m22(rotation.m22());
        return matrix;
    }

    public static final class WorldRenderContext {
        private final Camera camera;
        private final Vec3 cameraPosition;
        private final Matrix4f viewRotationMatrix;

        private WorldRenderContext(Camera camera, Vec3 cameraPosition, Matrix4f viewRotationMatrix) {
            this.camera = camera;
            this.cameraPosition = cameraPosition;
            this.viewRotationMatrix = viewRotationMatrix;
        }

        private static WorldRenderContext create(Camera camera) {
            if (camera == null || !camera.isInitialized()) {
                return null;
            }
            Matrix3f viewRotation = new Matrix3f().set(camera.rotation());
            return new WorldRenderContext(camera, camera.position(), matrixFromRotation(viewRotation));
        }

        public Camera getCamera() {
            return camera;
        }

        public Vec3 getCameraPosition() {
            return cameraPosition;
        }

        public PoseStack createWorldPoseStack() {
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(new Matrix4f(viewRotationMatrix));
            return poseStack;
        }

        public Vec3 toCameraSpace(Vec3 worldPos) {
            return worldPos.subtract(cameraPosition);
        }

        public AABB toCameraSpace(AABB worldBox) {
            return worldBox.move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        }
    }
}

