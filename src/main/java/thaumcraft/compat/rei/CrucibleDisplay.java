package thaumcraft.compat.rei;

import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.recipeview.RecipeCatalog;

public final class CrucibleDisplay extends BasicDisplay {
    public static final DisplaySerializer<CrucibleDisplay> SERIALIZER = TcRei.byKey(key -> {
        CrucibleRecipe recipe = RecipeCatalog.crucible(key);
        return recipe == null ? null : new CrucibleDisplay(recipe);
    }, display -> display.recipe.key());

    private final CrucibleRecipe recipe;

    public CrucibleDisplay(CrucibleRecipe recipe) {
        super(List.of(TcRei.ingredient(recipe.catalyst())), List.of(TcRei.ingredient(recipe.result())), Optional.empty());
        this.recipe = recipe;
    }

    public CrucibleRecipe recipe() {
        return recipe;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return TcRei.CRUCIBLE;
    }

    @Override
    public DisplaySerializer<CrucibleDisplay> getSerializer() {
        return SERIALIZER;
    }
}
