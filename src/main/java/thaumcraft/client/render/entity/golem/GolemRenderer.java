package thaumcraft.client.render.entity.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.geom.GolemItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.item.golem.GolemItems;

public final class GolemRenderer extends MobRenderer<GolemBase, GolemRenderState, GolemModel> {
    private static final Identifier DAMAGE = Thaumcraft.id("textures/model/golem_damage.png");
    private static final Identifier DECORATION = Thaumcraft.id("textures/model/golem_decoration.png");
    private final ItemModelResolver itemResolver;

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModelLayers.GOLEM)), 0.25F);
        itemResolver = context.getItemModelResolver();
        this.addLayer(new AccessoriesLayer(this, new GolemAccessoriesModel(context.bakeLayer(GolemModelLayers.ACCESSORIES))));
        this.addLayer(new DamageLayer(this, new GolemModel(context.bakeLayer(GolemModelLayers.GOLEM))));
        this.addLayer(new CarriedLayer(this));
        this.addLayer(new CoreLayer(this));
    }

    @Override
    public GolemRenderState createRenderState() {
        return new GolemRenderState();
    }

    @Override
    public void extractRenderState(GolemBase entity, GolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = Thaumcraft.id("textures/model/golem_" + entity.getGolemType().name().toLowerCase(java.util.Locale.ROOT) + ".png");
        state.core = entity.getCore();
        state.decoration = entity.getGolemDecoration();
        state.advanced = entity.isAdvanced();
        state.healthPercent = entity.getHealth() / entity.getMaxHealth();
        state.bootup = entity.bootup;
        state.inactive = entity.inactive;
        state.actionActive = entity.getActionTimer() > 0;
        state.actionTimer = entity.getActionTimer() - partialTicks;
        state.leftArmActive = entity.leftArm > 0;
        state.leftArm = entity.leftArm - partialTicks;
        state.rightArmActive = entity.rightArm > 0;
        state.rightArm = entity.rightArm - partialTicks;
        state.healing = entity.healing;
        state.carryLimit = entity.getCarryLimit();
        state.fishing = state.core == 11;
        ItemStack carried = entity.getCarriedForDisplay();
        state.carrying = !carried.isEmpty() && entity.deathTime == 0 && !state.fishing;
        state.carriedJar = entity.getCore() == 6 || carried.getItem() instanceof thaumcraft.essentia.FilledJarItem;
        state.carriedBlock = false;
        state.carriedScale = state.carriedJar && entity.getCore() == 6
            ? 0.5F + Math.min(64, entity.getCarryLimit()) / 128.0F
            : 1.0F;
        if (state.carrying) {
            itemResolver.updateForLiving(state.carried, carried, ItemDisplayContext.NONE, entity);
            state.carriedBlock = carried.getItem() instanceof BlockItem && !state.carriedJar && state.carried.usesBlockLight();
        } else {
            state.carried.clear();
        }
        if (state.fishing) {
            itemResolver.updateForLiving(state.fishingRod, new ItemStack(Items.FISHING_ROD), ItemDisplayContext.NONE, entity);
        } else {
            state.fishingRod.clear();
        }
        if (state.core >= 0) {
            itemResolver.updateForLiving(state.coreItem, GolemItems.core(state.core), ItemDisplayContext.NONE, entity);
        } else {
            state.coreItem.clear();
        }
        state.upgradeCount = Math.min(state.upgrades.length, entity.getGolemType().upgrades + (entity.isAdvanced() ? 1 : 0));
        for (int index = 0; index < state.upgradeCount; index++) {
            int upgrade = entity.getUpgrade(index);
            if (upgrade >= 0) {
                itemResolver.updateForLiving(state.upgrades[index], GolemItems.upgrade(upgrade), ItemDisplayContext.NONE, entity);
            } else {
                state.upgrades[index].clear();
            }
        }
        for (int index = state.upgradeCount; index < state.upgrades.length; index++) {
            state.upgrades[index].clear();
        }
    }

    @Override
    public Identifier getTextureLocation(GolemRenderState state) {
        return state.texture;
    }

    @Override
    protected int getModelTint(GolemRenderState state) {
        if (state.healing > 0) {
            float green = Math.min(1.0F, 0.9F + state.healing / 5.0F);
            float redBlue = Math.min(1.0F, 0.5F + state.healing / 10.0F);
            return net.minecraft.util.ARGB.colorFromFloat(1.0F, redBlue, green, redBlue);
        }
        return -1;
    }

    @Override
    protected void setupRotations(GolemRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.walkAnimationSpeed >= 0.01F) {
            float sway = GolemModel.triangle(state.walkAnimationPos + 6.0F, 13.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
    }

    private static class AccessoriesLayer extends RenderLayer<GolemRenderState, GolemModel> {
        private final GolemAccessoriesModel model;

        AccessoriesLayer(GolemRenderer renderer, GolemAccessoriesModel model) {
            super(renderer);
            this.model = model;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, GolemRenderState state, float yRot, float xRot) {
            if ((state.decoration.isEmpty() && !state.advanced) || state.isInvisible) {
                return;
            }
            model.setupAnim(state);
            collector.submitModel(model, state, poseStack, TcRenderTypes.entityTranslucent(DECORATION), light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), -1, null, state.outlineColor);
        }
    }

    private static class DamageLayer extends RenderLayer<GolemRenderState, GolemModel> {
        private final GolemModel model;

        DamageLayer(GolemRenderer renderer, GolemModel model) {
            super(renderer);
            this.model = model;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, GolemRenderState state, float yRot, float xRot) {
            if (state.healthPercent >= 1.0F || state.isInvisible) {
                return;
            }
            model.setupAnim(state);
            int alpha = (int) ((1.0F - Math.max(0.0F, state.healthPercent)) * 255.0F);
            collector.submitModel(model, state, poseStack, TcRenderTypes.entityTranslucent(DAMAGE), light, OverlayTexture.NO_OVERLAY, ARGB.color(alpha, 255, 255, 255), null, state.outlineColor);
        }
    }

    private static class CarriedLayer extends RenderLayer<GolemRenderState, GolemModel> {
        CarriedLayer(GolemRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, GolemRenderState state, float yRot, float xRot) {
            if (state.fishing) {
                poseStack.pushPose();
                poseStack.mulPose(GolemItemTransforms.fishingRod(GolemModel.armAngle(state, false, 0.0F)));
                state.fishingRod.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
                return;
            }
            if (!state.carrying || state.carried.isEmpty()) {
                return;
            }
            poseStack.pushPose();
            poseStack.mulPose(state.carriedJar ? GolemItemTransforms.carriedJar(state.carriedScale)
                : state.carriedBlock ? GolemItemTransforms.carriedBlock() : GolemItemTransforms.carriedItem());
            state.carried.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }

    private static class CoreLayer extends RenderLayer<GolemRenderState, GolemModel> {
        CoreLayer(GolemRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, GolemRenderState state, float yRot, float xRot) {
            if (state.core < 0 || state.isInvisible) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.8725F, -0.1675F - (state.decoration.contains("P") ? 0.03F : 0.0F));
            poseStack.scale(0.175F, 0.175F, 0.56F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            state.coreItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
            for (int index = 0; index < state.upgradeCount; index++) {
                if (state.upgrades[index].isEmpty()) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate((index - (state.upgradeCount - 1) / 2.0F) * 0.08F, 1.056F, -0.1F);
                poseStack.scale(0.1F, 0.1F, 0.1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                state.upgrades[index].submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
        }
    }
}
