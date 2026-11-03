package thaumcraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;

public abstract class AlphaMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    protected AlphaMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    protected abstract float getEntityAlpha(T entity);

    @Override
    protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (!bodyVisible || translucent || glowing) {
            return super.getRenderType(entity, bodyVisible, translucent, glowing);
        }
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (NeoForge.EVENT_BUS.post(new RenderLivingEvent.Pre<T, M>(entity, this, partialTick, poseStack, buffer, packedLight)).isCanceled()) {
            return;
        }
        poseStack.pushPose();
        model.attackTime = getAttackAnim(entity, partialTick);
        boolean shouldSit = entity.isPassenger() && entity.getVehicle() != null && entity.getVehicle().shouldRiderSit();
        model.riding = shouldSit;
        model.young = entity.isBaby();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;
        if (shouldSit && entity.getVehicle() instanceof net.minecraft.world.entity.LivingEntity vehicle) {
            bodyYaw = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            netHeadYaw = headYaw - bodyYaw;
            float wrapped = Mth.wrapDegrees(netHeadYaw);
            wrapped = Mth.clamp(wrapped, -85.0F, 85.0F);
            bodyYaw = headYaw - wrapped;
            if (wrapped * wrapped > 2500.0F) {
                bodyYaw += wrapped * 0.2F;
            }
            netHeadYaw = headYaw - bodyYaw;
        }
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
            pitch *= -1.0F;
            netHeadYaw *= -1.0F;
        }
        netHeadYaw = Mth.wrapDegrees(netHeadYaw);
        if (entity.hasPose(Pose.SLEEPING)) {
            var direction = entity.getBedOrientation();
            if (direction != null) {
                float offset = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                poseStack.translate(-direction.getStepX() * offset, 0.0F, -direction.getStepZ() * offset);
            }
        }
        float entityScale = entity.getScale();
        poseStack.scale(entityScale, entityScale, entityScale);
        float ageInTicks = getBob(entity, partialTick);
        setupRotations(entity, poseStack, ageInTicks, bodyYaw, partialTick, entityScale);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        scale(entity, poseStack, partialTick);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        float limbAmount = 0.0F;
        float limbSwing = 0.0F;
        if (!shouldSit && entity.isAlive()) {
            limbAmount = entity.walkAnimation.speed(partialTick);
            limbSwing = entity.walkAnimation.position(partialTick);
            if (entity.isBaby()) {
                limbSwing *= 3.0F;
            }
            limbAmount = Math.min(1.0F, limbAmount);
        }
        model.prepareMobModel(entity, limbSwing, limbAmount, partialTick);
        model.setupAnim(entity, limbSwing, limbAmount, ageInTicks, netHeadYaw, pitch);
        Minecraft minecraft = Minecraft.getInstance();
        boolean visible = isBodyVisible(entity);
        boolean translucent = !visible && !entity.isInvisibleTo(minecraft.player);
        boolean glowing = minecraft.shouldEntityAppearGlowing(entity);
        RenderType renderType = getRenderType(entity, visible, translucent, glowing);
        if (renderType != null) {
            VertexConsumer consumer = buffer.getBuffer(renderType);
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, getWhiteOverlayProgress(entity, partialTick));
            int alpha = Mth.clamp((int) (getEntityAlpha(entity) * 255.0F), 0, 255);
            model.renderToBuffer(poseStack, consumer, packedLight, overlay, alpha << 24 | 0xFFFFFF);
        }
        if (!entity.isSpectator()) {
            for (var layer : layers) {
                layer.render(poseStack, buffer, packedLight, entity, limbSwing, limbAmount, partialTick, ageInTicks, netHeadYaw, pitch);
            }
        }
        poseStack.popPose();
        RenderNameTagEvent nameTag = new RenderNameTagEvent(entity, entity.getDisplayName(), this, poseStack, buffer, packedLight, partialTick);
        NeoForge.EVENT_BUS.post(nameTag);
        if (nameTag.canRender().isTrue() || nameTag.canRender().isDefault() && shouldShowName(entity)) {
            renderNameTag(entity, nameTag.getContent(), poseStack, buffer, packedLight, partialTick);
        }
        NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post<T, M>(entity, this, partialTick, poseStack, buffer, packedLight));
    }
}
