package thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.model.ObjModel;
import thaumcraft.entity.projectile.FrostShard;

public class FrostShardRenderer extends EntityRenderer<FrostShard> {
    private static final ResourceLocation MODEL = Thaumcraft.id("models/obj/orb.obj");
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/block/frostshard.png");

    public FrostShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(FrostShard entity) {
        return thaumcraft.client.fx.ParticleFx.PARTICLES;
    }

    @Override
    public void render(FrostShard entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int lightCoords) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(net.minecraft.util.Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(net.minecraft.util.Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        Random random = new Random(entity.getId());
        float base = entity.getDamage() * 0.1F;
        poseStack.scale(base + random.nextFloat() * 0.1F, base + random.nextFloat() * 0.1F, base + random.nextFloat() * 0.1F);
        int light = lightCoords;
        ObjModel.get(MODEL).renderAll(poseStack.last(), buffers.getBuffer(TcRenderTypes.entityTranslucent(TEXTURE)), 0xFFFFFFFF, light);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, lightCoords);
    }
}
