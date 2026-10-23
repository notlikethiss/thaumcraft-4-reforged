package thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.entity.monster.taint.FallingTaint;

public final class FallingTaintRenderer extends EntityRenderer<FallingTaint, FallingTaintRenderState> {
    public FallingTaintRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public boolean shouldRender(FallingTaint entity, Frustum culler, double camX, double camY, double camZ) {
        return super.shouldRender(entity, culler, camX, camY, camZ)
            && entity.getBlockState() != entity.level().getBlockState(entity.blockPosition());
    }

    @Override
    public FallingTaintRenderState createRenderState() {
        return new FallingTaintRenderState();
    }

    @Override
    public void extractRenderState(FallingTaint entity, FallingTaintRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = entity.blockPosition();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = entity.getBlockState();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(FallingTaintRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        BlockState blockState = state.movingBlockRenderState.blockState;
        if (blockState != null && blockState.getRenderShape() == RenderShape.MODEL) {
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);
            collector.submitMovingBlock(poseStack, state.movingBlockRenderState);
            poseStack.popPose();
            super.submit(state, poseStack, collector, camera);
        }
    }
}
