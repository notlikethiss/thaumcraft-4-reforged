package thaumcraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import thaumcraft.client.geom.EldritchSurfaceGeometry;

public final class EldritchSurfaces {
    private static final int TUNNEL_COLOR = 0xFF191919;
    private static final int FAR_COLOR = 0xFF808080;

    private EldritchSurfaces() {
    }

    public static void submitPortalFaces(SubmitNodeCollector collector, PoseStack poseStack, int mask, boolean nearby, float time) {
        if (mask == 0) {
            return;
        }
        var tunnel = thaumcraft.Thaumcraft.id("textures/misc/tunnel.png");
        var far = thaumcraft.Thaumcraft.id("textures/misc/particlefield32.png");
        var field = thaumcraft.Thaumcraft.id("textures/misc/particlefield.png");
        collector.submitCustomGeometry(poseStack, TcRenderTypes.eldritchSurface(nearby ? tunnel : far), (pose, buffer) ->
            EldritchSurfaceGeometry.faces(new EntityQuadSink(pose, buffer), mask, nearby ? 0.125F : 1.0F, 0, nearby ? TUNNEL_COLOR : FAR_COLOR, 0xF000F0));
        if (nearby) {
            collector.order(1).submitCustomGeometry(poseStack, TcRenderTypes.additive(field), (pose, buffer) -> {
                EntityQuadSink sink = new EntityQuadSink(pose, buffer);
                for (int layer = 1; layer < 16; layer++) {
                    EldritchSurfaceGeometry.faces(sink, mask, 0.5F, EldritchSurfaceGeometry.rotationForLayer(layer, time), EldritchSurfaceGeometry.colorForLayer(layer), 0xF000F0);
                }
            });
        }
    }

    public static void submitPortalRectangle(SubmitNodeCollector collector, PoseStack poseStack, boolean nearby, float time, float x0, float y0, float z0, float x1, float y1, float z1) {
        var tunnel = thaumcraft.Thaumcraft.id("textures/misc/tunnel.png");
        var far = thaumcraft.Thaumcraft.id("textures/misc/particlefield32.png");
        var field = thaumcraft.Thaumcraft.id("textures/misc/particlefield.png");
        if (!nearby) {
            submitRectangle(collector, poseStack, TcRenderTypes.eldritchSurface(far), x0, y0, z0, x1, y1, z1, 1.0F, 0, FAR_COLOR, 0xF000F0);
            return;
        }
        collector.submitCustomGeometry(poseStack, TcRenderTypes.eldritchSurface(tunnel), (pose, buffer) ->
            EldritchSurfaceGeometry.tiledRectangle(new EntityQuadSink(pose, buffer), x0, y0, z0, x1, y1, z1, 0.125F, 0, TUNNEL_COLOR, 0xF000F0));
        collector.order(1).submitCustomGeometry(poseStack, TcRenderTypes.additive(field), (pose, buffer) -> {
            EntityQuadSink sink = new EntityQuadSink(pose, buffer);
            for (int layer = 1; layer < 16; layer++) {
                EldritchSurfaceGeometry.tiledRectangle(sink, x0, y0, z0, x1, y1, z1, 0.5F, EldritchSurfaceGeometry.rotationForLayer(layer, time), EldritchSurfaceGeometry.colorForLayer(layer), 0xF000F0);
            }
        });
    }

    public static void submitFace(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType, int side, float uvScale, float rotation, int color, int light) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> EldritchSurfaceGeometry.cubeFace(new EntityQuadSink(pose, buffer), side, uvScale, rotation, color, light));
    }

    public static void submitRectangle(OrderedSubmitNodeCollector collector, PoseStack poseStack, RenderType renderType, float x0, float y0, float z0, float x1, float y1, float z1, float uvScale, float rotation, int color, int light) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> EldritchSurfaceGeometry.rectangle(new EntityQuadSink(pose, buffer), x0, y0, z0, x1, y1, z1, uvScale, rotation, color, light));
    }
}
