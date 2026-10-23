package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.BoxGeometry;
import thaumcraft.client.geom.ExtrudedGeometry;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.TcRenderTypes.Kind;
import thaumcraft.client.render.model.ObjModel;

public final class EssentiaRendering {
    public static final Identifier ANIMATED_GLOW = Thaumcraft.id("block/animatedglow");
    private static final Identifier VALVE_TEXTURE = Thaumcraft.id("textures/model/valve.png");
    private static final Identifier CENTRIFUGE_TEXTURE = Thaumcraft.id("textures/model/centrifuge.png");
    private static final Identifier CRYSTALLIZER_TEXTURE = Thaumcraft.id("textures/model/crystalizer.png");
    private static final Identifier CRYSTALLIZER_MODEL = Thaumcraft.id("models/obj/crystalizer.obj");
    private static final Identifier RELAY_TEXTURE = Thaumcraft.id("textures/model/vis_relay.png");
    private static final Identifier RELAY_MODEL = Thaumcraft.id("models/obj/vis_relay.obj");
    private static final Identifier RESERVOIR_TEXTURE = Thaumcraft.id("textures/model/reservoir.png");
    private static final Identifier RESERVOIR_MODEL = Thaumcraft.id("models/obj/reservoir.obj");
    private static final int WHITE = 0xFFFFFFFF;

    private EssentiaRendering() {
    }

    public static TextureAtlasSprite blockSprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    public static BoxGeometry.UvRect rect(Identifier id) {
        TextureAtlasSprite sprite = blockSprite(id);
        return new BoxGeometry.UvRect(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    public static int liquidLight(int light) {
        return Math.max(light & 0xFFFF, 0xC8) | (light & 0xFFFF0000);
    }

    public static int viewerTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getCameraEntity() == null ? 0 : minecraft.getCameraEntity().tickCount;
    }

    public static void orientation(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            default -> {
            }
        }
        poseStack.translate(0.0F, 0.0F, -0.5F);
    }

