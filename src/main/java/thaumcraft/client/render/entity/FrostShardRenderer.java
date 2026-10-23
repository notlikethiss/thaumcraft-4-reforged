package thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.render.model.ObjModel;
import thaumcraft.entity.projectile.FrostShard;

public class FrostShardRenderer extends EntityRenderer<FrostShard, SpriteRenderState> {
    private static final Identifier MODEL = Thaumcraft.id("models/obj/orb.obj");
    private static final Identifier TEXTURE = Thaumcraft.id("textures/block/frostshard.png");

    public FrostShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpriteRenderState createRenderState() {
        return new SpriteRenderState();
    }

    @Override
    public void extractRenderState(FrostShard entity, SpriteRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.seed = entity.getId();
        state.damage = entity.getDamage();
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = entity.getXRot(partialTicks);
    }

    @Override
    public void submit(SpriteRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        Random random = new Random(state.seed);
        float base = state.damage * 0.1F;
        poseStack.scale(base + random.nextFloat() * 0.1F, base + random.nextFloat() * 0.1F, base + random.nextFloat() * 0.1F);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityTranslucent(TEXTURE), (pose, buffer) -> ObjModel.get(MODEL).renderAll(pose, buffer, 0xFFFFFFFF, light));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
