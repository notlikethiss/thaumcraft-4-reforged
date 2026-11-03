package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.alchemy.ThaumatoriumBlockEntity;
import thaumcraft.crafting.CrucibleRecipe;

public class ThaumatoriumRenderer implements BlockEntityRenderer<ThaumatoriumBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        final ItemSlot item = new ItemSlot();
        Direction facing = Direction.NORTH;
    }

    public ThaumatoriumRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ThaumatoriumBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(ThaumatoriumBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = entity.getFacing();
        state.item.clear();
        int count = entity.getRecipeKeys().size();
        if (entity.getLevel() == null || count == 0) {
            return;
        }
        int viewerTicks = EssentiaRendering.viewerTicks();
        CrucibleRecipe recipe = entity.recipeAt(viewerTicks / 40 % count);
        if (recipe != null) {
            ItemStack stack = recipe.result().create();
            stack.setCount(1);
            state.item.set(stack, ItemDisplayContext.FIXED, entity.getLevel(), (int) entity.getBlockPos().asLong());
        }
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        AlchemyRendering.thaumatorium(buffers, poseStack, state.lightCoords, state.facing);
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            AlchemyRendering.thaumatoriumItemTransform(poseStack, state.facing);
            state.item.submit(poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(ThaumatoriumBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0).inflate(1.0);
    }
}
