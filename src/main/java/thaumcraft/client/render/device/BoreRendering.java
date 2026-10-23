package thaumcraft.client.render.device;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.TcRenderTypes.Kind;

public final class BoreRendering {
    private static final Identifier BORE_TEXTURE = Thaumcraft.id("textures/model/bore.png");
    private static final Identifier JAR_TEXTURE = Thaumcraft.id("textures/model/jar.png");
    private static final Identifier VORTEX = Thaumcraft.id("textures/misc/vortex.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BEAM_LIGHT = 200;

    private BoreRendering() {
    }

    public record BoreState(
        float rotX,
        float rotZ,
        boolean baseDown,
        int topRotation,
        boolean hasFocus,
        float vortexRotation
    ) {
    }

    private static void box(QuadSink sink, float pivotX, float pivotY, float pivotZ, float x, float y, float z, int width, int height, int depth, int u, int v, int light) {
        ModelBoxGeometry.box(sink, pivotX + x, pivotY + y, pivotZ + z, width, height, depth, u, v, 128.0F, 64.0F, false, WHITE, light);
    }

    private static void model(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, boolean translucent, Consumer<QuadSink> geometry) {
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.of(translucent ? Kind.ENTITY_TRANSLUCENT : Kind.ENTITY_CUTOUT, texture),
            (pose, buffer) -> geometry.accept(new EntityQuadSink(pose, buffer))
        );
    }

    public static void boreBase(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction orientation) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        model(collector, poseStack, BORE_TEXTURE, false, sink -> {
            box(sink, 0.0F, 0.0F, 0.0F, -8.0F, 0.0F, -8.0F, 16, 2, 16, 64, 24, light);
            box(sink, 0.0F, 14.0F, 0.0F, -8.0F, 0.0F, -8.0F, 16, 2, 16, 64, 24, light);
            box(sink, 0.0F, 2.0F, 0.0F, -2.5F, 0.0F, -2.5F, 5, 12, 5, 84, 42, light);
            box(sink, -5.0F, 2.0F, -5.0F, -2.0F, 0.0F, -2.0F, 4, 12, 4, 64, 42, light);
            box(sink, -5.0F, 2.0F, 5.0F, -2.0F, 0.0F, -2.0F, 4, 12, 4, 64, 42, light);
            box(sink, 5.0F, 2.0F, 5.0F, -2.0F, 0.0F, -2.0F, 4, 12, 4, 64, 42, light);
            box(sink, 5.0F, 2.0F, -5.0F, -2.0F, 0.0F, -2.0F, 4, 12, 4, 64, 42, light);
        });
        poseStack.pushPose();
        switch (orientation) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            default -> {
            }
        }
        model(collector, poseStack, BORE_TEXTURE, false, sink -> {
            box(sink, 0.0F, 8.0F, 0.0F, 2.5F, -2.0F, -2.0F, 5, 4, 4, 106, 42, light);
            box(sink, 0.0F, 8.0F, 0.0F, 7.0F, -2.5F, -2.5F, 1, 5, 5, 106, 51, light);
        });
        poseStack.popPose();
        poseStack.popPose();
    }

    public static void bore(SubmitNodeCollector collector, PoseStack poseStack, int light, BoreState state) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotX()));
        poseStack.pushPose();
        if (state.baseDown()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
        model(collector, poseStack, BORE_TEXTURE, false, sink -> {
            box(sink, 0.0F, 0.0F, 0.0F, -6.0F, 0.0F, -6.0F, 12, 2, 12, 0, 32, light);
            box(sink, 0.0F, 0.0F, 0.0F, -2.0F, 2.0F, -5.5F, 4, 8, 1, 0, 0, light);
            box(sink, 0.0F, 0.0F, 0.0F, -2.0F, 2.0F, 4.5F, 4, 8, 1, 0, 0, light);
            box(sink, 0.0F, 8.0F, 0.0F, -1.0F, -1.0F, -6.0F, 2, 2, 12, 0, 48, light);
        });
        poseStack.popPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotZ()));
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -0.5F, 0.0F);
        model(collector, poseStack, BORE_TEXTURE, false, sink -> {
            box(sink, 0.0F, 8.0F, 0.0F, 4.0F, -2.5F, -2.5F, 4, 5, 5, 30, 14, light);
            box(sink, 0.0F, 8.0F, 0.0F, -2.0F, -4.0F, -4.0F, 6, 8, 8, 0, 14, light);
        });
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.topRotation()));
        poseStack.translate(0.0F, 0.5F, 0.0F);
        model(collector, poseStack, BORE_TEXTURE, false, sink -> {
            if (state.hasFocus()) {
                box(sink, 0.0F, 0.0F, 0.0F, -2.0F, 12.0F, -2.0F, 4, 4, 4, 66, 0, light);
            }
            box(sink, 0.0F, 8.0F, 0.0F, -2.0F, 0.0F, -2.0F, 4, 1, 4, 56, 16, light);
            box(sink, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -2.0F, 4, 1, 4, 56, 16, light);
            box(sink, 0.0F, 0.0F, 0.0F, -3.0F, 4.0F, -3.0F, 6, 1, 6, 56, 24, light);
            box(sink, 0.0F, 0.0F, 0.0F, -1.0F, 1.0F, -1.0F, 2, 11, 2, 56, 0, light);
        });
        poseStack.popPose();
        float rotation = state.vortexRotation();
        vortex(collector, poseStack, -0.17F, -(rotation * 8.0F), 10.0F, 0.4F, 1.0F);
        vortex(collector, poseStack, -0.21F, rotation * 8.0F, 10.0F, 0.3F, 0.8F);
        vortex(collector, poseStack, -0.25F, -(rotation * 8.0F), -10.0F, 0.2F, 0.8F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.3F, 0.0F);
        poseStack.scale(0.6F, 0.6F, 0.6F);
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.of(Kind.ENTITY_TRANSLUCENT, JAR_TEXTURE),
            (pose, buffer) -> ModelBoxGeometry.box(new EntityQuadSink(pose, buffer), -5.0F, -12.0F, -5.0F, 10, 12, 10, 0, 0, 64.0F, 32.0F, false, WHITE, light)
        );
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void vortex(SubmitNodeCollector collector, PoseStack poseStack, float offset, float spin, float tilt, float scale, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0F, offset, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        poseStack.mulPose(Axis.YP.rotationDegrees(tilt));
        poseStack.scale(scale, scale, scale);
        int argb = (int) (alpha * 255.0F) << 24 | 0xFFFFFF;
        ItemGeometry.blended(collector, poseStack, VORTEX, 771, sink -> {
            sink.vertex(-0.5F, 0.5F, 0.0F, 0.0F, 1.0F, argb, BEAM_LIGHT);
            sink.vertex(0.5F, 0.5F, 0.0F, 1.0F, 1.0F, argb, BEAM_LIGHT);
            sink.vertex(0.5F, -0.5F, 0.0F, 1.0F, 0.0F, argb, BEAM_LIGHT);
            sink.vertex(-0.5F, -0.5F, 0.0F, 0.0F, 0.0F, argb, BEAM_LIGHT);
        });
        poseStack.popPose();
    }
}
