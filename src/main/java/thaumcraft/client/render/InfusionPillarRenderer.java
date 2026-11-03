package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import thaumcraft.client.platform.ItemGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.model.ObjModel;
import thaumcraft.infusion.InfusionPillarBlockEntity;

public class InfusionPillarRenderer implements BlockEntityRenderer<InfusionPillarBlockEntity> {
    private static final ResourceLocation MODEL = Thaumcraft.id("models/obj/pillar.obj");
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/model/pillar.png");

    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        int orientation;
    }

    public InfusionPillarRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(InfusionPillarBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(InfusionPillarBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.orientation = entity.getOrientation();
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
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
        ItemGeometry.submit(
            buffers,
            poseStack,
            TcRenderTypes.entityCutout(TEXTURE),
            (pose, buffer) -> model.renderPart("Box001", pose, buffer, 0xFFFFFFFF, light)
        );
        poseStack.popPose();
    }
}
