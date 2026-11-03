package thaumcraft.client.geom;

public final class FloatyLineGeometry {
    public static final float DEFAULT_WIDTH = 0.15F;
    public static final float QUALITY = 16.0F;
    private static final int LIGHT = 240;

    private FloatyLineGeometry() {
    }

    public record Params(
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ,
        double originX,
        double originY,
        double originZ,
        float time,
        int color,
        float speed,
        float distance,
        float width
    ) {
    }

    public static void render(Params params, QuadSink sink) {
        double deltaX = params.startX() - params.endX();
        double deltaY = params.startY() - params.endY();
        double deltaZ = params.startZ() - params.endZ();
        float dist = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        float length = Math.round(dist) * (QUALITY / 2.0F);
        if (length <= 0.0F) {
            return;
        }
        float offsetX = (float) (params.endX() - params.originX());
        float offsetY = (float) (params.endY() - params.originY());
        float offsetZ = (float) (params.endZ() - params.originZ());
        float cx = (float) deltaX;
        float cy = (float) deltaY;
        float cz = (float) deltaZ;
        int rgb = params.color() & 0xFFFFFF;
        float time = params.time();
        double phaseX = params.startX() % 16.0;
        double phaseY = params.startY() % 16.0;
        double phaseZ = params.startZ() % 16.0;
        float width = params.width();
        int steps = (int) Math.floor(length * params.distance()) + 1;
        float[] pointX = new float[steps];
        float[] pointY = new float[steps];
        float[] pointZ = new float[steps];
        float[] texture = new float[steps];
        int[] colors = new int[steps];
        for (int index = 0; index < steps; index++) {
            float f2 = index / length;
            float f3 = 1.0F - Math.abs(index - length / 2.0F) / (length / 2.0F);
            float phase = dist * (1.0F - f2) * QUALITY / 2.0F - time % 32767.0F / 5.0F;
            float dx = cx + (float) Math.sin((phaseZ + phase) / 4.0) * 0.5F * f3;
            float dy = cy + (float) Math.sin((phaseX + phase) / 3.0) * 0.5F * f3;
            float dz = cz + (float) Math.sin((phaseY + phase) / 2.0) * 0.5F * f3;
            pointX[index] = dx * f2 + offsetX;
            pointY[index] = dy * f2 + offsetY;
            pointZ[index] = dz * f2 + offsetZ;
            texture[index] = (1.0F - f2) * dist - time * params.speed();
            colors[index] = Colors.withAlpha(rgb, f3);
        }
        for (int pass = 0; pass < 2; pass++) {
            float shiftX = pass == 0 ? 0.0F : width;
            float shiftY = pass == 0 ? width : 0.0F;
            for (int index = 1; index < steps; index++) {
                int previous = index - 1;
                sink.vertex(pointX[previous] - shiftX, pointY[previous] - shiftY, pointZ[previous], texture[previous], 1.0F, colors[previous], LIGHT);
                sink.vertex(pointX[previous] + shiftX, pointY[previous] + shiftY, pointZ[previous], texture[previous], 0.0F, colors[previous], LIGHT);
                sink.vertex(pointX[index] + shiftX, pointY[index] + shiftY, pointZ[index], texture[index], 0.0F, colors[index], LIGHT);
                sink.vertex(pointX[index] - shiftX, pointY[index] - shiftY, pointZ[index], texture[index], 1.0F, colors[index], LIGHT);
            }
        }
    }
}
