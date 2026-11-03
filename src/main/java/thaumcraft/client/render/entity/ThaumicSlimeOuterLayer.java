package thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.entity.monster.ThaumicSlime;

final class ThaumicSlimeOuterLayer extends RenderLayer<ThaumicSlime, SlimeModel<ThaumicSlime>> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/model/tslime.png");
    private final SlimeModel<ThaumicSlime> model;

    ThaumicSlimeOuterLayer(RenderLayerParent<ThaumicSlime, SlimeModel<ThaumicSlime>> parent, EntityModelSet modelSet) {
        super(parent);
        model = new SlimeModel<>(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ThaumicSlime entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isInvisible() || Minecraft.getInstance().shouldEntityAppearGlowing(entity)) {
            model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            model.renderToBuffer(poseStack, buffer.getBuffer(TcRenderTypes.entityTranslucentNoDepth(TEXTURE)), packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 0x66FFFFFF);
        }
    }
}
