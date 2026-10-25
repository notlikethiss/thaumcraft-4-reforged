package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.SeeThroughGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.item.gear.Goggles;
import thaumcraft.node.NodeView;
import thaumcraft.research.Knowledge;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class AspectContainerOverlay {
    private static final ResourceLocation UNKNOWN = Thaumcraft.id("textures/aspects/_unknown.png");
    private static final int LIGHT = 0xF000F0;
    private static final int ROW_SIZE = 5;
    private static final float SCALE = 0.3F / 16.0F;

    private AspectContainerOverlay() {
    }

    @SubscribeEvent
    static void onSubmit(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui
            || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof Goggles goggles) || !goggles.showIngamePopups(head, player)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        var entity = minecraft.level.getBlockEntity(pos);
        AspectList contents;
        if (entity instanceof NodeView node) {
            contents = node.getAspects();
        } else if (entity instanceof AspectContainer container) {
            contents = container.getAspects();
        } else {
            return;
        }
        if (contents == null || contents.isEmpty()) {
            return;
        }
        List<Aspect> aspects = contents.getAspectsSorted();
        Vec3 camera = event.getCamera().getPosition();
        boolean spaceAbove = minecraft.level.isEmptyBlock(pos.above());
        Direction side = spaceAbove ? Direction.UP : hit.getDirection();
        double x = pos.getX() + 0.5 + side.getStepX() * 0.6;
        double y = pos.getY() + 0.5 + side.getStepY() * 0.6 + (spaceAbove ? 0.3 : 0);
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.6;
        float yaw = (float) Math.toDegrees(Math.atan2(camera.x - x, camera.z - z));
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        for (int index = 0; index < aspects.size(); index++) {
            Aspect aspect = aspects.get(index);
            int row = index / ROW_SIZE;
            int count = Math.min(ROW_SIZE, aspects.size() - row * ROW_SIZE);
            poseStack.pushPose();
            poseStack.translate((index % ROW_SIZE - (count - 1) / 2.0F) * 20.0F, -row * 22.0F, 0);
            ResourceLocation texture = Knowledge.get(player).hasDiscoveredAspect(aspect) ? aspect.getImage() : UNKNOWN;
            int color = 0xC0000000 | (aspect.getColor() & 0xFFFFFF);
            SeeThroughGeometry.submit(poseStack, TcRenderTypes.translucentNoDepth(texture), (pose, buffer) -> {
                EntityQuadSink sink = new EntityQuadSink(pose, buffer);
                sink.vertex(-8, 8, 0, 0, 1, color, LIGHT);
                sink.vertex(8, 8, 0, 1, 1, color, LIGHT);
                sink.vertex(8, -8, 0, 1, 0, color, LIGHT);
                sink.vertex(-8, -8, 0, 0, 0, color, LIGHT);
            });
            int amount = contents.getAmount(aspect);
            if (amount >= 0) {
                Font font = minecraft.font;
                Component label = Component.literal(Integer.toString(amount));
                poseStack.translate(0, 0, -0.05F);
                poseStack.scale(0.65F, 0.65F, 0.65F);
                SeeThroughGeometry.submitText(poseStack, 13 - font.width(label), 6, label, true, LIGHT, 0xFFFFFFFF);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
