package thaumcraft.client.render.device;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.registry.ModBlocks;

public final class WardedRendering {
    private static final Identifier CHEST_TEXTURE = Thaumcraft.id("textures/model/chesthungry.png");
    private static final Identifier BANNER_BLANK = Thaumcraft.id("textures/model/banner_blank.png");
    private static final Identifier BANNER_CULTIST = Thaumcraft.id("textures/model/banner_cultist.png");
    private static final Identifier BRAIN_TEXTURE = Thaumcraft.id("textures/model/brain2.png");
    private static final Identifier BRINE_TEXTURE = Thaumcraft.id("textures/model/jarbrine.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final float DEGREES = 180.0F / (float) Math.PI;

    private WardedRendering() {
    }

    public static int viewerTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0 : minecraft.player.tickCount;
    }

    private static void part(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        Identifier texture,
        boolean translucent,
        int order,
        float pointX,
        float pointY,
        float pointZ,
        float angleX,
        Consumer<QuadSink> geometry
    ) {
        poseStack.pushPose();
        poseStack.translate(pointX / 16.0F, pointY / 16.0F, pointZ / 16.0F);
        if (angleX != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(angleX * DEGREES));
        }
        ItemGeometry.solid(collector.order(order), poseStack, texture, translucent, geometry);
        poseStack.popPose();
    }

    private static float chestRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    public static void hungryChest(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction facing, float lid) {
        float eased = 1.0F - lid;
        eased = 1.0F - eased * eased * eased;
        float angle = -(eased * (float) Math.PI / 2.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 1.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(chestRotation(facing)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        part(collector, poseStack, CHEST_TEXTURE, false, 0, 1.0F, 7.0F, 15.0F, angle, sink ->
            ModelBoxGeometry.box(sink, 0.0F, -5.0F, -14.0F, 14, 5, 14, 0, 0, 64.0F, 64.0F, false, WHITE, light));
        part(collector, poseStack, CHEST_TEXTURE, false, 0, 8.0F, 7.0F, 15.0F, angle, sink ->
            ModelBoxGeometry.box(sink, -1.0F, -2.0F, -15.0F, 2, 4, 1, 0, 0, 64.0F, 64.0F, false, WHITE, light));
        part(collector, poseStack, CHEST_TEXTURE, false, 0, 1.0F, 6.0F, 1.0F, 0.0F, sink ->
            ModelBoxGeometry.box(sink, 0.0F, 0.0F, 0.0F, 14, 10, 14, 0, 19, 64.0F, 64.0F, false, WHITE, light));
        poseStack.popPose();
    }

    public static void brainJar(SubmitNodeCollector collector, PoseStack poseStack, int light, float rotation, float ticks) {
        float bob = Mth.sin(ticks / 14.0F) * 0.03F + 0.03F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.01F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.8F + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * 180.0F / 3.141593F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        part(collector, poseStack, BRAIN_TEXTURE, false, 0, -6.0F, 8.0F, -8.0F, 0.0F, sink ->
            ModelBoxGeometry.box(sink, 0.0F, 0.0F, 0.0F, 12, 10, 16, 0, 0, 128.0F, 64.0F, true, WHITE, light));
        part(collector, poseStack, BRAIN_TEXTURE, false, 0, -4.0F, 18.0F, 0.0F, 0.0F, sink ->
            ModelBoxGeometry.box(sink, 0.0F, 0.0F, 0.0F, 8, 3, 7, 64, 0, 128.0F, 64.0F, true, WHITE, light));
        part(collector, poseStack, BRAIN_TEXTURE, false, 0, -1.0F, 18.0F, -2.0F, 0.4089647F, sink ->
            ModelBoxGeometry.box(sink, 0.0F, 0.0F, 0.0F, 2, 6, 2, 0, 32, 128.0F, 64.0F, true, WHITE, light));
        poseStack.popPose();
        part(collector, poseStack, BRINE_TEXTURE, true, 1, 0.0F, 0.0F, 0.0F, 0.0F, sink ->
            ModelBoxGeometry.box(sink, -4.0F, -11.0F, -4.0F, 8, 10, 8, 0, 0, 64.0F, 32.0F, true, WHITE, light));
        poseStack.popPose();
    }

    public static float bannerSwing(int x, int y, int z, long gameTime, float partialTicks) {
        float time = x * 7 + y * 9 + z * 13 + (float) gameTime + partialTicks;
        return (0.005F + 0.005F * Mth.cos(time * (float) Math.PI * 0.02F)) * (float) Math.PI;
    }

    public static void banner(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        int light,
        int color,
        @Nullable Aspect aspect,
        boolean inWorld,
        int facing,
        boolean onWall,
        float swing
    ) {
        Identifier texture = aspect == null && color == -1 ? BANNER_CULTIST : BANNER_BLANK;
        int tint = color < 0 || color >= ModBlocks.CANDLE_TINTS.length ? WHITE : ARGB.opaque(ModBlocks.CANDLE_TINTS[color]);
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        if (inWorld) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(facing * 360 / 16.0F));
        }
        if (!onWall) {
            part(collector, poseStack, texture, false, 0, -1.0F, -7.0F, -2.0F, 0.0F, sink ->
                ModelBoxGeometry.box(sink, 0.0F, 0.0F, -1.0F, 2, 31, 2, 62, 0, 128.0F, 64.0F, true, WHITE, light));
        } else {
            poseStack.translate(0.0F, 0.0F, -0.4125F);
        }
        part(collector, poseStack, texture, false, 0, 0.0F, 0.0F, 0.0F, 0.0F, sink ->
            ModelBoxGeometry.box(sink, -7.0F, -7.0F, -1.0F, 14, 2, 2, 30, 0, 128.0F, 64.0F, true, WHITE, light));
        part(collector, poseStack, texture, false, 0, 0.0F, 0.0F, 0.0F, 0.0F, sink -> {
            ModelBoxGeometry.box(sink, -5.0F, -7.5F, -1.5F, 2, 3, 3, 0, 29, 128.0F, 64.0F, true, tint, light);
            ModelBoxGeometry.box(sink, 3.0F, -7.5F, -1.5F, 2, 3, 3, 0, 29, 128.0F, 64.0F, true, tint, light);
        });
        part(collector, poseStack, texture, false, 0, 0.0F, -5.0F, 0.0F, swing, sink ->
            ModelBoxGeometry.box(sink, -7.0F, 0.0F, -0.5F, 14, 28, 1, 0, 0, 128.0F, 64.0F, true, tint, light));
        if (aspect != null) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.05001F);
            poseStack.scale(0.0375F, 0.0375F, 0.0375F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-swing * DEGREES * 2.0F));
            int argb = ARGB.color(Mth.floor(0.75F * 255.0F), aspect.getColor());
            ItemGeometry.solid(collector.order(1), poseStack, aspect.getImage(), true, sink -> tag(sink, -8.0F, 4.0F, argb, light));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void tag(QuadSink sink, float x, float y, int argb, int light) {
        sink.vertex(x, y + 16.0F, 0.0F, 0.0F, 1.0F, argb, light);
        sink.vertex(x + 16.0F, y + 16.0F, 0.0F, 1.0F, 1.0F, argb, light);
        sink.vertex(x + 16.0F, y, 0.0F, 1.0F, 0.0F, argb, light);
        sink.vertex(x, y, 0.0F, 0.0F, 0.0F, argb, light);
        sink.vertex(x, y, 0.0F, 0.0F, 0.0F, argb, light);
        sink.vertex(x + 16.0F, y, 0.0F, 1.0F, 0.0F, argb, light);
        sink.vertex(x + 16.0F, y + 16.0F, 0.0F, 1.0F, 1.0F, argb, light);
        sink.vertex(x, y + 16.0F, 0.0F, 0.0F, 1.0F, argb, light);
    }
}
