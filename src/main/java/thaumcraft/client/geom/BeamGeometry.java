package thaumcraft.client.geom;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BeamGeometry {
    private static final int LIGHT = 200;

    private BeamGeometry() {
    }

    public record Params(
        float x,
        float y,
        float z,
        float yaw,
        float pitch,
        float length,
        float width,
        float endMod,
        boolean pulse,
        boolean reverse,
        int rotationSpeed,
        int age,
        int maxAge,
        float prevSize,
        long worldTime,
        float playerTicks,
        float partialTick,
        float red,
        float green,
        float blue
    ) {
    }

    public static float render(Params params, QuadSink sink) {
        float partialTick = params.partialTick();
        float rot = (float) (params.worldTime() % (360 / params.rotationSpeed()) * params.rotationSpeed()) + params.rotationSpeed() * partialTick;
        float size = params.width();
        if (params.pulse()) {
            size = Math.min(params.age() / 4.0F, params.width());
            size = (float) (params.prevSize() + (double) (size - params.prevSize()) * partialTick);
        }
        float opacity = 0.4F;
        if (params.pulse() && params.maxAge() - params.age() <= 4) {
            opacity = 0.4F - (4 - (params.maxAge() - params.age())) * 0.1F;
        }
        float slide = params.playerTicks() + partialTick;
        if (params.reverse()) {
            slide *= -1.0F;
        }
        float offset = -slide * 0.2F - (float) Math.floor(-slide * 0.1F);
        Matrix4f matrix = new Matrix4f()
            .translate(params.x(), params.y(), params.z())
            .rotateX((float) Math.toRadians(90.0F))
            .rotateZ((float) Math.toRadians(-(180.0F + params.yaw())))
            .rotateX((float) Math.toRadians(params.pitch()))
            .rotateY((float) Math.toRadians(rot));
        float near = -0.15F * size;
        float far = 0.15F * size;
        float nearEnd = -0.15F * size * params.endMod();
        float farEnd = 0.15F * size * params.endMod();
        int argb = Colors.argb(params.red(), params.green(), params.blue(), opacity);
        Vector3f position = new Vector3f();
        for (int strip = 0; strip < 3; strip++) {
            float top = params.length() * size / params.width();
            float v0 = -1.0F + offset + strip / 3.0F;
            float v1 = top + v0;
            matrix.rotateY((float) Math.toRadians(60.0F));
            emit(matrix, position, sink, nearEnd, top, 1.0F, v1, argb);
            emit(matrix, position, sink, near, 0.0F, 1.0F, v0, argb);
            emit(matrix, position, sink, far, 0.0F, 0.0F, v0, argb);
            emit(matrix, position, sink, farEnd, top, 0.0F, v1, argb);
        }
        return size;
    }

    private static void emit(Matrix4f matrix, Vector3f position, QuadSink sink, float x, float y, float u, float v, int argb) {
        matrix.transformPosition(x, y, 0.0F, position);
        sink.vertex(position.x, position.y, position.z, u, v, argb, LIGHT);
    }
}
