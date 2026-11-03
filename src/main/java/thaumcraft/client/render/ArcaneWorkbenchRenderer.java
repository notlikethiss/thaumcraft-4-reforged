package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.wand.WandItem;

public class ArcaneWorkbenchRenderer implements BlockEntityRenderer<ArcaneWorkbenchBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        final ItemSlot wand = new ItemSlot();
    }

    public ArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneWorkbenchBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(ArcaneWorkbenchBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.wand.clear();
        ItemStack wand = entity.getWand();
        if (entity.getLevel() != null && wand.getItem() instanceof WandItem) {
            state.wand.set(wand.copyWithCount(1), ItemDisplayContext.FIXED, entity.getLevel(), (int) entity.getBlockPos().asLong());
        }
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        if (state.wand.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.65F, 1.0625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F));
        state.wand.submit(poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArcaneWorkbenchBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }
}
