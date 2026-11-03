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
import net.minecraft.util.Mth;
import thaumcraft.alchemy.BellowsBlock;
import thaumcraft.alchemy.BellowsBlockEntity;

public class BellowsRenderer implements BlockEntityRenderer<BellowsBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        Direction facing = Direction.NORTH;
        float inflation;
    }

    public BellowsRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(BellowsBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(BellowsBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = entity.getBlockState().getValue(BellowsBlock.FACING);
        state.inflation = Mth.lerp(partialTicks, entity.getPrevInflation(), entity.getInflation());
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        AlchemyRendering.bellows(buffers, poseStack, state.lightCoords, state.facing, state.inflation);
    }
}
