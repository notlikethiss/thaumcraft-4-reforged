package thaumcraft.client.platform;

import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class WarpPostEffects {
    private WarpPostEffects() {
    }

    public static void sync(LocalPlayer player, Collection<Identifier> ownedEffects, Collection<Identifier> activeEffects) {
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        Identifier current = gameRenderer.currentPostEffect();
        if (activeEffects.isEmpty()) {
            if (current != null && ownedEffects.contains(current)) {
                gameRenderer.clearPostEffect();
            }
            return;
        }
        if (current != null && !ownedEffects.contains(current)) {
            return;
        }
        Identifier next = activeEffects.contains(current) ? current : activeEffects.iterator().next();
        if (!next.equals(current)) {
            gameRenderer.setPostEffect(next);
        }
    }

    public static void clear(@Nullable LocalPlayer player, Collection<Identifier> ownedEffects) {
        if (player == null) {
            return;
        }
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        Identifier current = gameRenderer.currentPostEffect();
        if (current != null && ownedEffects.contains(current)) {
            gameRenderer.clearPostEffect();
        }
    }
}
