package thaumcraft.client.render.entity.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.TravelingTrunk;
import thaumcraft.item.golem.GolemItems;

public final class TravelingTrunkRenderer extends MobRenderer<TravelingTrunk, TravelingTrunkRenderState, TravelingTrunkModel> {
    private static final Identifier CALM = Thaumcraft.id("textures/model/trunk.png");
    private static final Identifier ANGRY = Thaumcraft.id("textures/model/trunkangry.png");
    private final ItemModelResolver itemResolver;

    public TravelingTrunkRenderer(EntityRendererProvider.Context context) {
        super(context, new TravelingTrunkModel(context.bakeLayer(GolemModelLayers.TRUNK)), 0.35F);
        itemResolver = context.getItemModelResolver();
        addLayer(new UpgradeLayer(this));
    }

    @Override
    public TravelingTrunkRenderState createRenderState() {
        return new TravelingTrunkRenderState();
    }

    @Override
    public void extractRenderState(TravelingTrunk entity, TravelingTrunkRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.upgrade = entity.getUpgrade();
        state.anger = entity.getAnger();
        state.lidOpen = entity.getLidOpen(partialTicks);
        state.texture = state.anger > 0 ? ANGRY : CALM;
        if (state.upgrade >= 0 && state.upgrade < GolemItems.UPGRADES.length) {
            itemResolver.updateForLiving(state.upgradeItem, GolemItems.upgrade(state.upgrade), ItemDisplayContext.NONE, entity);
        } else {
            state.upgradeItem.clear();
        }
    }

    @Override
    public Identifier getTextureLocation(TravelingTrunkRenderState state) {
        return state.texture;
    }

    @Override
    protected void scale(TravelingTrunkRenderState state, PoseStack poseStack) {
        float size = state.upgrade == 1 ? 2.0F / 1.33F : 2.0F / 1.5F;
        float horizontal = size / 1.4F;
        float vertical = size * 0.7F;
        poseStack.scale(horizontal, vertical, horizontal);
        poseStack.translate(-0.5F, 0.5F, -0.5F);
    }

    private static final class UpgradeLayer extends RenderLayer<TravelingTrunkRenderState, TravelingTrunkModel> {
        UpgradeLayer(TravelingTrunkRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, TravelingTrunkRenderState state, float yRot, float xRot) {
            if (state.upgradeItem.isEmpty()) {
                return;
            }
            getParentModel().setupAnim(state);
            poseStack.pushPose();
            getParentModel().translateUpgrade(poseStack);
            state.upgradeItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
