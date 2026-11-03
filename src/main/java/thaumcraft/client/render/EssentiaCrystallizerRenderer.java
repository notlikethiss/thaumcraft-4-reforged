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
import thaumcraft.essentia.EssentiaCrystallizerBlockEntity;

public class EssentiaCrystallizerRenderer implements BlockEntityRenderer<EssentiaCrystallizerBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        Direction facing = Direction.DOWN;
        float spin;
        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        float ticks;
    }

    public EssentiaCrystallizerRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(EssentiaCrystallizerBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(EssentiaCrystallizerBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = entity.getFacing();
        state.spin = entity.getSpin() + entity.getSpinIncrement() * partialTicks;
        state.red = entity.getCurrentRed();
        state.green = entity.getCurrentGreen();
        state.blue = entity.getCurrentBlue();
        state.ticks = EssentiaRendering.viewerTicks() + partialTicks;
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        EssentiaRendering.crystallizer(buffers, poseStack, state.facing, state.spin, state.red, state.green, state.blue, state.ticks, state.lightCoords);
    }
}
