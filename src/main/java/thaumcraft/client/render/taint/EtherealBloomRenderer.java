package thaumcraft.client.render.taint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.blockentity.EtherealBloomBlockEntity;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.geom.NodeStripGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.client.render.NodeRendering;

public class EtherealBloomRenderer implements BlockEntityRenderer<EtherealBloomBlockEntity, EtherealBloomRenderer.State> {
    private static final int LIGHT = 200;
    private static final int ADDITIVE = 1;

    public static class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        int counter;
        int growthCounter;
    }

    public EtherealBloomRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EtherealBloomBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.counter = entity.getCounter();
        state.growthCounter = entity.getGrowthCounter();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float growth = state.growthCounter;
        float rc1 = Math.min(growth, 100.0F);
        float rc2 = Math.min(growth, 50.0F);
        float rc3 = Math.clamp(growth - 33.0F, 0.0F, 33.0F);
        float rc4 = Math.clamp(growth - 66.0F, 0.0F, 33.0F) * 0.7F;
        float scale1 = rc1 / 100.0F;
        float scale2 = rc2 / 60.0F + 0.1666666F;
        float scale3 = rc3 / 33.0F;
        float scale4 = rc4 / 33.0F;
        OrderedSubmitNodeCollector ordered = collector.order(1);
        poseStack.pushPose();
        poseStack.translate(0.5F, scale1, 0.5F);
        float animationTicks = NodeRendering.clientTicks();
        int frame = (state.counter + (int) animationTicks) % 32;
        ItemGeometry.blended(ordered, poseStack, NodeRendering.TEXTURE, ADDITIVE,
            sink -> NodeStripGeometry.strip(sink, scale1, 32, 6, frame, 0x00AAFFFF, LIGHT));
        poseStack.popPose();

        renderLeaves(ordered, poseStack, "textures/block/purifier_leaves.png", 0.25F, scale1, scale3, 0.0F);
        renderLeaves(ordered, poseStack, "textures/block/purifier_leaves.png", 0.6F, scale1 * 0.7F, scale4, 45.0F);
        renderLeaves(ordered, poseStack, "textures/block/purifier_stalk.png", 0.5F + (1.0F - scale1) / 2.0F, scale1, scale2, 0.0F);
        if (scale4 > 0.0F) {
            renderCore(ordered, poseStack, scale4, scale1, state.counter);
        }
    }

    private static void renderLeaves(OrderedSubmitNodeCollector collector, PoseStack poseStack, String texture, float y, float height, float width, float rotation) {
        if (height <= 0.0F || width <= 0.0F) {
            return;
        }
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, y, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
            poseStack.scale(width, height, width);
            ItemGeometry.blended(collector, poseStack, Thaumcraft.id(texture), ADDITIVE, EtherealBloomRenderer::quad);
            poseStack.popPose();
        }
    }

    private static void renderCore(OrderedSubmitNodeCollector collector, PoseStack poseStack, float width, float height, int counter) {
        poseStack.pushPose();
        poseStack.translate(0.5F - width / 8.0F, height - width / 6.0F, 0.5F - width / 8.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(counter * 11.25F));
        poseStack.scale(width / 4.0F, height / 3.0F, width / 4.0F);
        ItemGeometry.blended(collector, poseStack, Thaumcraft.id("textures/model/crystalcapacitor.png"), ADDITIVE,
            sink -> ModelBoxGeometry.box(sink, 0.0F, 0.0F, 0.0F, 16, 16, 16, 0, 0, 64.0F, 32.0F, true, 0xFFFFFFFF, LIGHT));
        poseStack.popPose();
    }

    private static void quad(QuadSink sink) {
        sink.vertex(-0.5F, 0.5F, 0.0F, 0.0F, 1.0F, 0xFFFFFFFF, LIGHT);
        sink.vertex(0.5F, 0.5F, 0.0F, 1.0F, 1.0F, 0xFFFFFFFF, LIGHT);
        sink.vertex(0.5F, -0.5F, 0.0F, 1.0F, 0.0F, 0xFFFFFFFF, LIGHT);
        sink.vertex(-0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0xFFFFFFFF, LIGHT);
    }
}
