package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import thaumcraft.essentia.CentrifugeBlockEntity;

public class CentrifugeRenderer implements BlockEntityRenderer<CentrifugeBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        float rotation;
    }

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(CentrifugeBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(CentrifugeBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.rotation = entity.getRotation();
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        EssentiaRendering.centrifuge(buffers, poseStack, state.rotation, state.lightCoords);
    }
}
