package thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;

final class ThaumicSlimeOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
    private static final net.minecraft.resources.Identifier TEXTURE = Thaumcraft.id("textures/model/tslime.png");
    private final SlimeModel model;

    ThaumicSlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new SlimeModel(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SlimeRenderState state, float yRot, float xRot) {
        boolean glowingWithInvisibility = state.appearsGlowing() && state.isInvisible;
        if (!state.isInvisible || glowingWithInvisibility) {
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            collector.order(1).submitModel(
                this.model,
                state,
                poseStack,
                glowingWithInvisibility ? TcRenderTypes.outline(TEXTURE) : TcRenderTypes.entityTranslucentNoDepth(TEXTURE),
                light,
                overlay,
                state.outlineColor,
                null
            );
        }
    }
}
