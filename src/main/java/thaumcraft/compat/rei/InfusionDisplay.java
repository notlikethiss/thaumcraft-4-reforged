package thaumcraft.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.recipeview.RecipeCatalog;

public final class InfusionDisplay extends BasicDisplay {
    public static final DisplaySerializer<InfusionDisplay> SERIALIZER = TcRei.byKey(key -> {
        InfusionRecipe recipe = RecipeCatalog.infusion(key);
        return recipe == null ? null : new InfusionDisplay(recipe);
    }, display -> display.recipe.key());

    private final InfusionRecipe recipe;

    public InfusionDisplay(InfusionRecipe recipe) {
        super(inputs(recipe), List.of(TcRei.ingredient(recipe.result())), Optional.empty());
        this.recipe = recipe;
    }

    private static List<EntryIngredient> inputs(InfusionRecipe recipe) {
        List<EntryIngredient> inputs = new ArrayList<>();
        inputs.add(TcRei.ingredient(recipe.central()));
        recipe.components().forEach(component -> inputs.add(TcRei.ingredient(component)));
        return inputs;
    }

    public InfusionRecipe recipe() {
        return recipe;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return TcRei.INFUSION;
    }

    @Override
    public DisplaySerializer<InfusionDisplay> getSerializer() {
        return SERIALIZER;
    }
}
