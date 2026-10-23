package thaumcraft.client.render.device;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import thaumcraft.device.CrystalClusterBlock;

public final class DeviceSpecialRenderers {
    private DeviceSpecialRenderers() {
    }

    public record CrystalCluster(int kind) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            int ticks = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
            poseStack.pushPose();
            if (kind == CrystalClusterBlock.STRANGE) {
                DeviceRendering.strangeCrystal(collector, poseStack, lightCoords, Direction.UP, 0, ticks);
            } else {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(-0.5F, 0.0F, -0.5F);
                DeviceRendering.crystalCluster(collector, poseStack, kind, Direction.UP, kind, ticks);
            }
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            for (float x : new float[] {0.0F, 1.0F}) {
                for (float y : new float[] {0.0F, 1.0F}) {
                    for (float z : new float[] {0.0F, 1.0F}) {
                        output.accept(new Vector3f(x, y, z));
                    }
                }
            }
        }

        public record Unbaked(int kind) implements NoDataSpecialModelRenderer.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("kind").forGetter(Unbaked::kind)
            ).apply(instance, Unbaked::new));

            @Override
            public CrystalCluster bake(BakingContext context) {
                return new CrystalCluster(kind);
            }

            @Override
            public MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }
        }
    }
}
