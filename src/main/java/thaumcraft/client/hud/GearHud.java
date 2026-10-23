package thaumcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.fx.ParticleFx;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.item.armor.Hover;

public final class GearHud {
    private static int runicCharge;
    private static int runicMax;

    private GearHud() {
    }

    public static void setRunic(int charge, int max) {
        runicCharge = charge;
        runicMax = max;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        ItemStack harness = Hover.getHarness(player);
        if (!harness.isEmpty()) {
            renderHover(graphics, height, harness);
        }
        if (!player.isCreative() && runicCharge > 0 && runicMax > 0) {
            renderRunic(graphics, player, width, height);
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height, int argb) {
        if (width > 0 && height > 0) {
            GuiBlit.blit(graphics, ParticleFx.PARTICLES, x, y, width, height, u, v, width, height, 256, 256, argb);
        }
    }

    private static void renderRunic(GuiGraphicsExtractor graphics, Player player, int width, int height) {
        float fill = (float) runicCharge / runicMax;
        graphics.pose().pushMatrix();
        graphics.pose().translate(width / 2 - 91, height - 39);
        for (int index = 0; index < fill * 10.0F; index++) {
            blit(graphics, index * 8, 0, 160, 16, 9, 9, 0xFFFFFFFF);
            graphics.pose().pushMatrix();
            graphics.pose().scale(0.5F, 0.5F);
            float alpha = Mth.sin(player.tickCount / 4.0F + index) * 0.4F + 0.6F;
            blit(graphics, index * 16, 0, index * 16, 96, 16, 16, ((int) (alpha * 255.0F) << 24) | 0xFFBF3D);
            graphics.pose().popMatrix();
        }
        graphics.pose().popMatrix();
    }

    private static void renderHover(GuiGraphicsExtractor graphics, int height, ItemStack harness) {
        int fuel = Hover.getHarnessFuel(harness);
        int level = Math.round(fuel / 64.0F * 48.0F);
        blit(graphics, 6, height / 2 + 24 - level, 224, 48 - level, 8, level, 0xFF00FFBF);
        blit(graphics, 5, height / 2 - 28, 240, 0, 10, 56, 0xFFFFFFFF);
        if (Hover.isHovering(harness)) {
            blit(graphics, 2, height / 2 - 43, 16 * ((int) (Util.getMillis() % 700L) / 50), 32, 16, 16, 0xA8FFFFFF);
        }
        graphics.item(harness, 2, height / 2 - 43);
    }
}
