package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.TcRenderTypes.Kind;
import thaumcraft.client.render.model.ObjModel;

public class ThaumometerItemRenderer implements NoDataSpecialModelRenderer {
    private static final Identifier MODEL = Thaumcraft.id("models/obj/scanner.obj");
    private static final Identifier SCANNER = Thaumcraft.id("textures/model/scanner.png");
    private static final Identifier SCREEN = Thaumcraft.id("textures/model/scanscreen.png");
    private static final int WHITE = 0xFFFFFFFF;

    private final LegacyItemView view;

    public ThaumometerItemRenderer(LegacyItemView view) {
        this.view = view;
    }

    private void transform(PoseStack poseStack) {
        if (view == LegacyItemView.EQUIPPED) {
            poseStack.translate(0.5F, 0.5F, 0.2F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            return;
        }
        view.apply(poseStack);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        switch (view) {
            case INVENTORY -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(60.0F));
                poseStack.mulPose(Axis.ZN.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YN.rotationDegrees(248.0F));
            }
            default -> {
            }
        }
    }

    public static int screenLight(int offset) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return WorldRendering.emissiveLight(200.0F);
        }
        int random = player.level().getRandom().nextInt(2);
        return WorldRendering.emissiveLight((int) (190.0F + Mth.sin(offset + player.tickCount - random) * 10.0F + 10.0F));
    }

    public static void submitModel(SubmitNodeCollector collector, PoseStack poseStack, int light) {
        ObjModel model = ObjModel.get(MODEL);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.of(Kind.ENTITY_CUTOUT, SCANNER), (pose, buffer) -> model.renderPart("scanner", pose, buffer, WHITE, light));
    }

    public static void beginScreen(SubmitNodeCollector collector, PoseStack poseStack) {
        poseStack.pushPose();
        applyScreenTransform(poseStack);
        int light = screenLight(0);
        collector.submitCustomGeometry(poseStack, TcRenderTypes.translucent(SCREEN), (pose, buffer) -> {
            screenVertex(buffer, pose, -0.5F, 0.5F, 0.0F, 1.0F, light);
            screenVertex(buffer, pose, 0.5F, 0.5F, 1.0F, 1.0F, light);
            screenVertex(buffer, pose, 0.5F, -0.5F, 1.0F, 0.0F, light);
            screenVertex(buffer, pose, -0.5F, -0.5F, 0.0F, 0.0F, light);
        });
    }

    static void applyScreenTransform(PoseStack poseStack) {
        poseStack.translate(0.0F, 0.11F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(2.5F, 2.5F, 2.5F);
    }

    private static void screenVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(WHITE).setUv(u, v).setLight(light);
    }

    public static void endScreen(PoseStack poseStack) {
        poseStack.popPose();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (view == LegacyItemView.FIRST_PERSON) {
            return;
        }
        poseStack.pushPose();
        transform(poseStack);
        submitModel(collector, poseStack, lightCoords);
        beginScreen(collector, poseStack);
        endScreen(poseStack);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        transform(poseStack);
        for (float x : new float[] {-1.2124F, 1.2124F}) {
            for (float y : new float[] {0.0F, 0.2F}) {
                for (float z : new float[] {-1.4F, 1.4F}) {
                    output.accept(poseStack.last().pose().transformPosition(new Vector3f(x, y, z)));
                }
            }
        }
    }

    public record Unbaked(LegacyItemView view) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LegacyItemView.FIELD.forGetter(Unbaked::view)
        ).apply(instance, Unbaked::new));

        @Override
        public NoDataSpecialModelRenderer bake(BakingContext context) {
            return new ThaumometerItemRenderer(view);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
