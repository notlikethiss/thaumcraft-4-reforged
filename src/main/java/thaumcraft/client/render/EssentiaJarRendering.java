package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.ClientConfig;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.geom.BoxGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.ItemGeometry;

public final class EssentiaJarRendering {
    private static final Identifier LABEL = Thaumcraft.id("textures/model/label.png");
    private static final int WHITE = 0xFFFFFFFF;

    private EssentiaJarRendering() {
    }

    public static void submit(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        int light,
        int posX,
        @Nullable Aspect aspect,
        int amount,
        int maxAmount,
        @Nullable Aspect filter,
        Direction facing
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.01F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        if (amount > 0 && aspect != null) {
            liquid(collector, poseStack, light, aspect, amount / (float) maxAmount * 0.625F);
        }
        if (filter != null) {
            label(collector, poseStack, light, posX, filter, facing);
        }
        poseStack.popPose();
    }

    private static void liquid(SubmitNodeCollector collector, PoseStack poseStack, int light, Aspect aspect, float level) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        BoxGeometry.UvRect rect = EssentiaRendering.rect(EssentiaRendering.ANIMATED_GLOW);
        int color = ARGB.opaque(aspect.getColor());
        int liquidLight = EssentiaRendering.liquidLight(light);
        ItemGeometry.solid(
            collector.order(1),
            poseStack,
            TextureAtlas.LOCATION_BLOCKS,
            true,
            sink -> BoxGeometry.box(sink, 0.25F, 0.0625F, 0.25F, 0.75F, 0.0625F + level, 0.75F, rect, rect, rect, color, liquidLight)
        );
        poseStack.popPose();
    }

    private static void label(SubmitNodeCollector collector, PoseStack poseStack, int light, int posX, Aspect filter, Direction facing) {
        poseStack.pushPose();
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        float rotation = (filter.getTag().hashCode() + posX + facing.ordinal()) % 4 - 2;
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.4F, 0.315F);
        if (ClientConfig.crooked()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        ItemGeometry.solid(collector.order(2), poseStack, LABEL, true, sink -> quad(sink, -0.5F, -0.5F, 0.5F, 0.5F, WHITE, light));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.4F, 0.316F);
        if (ClientConfig.crooked()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        poseStack.scale(0.021F, 0.021F, 0.021F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        int tint = ARGB.opaque(filter.getColor());
        ItemGeometry.solid(collector.order(3), poseStack, filter.getImage(), true, sink -> quad(sink, -8.0F, -8.0F, 8.0F, 8.0F, tint, light));
        poseStack.popPose();
        poseStack.popPose();
    }

    static void quad(QuadSink sink, float minX, float minY, float maxX, float maxY, int argb, int light) {
        sink.vertex(minX, maxY, 0.0F, 0.0F, 1.0F, argb, light);
        sink.vertex(maxX, maxY, 0.0F, 1.0F, 1.0F, argb, light);
        sink.vertex(maxX, minY, 0.0F, 1.0F, 0.0F, argb, light);
        sink.vertex(minX, minY, 0.0F, 0.0F, 0.0F, argb, light);
        sink.vertex(minX, minY, 0.0F, 0.0F, 0.0F, argb, light);
        sink.vertex(maxX, minY, 0.0F, 1.0F, 0.0F, argb, light);
        sink.vertex(maxX, maxY, 0.0F, 1.0F, 1.0F, argb, light);
        sink.vertex(minX, maxY, 0.0F, 0.0F, 1.0F, argb, light);
    }

    public static void glass(SubmitNodeCollector collector, PoseStack poseStack, int light, boolean voidJar) {
        BoxGeometry.UvRect bottom = EssentiaRendering.rect(Thaumcraft.id("block/jar_bottom"));
        BoxGeometry.UvRect top = EssentiaRendering.rect(Thaumcraft.id(voidJar ? "block/jar_top_void" : "block/jar_top"));
        BoxGeometry.UvRect side = EssentiaRendering.rect(Thaumcraft.id(voidJar ? "block/jar_side_void" : "block/jar_side"));
        ItemGeometry.solid(collector.order(4), poseStack, TextureAtlas.LOCATION_BLOCKS, true, sink -> {
            BoxGeometry.box(sink, 0.1875F, 0.0F, 0.1875F, 0.8125F, 0.75F, 0.8125F, bottom, top, side, WHITE, light);
            BoxGeometry.box(sink, 0.3125F, 0.75F, 0.3125F, 0.6875F, 0.875F, 0.6875F, bottom, top, side, WHITE, light);
        });
    }
}
