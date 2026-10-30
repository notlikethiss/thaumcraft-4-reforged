package thaumcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.client.platform.HudVisibility;
import thaumcraft.registry.ModItems;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;

public final class SanityHud {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/gui/hud.png");

    private SanityHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || HudVisibility.hidden()) {
            return;
        }
        if (!player.getMainHandItem().is(ModItems.SANITY_CHECKER.get())) {
            return;
        }
        blit(graphics, 1, 1, 152, 0, 20, 76, 0xFFFFFFFF);
        float total = Warp.total(player);
        int permanent = Warp.get(player, WarpType.PERMANENT);
        int sticky = Warp.get(player, WarpType.STICKY);
        int temporary = Warp.get(player, WarpType.TEMPORARY);
        float scale = 1.0F;
        if (total > 100.0F) {
            scale = 100.0F / total;
            total = 100.0F;
        }
        int gap = (int) ((100.0F - total) / 100.0F * 48.0F);
        int temporaryHeight = (int) (temporary / 100.0F * 48.0F * scale);
        int stickyHeight = (int) (sticky / 100.0F * 48.0F * scale);
        if (temporary > 0) {
            blit(graphics, 7, 21 + gap, 200, gap, 8, temporaryHeight + gap, 0xFFFF80FF);
        }
        if (sticky > 0) {
            blit(graphics, 7, 21 + temporaryHeight + gap, 200, temporaryHeight + gap, 8, temporaryHeight + stickyHeight + gap, 0xFFBF00BF);
        }
        if (permanent > 0) {
            blit(graphics, 7, 21 + temporaryHeight + stickyHeight + gap, 200, temporaryHeight + stickyHeight + gap, 8, 48, 0xFF800080);
        }
        blit(graphics, 1, 1, 176, 0, 20, 76, 0xFFFFFFFF);
        if (total >= 100.0F) {
            blit(graphics, 1, 1, 216, 0, 20, 16, 0xFFFFFFFF);
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        GuiBlit.blit(graphics, TEXTURE, x, y, width, height, u, v, width, height, 256, 256, argb);
    }
}
