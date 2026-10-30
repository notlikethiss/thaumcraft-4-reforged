package thaumcraft.client.rei;

import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import thaumcraft.compat.rei.CrucibleDisplay;
import thaumcraft.compat.rei.TcRei;
import thaumcraft.registry.ModBlocks;

final class CrucibleCategory extends TcReiCategory<CrucibleDisplay> {
    private static final int WIDTH = 150;
    private static final int PER_ROW = 8;
    private static final int ASPECTS_Y = 30;

    CrucibleCategory() {
        super(TcRei.CRUCIBLE, "recipe.type.crucible", ModBlocks.CRUCIBLE.get(), WIDTH, ASPECTS_Y + 18 * 2 + 2);
    }

    @Override
    void addWidgets(CrucibleDisplay display, Point origin, List<Widget> widgets) {
        input(widgets, origin, 30, 4, display.getInputEntries().getFirst());
        widgets.add(Widgets.createArrow(new Point(origin.x + 62, origin.y + 4)));
        output(widgets, origin, 100, 4, display.getOutputEntries().getFirst());
        aspects(widgets, origin, display.recipe().aspects(), PER_ROW, WIDTH, ASPECTS_Y);
    }
}
