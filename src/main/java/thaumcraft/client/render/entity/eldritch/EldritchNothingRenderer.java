package thaumcraft.client.render.entity.eldritch;

import com.mojang.blaze3d.vertex.PoseStack;
import thaumcraft.client.platform.ImmediateGeometry;
import thaumcraft.client.platform.StateBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import thaumcraft.client.platform.BlockEntityRenderState;
import thaumcraft.client.platform.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import thaumcraft.client.platform.EldritchSurfaces;
import thaumcraft.eldritch.EldritchNothingBlockEntity;

public final class EldritchNothingRenderer implements StateBlockRenderer<EldritchNothingBlockEntity, EldritchNothingRenderer.State> {
    static final class State extends BlockEntityRenderState {
        int exposed;
        boolean nearby;
        float time;
    }

    public EldritchNothingRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EldritchNothingBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, @Nullable Object breakProgress) {
        StateBlockRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.nearby = cameraPosition.distanceToSqr(Vec3.atCenterOf(entity.getBlockPos())) < 512.0;
        state.time = (entity.getLevel() == null ? 0.0F : (float) entity.getLevel().getGameTime()) + partialTicks;
        state.exposed = 0;
        if (entity.getLevel() == null || !entity.exposed()) {
            return;
        }
        BlockPos pos = entity.getBlockPos();
        for (Direction direction : Direction.values()) {
            var neighbor = entity.getLevel().getBlockState(pos.relative(direction));
            if (thaumcraft.client.geom.EldritchSurfaceGeometry.exposed(neighbor.is(thaumcraft.registry.EldritchContent.ELDRITCH_NOTHING.get()), neighbor.isSolidRender(entity.getLevel(), pos.relative(direction)))) {
                state.exposed |= 1 << direction.ordinal();
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, ImmediateGeometry collector, CameraRenderState camera) {
        EldritchSurfaces.submitPortalFaces(collector, poseStack, state.exposed, state.nearby, state.time);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(EldritchNothingBlockEntity entity) {
        return new net.minecraft.world.phys.AABB(entity.getBlockPos()).inflate(0.1);
    }
}
