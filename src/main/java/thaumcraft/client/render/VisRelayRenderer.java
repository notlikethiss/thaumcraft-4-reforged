package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.PowerBeamGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.vis.VisNet;
import thaumcraft.vis.VisRelayBlockEntity;
import thaumcraft.client.geom.Colors;

public class VisRelayRenderer implements BlockEntityRenderer<VisRelayBlockEntity, VisRelayRenderer.State> {
    private static final Identifier BEAM = Thaumcraft.id("textures/misc/beam1.png");

    public static class State extends BlockEntityRenderState {
        Direction orientation = Direction.UP;
        int crystalColor = -1;
        boolean connected;
        int ambient;
        float ticks;
        float slide;
        boolean hasBeam;
        float startX;
        float startY;
        float startZ;
        float endX;
        float endY;
        float endZ;
        float opacity;
        int beamColor;
    }

    public VisRelayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VisRelayBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        int viewerTicks = viewer == null ? 0 : viewer.tickCount;
        state.orientation = entity.getOrientation();
        byte color = entity.getColor();
        state.crystalColor = color >= 0 && color < VisRelayBlockEntity.COLORS.length ? VisRelayBlockEntity.COLORS[color] : -1;
        state.connected = VisNet.isNodeValid(entity.getParent());
        state.ambient = WorldRendering.ambientLight(entity.getLevel(), entity.getBlockPos());
        state.ticks = viewerTicks + partialTicks;
        state.slide = viewerTicks + partialTicks;
        state.hasBeam = false;
        BlockPos parentPos = entity.getParentPos();
        if (parentPos != null && state.connected) {
            Direction parentSide = null;
            if (entity.getLevel() != null && entity.getLevel().getBlockEntity(parentPos) instanceof VisRelayBlockEntity parentRelay) {
                parentSide = parentRelay.getOrientation();
            }
            BlockPos pos = entity.getBlockPos();
            state.hasBeam = true;
            state.startX = parentPos.getX() + 0.5F - pos.getX() - (parentSide == null ? 0.0F : parentSide.getStepX() * 0.05F);
            state.startY = parentPos.getY() + 0.5F - pos.getY() - (parentSide == null ? 0.0F : parentSide.getStepY() * 0.05F);
            state.startZ = parentPos.getZ() + 0.5F - pos.getZ() - (parentSide == null ? 0.0F : parentSide.getStepZ() * 0.05F);
            state.endX = 0.5F - state.orientation.getStepX() * 0.05F;
            state.endY = 0.5F - state.orientation.getStepY() * 0.05F;
            state.endZ = 0.5F - state.orientation.getStepZ() * 0.05F;
            float pulseBoost = entity.getPulse() > 0 ? 0.5F * entity.getPulse() / 5.0F : 0.0F;
            float reveal = viewer instanceof LivingEntity living && NodeVisibility.wearsRevealer(living) ? 1.0F : 0.1F;
            state.opacity = (0.3F + pulseBoost) * reveal;
            state.beamColor = Colors.rgb(entity.getPulseRed(), entity.getPulseGreen(), entity.getPulseBlue());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        NodeDeviceRendering.relay(collector, poseStack, state.ambient, state.orientation, state.crystalColor, state.connected, state.ticks);
        if (state.hasBeam) {
            collector.submitCustomGeometry(
                poseStack,
                TcRenderTypes.additive(BEAM),
                (pose, buffer) -> PowerBeamGeometry.render(
                    state.startX,
                    state.startY,
                    state.startZ,
                    state.endX,
                    state.endY,
                    state.endZ,
                    state.slide,
                    state.opacity,
                    state.beamColor,
                    new VertexQuadSink(pose, buffer)
                )
            );
        }
    }

    @Override
    public AABB getRenderBoundingBox(VisRelayBlockEntity entity) {
        AABB box = new AABB(entity.getBlockPos()).inflate(1.0);
        BlockPos parent = entity.getParentPos();
        return parent == null ? box : box.minmax(new AABB(parent).inflate(1.0));
    }

    @Override
    public boolean shouldRender(VisRelayBlockEntity entity, Vec3 cameraPosition) {
        return true;
    }
}
