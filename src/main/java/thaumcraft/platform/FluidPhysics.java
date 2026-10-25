package thaumcraft.platform;

import net.neoforged.neoforge.fluids.FluidType;

public final class FluidPhysics {
    private FluidPhysics() {
    }

    public static FluidType create(FluidType.Properties properties) {
        return new FluidType(properties.isWaterLike(true));
    }
}
