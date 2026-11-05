package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.FacingQuads;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.node.NodeStabilizerBlockEntity;
import thaumcraft.client.geom.Colors;

public class NodeStabilizerRenderer implements BlockEntityRenderer<NodeStabilizerBlockEntity, NodeStabilizerRenderer.State> {
    private static final Identifier BUBBLE = Thaumcraft.id("textures/misc/node_bubble.png");

    public static class State extends BlockEntityRenderState {
        int count;
        int lock;
        int ambient;
        int viewerTicks;
    }

    public NodeStabilizerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(NodeStabilizerBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        state.count = entity.getCount();
        state.lock = entity.getLock();
        state.ambient = WorldRendering.ambientLight(entity.getLevel(), entity.getBlockPos());
        state.viewerTicks = viewer == null ? 0 : viewer.tickCount;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        NodeDeviceRendering.stabilizer(collector, poseStack, state.ambient, state.lock, state.count, state.viewerTicks, true);
        if (state.count > 0) {
            float alpha = Mth.sin(state.viewerTicks / 8.0F) * 0.1F + 0.5F;
            int color = Colors.withAlpha(state.lock == 1 ? 16777215 : 16729156, state.count / 37.0F * alpha);
            FacingQuads.Camera view = WorldRendering.camera(camera, state.blockPos);
            collector.submitCustomGeometry(
                poseStack,
                TcRenderTypes.additive(BUBBLE),
                (pose, buffer) -> FacingQuads.frame(new VertexQuadSink(pose, buffer), view, 0.5F, 1.5F, 0.5F, 0.0F, 0.9F, 1, 0, color, 220)
            );
        }
    }

    @Override
    public AABB getRenderBoundingBox(NodeStabilizerBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 2.0, 0.0).inflate(1.0);
    }
}
