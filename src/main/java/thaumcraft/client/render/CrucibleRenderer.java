package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.util.FastColor;
import thaumcraft.alchemy.CrucibleBlockEntity;

public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        boolean liquid;
        float height;
        int color;
    }

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(CrucibleBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(CrucibleBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.liquid = entity.getWaterAmount() > 0;
        state.height = entity.getFluidHeight();
        float recolor = entity.tagAmount() / 100.0F;
        if (recolor > 0.0F) {
            recolor = 0.5F + recolor / 2.0F;
        }
        int water = entity.getLevel() instanceof BlockAndTintGetter tintGetter ? BiomeColors.getAverageWaterColor(tintGetter, entity.getBlockPos()) : 0x3F76E4;
        float red = Math.max(0.0F, 1.0F - recolor / 3.0F) * FastColor.ARGB32.red(water) / 255.0F;
        float green = Math.max(0.0F, 1.0F - recolor) * FastColor.ARGB32.green(water) / 255.0F;
        float blue = Math.max(0.0F, 1.0F - recolor / 2.0F) * FastColor.ARGB32.blue(water) / 255.0F;
        state.color = FastColor.ARGB32.colorFromFloat(1.0F, red, green, blue);
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        if (state.liquid) {
            AlchemyRendering.crucibleWater(buffers, poseStack, state.height, state.color, state.lightCoords);
        }
    }
}
