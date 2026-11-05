package thaumcraft.client.rei;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.recipeview.AspectGrid;

abstract class TcReiCategory<D extends Display> implements DisplayCategory<D> {
    private static final int PADDING = 5;

    private final CategoryIdentifier<D> id;
    private final String title;
    private final Renderer icon;
    private final int width;
    private final int height;

    TcReiCategory(CategoryIdentifier<D> id, String title, ItemLike icon, int width, int height) {
        this.id = id;
        this.title = title;
        this.icon = EntryStacks.of(icon);
        this.width = width;
        this.height = height;
    }

    abstract void addWidgets(D display, Point origin, List<Widget> widgets);

    @Override
    public CategoryIdentifier<? extends D> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(title);
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayWidth(D display) {
        return width + PADDING * 2;
    }

    @Override
    public int getDisplayHeight() {
        return height + PADDING * 2;
    }

    @Override
    public List<Widget> setupDisplay(D display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        addWidgets(display, new Point(bounds.x + PADDING, bounds.y + PADDING), widgets);
        return widgets;
    }

    static void input(List<Widget> widgets, Point origin, int x, int y, EntryIngredient entries) {
        widgets.add(Widgets.createSlot(new Point(origin.x + x, origin.y + y)).entries(entries).markInput());
    }

    static void output(List<Widget> widgets, Point origin, int x, int y, EntryIngredient entries) {
        widgets.add(Widgets.createSlot(new Point(origin.x + x, origin.y + y)).entries(entries).markOutput());
    }

    static void aspects(List<Widget> widgets, Point origin, AspectList aspects, int perRow, int width, int top) {
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
            AspectGrid.draw(graphics, aspects, perRow, width, origin.x, origin.y + top)));
        List<Aspect> sorted = aspects.getAspectsSorted();
        for (int index = 0; index < sorted.size(); index++) {
            int x = origin.x + AspectGrid.x(index, sorted.size(), perRow, width);
            int y = origin.y + AspectGrid.y(index, perRow, top);
            widgets.add(Widgets.createTooltip(new Rectangle(x, y, 16, 16), AspectGrid.tooltip(sorted.get(index))));
        }
    }
}
