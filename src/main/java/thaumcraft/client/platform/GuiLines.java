package thaumcraft.client.platform;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class GuiLines {
    private static final float LINE_WIDTH = 3.0F;

    private GuiLines() {
    }

    public static void strip(GuiGraphicsExtractor graphics, float[] xs, float[] ys, int[] colors, boolean additive) {
        if (xs.length < 2) {
            return;
        }
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float inner = (LINE_WIDTH / 2.0F - 0.5F) / scale;
        float outer = (LINE_WIDTH / 2.0F + 0.5F) / scale;
        RenderPipeline pipeline = additive ? ModRenderPipelines.GUI_ADDITIVE : RenderPipelines.GUI;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new Strip(pipeline, pose, xs.clone(), ys.clone(), colors.clone(), inner, outer, scissor, bounds(xs, ys, outer, pose, scissor)));
    }

    private static @Nullable ScreenRectangle bounds(float[] xs, float[] ys, float outer, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int index = 0; index < xs.length; index++) {
            minX = Math.min(minX, xs[index]);
            minY = Math.min(minY, ys[index]);
            maxX = Math.max(maxX, xs[index]);
            maxY = Math.max(maxY, ys[index]);
        }
        int left = (int) Math.floor(minX - outer);
        int top = (int) Math.floor(minY - outer);
        int right = (int) Math.ceil(maxX + outer);
        int bottom = (int) Math.ceil(maxY + outer);
        ScreenRectangle area = new ScreenRectangle(left, top, right - left, bottom - top).transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(area) : area;
    }

    private record Strip(
        RenderPipeline pipeline,
        Matrix3x2fc pose,
        float[] xs,
        float[] ys,
        int[] colors,
        float inner,
        float outer,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            int count = xs.length;
            float[] normalX = new float[count];
            float[] normalY = new float[count];
            for (int index = 0; index < count; index++) {
                float directionX = 0.0F;
                float directionY = 0.0F;
                if (index > 0) {
                    float[] segment = direction(index - 1, index);
                    directionX += segment[0];
                    directionY += segment[1];
                }
                if (index < count - 1) {
                    float[] segment = direction(index, index + 1);
                    directionX += segment[0];
                    directionY += segment[1];
                }
                float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
                if (length > 0.0F) {
                    normalX[index] = -directionY / length;
                    normalY[index] = directionX / length;
                }
            }
            for (int index = 0; index < count - 1; index++) {
                int next = index + 1;
                int startColor = colors[index];
                int endColor = colors[next];
                int startClear = ARGB.color(0, startColor);
                int endClear = ARGB.color(0, endColor);
                band(consumer, index, next, outer, inner, startClear, endClear, startColor, endColor, normalX, normalY);
                band(consumer, index, next, inner, -inner, startColor, endColor, startColor, endColor, normalX, normalY);
                band(consumer, index, next, -inner, -outer, startColor, endColor, startClear, endClear, normalX, normalY);
            }
        }

        private float[] direction(int from, int to) {
            float deltaX = xs[to] - xs[from];
            float deltaY = ys[to] - ys[from];
            float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            return length > 0.0F ? new float[] {deltaX / length, deltaY / length} : new float[] {0.0F, 0.0F};
        }

        private void band(
            VertexConsumer consumer,
            int start,
            int end,
            float firstOffset,
            float secondOffset,
            int startFirst,
            int endFirst,
            int startSecond,
            int endSecond,
            float[] normalX,
            float[] normalY
        ) {
            consumer.addVertexWith2DPose(pose, xs[start] + normalX[start] * secondOffset, ys[start] + normalY[start] * secondOffset).setColor(startSecond);
            consumer.addVertexWith2DPose(pose, xs[start] + normalX[start] * firstOffset, ys[start] + normalY[start] * firstOffset).setColor(startFirst);
            consumer.addVertexWith2DPose(pose, xs[end] + normalX[end] * firstOffset, ys[end] + normalY[end] * firstOffset).setColor(endFirst);
            consumer.addVertexWith2DPose(pose, xs[end] + normalX[end] * secondOffset, ys[end] + normalY[end] * secondOffset).setColor(endSecond);
        }
    }
}
