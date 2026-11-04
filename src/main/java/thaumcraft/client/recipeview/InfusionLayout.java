package thaumcraft.client.recipeview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class InfusionLayout {
    public static final int WIDTH = 130;
    public static final int HEIGHT = 102 + 18 * 3 + 12;
    public static final int CENTER_X = 49;
    public static final int CENTER_Y = 49;
    public static final int PER_ROW = 5;
    public static final int ASPECTS_Y = 102;
    public static final int OUTPUT_X = WIDTH - 20;
    public static final int OUTPUT_Y = 0;

    private InfusionLayout() {
    }

    public static int componentX(int index, int count) {
        return CENTER_X + (int) (Mth.cos(angle(index, count) / 180.0F * Mth.PI) * 40.0F) - 8;
    }

    public static int componentY(int index, int count) {
        return CENTER_Y + (int) (Mth.sin(angle(index, count) / 180.0F * Mth.PI) * 40.0F) - 8;
    }

    private static float angle(int index, int count) {
        float slice = count == 0 ? 0.0F : 360 / count;
        return -90.0F + slice * index;
    }

    public static void instability(GuiGraphicsExtractor graphics, int instability, int left, int top) {
        Language language = Language.getInstance();
        int level = Math.min(5, instability / 2);
        Component text = Component.literal(language.getOrDefault("tc.inst") + " " + language.getOrDefault("tc.inst." + level));
        Font font = Minecraft.getInstance().font;
        graphics.text(font, text, left + (WIDTH - font.width(text)) / 2, top + HEIGHT - 10, 0xFF404040, false);
    }
}
