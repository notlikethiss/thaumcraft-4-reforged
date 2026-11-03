package thaumcraft.client.render.entity.eldritch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.monster.eldritch.EldritchCrab;

public final class EldritchCrabRenderer extends MobRenderer<EldritchCrab, EldritchCrabModel> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/model/crab.png");
    private static final ResourceLocation OVERLAY = Thaumcraft.id("textures/model/craboverlay.png");

    public EldritchCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new EldritchCrabModel(context.bakeLayer(EldritchCrabModel.LAYER)), 1.0F);
        addLayer(new HelmOverlay(this));
    }

    @Override
    public ResourceLocation getTextureLocation(EldritchCrab entity) {
        return TEXTURE;
    }

    private static final class HelmOverlay extends RenderLayer<EldritchCrab, EldritchCrabModel> {
        private HelmOverlay(RenderLayerParent<EldritchCrab, EldritchCrabModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, EldritchCrab entity,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean glowing = Minecraft.getInstance().shouldEntityAppearGlowing(entity);
            if (!entity.hasHelm() || entity.isInvisible() && !glowing) {
                return;
            }
            getParentModel().setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
            int light = glowing && entity.isInvisible() ? 0x00F000F0 : LightTexture.pack(12, 0);
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(OVERLAY)), light, overlay, 0xFFFFFFFF);
        }
    }
}
