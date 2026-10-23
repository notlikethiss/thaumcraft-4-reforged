package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.FacingQuads;
import thaumcraft.client.geom.FloatyLineGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;

public final class WorldRendering {
    public static final QuadSink NOOP = (x, y, z, u, v, argb, light) -> {
    };
    public static final Identifier WISPY = Thaumcraft.id("textures/misc/wispy.png");

    private WorldRendering() {
    }

    public static FacingQuads.Camera camera(CameraRenderState camera, BlockPos origin) {
        return new FacingQuads.Camera(
            camera.yRot,
            camera.xRot,
            (float) (camera.pos.x - origin.getX()),
            (float) (camera.pos.y - origin.getY()),
            (float) (camera.pos.z - origin.getZ())
        );
    }

    public static int ambientLight(@Nullable Level level, BlockPos pos) {
        if (level == null) {
            return LightCoordsUtil.FULL_BRIGHT;
        }
        int light = LevelRenderer.getLightCoords(level, pos);
        for (Direction direction : Direction.values()) {
            light = LightCoordsUtil.max(light, LevelRenderer.getLightCoords(level, pos.relative(direction)));
        }
        return light;
    }

    public static int emissiveLight(float level) {
        return Math.clamp((int) level, 0, 240);
    }

    public static void submitFloatyLine(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        BlockPos origin,
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ,
        int color,
        float distance,
        long nanoTime
    ) {
        FloatyLineGeometry.Params params = new FloatyLineGeometry.Params(
            startX,
            startY,
            startZ,
            endX,
            endY,
            endZ,
            origin.getX(),
            origin.getY(),
            origin.getZ(),
            (float) (nanoTime / 30000000L),
            color,
            -0.02F,
            distance,
            FloatyLineGeometry.DEFAULT_WIDTH
        );
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.additive(WISPY),
            (pose, buffer) -> FloatyLineGeometry.render(params, new VertexQuadSink(pose, buffer))
        );
    }
}
