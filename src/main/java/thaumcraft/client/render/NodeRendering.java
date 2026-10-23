package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.geom.NodeStripGeometry;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;

public final class NodeRendering {
    public static final Identifier TEXTURE = Thaumcraft.id("textures/misc/nodes.png");
    private static final int FRAMES = 32;
    private static final int LIGHT = 0xF000F0;
    private static final int BLEND_ADDITIVE = 1;
    private static final int BLEND_TRANSLUCENT = 771;

    private NodeRendering() {
    }

    public static float clientTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0.0F : minecraft.player.tickCount;
    }

    public static void submitCross(
        OrderedSubmitNodeCollector collector,
        PoseStack poseStack,
        AspectList aspects,
        NodeType type,
        @Nullable NodeModifier modifier
    ) {
        float ticks = clientTicks();
        submit(collector, poseStack, aspects, type, modifier, ticks);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        submit(collector, poseStack, aspects, type, modifier, ticks);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        submit(collector, poseStack, aspects, type, modifier, ticks);
        poseStack.popPose();
    }

    public static void submit(
        OrderedSubmitNodeCollector collector,
        PoseStack poseStack,
        AspectList aspects,
        NodeType type,
        @Nullable NodeModifier modifier,
        float ticks
    ) {
        if (aspects.size() == 0) {
            return;
        }
        float alpha = 0.5F;
        if (modifier != null) {
            switch (modifier) {
                case BRIGHT -> alpha *= 1.5F;
                case PALE -> alpha *= 0.66F;
                case FADING -> alpha *= (float) Math.sin(ticks / 3.0F) * 0.25F + 0.33F;
            }
        }
        long nanos = System.nanoTime();
        int frame = (int) ((nanos / 40000000L + 1L) % FRAMES);
        int count = 0;
        float average = 0.0F;
        for (Aspect aspect : aspects.getAspects()) {
            float layerAlpha = aspect.getBlend() == BLEND_TRANSLUCENT ? alpha * 1.5F : alpha;
            int amount = aspects.getAmount(aspect);
            average += amount;
            float wave = (float) Math.sin(ticks / (14.0F - count)) * 0.25F + 0.5F;
            float layerScale = 0.2F + wave * (amount / 50.0F);
            int argb = argb(layerAlpha / aspects.size(), aspect.getColor());
            ItemGeometry.blended(
                collector,
                poseStack,
                TEXTURE,
                aspect.getBlend(),
                sink -> NodeStripGeometry.strip(sink, layerScale, FRAMES, 0, frame, argb, LIGHT)
            );
            count++;
        }
        average /= aspects.size();
        float scale = 0.1F + average / 150.0F;
        int strip = 1;
        int blend = BLEND_ADDITIVE;
        switch (type) {
            case NORMAL -> {
            }
            case UNSTABLE -> strip = 6;
            case DARK -> {
                blend = BLEND_TRANSLUCENT;
                strip = 2;
            }
            case TAINTED -> {
                blend = BLEND_TRANSLUCENT;
                strip = 5;
            }
            case PURE -> strip = 4;
            case HUNGRY -> {
                scale *= 0.75F;
                strip = 3;
            }
        }
        int centerStrip = strip;
        float centerScale = scale;
        int argb = argb(alpha, 0xFFFFFF);
        ItemGeometry.blended(
            collector,
            poseStack,
            TEXTURE,
            blend,
            sink -> NodeStripGeometry.strip(sink, centerScale, FRAMES, centerStrip, frame, argb, LIGHT)
        );
    }

    private static int argb(float alpha, int rgb) {
        int channel = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        return (channel << 24) | (rgb & 0xFFFFFF);
    }
}
