package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.FastColor;
import thaumcraft.essentia.FilterTubeBlockEntity;

public class TubeFilterRenderer implements BlockEntityRenderer<FilterTubeBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        int color = 0xFFFFFFFF;
    }

    public TubeFilterRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(FilterTubeBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(FilterTubeBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.color = entity.getAspectFilter() == null ? 0xFFFFFFFF : FastColor.ARGB32.opaque(entity.getAspectFilter().getColor());
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        EssentiaRendering.filterCore(buffers, poseStack, state.color, state.lightCoords);
    }
}
