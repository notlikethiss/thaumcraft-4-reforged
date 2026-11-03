package thaumcraft.client.render.entity.taint;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.AlphaMobRenderer;
import thaumcraft.client.platform.ClientLayers;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.entity.monster.taint.TaintSpore;
import thaumcraft.entity.monster.taint.TaintedCreeper;

public class TaintMobRenderer<T extends Mob> extends AlphaMobRenderer<T, TaintEntityModel<T>> {
    private final TaintEntityModel.Kind kind;

    public TaintMobRenderer(EntityRendererProvider.Context context, TaintEntityModel.Kind kind, float shadowRadius) {
        super(context, new TaintEntityModel<>(context.bakeLayer(kind.layer()), kind), shadowRadius);
        this.kind = kind;
        if (kind == TaintEntityModel.Kind.SPIDER) {
            addLayer(ClientLayers.fullbrightEyes(this, Thaumcraft.id("textures/model/taint_spider_eyes.png")));
        }
        if (kind == TaintEntityModel.Kind.SHEEP) {
            addLayer(new TaintSheepFurLayer<>(this, new TaintSheepFurModel(context.bakeLayer(TaintSheepFurModel.LAYER))));
        }
        if (kind == TaintEntityModel.Kind.SWARMER) {
            addLayer(new TaintSwarmerSporeLayer<>(this, new TaintSwarmerSporeModel(context.bakeLayer(TaintSwarmerSporeModel.LAYER))));
        }
    }

    public static <T extends Mob> EntityRendererProvider<T> provider(TaintEntityModel.Kind kind, float shadowRadius) {
        return context -> new TaintMobRenderer<>(context, kind, shadowRadius);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return kind.texture();
    }

    @Override
    protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return kind == TaintEntityModel.Kind.SPIDER
            ? TcRenderTypes.entityTranslucentNoDepth(getTextureLocation(entity))
            : super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        float ageInTicks = entity.tickCount + partialTick;
        if (kind == TaintEntityModel.Kind.TENTACLE || kind == TaintEntityModel.Kind.TENTACLE_TINY) {
            float height = entity.getBbHeight();
            float scale = height / 3.0F;
            float riseTime = height * 10.0F;
            float rise = ageInTicks < riseTime ? (riseTime - ageInTicks) / riseTime * height : 0.0F;
            poseStack.translate(0.0F, (height == 3.0F ? 0.6F : 1.2F) + rise, 0.0F);
            poseStack.scale(scale, scale, scale);
        } else if (kind == TaintEntityModel.Kind.SPORE && entity instanceof TaintSpore spore) {
            float size = spore.displaySize;
            float pulse = 0.025F * Mth.sin(ageInTicks * 0.075F);
            poseStack.scale(-0.12F * size - pulse, -0.12F * size + pulse, -0.12F * size - pulse);
        } else if (kind == TaintEntityModel.Kind.CREEPER && entity instanceof TaintedCreeper creeper) {
            float swelling = creeper.getSwelling(partialTick);
            float pulse = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
            float charge = Mth.clamp(swelling, 0.0F, 1.0F);
            charge *= charge;
            charge *= charge;
            float scale = (1.0F + charge * 0.4F) * pulse;
            float ageScale = (1.0F + charge * 0.1F) / pulse;
            poseStack.scale(scale, ageScale, scale);
        } else if (kind == TaintEntityModel.Kind.SPIDER) {
            poseStack.scale(0.4F, 0.5F, 0.4F);
        } else if (kind == TaintEntityModel.Kind.VILLAGER) {
            poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        }
    }

    @Override
    protected float getFlipDegrees(T entity) {
        if (kind == TaintEntityModel.Kind.SPORE || kind == TaintEntityModel.Kind.SWARMER) {
            return 0.0F;
        }
        return kind == TaintEntityModel.Kind.SPIDER ? 180.0F : super.getFlipDegrees(entity);
    }

    @Override
    protected float getEntityAlpha(T entity) {
        if (kind == TaintEntityModel.Kind.CREEPER && entity instanceof TaintedCreeper creeper) {
            float swelling = creeper.getSwelling(0.0F);
            if ((int) (swelling * 10.0F) % 2 != 0) {
                return Mth.clamp(Mth.clamp(swelling, 0.0F, 1.0F) * 0.2F, 0.0F, 1.0F);
            }
        }
        return 1.0F;
    }
}
