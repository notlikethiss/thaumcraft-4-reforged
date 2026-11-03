package thaumcraft.client.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.FacingQuads;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.WorldFx;
import thaumcraft.client.geom.Colors;

public class VisSparkleFx extends WorldFx {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/misc/particles.png");

    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int color;
    private final float sizeMod;
    private double motionX;
    private double motionY;
    private double motionZ;
    private float scale;

    public VisSparkleFx(RandomSource random, double x, double y, double z, double targetX, double targetY, double targetZ, int color) {
        super(x, y, z, 1000);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.color = color;
        motionX = (float) random.nextGaussian() * 0.01F;
        motionY = (float) random.nextGaussian() * 0.01F;
        motionZ = (float) random.nextGaussian() * 0.01F;
        sizeMod = 45 + random.nextInt(15);
    }

    @Override
    public void tick() {
        prevX = x;
        prevY = y;
        prevZ = z;
        if (age++ >= maxAge) {
            return;
        }
        x += motionX;
        y += motionY;
        z += motionZ;
        motionX *= 0.985;
        motionY *= 0.985;
        motionZ *= 0.985;
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 2.0) {
            scale *= 0.95F;
        }
        if (distance < 0.2) {
            maxAge = age;
        }
        if (age < 10) {
            scale = age / sizeMod;
        }
        if (distance > 1.0E-6) {
            dx /= distance;
            dy /= distance;
            dz /= distance;
        }
        motionX = Mth.clamp((float) (motionX + dx * 0.1F), -0.1F, 0.1F);
        motionY = Mth.clamp((float) (motionY + dy * 0.1F), -0.1F, 0.1F);
        motionZ = Mth.clamp((float) (motionZ + dz * 0.1F), -0.1F, 0.1F);
    }

    @Override
    public ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    public int blendMode() {
        return 1;
    }

    @Override
    public void render(QuadSink sink, float partialTick, Vec3 camera, float cameraYaw, float cameraPitch) {
        float bob = Mth.sin(age / 3.0F) * 0.3F + 6.0F;
        int part = age % 16;
        float u0 = part / 16.0F;
        float u1 = u0 + 0.0624375F;
        float size = 0.1F * scale * bob;
        FacingQuads.Camera view = new FacingQuads.Camera(cameraYaw, cameraPitch, 0.0F, 0.0F, 0.0F);
        FacingQuads.particle(
            sink,
            view,
            renderX(partialTick, camera),
            renderY(partialTick, camera),
            renderZ(partialTick, camera),
            size,
            u0,
            0.5F,
            u1,
            0.5625F,
            Colors.withAlpha(color, 0.5F),
            240
        );
    }
}
