package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.TcRenderTypes.Kind;
import thaumcraft.client.render.model.ObjModel;

public final class AlchemyRendering {
    private static final Identifier ALEMBIC_MODEL = Thaumcraft.id("models/obj/alembic.obj");
    private static final Identifier ALEMBIC_TEXTURE = Thaumcraft.id("textures/model/alembic.png");
    private static final Identifier LABEL = Thaumcraft.id("textures/model/label.png");
    private static final Identifier BORE_TEXTURE = Thaumcraft.id("textures/model/bore.png");
    private static final Identifier FURNACE_MODEL = Thaumcraft.id("models/obj/adv_alch_furnace.obj");
    private static final Identifier THAUMATORIUM_MODEL = Thaumcraft.id("models/obj/thaumatorium.obj");
    private static final Identifier THAUMATORIUM_TEXTURE = Thaumcraft.id("textures/model/thaumatorium.png");
    private static final Identifier BELLOWS_TEXTURE = Thaumcraft.id("textures/model/bellows.png");
    private static final Identifier CAP_MODEL = Thaumcraft.id("models/obj/obelisk_cap.obj");
    private static final Identifier SCRUBBER_TEXTURE = Thaumcraft.id("textures/model/fluxscrubber.png");
    private static final int WHITE = 0xFFFFFFFF;

    private AlchemyRendering() {
    }

