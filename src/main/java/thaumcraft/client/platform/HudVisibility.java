package thaumcraft.client.platform;

import net.minecraft.client.Minecraft;

public final class HudVisibility {
    private HudVisibility() {
    }

    public static boolean hidden() {
        return Minecraft.getInstance().options.hideGui;
    }
}
