package thaumcraft.client.render.device;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.EntityQuadSink;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.TcRenderTypes.Kind;
import thaumcraft.client.render.WorldRendering;
import thaumcraft.client.render.model.ObjModel;
import thaumcraft.device.CrystalClusterBlock;

public final class DeviceRendering {
    private static final Identifier BORE_TEXTURE = Thaumcraft.id("textures/model/bore.png");
    private static final Identifier CRYSTAL_TEXTURE = Thaumcraft.id("textures/model/crystal.png");
    private static final Identifier STRANGE_MODEL = Thaumcraft.id("models/obj/vcrystal.obj");
    private static final Identifier STRANGE_BASE = Thaumcraft.id("textures/block/crust.png");
    private static final Identifier STRANGE_CRYSTAL = Thaumcraft.id("textures/model/vcrystal.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final float MODEL_ANGLE = 0.7071F * 57.29578F;

    private DeviceRendering() {
    }

    public static void lampNozzle(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction facing, boolean boreBase) {
        if (boreBase) {
            poseStack.pushPose();
            poseStack.translate(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            nozzle(collector, poseStack, light, facing.getOpposite());
            poseStack.popPose();
        }
        nozzle(collector, poseStack, light, facing);
    }

    private static void nozzle(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction facing) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
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
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, BORE_TEXTURE), (pose, buffer) -> {
            EntityQuadSink sink = new EntityQuadSink(pose, buffer);
            ModelBoxGeometry.box(sink, 2.5F, 8.0F - 2.0F, -2.0F, 5, 4, 4, 106, 42, 128.0F, 64.0F, true, WHITE, light);
            ModelBoxGeometry.box(sink, 7.0F, 8.0F - 2.5F, -2.5F, 1, 5, 5, 106, 51, 128.0F, 64.0F, true, WHITE, light);
        });
        poseStack.popPose();
    }

    private static void translateFromOrientation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.5F, 1.3F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }
            case UP -> poseStack.translate(0.5F, -0.3F, 0.5F);
            case NORTH -> {
                poseStack.translate(0.5F, 0.5F, 1.3F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.translate(0.5F, 0.5F, -0.3F);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case WEST -> {
                poseStack.translate(1.3F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            case EAST -> {
                poseStack.translate(-0.3F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
        }
    }

    private static int tint(int color, float scale) {
        int red = Math.min(255, Math.round((color >> 16 & 255) / 220.0F * 255.0F * scale));
        int green = Math.min(255, Math.round((color >> 8 & 255) / 220.0F * 255.0F * scale));
        int blue = Math.min(255, Math.round((color & 255) / 220.0F * 255.0F * scale));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static void drawCrystal(
        SubmitNodeCollector collector,
        PoseStack poseStack,
        Direction facing,
        float angleY,
        float angleX,
        Random random,
        int color,
        float size,
        int ticks
    ) {
        float shade = Mth.sin((ticks + random.nextInt(10)) / (5.0F + random.nextFloat())) * 0.075F + 0.925F;
        int argb = tint(color, 1.0F);
        poseStack.pushPose();
        translateFromOrientation(poseStack, facing);
        poseStack.mulPose(Axis.YP.rotationDegrees(angleY));
        poseStack.mulPose(Axis.XP.rotationDegrees(angleX));
        poseStack.scale((0.15F + random.nextFloat() * 0.075F) * size, (0.5F + random.nextFloat() * 0.1F) * size, (0.15F + random.nextFloat() * 0.05F) * size);
        int light = WorldRendering.emissiveLight(210.0F * shade);
        poseStack.translate(0.0F, 2.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(MODEL_ANGLE));
        poseStack.mulPose(Axis.XP.rotationDegrees(MODEL_ANGLE));
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.of(Kind.ENTITY_TRANSLUCENT, CRYSTAL_TEXTURE),
            (pose, buffer) -> ModelBoxGeometry.box(new EntityQuadSink(pose, buffer), -16.0F, -16.0F, 0.0F, 16, 16, 16, 0, 0, 64.0F, 32.0F, true, argb, light)
        );
        poseStack.popPose();
    }

    public static void crystalCluster(SubmitNodeCollector collector, PoseStack poseStack, int kind, Direction facing, long seed, int ticks) {
        Random random = new Random(seed);
        int color = kind == CrystalClusterBlock.MIXED ? CrystalClusterBlock.COLORS[5] : CrystalClusterBlock.COLORS[kind + 1];
        drawCrystal(collector, poseStack, facing, (random.nextFloat() - random.nextFloat()) * 5.0F, (random.nextFloat() - random.nextFloat()) * 5.0F, random, color, 1.1F, ticks);
        for (int index = 1; index < 6; index++) {
            if (kind == CrystalClusterBlock.MIXED) {
                color = CrystalClusterBlock.COLORS[index == 5 ? 6 : index];
            }
            int angleY = random.nextInt(36) + 72 * index;
            int angleX = 15 + random.nextInt(15);
            drawCrystal(collector, poseStack, facing, angleY, angleX, random, color, 0.8F, ticks);
        }
    }

    public static void strangeCrystal(SubmitNodeCollector collector, PoseStack poseStack, int light, Direction facing, int rotation, int ticks) {
        ObjModel obj = ObjModel.get(STRANGE_MODEL);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        poseStack.translate(0.0F, 0.0F, -0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F * rotation));
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, STRANGE_BASE), (pose, buffer) -> obj.renderPart("Base", pose, buffer, WHITE, light));
        float shade = Mth.sin(ticks / 6.0F) * 0.075F + 0.925F;
        int glow = WorldRendering.emissiveLight(210.0F * shade);
        int translucent = 0xB3FFFFFF;
        collector.submitCustomGeometry(
            poseStack,
            TcRenderTypes.of(Kind.ENTITY_TRANSLUCENT, STRANGE_CRYSTAL),
            (pose, buffer) -> obj.renderPart("Crystal", pose, buffer, translucent, glow)
        );
        poseStack.popPose();
    }
}
