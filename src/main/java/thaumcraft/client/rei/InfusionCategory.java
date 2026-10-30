package thaumcraft.client.rei;

import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.recipeview.InfusionLayout;
import thaumcraft.compat.rei.EnchantmentDisplay;
import thaumcraft.compat.rei.InfusionDisplay;
import thaumcraft.compat.rei.TcRei;
import thaumcraft.registry.ModBlocks;

abstract class InfusionCategory<D extends Display> extends TcReiCategory<D> {
    InfusionCategory(CategoryIdentifier<D> id, String title) {
        super(id, title, ModBlocks.RUNIC_MATRIX.get(), InfusionLayout.WIDTH, InfusionLayout.HEIGHT);
    }

    abstract AspectList aspects(D display);

    abstract int instability(D display);

    @Override
    void addWidgets(D display, Point origin, List<Widget> widgets) {
        List<EntryIngredient> inputs = display.getInputEntries();
        input(widgets, origin, InfusionLayout.CENTER_X - 8, InfusionLayout.CENTER_Y - 8, inputs.getFirst());
        int count = inputs.size() - 1;
        for (int index = 0; index < count; index++) {
            input(widgets, origin, InfusionLayout.componentX(index, count), InfusionLayout.componentY(index, count), inputs.get(index + 1));
        }
        output(widgets, origin, InfusionLayout.OUTPUT_X, InfusionLayout.OUTPUT_Y, display.getOutputEntries().getFirst());
        aspects(widgets, origin, aspects(display), InfusionLayout.PER_ROW, InfusionLayout.WIDTH, InfusionLayout.ASPECTS_Y);
        int instability = instability(display);
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
            InfusionLayout.instability(graphics, instability, origin.x, origin.y)));
    }

    static final class Infusion extends InfusionCategory<InfusionDisplay> {
        Infusion() {
            super(TcRei.INFUSION, "recipe.type.infusion");
        }

        @Override
        AspectList aspects(InfusionDisplay display) {
            return display.recipe().aspects();
        }

        @Override
        int instability(InfusionDisplay display) {
            return display.recipe().instability();
        }
    }

    static final class Enchantment extends InfusionCategory<EnchantmentDisplay> {
        Enchantment() {
            super(TcRei.INFUSION_ENCHANTMENT, "recipe.type.infusionenchantment");
        }

        @Override
        AspectList aspects(EnchantmentDisplay display) {
            return display.view().recipe().aspects();
        }

        @Override
        int instability(EnchantmentDisplay display) {
            return display.view().recipe().instability();
        }
    }
}
