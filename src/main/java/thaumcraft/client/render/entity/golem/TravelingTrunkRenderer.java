package thaumcraft.client.render.entity.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import thaumcraft.client.platform.ImmediateGeometry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import thaumcraft.client.platform.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.TravelingTrunk;
import thaumcraft.item.golem.GolemItems;

public final class TravelingTrunkRenderer extends MobRenderer<TravelingTrunk, TravelingTrunkModel> {
    private static final ResourceLocation CALM = Thaumcraft.id("textures/model/trunk.png");
    private static final ResourceLocation ANGRY = Thaumcraft.id("textures/model/trunkangry.png");
    private final ItemModelResolver itemResolver;

    public TravelingTrunkRenderer(EntityRendererProvider.Context context) {
        super(context, new TravelingTrunkModel(context.bakeLayer(GolemModelLayers.TRUNK)), 0.35F);
        itemResolver = new ItemModelResolver();
        addLayer(new UpgradeLayer(this));
    }

    @Override
    public void render(TravelingTrunk entity, float yaw, float partialTicks, PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        var state = createRenderState();
        extractRenderState(entity, state, partialTicks);
        getModel().state = state;
        super.render(entity, yaw, partialTicks, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TravelingTrunk entity) {
        return getTextureLocation(getModel().state);
    }

    public TravelingTrunkRenderState createRenderState() {
        return new TravelingTrunkRenderState();
    }

    public void extractRenderState(TravelingTrunk entity, TravelingTrunkRenderState state, float partialTicks) {
        thaumcraft.client.platform.LivingEntityRenderState.extract(entity, state, partialTicks);
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

    public ResourceLocation getTextureLocation(TravelingTrunkRenderState state) {
        return state.texture;
    }

    @Override
    protected void scale(TravelingTrunk entity, PoseStack poseStack, float partialTicks) {
        var state = getModel().state;
        float size = state.upgrade == 1 ? 2.0F / 1.33F : 2.0F / 1.5F;
        float horizontal = size / 1.4F;
        float vertical = size * 0.7F;
        poseStack.scale(horizontal, vertical, horizontal);
        poseStack.translate(-0.5F, 0.5F, -0.5F);
    }

    private static final class UpgradeLayer extends RenderLayer<TravelingTrunk, TravelingTrunkModel> {
        UpgradeLayer(TravelingTrunkRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers, int light, TravelingTrunk entity, float walk, float speed, float partialTicks, float age, float yRot, float xRot) {
            var state = getParentModel().state;
            var collector = new ImmediateGeometry(buffers);
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
