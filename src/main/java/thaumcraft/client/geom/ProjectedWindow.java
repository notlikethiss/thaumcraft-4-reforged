package thaumcraft.client.geom;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class ProjectedWindow {
    private static final List<Vector4f> HIDDEN = List.of(new Vector4f(0, 0, 0, -1));

    private ProjectedWindow() {
    }

    public static List<Vector4f> hidden() {
        return HIDDEN;
    }

    public static List<Vector4f> planes(float[][] aperture, Matrix4f apertureToClip, Matrix4f sceneToClip) {
        Vector4f center = apertureToClip.transform(new Vector4f(0, 0, 0, 1));
        Vector4f[] corners = new Vector4f[aperture.length];
        for (int i = 0; i < aperture.length; i++) {
            corners[i] = apertureToClip.transform(new Vector4f(aperture[i][0], aperture[i][1], 0, 1));
            if (corners[i].w <= 0.0F || !corners[i].isFinite()) {
                return HIDDEN;
            }
        }
        Matrix4f transpose = new Matrix4f(sceneToClip).transpose();
        List<Vector4f> planes = new ArrayList<>();
        for (int i = 0; i < corners.length; i++) {
            Vector4f a = corners[i];
            Vector4f b = corners[(i + 1) % corners.length];
            Vector4f edge = new Vector4f(a.y * b.w - a.w * b.y, a.w * b.x - a.x * b.w, 0, a.x * b.y - a.y * b.x);
            if (edge.dot(center) < 0) {
                edge.negate();
            }
            planes.add(transpose.transform(edge));
        }
        planes.add(transpose.transform(new Vector4f(0, 0, 0, 1)));
        return List.copyOf(planes);
    }
}
