package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.node.NodeTransducerBlockEntity;

public class NodeTransducerRenderer implements BlockEntityRenderer<NodeTransducerBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        int count;
        int status;
        int ambient;
        int viewerTicks;
    }

    public NodeTransducerRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(NodeTransducerBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(NodeTransducerBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        state.count = entity.getCount();
        state.status = entity.getStatus();
        state.ambient = WorldRendering.ambientLight(entity.getLevel(), entity.getBlockPos());
        state.viewerTicks = viewer == null ? 0 : viewer.tickCount;
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        NodeDeviceRendering.transducer(buffers, poseStack, state.ambient, state.status, state.count, state.viewerTicks, true);
    }

    @Override
    public AABB getRenderBoundingBox(NodeTransducerBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(1.0);
    }
}
