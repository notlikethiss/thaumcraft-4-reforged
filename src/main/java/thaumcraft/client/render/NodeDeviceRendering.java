package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.TcRenderTypes.Kind;
import thaumcraft.client.render.model.ObjModel;

public final class NodeDeviceRendering {
    private static final Identifier DEVICE_MODEL = Thaumcraft.id("models/obj/node_stabilizer.obj");
    private static final Identifier RELAY_MODEL = Thaumcraft.id("models/obj/vis_relay.obj");
    private static final Identifier STABILIZER = Thaumcraft.id("textures/model/node_stabilizer.png");
    private static final Identifier STABILIZER_OVER = Thaumcraft.id("textures/model/node_stabilizer_over.png");
    private static final Identifier CONVERTER = Thaumcraft.id("textures/model/node_converter.png");
    private static final Identifier CONVERTER_OVER = Thaumcraft.id("textures/model/node_converter_over.png");
    private static final Identifier RELAY = Thaumcraft.id("textures/model/vis_relay.png");
    private static final int WHITE = 0xFFFFFFFF;

    private record Part(Kind kind, Identifier texture) {
    }

    private NodeDeviceRendering() {
    }

    public static int color(float red, float green, float blue) {
        return 0xFF000000
            | Math.clamp(Math.round(red * 255.0F), 0, 255) << 16
            | Math.clamp(Math.round(green * 255.0F), 0, 255) << 8
            | Math.clamp(Math.round(blue * 255.0F), 0, 255);
    }

    private static void part(SubmitNodeCollector collector, PoseStack poseStack, Part texture, ObjModel model, String name, int argb, int light) {
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(texture.kind(), texture.texture()), (pose, buffer) -> model.renderPart(name, pose, buffer, argb, light));
    }

    public static void stabilizer(SubmitNodeCollector collector, PoseStack poseStack, int light, int lock, int count, int viewerTicks, boolean animated) {
        ObjModel model = ObjModel.get(DEVICE_MODEL);
        Part base = new Part(Kind.ENTITY_CUTOUT, STABILIZER);
        Part over = new Part(Kind.ENTITY_CUTOUT, STABILIZER_OVER);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        part(collector, poseStack, base, model, "lock", WHITE, light);
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 * index));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.translate(0.0F, 0.0F, count / 100.0F);
            part(collector, poseStack, base, model, "piston", WHITE, light);
            int tint = lock == 2 ? color(1.0F, 0.2F, 0.2F) : WHITE;
            int overLight = light;
            if (animated) {
                float scale = Mth.sin((viewerTicks + index * 5) / 3.0F) * 0.1F + 0.9F;
                overLight = WorldRendering.emissiveLight(50 + (int) (170.0F * (count / 37.0F * scale)));
            }
            part(collector, poseStack, over, model, "piston", tint, overLight);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void transducer(SubmitNodeCollector collector, PoseStack poseStack, int light, int status, int count, int viewerTicks, boolean animated) {
        ObjModel model = ObjModel.get(DEVICE_MODEL);
        Part base = new Part(Kind.ENTITY_CUTOUT, CONVERTER);
        Part over = new Part(Kind.ENTITY_CUTOUT, CONVERTER_OVER);
        int tint = status == 2 ? color(1.0F, 0.0F, 0.3F) : status == 1 ? color(1.0F, 0.6F, 0.1F) : color(0.5F, 1.0F, 0.5F);
        float travel = Math.min(50, count) / 137.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        part(collector, poseStack, base, model, "lock", WHITE, light);
        int lockLight = light;
        if (animated) {
            float scale = Mth.sin(viewerTicks / 3.0F) * 0.1F + 0.9F;
            lockLight = WorldRendering.emissiveLight(50 + (int) (170.0F * (travel * 2.5F * scale)));
        }
        part(collector, poseStack, over, model, "lock", tint, lockLight);
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 * index));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.translate(0.0F, 0.0F, travel);
            part(collector, poseStack, base, model, "piston", WHITE, light);
            int overLight = light;
            if (animated) {
                float scale = Mth.sin((viewerTicks + index * 5) / 3.0F) * 0.1F + 0.9F;
                overLight = WorldRendering.emissiveLight(50 + (int) (170.0F * (travel * 2.5F * scale)));
            }
            part(collector, poseStack, over, model, "piston", tint, overLight);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void charger(SubmitNodeCollector collector, PoseStack poseStack, int light, int crystalColor, boolean connected, float ticks) {
        ObjModel model = ObjModel.get(RELAY_MODEL);
        Part solid = new Part(Kind.ENTITY_CUTOUT, RELAY);
        Part glass = new Part(Kind.ENTITY_TRANSLUCENT, RELAY);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        part(collector, poseStack, solid, model, "RingFloat", WHITE, light);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.5F);
        for (int index = 0; index < 4; index++) {
            part(collector, poseStack, solid, model, "Support", WHITE, light);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
        poseStack.popPose();
        int tint = WHITE;
        if (crystalColor >= 0) {
            tint = color((crystalColor >> 16 & 255) / 200.0F, (crystalColor >> 8 & 255) / 200.0F, (crystalColor & 255) / 200.0F);
        }
        float scale = Mth.sin(ticks / 2.0F) * 0.05F + 0.95F;
        int crystalLight = WorldRendering.emissiveLight((connected ? 50 : 0) + (int) (150.0F * scale));
        part(collector, poseStack, glass, model, "Crystal", tint, crystalLight);
        poseStack.popPose();
    }

    public static void relay(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction orientation, int crystalColor, boolean connected, float ticks) {
        ObjModel model = ObjModel.get(RELAY_MODEL);
        Part solid = new Part(Kind.ENTITY_CUTOUT, RELAY);
        Part glass = new Part(Kind.ENTITY_TRANSLUCENT, RELAY);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (orientation) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            default -> {
            }
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.pushPose();
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0F, 0.0F, -0.16F);
        part(collector, poseStack, solid, model, "RingBase", WHITE, light);
        poseStack.popPose();
        part(collector, poseStack, solid, model, "RingFloat", WHITE, light);
        int tint = WHITE;
        if (crystalColor >= 0) {
            tint = color((crystalColor >> 16 & 255) / 200.0F, (crystalColor >> 8 & 255) / 200.0F, (crystalColor & 255) / 200.0F);
        }
        float scale = Mth.sin(ticks / 2.0F) * 0.05F + 0.95F;
        int crystalLight = WorldRendering.emissiveLight((connected ? 50 : 0) + (int) (150.0F * scale));
        part(collector, poseStack, glass, model, "Crystal", tint, crystalLight);
        poseStack.popPose();
    }
}
