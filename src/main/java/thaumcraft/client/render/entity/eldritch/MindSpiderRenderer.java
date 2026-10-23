package thaumcraft.client.render.entity.eldritch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.AbstractTranslucentMobRenderer;
import thaumcraft.client.platform.ClientLayers;
import thaumcraft.entity.monster.eldritch.MindSpider;

public class MindSpiderRenderer extends AbstractTranslucentMobRenderer<MindSpider, LivingEntityRenderState, SpiderModel> {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/model/taint_spider.png");

    public MindSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel(context.bakeLayer(ModelLayers.SPIDER)), 0.0F);
        this.addLayer(ClientLayers.fullbrightEyes(this, Thaumcraft.id("textures/model/taint_spider_eyes.png")));
    }

    @Override
    public boolean shouldRender(MindSpider entity, Frustum culler, double camX, double camY, double camZ) {
        if (!super.shouldRender(entity, culler, camX, camY, camZ)) {
            return false;
        }
        String viewer = entity.getViewer();
        return viewer.isEmpty() || Minecraft.getInstance().player != null
            && viewer.equals(Minecraft.getInstance().player.getUUID().toString());
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected int getModelTint(LivingEntityRenderState state) {
        return ARGB.colorFromFloat(Math.min(0.1F, state.ageInTicks / 100.0F), 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(0.3F, 0.3F, 0.3F);
    }
}
