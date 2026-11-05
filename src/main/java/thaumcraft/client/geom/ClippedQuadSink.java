package thaumcraft.client.geom;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector4f;

public final class ClippedQuadSink implements QuadSink {
    private final QuadSink output;
    private final List<Vector4f> planes;
    private final List<Vertex> quad = new ArrayList<>(4);

    public ClippedQuadSink(QuadSink output, List<Vector4f> planes) {
        this.output = output;
        this.planes = planes;
    }

    @Override
    public void vertex(float x, float y, float z, float u, float v, int argb, int light) {
        quad.add(new Vertex(x, y, z, u, v, argb, light));
        if (quad.size() == 4) {
            flush();
            quad.clear();
        }
    }

    private void flush() {
        List<Vertex> polygon = quad;
        for (Vector4f plane : planes) {
            if (polygon.isEmpty()) {
                return;
            }
            List<Vertex> clipped = new ArrayList<>();
            Vertex previous = polygon.getLast();
            float previousDistance = previous.distance(plane);
            for (Vertex current : polygon) {
                float distance = current.distance(plane);
                if ((distance >= 0.0F) != (previousDistance >= 0.0F)) {
                    float t = previousDistance / (previousDistance - distance);
                    clipped.add(previous.interpolate(current, t));
                }
                if (distance >= 0.0F) {
                    clipped.add(current);
                }
                previous = current;
                previousDistance = distance;
            }
            polygon = clipped;
        }
        if (polygon.size() == 4) {
            polygon.forEach(this::emit);
        } else {
            for (int i = 1; i + 1 < polygon.size(); i++) {
                emit(polygon.getFirst());
                emit(polygon.get(i));
                emit(polygon.get(i + 1));
                emit(polygon.get(i + 1));
            }
        }
    }

    private void emit(Vertex vertex) {
        output.vertex(vertex.x, vertex.y, vertex.z, vertex.u, vertex.v, vertex.argb, vertex.light);
    }

    private record Vertex(float x, float y, float z, float u, float v, int argb, int light) {
        float distance(Vector4f plane) {
            return plane.x * x + plane.y * y + plane.z * z + plane.w;
        }

        Vertex interpolate(Vertex other, float t) {
            return new Vertex(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t,
                u + (other.u - u) * t, v + (other.v - v) * t, argb, light);
        }
    }
}
