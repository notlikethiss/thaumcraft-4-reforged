package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.blockentity.WandPedestalBlockEntity;

public class WandPedestalRenderer implements BlockEntityRenderer<WandPedestalBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        final ItemSlot item = new ItemSlot();
        float ticks;
        float bob;
        boolean draining;
        BlockPos drainPos = BlockPos.ZERO;
        int drainColor;
        long nanoTime;
    }

    public WandPedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WandPedestalBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(WandPedestalBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        ItemStack stack = entity.getStack();
        state.item.clear();
        state.draining = false;
        if (stack.isEmpty() || entity.getLevel() == null) {
            return;
        }
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        state.ticks = (viewer == null ? 0 : viewer.tickCount) + partialTicks;
        state.bob = Mth.sin(state.ticks % 32767.0F / 16.0F) * 0.05F;
        state.nanoTime = System.nanoTime();
        state.item.set(stack.copyWithCount(1), ItemDisplayContext.GROUND, entity.getLevel(), (int) entity.getBlockPos().asLong());
        state.draining = entity.isDraining();
        state.drainPos = entity.getDrainPos();
        state.drainColor = entity.getDrainColor();
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.15F + state.bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks % 360.0F));
        state.item.submit(poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        if (state.draining) {
            BlockPos pos = state.blockPos;
            WorldRendering.submitFloatyLine(
                buffers,
                poseStack,
                pos,
                pos.getX() + 0.5,
                pos.getY() + 1.65 - state.bob * 2.0F,
                pos.getZ() + 0.5,
                state.drainPos.getX() + 0.5,
                state.drainPos.getY() + 0.5,
                state.drainPos.getZ() + 0.5,
                state.drainColor,
                Math.min(state.ticks, 10.0F) / 10.0F,
                state.nanoTime
            );
        }
    }

    @Override
    public AABB getRenderBoundingBox(WandPedestalBlockEntity entity) {
        AABB box = new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
        return entity.isDraining() ? box.minmax(new AABB(entity.getDrainPos())) : box;
    }
}
