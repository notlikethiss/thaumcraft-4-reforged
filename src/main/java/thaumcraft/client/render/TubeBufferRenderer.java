package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import thaumcraft.essentia.Essentia;
import thaumcraft.essentia.EssentiaBufferBlockEntity;

public class TubeBufferRenderer implements BlockEntityRenderer<EssentiaBufferBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        final int[] choked = new int[6];
    }

    public TubeBufferRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(EssentiaBufferBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(EssentiaBufferBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        for (Direction direction : Direction.values()) {
            boolean shown = entity.getLevel() != null
                && entity.getChokedSide(direction) != 0
                && entity.isSideOpen(direction)
                && Essentia.getConnectable(entity.getLevel(), entity.getBlockPos(), direction) != null;
            state.choked[direction.ordinal()] = shown ? entity.getChokedSide(direction) : 0;
        }
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        for (Direction direction : Direction.values()) {
            int choke = state.choked[direction.ordinal()];
            if (choke != 0) {
                EssentiaRendering.buffer(buffers, poseStack, direction, choke == 2, state.lightCoords);
            }
        }
    }
}
