package thaumcraft.client.geom;

public final class LightningGeometry {
    private static final int LIGHT = 240;

    private LightningGeometry() {
    }

    public static void render(LightningBolt bolt, int age, float partialTick, float cameraX, float cameraY, float cameraZ, float lookX, float lookY, float lookZ, float red, float green, float blue, int pass, QuadSink sink) {
        BoltVector viewVector = new BoltVector(lookX, lookY, lookZ);
        float boltAge = age >= 0 ? (float) age / bolt.particleMaxAge : 0.0F;
        float mainAlpha = pass == 0 ? (1.0F - boltAge) * 0.4F : 1.0F - boltAge * 0.5F;
        int lengthTicks = Math.max(1, (int) (bolt.length * 3.0F));
        int renderLength = (int) ((age + partialTick + lengthTicks) / lengthTicks * bolt.numSegments0);
        for (LightningBolt.Segment segment : bolt.segments) {
            if (segment.segmentNo > renderLength) {
                continue;
            }
            BoltVector startVec = segment.startPoint.point;
            BoltVector endVec = segment.endPoint.point;
            BoltVector relative = new BoltVector(cameraX - startVec.x, cameraY - startVec.y, cameraZ - startVec.z);
            float width = bolt.width * (relative.length() / 5.0F + 1.0F) * (1.0F + segment.light) * 0.5F;
            BoltVector diff1 = BoltVector.crossProduct(viewVector, segment.prevDiff).scale(width / segment.sinPrev);
            BoltVector diff2 = BoltVector.crossProduct(viewVector, segment.nextDiff).scale(width / segment.sinNext);
            float rx1 = startVec.x - cameraX;
            float ry1 = startVec.y - cameraY;
            float rz1 = startVec.z - cameraZ;
            float rx2 = endVec.x - cameraX;
            float ry2 = endVec.y - cameraY;
            float rz2 = endVec.z - cameraZ;
            int argb = Colors.withAlpha(rgb(red, green, blue), mainAlpha * segment.light);
            sink.vertex(rx2 - diff2.x, ry2 - diff2.y, rz2 - diff2.z, 0.5F, 0.0F, argb, LIGHT);
            sink.vertex(rx1 - diff1.x, ry1 - diff1.y, rz1 - diff1.z, 0.5F, 0.0F, argb, LIGHT);
            sink.vertex(rx1 + diff1.x, ry1 + diff1.y, rz1 + diff1.z, 0.5F, 1.0F, argb, LIGHT);
            sink.vertex(rx2 + diff2.x, ry2 + diff2.y, rz2 + diff2.z, 0.5F, 1.0F, argb, LIGHT);
            if (segment.next == null) {
                BoltVector roundEnd = endVec.copy().add(segment.diff.copy().normalize().scale(width));
                float rx3 = roundEnd.x - cameraX;
                float ry3 = roundEnd.y - cameraY;
                float rz3 = roundEnd.z - cameraZ;
                sink.vertex(rx3 - diff2.x, ry3 - diff2.y, rz3 - diff2.z, 0.0F, 0.0F, argb, LIGHT);
                sink.vertex(rx2 - diff2.x, ry2 - diff2.y, rz2 - diff2.z, 0.5F, 0.0F, argb, LIGHT);
                sink.vertex(rx2 + diff2.x, ry2 + diff2.y, rz2 + diff2.z, 0.5F, 1.0F, argb, LIGHT);
                sink.vertex(rx3 + diff2.x, ry3 + diff2.y, rz3 + diff2.z, 0.0F, 1.0F, argb, LIGHT);
            }
            if (segment.prev == null) {
                BoltVector roundEnd = startVec.copy().sub(segment.diff.copy().normalize().scale(width));
                float rx3 = roundEnd.x - cameraX;
                float ry3 = roundEnd.y - cameraY;
                float rz3 = roundEnd.z - cameraZ;
                sink.vertex(rx1 - diff1.x, ry1 - diff1.y, rz1 - diff1.z, 0.5F, 0.0F, argb, LIGHT);
                sink.vertex(rx3 - diff1.x, ry3 - diff1.y, rz3 - diff1.z, 0.0F, 0.0F, argb, LIGHT);
                sink.vertex(rx3 + diff1.x, ry3 + diff1.y, rz3 + diff1.z, 0.0F, 1.0F, argb, LIGHT);
                sink.vertex(rx1 + diff1.x, ry1 + diff1.y, rz1 + diff1.z, 0.5F, 1.0F, argb, LIGHT);
            }
        }
    }

    private static int rgb(float red, float green, float blue) {
        return (int) (red * 255.0F) << 16 | (int) (green * 255.0F) << 8 | (int) (blue * 255.0F);
    }
}
