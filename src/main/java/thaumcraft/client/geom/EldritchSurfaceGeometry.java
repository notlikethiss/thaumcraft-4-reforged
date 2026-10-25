package thaumcraft.client.geom;

public final class EldritchSurfaceGeometry {
    private static final float[][] UV = {{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};
    private static final float[][][] FACES = {
        {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
        {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}},
        {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}},
        {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}},
        {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
        {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}}
    };

    private static final int[] COLORS = new int[16];
    private static final float[] ROTATIONS = new float[16];

    static {
        COLORS[0] = 0xFFFFFFFF;
        java.util.Random random = new java.util.Random(31100L);
        for (int layer = 0; layer < 16; layer++) {
            float alpha = 1.0F / (17 - layer);
            int red = (int) ((random.nextFloat() * 0.5F + 0.1F) * 255 * alpha);
            int green = (int) ((random.nextFloat() * 0.5F + 0.4F) * 255 * alpha);
            int blue = (int) ((random.nextFloat() * 0.5F + 0.5F) * 255 * alpha);
            if (layer > 0) {
                COLORS[layer] = 0xFF000000 | red << 16 | green << 8 | blue;
            }
            ROTATIONS[layer] = (float) Math.toRadians((layer * layer * 4321.0F + layer * 9.0F) * 2.0F);
        }
    }

    public static boolean exposed(boolean sameSurface, boolean solidNeighbor) {
        return !sameSurface && !solidNeighbor;
    }

    public static int colorForLayer(int layer) {
        return COLORS[layer];
    }

    public static float rotationForLayer(int layer, float time) {
        return ROTATIONS[layer] + (float) Math.toRadians(time * 0.004F * (layer + 1));
    }

    public static void faces(QuadSink sink, int mask, float uvScale, float rotation, int color, int light) {
        for (int side = 0; side < 6; side++) {
            if ((mask & (1 << side)) != 0) {
                cubeFace(sink, side, uvScale, rotation, color, light);
            }
        }
    }

    private EldritchSurfaceGeometry() {
    }

    public static void portal(QuadSink sink, float width, float height, int frame, int color, int light) {
        float u0 = (frame & 15) / 16.0F;
        float u1 = u0 + 1.0F / 16.0F;
        sink.vertex(-width, -height, 0, u1, 1, color, light);
        sink.vertex(width, -height, 0, u1, 0, color, light);
        sink.vertex(width, height, 0, u0, 0, color, light);
        sink.vertex(-width, height, 0, u0, 1, color, light);
    }

    public static void cubeFace(QuadSink sink, int side, float uvScale, float rotation, int color, int light) {
        if (side < 0 || side >= FACES.length) {
            throw new IllegalArgumentException("Invalid cube face " + side);
        }
        emit(sink, FACES[side], UV, uvScale, uvScale, rotation, color, light);
    }

    public static void rectangle(QuadSink sink, float x0, float y0, float z0, float x1, float y1, float z1, float uvScale, float rotation, int color, int light) {
        float[][] positions = {{x0, y1, z0}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z0}};
        emit(sink, positions, UV, uvScale, uvScale, rotation, color, light);
    }

    public static void tiledRectangle(QuadSink sink, float x0, float y0, float z0, float x1, float y1, float z1, float uvScale, float rotation, int color, int light) {
        float[][] positions = {{x0, y1, z0}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z0}};
        float width = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));
        float height = Math.abs(y1 - y0);
        emit(sink, positions, UV, uvScale * width, uvScale * height, rotation, color, light);
    }

    private static void emit(QuadSink sink, float[][] positions, float[][] uvs, float uScale, float vScale, float rotation, int color, int light) {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        for (int index = 0; index < 4; index++) {
            float u = (uvs[index][0] - 0.5F) * uScale;
            float v = (uvs[index][1] - 0.5F) * vScale;
            float rotatedU = u * cos - v * sin + 0.5F;
            float rotatedV = u * sin + v * cos + 0.5F;
            sink.vertex(positions[index][0], positions[index][1], positions[index][2], rotatedU, rotatedV, color, light);
        }
    }
}
