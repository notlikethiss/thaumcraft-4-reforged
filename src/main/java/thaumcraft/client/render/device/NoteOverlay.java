package thaumcraft.client.render.device;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.device.ArcaneEarBlockEntity;
import thaumcraft.item.gear.Goggles;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class NoteOverlay {
    private static final int SHADOW_COLOR = 0xFF111111;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private NoteOverlay() {
    }

    @SubscribeEvent
    static void onSubmit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof Goggles goggles) || !goggles.showIngamePopups(head, player)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        int note = -1;
        if (minecraft.level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) {
            note = ear.getNote();
        } else {
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.getBlock() instanceof NoteBlock) {
                note = state.getValue(NoteBlock.NOTE);
            }
        }
        if (note < 0) {
            return;
        }
        Component text = Component.translatable("tc.note", note);
        Font font = minecraft.font;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        double dx = camera.x - (pos.getX() + 0.5);
        double dz = camera.z - (pos.getZ() + 0.5);
        float yaw = (float) (Math.atan2(dx, dz) * 180.0 / Math.PI);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.getX() + 0.5 - camera.x, pos.getY() + 1.5 - camera.y, pos.getZ() + 0.5 - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.02F, 0.02F, 0.02F);
        int width = font.width(text);
        int light = 0xF000F0;
        event.getSubmitNodeCollector().submitText(poseStack, 1 - width / 2, 1, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, SHADOW_COLOR, 0, 0);
        poseStack.translate(0.0, 0.0, -0.1);
        event.getSubmitNodeCollector().submitText(poseStack, -width / 2, 0, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, TEXT_COLOR, 0, 0);
        poseStack.popPose();
    }
}
