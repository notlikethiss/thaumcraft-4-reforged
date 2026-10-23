package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.CubeGeometry;
import thaumcraft.client.geom.InfusionHaloGeometry;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;

public final class RunicMatrixRendering {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/model/infuser.png");
    private static final Identifier PARTICLES = Thaumcraft.id("textures/misc/particles.png");
    private static final float WHITE_U = 118.5F / 256.0F;
    private static final float WHITE_V = 40.5F / 256.0F;
    private static final int WHITE = 0xFFFFFFFF;

    private RunicMatrixRendering() {
    }

    private static void cube(PoseStack poseStack, int a, int b, int c, float b1, float b2, float b3) {
        poseStack.translate(b1 + (a == 0 ? -1 : 1) * 0.25F, b2 + (b == 0 ? -1 : 1) * 0.25F, b3 + (c == 0 ? -1 : 1) * 0.25F);
        if (a > 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        if (b > 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        if (c > 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.45F, 0.45F, 0.45F);
    }

    public static void submit(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        int light,
        boolean animated,
        boolean active,
        boolean crafting,
        float startUp,
        int instabilityValue,
        int craftCount,
        float ticks
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (crafting) {
            collector.order(1).submitCustomGeometry(
                poseStack,
                TcRenderTypes.additive(PARTICLES),
                (pose, buffer) -> InfusionHaloGeometry.render(new VertexQuadSink(pose, buffer), craftCount, true, WHITE_U, WHITE_V, LightCoordsUtil.FULL_BRIGHT)
            );
        }
        if (animated) {
            poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0F * startUp));
            poseStack.mulPose(Axis.XP.rotationDegrees(35.0F * startUp));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F * startUp));
        }
        float instability = Math.min(6.0F, 1.0F + instabilityValue * 0.66F * (Math.min(craftCount, 50) / 50.0F));
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                for (int c = 0; c < 2; c++) {
                    float b1 = 0.0F;
                    float b2 = 0.0F;
                    float b3 = 0.0F;
                    if (active) {
                        b1 = Mth.sin((ticks + a * 10) / (15.0F - instability / 2.0F)) * 0.01F * startUp * instability;
                        b2 = Mth.sin((ticks + b * 10) / (14.0F - instability / 2.0F)) * 0.01F * startUp * instability;
                        b3 = Mth.sin((ticks + c * 10) / (13.0F - instability / 2.0F)) * 0.01F * startUp * instability;
                    }
                    poseStack.pushPose();
                    cube(poseStack, a, b, c, b1, b2, b3);
                    collector.submitCustomGeometry(
                        poseStack,
                        TcRenderTypes.entityCutout(TEXTURE),
                        (pose, buffer) -> CubeGeometry.render(new EntityQuadSink(pose, buffer), 0, WHITE, light)
                    );
                    poseStack.popPose();
                }
            }
        }
        if (active) {
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    for (int c = 0; c < 2; c++) {
                        float b1 = Mth.sin((ticks + a * 10) / (15.0F - instability / 2.0F)) * 0.01F * startUp * instability;
                        float b2 = Mth.sin((ticks + b * 10) / (14.0F - instability / 2.0F)) * 0.01F * startUp * instability;
                        float b3 = Mth.sin((ticks + c * 10) / (13.0F - instability / 2.0F)) * 0.01F * startUp * instability;
                        float alpha = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4.0F) * 0.1F + 0.2F) * startUp;
                        int color = (Math.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | (204 << 16) | (26 << 8) | 255;
                        poseStack.pushPose();
                        cube(poseStack, a, b, c, b1, b2, b3);
                        collector.order(1).submitCustomGeometry(
                            poseStack,
                            TcRenderTypes.additive(TEXTURE),
                            (pose, buffer) -> CubeGeometry.render(new VertexQuadSink(pose, buffer), 32, color, LightCoordsUtil.FULL_BRIGHT)
                        );
                        poseStack.popPose();
                    }
                }
            }
        }
        poseStack.popPose();
    }
}
