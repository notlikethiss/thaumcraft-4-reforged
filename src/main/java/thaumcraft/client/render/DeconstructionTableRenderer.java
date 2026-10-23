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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.arcane.DeconstructionTableBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.registry.ModItems;

public class DeconstructionTableRenderer implements BlockEntityRenderer<DeconstructionTableBlockEntity, DeconstructionTableRenderer.State> {
    private static final float TAG_SCALE = 0.024F;

    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState thaumometer = new ItemStackRenderState();
        final ItemStackRenderState item = new ItemStackRenderState();
        @Nullable Aspect aspect;
        float ticks;
        float hover;
    }

    public DeconstructionTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DeconstructionTableBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.thaumometer.clear();
        state.item.clear();
        state.aspect = null;
        if (entity.getLevel() == null) {
            return;
        }
        int seed = (int) entity.getBlockPos().asLong();
        itemModelResolver.updateForTopItem(state.thaumometer, new ItemStack(ModItems.THAUMOMETER.get()), ItemDisplayContext.FIXED, entity.getLevel(), null, seed);
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        state.ticks = (viewer == null ? 0 : viewer.tickCount) + partialTicks;
        state.hover = Mth.sin(state.ticks / 14.0F) * 0.2F + 0.2F;
        ItemStack stack = entity.getItem(0);
        if (!stack.isEmpty()) {
            itemModelResolver.updateForTopItem(state.item, stack.copyWithCount(1), ItemDisplayContext.GROUND, entity.getLevel(), null, seed + 1);
        }
        state.aspect = entity.getAspect();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.thaumometer.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.92F, 0.5F);
            poseStack.scale(0.8F, 0.8F, 0.8F);
            state.thaumometer.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.15F + Mth.sin(state.hover) * 0.1F + 0.1F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks % 360.0F));
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        Aspect aspect = state.aspect;
        if (aspect != null) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.081F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.ticks % 360.0F));
            poseStack.scale(TAG_SCALE, TAG_SCALE, TAG_SCALE);
            Identifier image = aspect.getImage();
            int color = 0xCC000000 | aspect.getColor() & 0xFFFFFF;
            int light = state.lightCoords;
            collector.submitCustomGeometry(poseStack, TcRenderTypes.translucent(image), (pose, buffer) -> {
                VertexQuadSink sink = new VertexQuadSink(pose, buffer);
                sink.vertex(-8.0F, -8.0F, 0.0F, 0.0F, 0.0F, color, light);
                sink.vertex(-8.0F, 8.0F, 0.0F, 0.0F, 1.0F, color, light);
                sink.vertex(8.0F, 8.0F, 0.0F, 1.0F, 1.0F, color, light);
                sink.vertex(8.0F, -8.0F, 0.0F, 1.0F, 0.0F, color, light);
            });
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(DeconstructionTableBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }
}
