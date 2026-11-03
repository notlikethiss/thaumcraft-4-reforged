package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import thaumcraft.client.platform.ItemGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.geom.FacingQuads;
import thaumcraft.client.geom.NodeGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.NodeRenderTypes;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;
import thaumcraft.client.geom.Colors;

public abstract class NodeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation NODES = Thaumcraft.id("textures/misc/nodes.png");
    private static final ResourceLocation RING = Thaumcraft.id("textures/item/lightningringv.png");
    private static final int RING_FRAMES = 16;
    private static final int RING_LIGHT = 220;

    public record Drain(double startX, double startY, double startZ, double endX, double endY, double endZ, int color, float distance) {
    }

    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        boolean skip;
        boolean full;
        boolean depthIgnore;
        boolean needsTranslucent;
        float distance;
        float viewDistance;
        float size = 1.0F;
        float yOffset;
        List<NodeGeometry.AspectLayer> layers = List.of();
        NodeGeometry.CoreType type = NodeGeometry.CoreType.NORMAL;
        NodeGeometry.Modifier modifier = NodeGeometry.Modifier.NONE;
        int viewerTicks;
        long nanoTime;
        boolean ring;
        @Nullable Drain drain;
    }

    protected NodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    protected abstract void fill(T entity, State state, float partialTicks);


    @Override
    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(T entity, State state, float partialTicks, Vec3 cameraPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity viewer = minecraft.getCameraEntity();
        state.nanoTime = System.nanoTime();
        state.viewerTicks = viewer == null ? 0 : viewer.tickCount;
        state.skip = false;
        state.full = false;
        state.depthIgnore = false;
        state.ring = false;
        state.drain = null;
        state.size = 1.0F;
        state.yOffset = 0.0F;
        state.viewDistance = 64.0F;
        fill(entity, state, partialTicks);
        BlockPos pos = entity.getBlockPos();
        state.distance = (float) cameraPosition.distanceTo(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5 + state.yOffset, pos.getZ() + 0.5));
        if (state.full && state.distance > state.viewDistance) {
            state.skip = true;
        }
        state.needsTranslucent = false;
        if (state.full) {
            for (NodeGeometry.AspectLayer layer : state.layers) {
                if (layer.blend() == NodeGeometry.TRANSLUCENT) {
                    state.needsTranslucent = true;
                }
            }
            if (state.type == NodeGeometry.CoreType.DARK || state.type == NodeGeometry.CoreType.TAINTED) {
                state.needsTranslucent = true;
            }
        }
    }

    protected static void fillNodeData(State state, AspectList aspects, NodeType type, @Nullable NodeModifier modifier) {
        List<NodeGeometry.AspectLayer> layers = new ArrayList<>();
        for (Aspect aspect : aspects.getAspects()) {
            layers.add(new NodeGeometry.AspectLayer(aspect.getColor(), aspects.getAmount(aspect), aspect.getBlend()));
        }
        state.layers = layers;
        state.type = NodeGeometry.CoreType.valueOf(type.name());
        state.modifier = modifier == null ? NodeGeometry.Modifier.NONE : NodeGeometry.Modifier.valueOf(modifier.name());
    }

    protected static void fillRevealed(State state, BlockPos pos) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        double distance = viewer == null ? 0.0 : NodeVisibility.revealDistance(viewer, pos.getX(), pos.getY(), pos.getZ());
        if (distance > 0.0) {
            state.full = true;
            state.depthIgnore = true;
            state.viewDistance = (float) distance;
        }
    }

    protected static @Nullable Drain drainFor(NodeBlockEntity node, float partialTicks) {
        Entity drainEntity = node.getDrainEntity();
        HitResult collision = node.getDrainCollision();
        if (drainEntity == null || !(collision instanceof BlockHitResult blockHit)) {
            return null;
        }
        int using = 0;
        float sway = 0.0F;
        if (drainEntity instanceof Player player) {
            if (!player.isUsingItem()) {
                return null;
            }
            using = player.getTicksUsingItem();
            sway = Mth.sin(using / 10.0F) * 10.0F;
        }
        float[] offset = {-0.1F, -0.1F, 0.5F};
        rotateX(offset, -Mth.lerp(partialTicks, drainEntity.xRotO, drainEntity.getXRot()) * (float) Math.PI / 180.0F);
        rotateY(offset, -Mth.lerp(partialTicks, drainEntity.yRotO, drainEntity.getYRot()) * (float) Math.PI / 180.0F);
        rotateY(offset, -sway * 0.01F);
        rotateX(offset, -sway * 0.015F);
        double startX = Mth.lerp(partialTicks, drainEntity.xo, drainEntity.getX()) + offset[0];
        double startY = Mth.lerp(partialTicks, drainEntity.yo, drainEntity.getY()) + offset[1] + drainEntity.getEyeHeight();
        double startZ = Mth.lerp(partialTicks, drainEntity.zo, drainEntity.getZ()) + offset[2];
        BlockPos target = blockHit.getBlockPos();
        return new Drain(startX, startY, startZ, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, node.getColor(), Math.min(using, 10) / 10.0F);
    }

    private static void rotateX(float[] vector, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float y = vector[1] * cos + vector[2] * sin;
        float z = vector[2] * cos - vector[1] * sin;
        vector[1] = y;
        vector[2] = z;
    }

    private static void rotateY(float[] vector, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float x = vector[0] * cos + vector[2] * sin;
        float z = vector[2] * cos - vector[0] * sin;
        vector[0] = x;
        vector[2] = z;
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        BlockPos pos = state.blockPos;
        if (!state.skip) {
            FacingQuads.Camera view = WorldRendering.camera(camera, pos);
            NodeGeometry.Params params = new NodeGeometry.Params(
                view,
                0.5F,
                0.5F + state.yOffset,
                0.5F,
                pos.getX(),
                state.full,
                state.distance,
                state.viewDistance,
                state.size,
                state.layers,
                state.type,
                state.modifier,
                state.viewerTicks,
                state.nanoTime
            );
            ItemGeometry.submit(buffers, poseStack, NodeRenderTypes.of(NODES, false, state.depthIgnore), (pose, buffer) -> NodeGeometry.render(params, new VertexQuadSink(pose, buffer), WorldRendering.NOOP));
            if (state.needsTranslucent) {
                ItemGeometry.submit(buffers, poseStack, NodeRenderTypes.of(NODES, true, state.depthIgnore), (pose, buffer) -> NodeGeometry.render(params, WorldRendering.NOOP, new VertexQuadSink(pose, buffer)));
            }
            if (state.ring) {
                int frame = (int) ((state.nanoTime / 40000000L + pos.getX()) % RING_FRAMES);
                ItemGeometry.submit(buffers, poseStack, TcRenderTypes.additive(RING), (pose, buffer) -> {
                    QuadSink sink = new VertexQuadSink(pose, buffer);
                    FacingQuads.frame(sink, view, 0.5F, 0.5F, 0.5F, 0.0F, 0.33F, RING_FRAMES, frame, Colors.withAlpha(0xFFFFFF, 0.9F), RING_LIGHT);
                });
            }
        }
        Drain drain = state.drain;
        if (drain != null) {
            WorldRendering.submitFloatyLine(
                buffers,
                poseStack,
                pos,
                drain.startX(),
                drain.startY(),
                drain.startZ(),
                drain.endX(),
                drain.endY(),
                drain.endZ(),
                drain.color(),
                drain.distance(),
                state.nanoTime
            );
        }
    }

    @Override
    public boolean shouldRenderOffScreen(T entity) {
        return true;
    }

    @Override
    public boolean shouldRender(T entity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(T entity) {
        return new AABB(entity.getBlockPos()).inflate(8.0);
    }
}
