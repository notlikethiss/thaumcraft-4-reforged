package thaumcraft.recipeview;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ArcaneRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;

public record ArcaneView(String key, List<TcIngredient> ingredients, int width, boolean shapeless, ItemStack result, AspectList aspects) {
    public ArcaneView {
        ingredients = List.copyOf(ingredients);
    }

    static ArcaneView of(ArcaneRecipe recipe) {
        if (recipe instanceof ShapedArcaneRecipe shaped) {
            return new ArcaneView(recipe.key(), shaped.grid(), shaped.width(), false, recipe.result().create(), recipe.aspects());
        }
        return new ArcaneView(recipe.key(), ((ShapelessArcaneRecipe) recipe).ingredients(), 3, true, recipe.result().create(), recipe.aspects());
    }
}
