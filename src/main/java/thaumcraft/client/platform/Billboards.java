package thaumcraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import thaumcraft.client.platform.EntityQuadSink;

public final class Billboards {
    private Billboards() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, RenderType renderType, float u0, float u1, float v0, float v1, float scale, int argb, int light) {
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            EntityQuadSink sink = new EntityQuadSink(pose, buffer);
            sink.vertex(-0.5F, -0.5F, 0.0F, u0, v1, argb, light);
            sink.vertex(0.5F, -0.5F, 0.0F, u1, v1, argb, light);
            sink.vertex(0.5F, 0.5F, 0.0F, u1, v0, argb, light);
            sink.vertex(-0.5F, 0.5F, 0.0F, u0, v0, argb, light);
        });
        poseStack.popPose();
    }
}
