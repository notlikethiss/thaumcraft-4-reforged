package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import thaumcraft.alchemy.AdvancedAlchemicalFurnaceBlockEntity;

public class AdvancedFurnaceRenderer implements BlockEntityRenderer<AdvancedAlchemicalFurnaceBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        int heat;
        int vis;
    }

    public AdvancedFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(AdvancedAlchemicalFurnaceBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(AdvancedAlchemicalFurnaceBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.heat = entity.getHeat();
        state.vis = entity.getVis();
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        AlchemyRendering.advancedFurnace(buffers, poseStack, state.lightCoords, state.heat, state.vis, AdvancedAlchemicalFurnaceBlockEntity.MAX_VIS, AdvancedAlchemicalFurnaceBlockEntity.MAX_POWER);
    }
}
