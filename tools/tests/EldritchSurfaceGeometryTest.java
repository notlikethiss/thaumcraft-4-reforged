import thaumcraft.client.geom.EldritchSurfaceGeometry;
import thaumcraft.client.geom.QuadSink;

public final class EldritchSurfaceGeometryTest {
    private static final int[][] OFFSETS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    private static final class Output implements QuadSink {
        int vertices;
        boolean sharedPlane;
        final int blockX;
        Output(int blockX) {
            this.blockX = blockX;
        }
        public void vertex(float x, float y, float z, float u, float v, int color, int light) {
            if (!Float.isFinite(u) || !Float.isFinite(v)) throw new AssertionError("Non-finite UV");
            if (vertices % 4 == 0) sharedPlane = true;
            sharedPlane &= x + blockX == 1;
            vertices++;
            if (vertices % 4 == 0 && sharedPlane) throw new AssertionError("Internal face was emitted");
        }
    }
    public static void main(String[] args) {
        int vertices = 0;
        for (int blockX = 0; blockX < 2; blockX++) {
            int mask = 0;
            for (int side = 0; side < 6; side++) {
                int[] step = OFFSETS[side];
                boolean neighborIsVoid = blockX + step[0] >= 0 && blockX + step[0] < 2 && step[1] == 0 && step[2] == 0;
                if (EldritchSurfaceGeometry.exposed(neighborIsVoid, false)) mask |= 1 << side;
            }
            Output out = new Output(blockX);
            EldritchSurfaceGeometry.faces(out, mask, 0.125F, 0, -1, 0xF000F0);
            vertices += out.vertices;
        }
        if (vertices != 40) throw new AssertionError("Two adjacent cubes must emit ten exterior faces");
        if (EldritchSurfaceGeometry.exposed(false, true)) throw new AssertionError("Solid neighbor must hide a face");
        for (float width : new float[] {0.2F, 0.75F, 1.0F}) {
            for (float height : new float[] {1.0F / 30.0F, 0.25F, 1.0F}) {
                for (int frame = 0; frame < 16; frame++) {
                    final float u0 = frame / 16.0F;
                    final float u1 = u0 + 1.0F / 16.0F;
                    final int[] count = {0};
                    final int[] corners = {0};
                    EldritchSurfaceGeometry.portal((x, y, z, u, v, color, light) -> {
                        if (z != 0 || Math.abs(x) != width || Math.abs(y) != height) {
                            throw new AssertionError("Portal must stand in local XY before camera orientation is applied");
                        }
                        if (u != (y < 0 ? u1 : u0) || v != (x < 0 ? 1 : 0)) {
                            throw new AssertionError("Portal frame must rotate 90 degrees clockwise inside its atlas frame");
                        }
                        if (color != -1 || light != 0xF000F0) throw new AssertionError("Portal lost its color or light");
                        corners[0] |= 1 << ((x > 0 ? 1 : 0) | (y > 0 ? 2 : 0));
                        count[0]++;
                    }, width, height, frame, -1, 0xF000F0);
                    if (count[0] != 4 || corners[0] != 15) throw new AssertionError("Portal must emit four distinct corners");
                }
            }
        }
        final float[] span = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
        EldritchSurfaceGeometry.tiledRectangle((x, y, z, u, v, color, light) -> {
            span[0] = Math.min(span[0], u);
            span[1] = Math.max(span[1], u);
            span[2] = Math.min(span[2], v);
            span[3] = Math.max(span[3], v);
        }, -2.5F, -2.0F, 0.0F, 2.5F, 3.0F, 0.0F, 0.125F, 0.0F, -1, 0xF000F0);
        if (Math.abs(span[1] - span[0] - 0.625F) > 1.0E-5F || Math.abs(span[3] - span[2] - 0.625F) > 1.0E-5F) {
            throw new AssertionError("Tiled portal rectangle must keep the texgen density of one block per 0.125 UV");
        }
        java.util.Random random = new java.util.Random(31100L);
        for (int layer = 0; layer < 16; layer++) {
            float alpha = 1.0F / (17 - layer);
            int red = (int) ((random.nextFloat() * 0.5F + 0.1F) * 255 * alpha);
            int green = (int) ((random.nextFloat() * 0.5F + 0.4F) * 255 * alpha);
            int blue = (int) ((random.nextFloat() * 0.5F + 0.5F) * 255 * alpha);
            if (layer > 0 && EldritchSurfaceGeometry.colorForLayer(layer) != (0xFF000000 | red << 16 | green << 8 | blue)) {
                throw new AssertionError("Layer " + layer + " color must follow the single TC4 random sequence");
            }
        }
        System.out.println("Eldritch geometry passed: ten exterior faces, camera-facing portal plane, horizontal image, all 16 frames and opening sizes, tiled portal density, TC4 layer colors.");
    }
}
