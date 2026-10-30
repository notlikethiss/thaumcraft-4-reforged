package thaumcraft.client.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.recipeview.AspectGrid;
import thaumcraft.client.recipeview.InfusionLayout;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;

final class TcJei {
    private TcJei() {
    }

    static void accept(IIngredientAcceptor<?> acceptor, TcIngredient ingredient) {
        List<ItemStack> stacks = ingredient.displayStacks();
        if (!stacks.isEmpty()) {
            acceptor.addItemStacks(stacks);
        }
    }

    static void accept(IIngredientAcceptor<?> acceptor, TcStack stack) {
        ItemStack created = stack.create();
        if (!created.isEmpty()) {
            acceptor.add(created);
        }
    }

    static void components(IRecipeLayoutBuilder builder, List<TcIngredient> components) {
        for (int index = 0; index < components.size(); index++) {
            int x = InfusionLayout.componentX(index, components.size());
            int y = InfusionLayout.componentY(index, components.size());
            accept(builder.addInputSlot(x, y).setStandardSlotBackground(), components.get(index));
        }
    }

    static void aspectTooltip(ITooltipBuilder tooltip, AspectList aspects, int perRow, int width, int top, double mouseX, double mouseY) {
        Aspect aspect = AspectGrid.at(aspects, perRow, width, top, mouseX, mouseY);
        if (aspect != null) {
            AspectGrid.tooltip(aspect).forEach(tooltip::add);
        }
    }
}
