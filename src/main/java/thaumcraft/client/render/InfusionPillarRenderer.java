package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.model.ObjModel;
import thaumcraft.infusion.InfusionPillarBlockEntity;

public class InfusionPillarRenderer implements BlockEntityRenderer<InfusionPillarBlockEntity, InfusionPillarRenderer.State> {
    private static final Identifier MODEL = Thaumcraft.id("models/obj/pillar.obj");
    private static final Identifier TEXTURE = Thaumcraft.id("textures/model/pillar.png");

    public static class State extends BlockEntityRenderState {
        int orientation;
    }

    public InfusionPillarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(InfusionPillarBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(0.1, 0.0, 0.1).expandTowards(0.0, 1.1, 0.0);
    }

    @Override
    public void extractRenderState(InfusionPillarBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.orientation = entity.getOrientation();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ObjModel model = ObjModel.get(MODEL);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        if (state.orientation == 3) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        } else if (state.orientation == 4) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(270.0F));
        } else if (state.orientation == 5) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        int light = state.lightCoords;
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.entityCutout(TEXTURE),
            (pose, buffer) -> model.renderPart("Box001", pose, buffer, 0xFFFFFFFF, light)
        );
        poseStack.popPose();
    }
}
