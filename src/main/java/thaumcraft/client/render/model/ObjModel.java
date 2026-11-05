package thaumcraft.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.QuadSink;

public final class ObjModel {
    private static final Map<Identifier, ObjModel> CACHE = new HashMap<>();

    private final Map<String, List<float[][]>> groups;

    private ObjModel(Map<String, List<float[][]>> groups) {
        this.groups = groups;
    }

    public static ObjModel get(Identifier location) {
        return CACHE.computeIfAbsent(location, ObjModel::load);
    }

    public static ObjModel load(Identifier location) {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        Map<String, List<float[][]>> groups = new HashMap<>();
        List<float[][]> current = groups.computeIfAbsent("", key -> new ArrayList<>());
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(floats(parts, 3));
                    case "vt" -> uvs.add(floats(parts, 2));
                    case "vn" -> normals.add(floats(parts, 3));
                    case "g", "o" -> current = groups.computeIfAbsent(parts.length > 1 ? parts[1] : "", key -> new ArrayList<>());
                    case "f" -> current.add(face(parts, positions, uvs, normals));
                    default -> {
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            Thaumcraft.LOGGER.error("Failed to load model {}", location, exception);
        }
        return new ObjModel(groups);
    }

    private static float[][] face(String[] parts, List<float[]> positions, List<float[]> uvs, List<float[]> normals) {
        int count = parts.length - 1;
        float[][] face = new float[Math.max(count, 4)][];
        for (int index = 0; index < face.length; index++) {
            String[] indices = parts[1 + Math.min(index, count - 1)].split("/");
            float[] position = positions.get(Integer.parseInt(indices[0]) - 1);
            float[] uv = indices.length > 1 && !indices[1].isEmpty() ? uvs.get(Integer.parseInt(indices[1]) - 1) : new float[] {0.0F, 0.0F};
            float[] normal = indices.length > 2 ? normals.get(Integer.parseInt(indices[2]) - 1) : new float[] {0.0F, 1.0F, 0.0F};
            face[index] = new float[] {position[0], position[1], position[2], uv[0], 1.0F - uv[1], normal[0], normal[1], normal[2]};
        }
        return face;
    }

    private static float[] floats(String[] parts, int count) {
        float[] values = new float[count];
        for (int index = 0; index < count; index++) {
            values[index] = Float.parseFloat(parts[1 + index]);
        }
        return values;
    }

    public void renderPart(String name, PoseStack.Pose pose, VertexConsumer buffer, int argb, int light) {
        List<float[][]> faces = groups.get(name);
        if (faces == null) {
            return;
        }
        for (float[][] face : faces) {
            for (float[] vertex : face) {
                buffer.addVertex(pose, vertex[0], vertex[1], vertex[2])
                    .setColor(argb)
                    .setUv(vertex[3], vertex[4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, vertex[5], vertex[6], vertex[7]);
            }
        }
    }

    public void renderAll(Matrix4f transform, QuadSink sink, int argb, int light) {
        Vector3f position = new Vector3f();
        for (List<float[][]> faces : groups.values()) {
            for (float[][] face : faces) {
                for (float[] vertex : face) {
                    transform.transformPosition(vertex[0], vertex[1], vertex[2], position);
                    sink.vertex(position.x, position.y, position.z, vertex[3], vertex[4], argb, light);
                }
            }
        }
    }

    public void renderAll(PoseStack.Pose pose, VertexConsumer buffer, int argb, int light) {
        for (String name : groups.keySet()) {
            renderPart(name, pose, buffer, argb, light);
        }
    }
}
