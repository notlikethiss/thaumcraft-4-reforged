package thaumcraft.client.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.arcane.VisChargeRelayBlockEntity;
import thaumcraft.vis.VisNet;
import thaumcraft.vis.VisRelayBlockEntity;

public class ChargeRelayRenderer implements BlockEntityRenderer<VisChargeRelayBlockEntity> {
    public static class State {
        int lightCoords;
        BlockPos blockPos = BlockPos.ZERO;
        int crystalColor = -1;
        boolean connected;
        int ambient;
        float ticks;
    }

    public ChargeRelayRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public void render(VisChargeRelayBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        State state = new State();
        state.lightCoords = packedLight;
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTick, camera.getPosition());
        submit(state, poseStack, buffers, camera);
    }

    private void extract(VisChargeRelayBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        byte color = entity.getColor();
        state.crystalColor = color >= 0 && color < VisRelayBlockEntity.COLORS.length ? VisRelayBlockEntity.COLORS[color] : -1;
        state.connected = VisNet.isNodeValid(entity.getParent());
        state.ambient = WorldRendering.ambientLight(entity.getLevel(), entity.getBlockPos());
        state.ticks = (viewer == null ? 0 : viewer.tickCount) + partialTicks;
    }

    private void submit(State state, PoseStack poseStack, MultiBufferSource buffers, Camera camera) {
        NodeDeviceRendering.charger(buffers, poseStack, state.ambient, state.crystalColor, state.connected, state.ticks);
    }
}