    public static void tubeOrientation(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (facing.getStepY() == 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        } else {
            poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F * facing.getStepY()));
        }
        poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.PI / 2.0F, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
    }

    public static void valveRod(SubmitNodeCollector collector, PoseStack poseStack, int argb, int light) {
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.of(Kind.ENTITY_CUTOUT, VALVE_TEXTURE),
            (pose, buffer) -> ModelBoxGeometry.box(new EntityQuadSink(pose, buffer), -1.0F, 2.0F, -1.0F, 2, 2, 2, 0, 10, 64.0F, 32.0F, true, argb, light)
        );
    }

    public static void valve(SubmitNodeCollector collector, PoseStack poseStack, Direction facing, float rotation, int light) {
        poseStack.pushPose();
        tubeOrientation(poseStack, facing);
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation * 1.5F));
        poseStack.translate(0.0F, -(rotation / 360.0F) * 0.12F, 0.0F);
        valveRod(collector, poseStack, WHITE, light);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.25F, -0.25F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        TextureAtlasSprite sprite = blockSprite(Thaumcraft.id("block/pipe_valve"));
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float minU = sprite.getU0();
        float maxV = sprite.getV1();
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        ItemGeometry.solid(
            collector,
            poseStack,
            TextureAtlas.LOCATION_BLOCKS,
            false,
            sink -> ExtrudedGeometry.render(sink, maxU, minV, minU, maxV, width, height, 0.1F, WHITE, light)
        );
        poseStack.popPose();
        poseStack.popPose();
    }

    public static void oneway(SubmitNodeCollector collector, PoseStack poseStack, Direction facing, int light) {
        int tint = NodeDeviceRendering.color(0.45F, 0.5F, 1.0F);
        poseStack.pushPose();
        tubeOrientation(poseStack, facing);
        poseStack.scale(1.1F, 0.5F, 1.1F);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        valveRod(collector, poseStack, tint, light);
        poseStack.translate(0.0F, -0.25F, 0.0F);
        valveRod(collector, poseStack, tint, light);
        poseStack.translate(0.0F, -0.25F, 0.0F);
        valveRod(collector, poseStack, tint, light);
        poseStack.popPose();
    }

    public static void buffer(SubmitNodeCollector collector, PoseStack poseStack, Direction side, boolean blocked, int light) {
        int tint = blocked ? NodeDeviceRendering.color(1.0F, 0.3F, 0.3F) : NodeDeviceRendering.color(0.3F, 0.3F, 1.0F);
        poseStack.pushPose();
        tubeOrientation(poseStack, side.getOpposite());
        poseStack.scale(1.2F, 1.0F, 1.2F);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        valveRod(collector, poseStack, tint, light);
        poseStack.popPose();
    }

    public static void filterCore(SubmitNodeCollector collector, PoseStack poseStack, int argb, int light) {
        BoxGeometry.UvRect rect = rect(Thaumcraft.id("block/pipe_filter_core"));
        float low = 0.375F - 0.03125F;
        float high = 0.625F + 0.03125F;
        ItemGeometry.solid(
            collector,
            poseStack,
            TextureAtlas.LOCATION_BLOCKS,
            false,
            sink -> BoxGeometry.box(sink, low, low, low, high, high, high, rect, rect, rect, argb, light)
        );
    }

    public static void centrifuge(SubmitNodeCollector collector, PoseStack poseStack, float rotation, int light) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, CENTRIFUGE_TEXTURE), (pose, buffer) -> {
            EntityQuadSink sink = new EntityQuadSink(pose, buffer);
            ModelBoxGeometry.box(sink, -4.0F, -8.0F, -4.0F, 8, 4, 8, 20, 16, 64.0F, 32.0F, true, WHITE, light);
            ModelBoxGeometry.box(sink, -4.0F, 4.0F, -4.0F, 8, 4, 8, 20, 16, 64.0F, 32.0F, true, WHITE, light);
        });
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, CENTRIFUGE_TEXTURE), (pose, buffer) -> {
            EntityQuadSink sink = new EntityQuadSink(pose, buffer);
            ModelBoxGeometry.box(sink, -4.0F, -1.0F, -1.0F, 8, 2, 2, 16, 0, 64.0F, 32.0F, true, WHITE, light);
            ModelBoxGeometry.box(sink, 4.0F, -3.0F, -2.0F, 4, 6, 4, 0, 16, 64.0F, 32.0F, true, WHITE, light);
            ModelBoxGeometry.box(sink, -8.0F, -3.0F, -2.0F, 4, 6, 4, 0, 16, 64.0F, 32.0F, true, WHITE, light);
            ModelBoxGeometry.box(sink, -1.5F, -4.0F, -1.5F, 3, 8, 3, 0, 0, 64.0F, 32.0F, true, WHITE, light);
        });
        poseStack.popPose();
    }

    public static void crystallizer(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        Direction facing,
        float spin,
        float red,
        float green,
        float blue,
        float ticks,
        int light
    ) {
        poseStack.pushPose();
        orientation(poseStack, facing);
        ObjModel base = ObjModel.get(CRYSTALLIZER_MODEL);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, CRYSTALLIZER_TEXTURE), (pose, buffer) -> base.renderAll(pose, buffer, WHITE, light));
        ObjModel relay = ObjModel.get(RELAY_MODEL);
        int tint = NodeDeviceRendering.color(red, green, blue);
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            poseStack.scale(0.75F, 0.75F, 0.75F);
            float glow = Mth.sin((ticks + index * 10) / 2.0F) * 0.05F + 0.95F;
            int crystalLight = WorldRendering.emissiveLight(50 + (int) (150.0F * glow));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 * index));
            poseStack.translate(0.34F, 0.0F, 1.2125F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
            collector.submitCustomGeometry(
                poseStack,
                TcRenderTypes.of(Kind.ENTITY_TRANSLUCENT, RELAY_TEXTURE),
                (pose, buffer) -> relay.renderPart("Crystal", pose, buffer, tint, crystalLight)
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void reservoir(SubmitNodeCollector collector, PoseStack poseStack, Direction facing, int light) {
        poseStack.pushPose();
        orientation(poseStack, facing);
        ObjModel model = ObjModel.get(RESERVOIR_MODEL);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, RESERVOIR_TEXTURE), (pose, buffer) -> model.renderAll(pose, buffer, WHITE, light));
        poseStack.popPose();
    }

    public static void reservoirLiquid(SubmitNodeCollector collector, PoseStack poseStack, float level, float red, float green, float blue) {
        BoxGeometry.UvRect rect = rect(ANIMATED_GLOW);
        int color = ARGB.colorFromFloat(0.9F, red, green, blue);
        float top = 0.1875F + 0.625F * level;
        int light = liquidLight(0);
        ItemGeometry.solid(
            collector.order(1),
            poseStack,
            TextureAtlas.LOCATION_BLOCKS,
            true,
            sink -> BoxGeometry.box(sink, 0.1875F, 0.1875F, 0.1875F, 0.8125F, top, 0.8125F, rect, rect, rect, color, light)
        );
    }
}
