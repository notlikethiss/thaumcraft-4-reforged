package thaumcraft.client.geom;

public final class Colors {
    private Colors() {
    }

    public static int argb(float red, float green, float blue, float alpha) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    public static int rgb(float red, float green, float blue) {
        return argb(red, green, blue, 1.0F);
    }

    public static int withAlpha(int rgb, float alpha) {
        return Math.clamp((int) (alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    private static int channel(float value) {
        return Math.clamp(Math.round(value * 255.0F), 0, 255);
    }
}
