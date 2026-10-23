package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.research.ThaumometerClient;
import thaumcraft.node.NodeView;
import thaumcraft.node.NodeModifier;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ScanKind;
import thaumcraft.research.ScanManager;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHandRenderer {
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#######.##");
    private static final int NODE_TEXT_COLOR = 0xFFEEAE16;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private ThaumometerHandRenderer() {
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ItemStack stack = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || player == null || !stack.is(ModItems.THAUMOMETER.get())) {
            return;
        }
        event.setCanceled(true);
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        int light = event.getPackedLight();
        float swing = event.getSwingProgress();
        float hidden = event.getEquipProgress();
        AvatarRenderState avatar = event.getPlayerState().avatarRenderState;

        poseStack.pushPose();
        applyEquipTransform(poseStack, swing, hidden, avatar != null && avatar.isUsingItem);

        poseStack.pushPose();
        var hands = event.getPlayerState().firstPersonHandsAndItems;
        applyGripTransform(poseStack, hands.viewXRot - hands.xBob, hands.viewYRot - hands.yBob);

        if (avatar != null) {
            poseStack.pushPose();
            poseStack.scale(5.0F, 5.0F, 5.0F);
            AvatarRenderer<?> renderer = (AvatarRenderer<?>) minecraft.getEntityRenderDispatcher().getRenderer(avatar);
            Identifier skin = avatar.skin.body().texturePath();
            for (int index = 0; index < 2; index++) {
                int side = index * 2 - 1;
                poseStack.pushPose();
                poseStack.translate(0.0F, -0.6F, 1.1F * side);
                poseStack.mulPose(Axis.XP.rotationDegrees(-45 * side));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(59.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-65 * side));
                renderer.renderRightHand(poseStack, collector, light, skin, avatar.showRightSleeve);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        applyModelTransform(poseStack);
        ThaumometerItemRenderer.submitModel(collector, poseStack, light);
        ThaumometerItemRenderer.beginScreen(collector, poseStack);
        renderReadout(minecraft, player, poseStack, collector);
        ThaumometerItemRenderer.endScreen(poseStack);

        poseStack.popPose();
        poseStack.popPose();
    }

    static void applyEquipTransform(PoseStack poseStack, float swing, float hidden, boolean usingItem) {
        float swingSqrt = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        if (!usingItem) {
            poseStack.translate(-swingSqrt * 0.4F, Mth.sin(Mth.sqrt(swing) * Mth.PI * 2.0F) * 0.2F, -Mth.sin(swing * Mth.PI) * 0.2F);
        }
        poseStack.translate(0.7F * 0.8F, -0.65F * 0.8F - hidden * 1.5F, -0.9F * 0.8F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.sin(swing * swing * Mth.PI) * 20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-swingSqrt * 20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-swingSqrt * 80.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    static void applyGripTransform(PoseStack poseStack, float pitchBob, float yawBob) {
        poseStack.translate(1.0F, 0.75F, -1.0F);
        poseStack.mulPose(Axis.YN.rotationDegrees(135.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchBob * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawBob * 0.1F));
        // The outer transform already lowers the instrument during a slot change.
        // An equip offset here used to cancel that movement after the 0.4 scale.
        poseStack.translate(-0.7F * 0.8F, 0.65F * 0.8F, 0.9F * 0.8F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 0.0F, -0.9F * 0.8F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
    }

    static void applyModelTransform(PoseStack poseStack) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.4F, -0.4F, 0.0F);
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    private static void renderReadout(Minecraft minecraft, LocalPlayer player, PoseStack poseStack, SubmitNodeCollector collector) {
        Level level = player.level();
        ThaumometerClient.Hit hit = ThaumometerClient.lookAt(level, player);
        if (hit == null) {
            return;
        }
        ScanManager.Target target = hit.target();
        Font font = minecraft.font;
        poseStack.translate(0.0F, 0.0F, -0.01F);
        Component text = Component.literal("?");
        AspectList aspects = null;
        boolean scanned = ScanManager.hasBeenScanned(player, target);
        if (target.kind() == ScanKind.ENTITY) {
            Entity entity = level.getEntity(hit.entityId());
            if (entity != null) {
                text = entity.getName();
            }
            if (scanned) {
                aspects = target.aspects();
            }
        } else if (target.kind() == ScanKind.OBJECT) {
            if (!target.clueStack().isEmpty()) {
                text = target.clueStack().getHoverName();
            } else if (hit.kind() == ScanManager.KIND_BLOCK) {
                text = level.getBlockState(hit.pos()).getBlock().getName();
            }
            if (scanned) {
                aspects = target.aspects();
            }
        } else if (target.kind() == ScanKind.PHENOMENON) {
            BlockPos pos = hit.pos();
            ItemStack picked = level.getBlockState(pos).getCloneItemStack(level, pos, false);
            text = picked.isEmpty() ? level.getBlockState(pos).getBlock().getName() : picked.getHoverName();
            if (scanned && level.getBlockEntity(pos) instanceof NodeView node) {
                aspects = node.getAspects();
                Component nodeText = Component.translatable(node.getNodeType().getTranslationKey());
                NodeModifier modifier = node.getNodeModifier();
                if (modifier != null) {
                    nodeText = Component.empty().append(nodeText).append(", ").append(Component.translatable(modifier.getTranslationKey()));
                }
                poseStack.pushPose();
                float scale = 0.004F;
                poseStack.scale(scale, scale, scale);
                collector.submitText(poseStack, -font.width(nodeText) / 2, -40.0F, nodeText.getVisualOrderText(), false, Font.DisplayMode.NORMAL, ThaumometerItemRenderer.screenLight(0), NODE_TEXT_COLOR, 0, 0);
                poseStack.popPose();
            }
        }

        if (aspects != null) {
            int posX = 0;
            int posY = 0;
            int remaining = aspects.size();
            int baseX = Math.min(5, remaining) * 8;
            for (Aspect aspect : aspects.getAspectsSorted()) {
                poseStack.pushPose();
                poseStack.scale(0.0075F, 0.0075F, 0.0075F);
                int light = ThaumometerItemRenderer.screenLight(posX);
                drawTag(font, poseStack, collector, -baseX + posX * 16, -8 + posY * 16, aspect, aspects.getAmount(aspect), light);
                poseStack.popPose();
                if (++posX >= 5 - posY) {
                    posX = 0;
                    posY++;
                    remaining -= 5 - posY;
                    baseX = Math.min(5 - posY, remaining) * 8;
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, -0.25F, 0.0F);
        int width = font.width(text);
        float scale = 0.005F;
        if (width > 90) {
            scale -= 2.5E-5F * (width - 90);
        }
        poseStack.scale(scale, scale, scale);
        collector.submitText(poseStack, -width / 2, 0.0F, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, ThaumometerItemRenderer.screenLight(0), TEXT_COLOR, 0, 0);
        poseStack.popPose();
    }

    private static void drawTag(Font font, PoseStack poseStack, SubmitNodeCollector collector, int x, int y, Aspect aspect, int amount, int light) {
        int color = ARGB.color(255, aspect.getColor());
        collector.submitCustomGeometry(poseStack, TcRenderTypes.additive(aspect.getImage()), (pose, buffer) -> {
            vertex(buffer, pose, x, y + 16.0F, 0.0F, 1.0F, color, light);
            vertex(buffer, pose, x + 16.0F, y + 16.0F, 1.0F, 1.0F, color, light);
            vertex(buffer, pose, x + 16.0F, y, 1.0F, 0.0F, color, light);
            vertex(buffer, pose, x, y, 0.0F, 0.0F, color, light);
        });
        if (amount > 0) {
            String label = AMOUNT_FORMAT.format(amount);
            poseStack.pushPose();
            poseStack.scale(0.5F, 0.5F, 0.5F);
            Component component = Component.literal(label);
            collector.submitText(poseStack, 32 - font.width(component) + x * 2, 32 - font.lineHeight + y * 2, component.getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, TEXT_COLOR, 0, 0);
            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color, int light) {
        buffer.addVertex(pose, x, y, 0.01F).setColor(color).setUv(u, v).setLight(light);
    }
}
