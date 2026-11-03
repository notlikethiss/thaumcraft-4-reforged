package thaumcraft.client.geom;

import java.util.List;

public final class NodeGeometry {
    public static final int FRAMES = 32;
    public static final int ADDITIVE = 1;
    public static final int TRANSLUCENT = 771;
    private static final int LIGHT = 0xF000F0;
    private static final float TAU = (float) (Math.PI * 2);

    private NodeGeometry() {
    }

    public enum CoreType {
        NORMAL,
        UNSTABLE,
        DARK,
        TAINTED,
        HUNGRY,
        PURE
    }

    public enum Modifier {
        NONE,
        BRIGHT,
        PALE,
        FADING
    }

    public record AspectLayer(int color, int amount, int blend) {
    }

    public record Params(
        FacingQuads.Camera camera,
        float x,
        float y,
        float z,
        int blockX,
        boolean full,
        float distance,
        float viewDistance,
        float size,
        List<AspectLayer> layers,
        CoreType type,
        Modifier modifier,
        int viewerTicks,
        long nanoTime
    ) {
    }

    public static void render(Params params, QuadSink additive, QuadSink translucent) {
        if (params.full() && !params.layers().isEmpty()) {
            renderFull(params, additive, translucent);
        } else {
            renderFaint(params, additive);
        }
    }

    private static QuadSink sinkFor(int blend, QuadSink additive, QuadSink translucent) {
        return blend == TRANSLUCENT ? translucent : additive;
    }

    private static void renderFull(Params params, QuadSink additive, QuadSink translucent) {
        long nanoTime = params.nanoTime();
        List<AspectLayer> layers = params.layers();
        float alpha = (params.viewDistance() - params.distance()) / params.viewDistance();
        switch (params.modifier()) {
            case BRIGHT -> alpha *= 1.5F;
            case PALE -> alpha *= 0.66F;
            case FADING -> alpha *= (float) Math.sin(params.viewerTicks() / 3.0F) * 0.25F + 0.33F;
            default -> {
            }
        }
        long time = nanoTime / 5000000L;
        float baseScale = 0.25F;
        int frame = (int) ((nanoTime / 40000000L + params.blockX()) % FRAMES);
        int count = 0;
        float angle = 0.0F;
        float average = 0.0F;
        for (AspectLayer layer : layers) {
            float layerAlpha = layer.blend() == TRANSLUCENT ? alpha * 1.5F : alpha;
            average += layer.amount();
            float scale = (float) Math.sin(params.viewerTicks() / (14.0F - count)) * baseScale + baseScale * 2.0F;
            scale = 0.2F + scale * (layer.amount() / 50.0F);
            scale *= params.size();
            angle = (float) (time % (5000 + 500 * count)) / (5000.0F + 500 * count) * TAU;
            float stripAlpha = layerAlpha / Math.max(1.0F, layers.size() / 2.0F);
            FacingQuads.strip(
                sinkFor(layer.blend(), additive, translucent),
                params.camera(),
                params.x(),
                params.y(),
                params.z(),
                angle,
                scale,
                FRAMES,
                0,
                frame,
                Colors.withAlpha(layer.color(), stripAlpha),
                LIGHT
            );
            count++;
        }
        average /= layers.size();
        float scale = (0.1F + average / 150.0F) * params.size();
        int strip = 1;
        int blend = ADDITIVE;
        switch (params.type()) {
            case UNSTABLE -> {
                strip = 6;
                angle = 0.0F;
            }
            case DARK -> {
                blend = TRANSLUCENT;
                strip = 2;
            }
            case TAINTED -> {
                blend = TRANSLUCENT;
                strip = 5;
            }
            case PURE -> strip = 4;
            case HUNGRY -> {
                scale *= 0.75F;
                strip = 3;
            }
            default -> {
            }
        }
        FacingQuads.strip(
            sinkFor(blend, additive, translucent),
            params.camera(),
            params.x(),
            params.y(),
            params.z(),
            angle,
            scale,
            FRAMES,
            strip,
            frame,
            Colors.withAlpha(0xFFFFFF, alpha),
            LIGHT
        );
    }

    private static void renderFaint(Params params, QuadSink additive) {
        int frame = (int) ((params.nanoTime() / 40000000L + params.blockX()) % FRAMES);
        FacingQuads.strip(additive, params.camera(), params.x(), params.y(), params.z(), 0.0F, 0.5F, FRAMES, 1, frame, Colors.withAlpha(0xFFFFFF, 0.1F), LIGHT);
    }
}
