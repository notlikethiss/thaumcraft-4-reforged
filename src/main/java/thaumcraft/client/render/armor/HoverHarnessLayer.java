package thaumcraft.client.render.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.model.ObjModel;
import thaumcraft.item.armor.Hover;
import thaumcraft.item.armor.HoverHarnessItem;

public class HoverHarnessLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier MODEL = Thaumcraft.id("models/obj/hoverharness.obj");
    private static final Identifier TEXTURE = Thaumcraft.id("textures/model/hoverharness2.png");
    private static final Identifier RING = Thaumcraft.id("item/lightningring");
    private static final int RING_LIGHT = 230;

    public HoverHarnessLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (!(state.chestEquipment.getItem() instanceof HoverHarnessItem)) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(0.1F, 0.1F, 0.1F);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        if (state.isCrouching) {
            poseStack.mulPose(Axis.XP.rotationDegrees(28.64789F));
        }
        poseStack.translate(0.0F, 0.33F, -3.7F);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityCutout(TEXTURE), (pose, buffer) -> ObjModel.get(MODEL).renderAll(pose, buffer, 0xFFFFFFFF, lightCoords));
        poseStack.popPose();
        if (!Hover.isHovering(state.chestEquipment)) {
            return;
        }
        TextureAtlasSprite ring = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getSprite(RING);
        poseStack.pushPose();
        if (state.isCrouching) {
            poseStack.mulPose(Axis.XP.rotationDegrees(28.64789F));
            poseStack.translate(0.0F, 0.075F, -0.05F);
        }
        poseStack.translate(0.0F, 0.2F, 0.55F);
        poseStack.pushPose();
        poseStack.scale(2.5F, 2.5F, 2.5F);
        ItemGeometry.blended(collector.order(1), poseStack, ring.atlasLocation(), 1, sink -> quad(sink, ring, 0xFFFFFFFF));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.03F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        ItemGeometry.blended(collector.order(1), poseStack, ring.atlasLocation(), 1, sink -> quad(sink, ring, 0xFFFF80FF));
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void quad(QuadSink sink, TextureAtlasSprite sprite, int argb) {
        sink.vertex(-0.5F, 0.5F, 0.0F, sprite.getU1(), sprite.getV1(), argb, RING_LIGHT);
        sink.vertex(0.5F, 0.5F, 0.0F, sprite.getU0(), sprite.getV1(), argb, RING_LIGHT);
        sink.vertex(0.5F, -0.5F, 0.0F, sprite.getU0(), sprite.getV0(), argb, RING_LIGHT);
        sink.vertex(-0.5F, -0.5F, 0.0F, sprite.getU1(), sprite.getV0(), argb, RING_LIGHT);
    }
}
