package thaumcraft.client.recipeview;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.aspect.AspectRenderer;

public final class AspectGrid {
    public static final int SIZE = 18;

    private AspectGrid() {
    }

    public static int rows(AspectList aspects, int perRow) {
        return aspects.size() == 0 ? 0 : (aspects.size() + perRow - 1) / perRow;
    }

    public static int x(int index, int count, int perRow, int width) {
        int inRow = Math.min(count, perRow);
        return (width - inRow * SIZE) / 2 + index % perRow * SIZE;
    }

    public static int y(int index, int perRow, int top) {
        return top + index / perRow * SIZE;
    }

    public static void draw(GuiGraphicsExtractor graphics, AspectList aspects, int perRow, int width, int left, int top) {
        List<Aspect> sorted = aspects.getAspectsSorted();
        for (int index = 0; index < sorted.size(); index++) {
            Aspect aspect = sorted.get(index);
            AspectRenderer.drawTag(graphics, left + x(index, sorted.size(), perRow, width), y(index, perRow, top), aspect, aspects.getAmount(aspect));
        }
    }

    public static @Nullable Aspect at(AspectList aspects, int perRow, int width, int top, double mouseX, double mouseY) {
        List<Aspect> sorted = aspects.getAspectsSorted();
        for (int index = 0; index < sorted.size(); index++) {
            int x = x(index, sorted.size(), perRow, width);
            int y = y(index, perRow, top);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return sorted.get(index);
            }
        }
        return null;
    }

    public static List<Component> tooltip(Aspect aspect) {
        return List.of(Component.literal(aspect.getName()), aspect.getDescription());
    }
}
