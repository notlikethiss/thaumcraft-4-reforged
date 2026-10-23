package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.blockentity.WandPedestalBlockEntity;

public class WandPedestalRenderer implements BlockEntityRenderer<WandPedestalBlockEntity, WandPedestalRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float ticks;
        float bob;
        boolean draining;
        BlockPos drainPos = BlockPos.ZERO;
        int drainColor;
        long nanoTime;
    }

    public WandPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WandPedestalBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
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
        itemModelResolver.updateForTopItem(state.item, stack.copyWithCount(1), ItemDisplayContext.GROUND, entity.getLevel(), null, (int) entity.getBlockPos().asLong());
        state.draining = entity.isDraining();
        state.drainPos = entity.getDrainPos();
        state.drainColor = entity.getDrainColor();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.15F + state.bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks % 360.0F));
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (state.draining) {
            BlockPos pos = state.blockPos;
            WorldRendering.submitFloatyLine(
                collector,
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
