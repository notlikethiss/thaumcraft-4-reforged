package thaumcraft.client.render.entity.eldritch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.entity.monster.eldritch.EldritchCrab;

public final class EldritchCrabRenderer extends MobRenderer<EldritchCrab, EldritchCrabRenderState, EldritchCrabModel> {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/model/crab.png");
    private static final Identifier OVERLAY = Thaumcraft.id("textures/model/craboverlay.png");

    public EldritchCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new EldritchCrabModel(context.bakeLayer(EldritchCrabModel.LAYER)), 1.0F);
        this.addLayer(new HelmLayer(this));
    }

    @Override
    public EldritchCrabRenderState createRenderState() {
        return new EldritchCrabRenderState();
    }

    @Override
    public void extractRenderState(EldritchCrab entity, EldritchCrabRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.hasHelm = entity.hasHelm();
    }

    @Override
    public Identifier getTextureLocation(EldritchCrabRenderState state) {
        return TEXTURE;
    }

    private static final class HelmLayer extends RenderLayer<EldritchCrabRenderState, EldritchCrabModel> {
        private HelmLayer(EldritchCrabRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, EldritchCrabRenderState state, float yRot, float xRot) {
            if (!state.hasHelm || state.isInvisible && !state.appearsGlowing()) {
                return;
            }
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            collector.order(1).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                state.appearsGlowing() && state.isInvisible ? TcRenderTypes.outline(OVERLAY) : TcRenderTypes.entityTranslucent(OVERLAY),
                LightCoordsUtil.pack(200, 0),
                overlay,
                state.outlineColor,
                null
            );
        }
    }
}
