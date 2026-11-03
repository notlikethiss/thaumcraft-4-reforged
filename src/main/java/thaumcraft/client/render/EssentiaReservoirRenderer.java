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
import thaumcraft.essentia.EssentiaReservoirBlockEntity;

public class EssentiaReservoirRenderer implements BlockEntityRenderer<EssentiaReservoirBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        Direction facing = Direction.DOWN;
        float level;
        float red;
        float green;
        float blue;
    }

    public EssentiaReservoirRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(EssentiaReservoirBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(EssentiaReservoirBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = entity.getFacing();
        int total = entity.getAspects().visSize();
        state.level = entity.getDisplayAspect() != null && total != 0 ? (float) total / entity.getMaxAmount() : 0.0F;
        state.red = entity.getCurrentRed();
        state.green = entity.getCurrentGreen();
        state.blue = entity.getCurrentBlue();
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        EssentiaRendering.reservoir(buffers, poseStack, state.facing, state.lightCoords);
        if (state.level > 0.0F) {
            EssentiaRendering.reservoirLiquid(buffers, poseStack, state.level, state.red, state.green, state.blue);
        }
    }
}