    private static void model(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, Consumer<EntityQuadSink> geometry, boolean translucent) {
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.of(translucent ? Kind.ENTITY_TRANSLUCENT : Kind.ENTITY_CUTOUT, texture),
            (pose, buffer) -> geometry.accept(new EntityQuadSink(pose, buffer))
        );
    }

    private static void part(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, ObjModel obj, int light, String... names) {
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, texture), (pose, buffer) -> {
            for (String name : names) {
                obj.renderPart(name, pose, buffer, WHITE, light);
            }
        });
    }

    private static void doubleSided(QuadSink sink, float[][] corners, float[][] uvs, int argb, int light) {
        for (int index = 0; index < 4; index++) {
            sink.vertex(corners[index][0], corners[index][1], corners[index][2], uvs[index][0], uvs[index][1], argb, light);
        }
        for (int index = 3; index >= 0; index--) {
            sink.vertex(corners[index][0], corners[index][1], corners[index][2], uvs[index][0], uvs[index][1], argb, light);
        }
    }

    private static void atlasQuad(OrderedSubmitNodeCollector collector, PoseStack poseStack, Identifier spriteId, float top, int argb, int light) {
        TextureAtlasSprite sprite = EssentiaRendering.blockSprite(spriteId);
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float minU = sprite.getU0();
        float maxV = sprite.getV1();
        ItemGeometry.solid(collector, poseStack, TextureAtlas.LOCATION_BLOCKS, true, sink -> doubleSided(
            sink,
            new float[][] {{0.0F, 1.0F, 0.0F}, {1.0F, 1.0F, 0.0F}, {1.0F, top, 0.0F}, {0.0F, top, 0.0F}},
            new float[][] {{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}},
            argb,
            light
        ));
    }

    public static void crucibleWater(SubmitNodeCollector collector, PoseStack poseStack, float height, int argb, int light) {
        TextureAtlasSprite sprite = EssentiaRendering.blockSprite(Identifier.withDefaultNamespace("block/water_still"));
        ItemGeometry.solid(collector, poseStack, TextureAtlas.LOCATION_BLOCKS, true, sink -> {
            sink.vertex(0.0F, height, 0.0F, sprite.getU0(), sprite.getV0(), argb, light);
            sink.vertex(0.0F, height, 1.0F, sprite.getU0(), sprite.getV1(), argb, light);
            sink.vertex(1.0F, height, 1.0F, sprite.getU1(), sprite.getV1(), argb, light);
            sink.vertex(1.0F, height, 0.0F, sprite.getU1(), sprite.getV0(), argb, light);
        });
    }

    private static void rotateAlembic(PoseStack poseStack, Direction facing, Axis axis) {
        switch (facing) {
            case NORTH -> poseStack.mulPose(axis.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(axis.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(axis.rotationDegrees(180.0F));
            default -> {
            }
        }
    }

    public static void alembic(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        int light,
        boolean placed,
        Direction facing,
        boolean aboveFurnace,
        boolean aboveAlembic,
        @Nullable Aspect filter,
        boolean[] nozzles
    ) {
        ObjModel obj = ObjModel.get(ALEMBIC_MODEL);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        if (placed) {
            rotateAlembic(poseStack, facing, Axis.ZP);
            if (aboveFurnace) {
                part(collector, poseStack, ALEMBIC_TEXTURE, obj, light, "TubeMain", "Legs");
            } else if (aboveAlembic) {
                part(collector, poseStack, ALEMBIC_TEXTURE, obj, light, "TubeMain", "TubeSmall");
            } else {
                part(collector, poseStack, ALEMBIC_TEXTURE, obj, light, "Legs");
            }
        } else {
            poseStack.translate(0.0F, 0.0F, -0.4F);
            part(collector, poseStack, ALEMBIC_TEXTURE, obj, light, "Legs");
        }
        part(collector, poseStack, ALEMBIC_TEXTURE, obj, light, "Pot", "Panel");
        poseStack.popPose();
        if (filter != null) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            rotateAlembic(poseStack, facing, Axis.YP);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.468F, -0.409F);
            poseStack.scale(0.27F, 0.27F, 0.27F);
            ItemGeometry.solid(collector.order(2), poseStack, LABEL, true, sink -> EssentiaJarRendering.quad(sink, -0.5F, -0.5F, 0.5F, 0.5F, WHITE, light));
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.468F, -0.41F);
            poseStack.scale(0.013F, 0.013F, 0.013F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            int tint = ARGB.opaque(filter.getColor());
            ItemGeometry.solid(collector.order(3), poseStack, filter.getImage(), true, sink -> EssentiaJarRendering.quad(sink, -8.0F, -8.0F, 8.0F, 8.0F, tint, light));
            poseStack.popPose();
            poseStack.popPose();
        }
        poseStack.popPose();
        for (Direction direction : Direction.values()) {
            if (nozzles[direction.ordinal()]) {
                poseStack.pushPose();
                poseStack.translate(0.5F, 0.0F, 0.5F);
                switch (direction) {
                    case DOWN -> {
                        poseStack.translate(-0.5F, 0.5F, 0.0F);
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90.0F));
                    }
                    case UP -> {
                        poseStack.translate(0.5F, 0.5F, 0.0F);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    }
                    case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                    case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                    case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    default -> {
                    }
                }
                model(collector, poseStack, BORE_TEXTURE, sink -> {
                    ModelBoxGeometry.box(sink, 2.5F, 8.0F - 2.0F, -2.0F, 5, 4, 4, 106, 42, 128.0F, 64.0F, true, WHITE, light);
                    ModelBoxGeometry.box(sink, 7.0F, 8.0F - 2.5F, -2.5F, 1, 5, 5, 106, 51, 128.0F, 64.0F, true, WHITE, light);
                }, false);
                poseStack.popPose();
            }
        }
    }

    public static void advancedFurnace(SubmitNodeCollector collector, PoseStack poseStack, int light, int heat, int vis, int maxVis, int maxPower) {
        ObjModel obj = ObjModel.get(FURNACE_MODEL);
        Identifier baseTexture = Thaumcraft.id(heat <= 100 ? "textures/model/alch_furnace.png" : "textures/model/alch_furnace_on.png");
        Identifier tankTexture = Thaumcraft.id(vis <= 0 ? "textures/model/alch_furnace_tank.png" : "textures/model/alch_furnace_tank_on.png");
        Identifier frame = Thaumcraft.id("block/alchemyblockadv");
        Identifier goo = Thaumcraft.id("block/fluxgoo");
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        part(collector, poseStack, baseTexture, obj, light, "Base");
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 * index));
            part(collector, poseStack, tankTexture, obj, light, "Tank");
            poseStack.popPose();
        }
        int goLight = WorldRendering.emissiveLight(190);
        int frameLight = WorldRendering.emissiveLight(150);
        if (vis > 0) {
            poseStack.pushPose();
            poseStack.translate(0.5F, -0.5F, 1.1F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            atlasQuad(collector.order(1), poseStack, goo, 0.0F, WHITE, goLight);
            poseStack.popPose();
            float drop = 1.0F - (float) vis / maxVis;
            for (int index = 0; index < 4; index++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZP.rotationDegrees(90 * index));
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
                poseStack.translate(0.85F, -1.8F, -1.4F);
                poseStack.scale(0.3F, 0.6F, 1.0F);
                atlasQuad(collector.order(2), poseStack, frame, 0.0F, WHITE, frameLight);
                poseStack.translate(0.0F, 0.0F, -0.01F);
                atlasQuad(collector.order(3), poseStack, goo, drop, WHITE, goLight);
                poseStack.popPose();
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZN.rotationDegrees(90 * index));
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.translate(1.15F, 1.8F, -1.4F);
                poseStack.scale(-0.3F, -0.6F, -1.0F);
                atlasQuad(collector.order(2), poseStack, frame, 0.0F, WHITE, frameLight);
                poseStack.translate(0.0F, 0.0F, 0.01F);
                atlasQuad(collector.order(3), poseStack, goo, drop, WHITE, goLight);
                poseStack.popPose();
            }
        }
        if (heat > 100) {
            Identifier fire = Identifier.withDefaultNamespace("block/fire_0");
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 1.0F);
            for (int index = 0; index < 4; index++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZP.rotationDegrees(90 * index));
                poseStack.mulPose(Axis.XP.rotationDegrees(135.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                poseStack.translate(-0.5F, 0.0F, -1.0F);
                poseStack.pushPose();
                atlasQuad(collector.order(2), poseStack, fire, 1.0F - Math.min(1.0F, (float) heat / maxPower), WHITE, WorldRendering.emissiveLight(220));
                poseStack.popPose();
                poseStack.translate(0.0F, 0.0F, 0.05F);
                atlasQuad(collector.order(3), poseStack, frame, 0.0F, WHITE, frameLight);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void thaumatorium(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction facing) {
        ObjModel obj = ObjModel.get(THAUMATORIUM_MODEL);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        rotateAlembic(poseStack, facing, Axis.ZP);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, THAUMATORIUM_TEXTURE), (pose, buffer) -> obj.renderAll(pose, buffer, WHITE, light));
        poseStack.popPose();
    }

    public static void thaumatoriumItemTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F + facing.getStepX() / 1.99F, 1.325F, 0.5F + facing.getStepZ() / 1.99F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        poseStack.scale(0.75F, 0.75F, 0.75F);
    }

    public static void bellows(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction orientation, float inflation) {
        float tscale = 0.125F + inflation * 0.875F;
        poseStack.pushPose();
        poseStack.translate(0.5F, -0.5F, 0.5F);
        switch (orientation) {
            case DOWN -> {
                poseStack.translate(0.0F, 1.0F, -1.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                poseStack.translate(0.0F, 1.0F, 1.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(270.0F));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.pushPose();
        poseStack.scale(0.5F, (inflation + 0.1F) / 2.0F, 0.5F);
        model(collector, poseStack, BELLOWS_TEXTURE, sink ->
            ModelBoxGeometry.box(sink, -10.0F, -12.03333F + 0.5F, -10.0F, 20, 24, 20, 48, 0, 128.0F, 64.0F, true, WHITE, light), true);
        poseStack.popPose();
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, -tscale / 2.0F + 0.5F, 0.0F);
        model(collector, poseStack, BELLOWS_TEXTURE, sink ->
            ModelBoxGeometry.box(sink, -6.0F, 8.0F, -6.0F, 12, 2, 12, 0, 0, 128.0F, 64.0F, true, WHITE, light), false);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, tscale / 2.0F - 0.5F, 0.0F);
        model(collector, poseStack, BELLOWS_TEXTURE, sink ->
            ModelBoxGeometry.box(sink, -6.0F, 22.0F, -6.0F, 12, 2, 12, 0, 0, 128.0F, 64.0F, true, WHITE, light), false);
        poseStack.popPose();
        model(collector, poseStack, BELLOWS_TEXTURE, sink -> {
            ModelBoxGeometry.box(sink, -6.0F, 16.0F - 1.0F, -6.0F, 12, 2, 12, 0, 0, 128.0F, 64.0F, true, WHITE, light);
            ModelBoxGeometry.box(sink, -2.0F, 16.0F - 2.0F, 6.0F, 4, 4, 2, 0, 36, 128.0F, 64.0F, true, WHITE, light);
        }, false);
        poseStack.popPose();
    }

    public static void fluxScrubber(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction facing, float ticks) {
        ObjModel obj = ObjModel.get(CAP_MODEL);
        poseStack.pushPose();
        EssentiaRendering.orientation(poseStack, facing);
        part(collector, poseStack, SCRUBBER_TEXTURE, obj, light, "Cap");
        float bob = Mth.sin(ticks / 8.0F) * 0.075F + 0.075F;
        poseStack.translate(0.0F, 0.0F, -bob);
        part(collector, poseStack, SCRUBBER_TEXTURE, obj, light, "Tip");
        poseStack.popPose();
    }
}
