package thaumcraft.client.rei;

import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import thaumcraft.compat.rei.ArcaneDisplay;
import thaumcraft.compat.rei.TcRei;
import thaumcraft.recipeview.ArcaneView;
import thaumcraft.registry.ModBlocks;

final class ArcaneCategory extends TcReiCategory<ArcaneDisplay> {
    private static final int WIDTH = 130;
    private static final int PER_ROW = 7;
    private static final int COST_Y = 60;

    ArcaneCategory() {
        super(TcRei.ARCANE, "recipe.type.arcane", ModBlocks.ARCANE_WORKTABLE.get(), WIDTH, COST_Y + 20);
    }

    @Override
    void addWidgets(ArcaneDisplay display, Point origin, List<Widget> widgets) {
        ArcaneView view = display.view();
        List<EntryIngredient> inputs = display.getInputEntries();
        for (int index = 0; index < inputs.size() && index < 9; index++) {
            input(widgets, origin, index % view.width() * 18, index / view.width() * 18, inputs.get(index));
        }
        widgets.add(Widgets.createArrow(new Point(origin.x + 70, origin.y + 18)));
        output(widgets, origin, 104, 18, display.getOutputEntries().getFirst());
        if (view.shapeless()) {
            widgets.add(Widgets.createShapelessIcon(new Rectangle(origin.x, origin.y, WIDTH, COST_Y)));
        }
        aspects(widgets, origin, view.aspects(), PER_ROW, WIDTH, COST_Y);
    }
}
