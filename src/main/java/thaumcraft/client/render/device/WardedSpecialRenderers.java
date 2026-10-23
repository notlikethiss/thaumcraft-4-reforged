package thaumcraft.client.render.device;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.device.WardedKeys;

public final class WardedSpecialRenderers {
    private WardedSpecialRenderers() {
    }

    private static void extents(Consumer<Vector3fc> output, float minY, float maxY) {
        for (float x : new float[] {0.0F, 1.0F}) {
            for (float y : new float[] {minY, maxY}) {
                for (float z : new float[] {0.0F, 1.0F}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    private static void inventoryTurn(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    public record HungryChest() implements NoDataSpecialModelRenderer {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            inventoryTurn(poseStack);
            WardedRendering.hungryChest(collector, poseStack, lightCoords, Direction.SOUTH, 0.0F);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            extents(output, 0.0F, 1.0F);
        }

        public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

            @Override
            public HungryChest bake(BakingContext context) {
                return new HungryChest();
            }

            @Override
            public MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }
        }
    }

    public record BrainJar() implements NoDataSpecialModelRenderer {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            inventoryTurn(poseStack);
            WardedRendering.brainJar(collector, poseStack, lightCoords, 0.0F, WardedRendering.viewerTicks());
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            extents(output, 0.0F, 0.75F);
        }

        public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

            @Override
            public BrainJar bake(BakingContext context) {
                return new BrainJar();
            }

            @Override
            public MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }
        }
    }

    public record BannerData(int color, @Nullable Aspect aspect) {
    }

    public record Banner() implements SpecialModelRenderer<BannerData> {
        @Override
        public void submit(
            @Nullable BannerData data,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
        ) {
            BannerData banner = data == null ? new BannerData(-1, null) : data;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.5F, -1.0F, -0.5F);
            WardedRendering.banner(collector, poseStack, lightCoords, banner.color(), banner.aspect(), false, 0, false, WardedRendering.bannerSwing(0, 0, 0, 0L, 0.0F));
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            extents(output, 0.0F, 2.0F);
        }

        @Override
        public @Nullable BannerData extractArgument(ItemStack stack) {
            Integer color = WardedKeys.BANNER_COLOR.get(stack);
            return new BannerData(color == null ? -1 : color, WardedKeys.BANNER_ASPECT.get(stack));
        }

        public record Unbaked() implements SpecialModelRenderer.Unbaked<BannerData> {
            public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

            @Override
            public SpecialModelRenderer<BannerData> bake(SpecialModelRenderer.BakingContext context) {
                return new Banner();
            }

            @Override
            public MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }
        }
    }
}
