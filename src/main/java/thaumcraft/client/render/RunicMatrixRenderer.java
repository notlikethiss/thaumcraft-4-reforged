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
import thaumcraft.infusion.InfusionMatrixBlockEntity;

public class RunicMatrixRenderer implements BlockEntityRenderer<InfusionMatrixBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        boolean active;
        boolean crafting;
        float startUp;
        int instability;
        int craftCount;
        float ticks;
    }

    public RunicMatrixRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(InfusionMatrixBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(InfusionMatrixBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        state.ticks = (viewer == null ? 0 : viewer.tickCount) + partialTicks;
        state.active = entity.isActive();
        state.crafting = entity.isCrafting();
        state.startUp = entity.getStartUp();
        state.instability = entity.getInstability();
        state.craftCount = entity.getCraftCount();
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        RunicMatrixRendering.submit(buffers, poseStack, state.lightCoords, true, state.active, state.crafting, state.startUp, state.instability, state.craftCount, state.ticks);
    }

    @Override
    public AABB getRenderBoundingBox(InfusionMatrixBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(2.0);
    }
}
