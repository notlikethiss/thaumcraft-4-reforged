package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import thaumcraft.client.platform.ItemGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.FacingQuads;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.node.NodeStabilizerBlockEntity;
import thaumcraft.client.geom.Colors;

public class NodeStabilizerRenderer implements BlockEntityRenderer<NodeStabilizerBlockEntity> {
    private static final ResourceLocation BUBBLE = Thaumcraft.id("textures/misc/node_bubble.png");

    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        int count;
        int lock;
        int ambient;
        int viewerTicks;
    }

    public NodeStabilizerRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(NodeStabilizerBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(NodeStabilizerBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        state.count = entity.getCount();
        state.lock = entity.getLock();
        state.ambient = WorldRendering.ambientLight(entity.getLevel(), entity.getBlockPos());
        state.viewerTicks = viewer == null ? 0 : viewer.tickCount;
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        NodeDeviceRendering.stabilizer(buffers, poseStack, state.ambient, state.lock, state.count, state.viewerTicks, true);
        if (state.count > 0) {
            float alpha = Mth.sin(state.viewerTicks / 8.0F) * 0.1F + 0.5F;
            int color = Colors.withAlpha(state.lock == 1 ? 16777215 : 16729156, state.count / 37.0F * alpha);
            FacingQuads.Camera view = WorldRendering.camera(camera, state.blockPos);
            ItemGeometry.submit(
                buffers,
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
