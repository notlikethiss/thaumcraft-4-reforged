package thaumcraft.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.AspectList;
import thaumcraft.research.ResearchGate;

public final class ArcaneCrafting {
    public static final int GRID_SIZE = 3;

    private static final List<DynamicArcaneRecipe> DYNAMIC = new CopyOnWriteArrayList<>(List.of(new ArcaneWandRecipe(), new ArcaneSceptreRecipe()));

    public record Match(ItemStack result, AspectList aspects) {
    }

    private ArcaneCrafting() {
    }

    public static void registerDynamic(DynamicArcaneRecipe recipe) {
        DYNAMIC.add(recipe);
    }

    public static @Nullable Match find(List<ItemStack> grid, Player player) {
        for (DynamicArcaneRecipe recipe : DYNAMIC) {
            if (recipe.matches(grid, player)) {
                return new Match(recipe.result(grid), recipe.aspects(grid));
            }
        }
        for (ArcaneRecipe recipe : TcRecipes.all(ArcaneRecipe.class)) {
            if (isUnlocked(recipe, player) && matches(recipe, grid)) {
                return new Match(recipe.result().create(), recipe.aspects().copy());
            }
        }
        return null;
    }

    public static AspectList findAspects(List<ItemStack> grid, Player player) {
        Match match = find(grid, player);
        return match == null ? new AspectList() : match.aspects();
    }

    private static boolean isUnlocked(ArcaneRecipe recipe, Player player) {
        return recipe.research().isEmpty() || ResearchGate.isComplete(player, recipe.research());
    }

    private static boolean matches(ArcaneRecipe recipe, List<ItemStack> grid) {
        if (recipe instanceof ShapedArcaneRecipe shaped) {
            return matchesShaped(shaped, grid);
        }
        return matchesShapeless((ShapelessArcaneRecipe) recipe, grid);
    }

    private static boolean matchesShaped(ShapedArcaneRecipe recipe, List<ItemStack> grid) {
        for (int offsetX = 0; offsetX <= GRID_SIZE - recipe.width(); offsetX++) {
            for (int offsetY = 0; offsetY <= GRID_SIZE - recipe.height(); offsetY++) {
                if (matchesShapedAt(recipe, grid, offsetX, offsetY, false) || matchesShapedAt(recipe, grid, offsetX, offsetY, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesShapedAt(ShapedArcaneRecipe recipe, List<ItemStack> grid, int offsetX, int offsetY, boolean mirror) {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                int subX = x - offsetX;
                int subY = y - offsetY;
                TcIngredient ingredient = TcIngredient.EMPTY;
                if (subX >= 0 && subY >= 0 && subX < recipe.width() && subY < recipe.height()) {
                    int column = mirror ? recipe.width() - subX - 1 : subX;
                    ingredient = recipe.grid().get(column + subY * recipe.width());
                }
                if (!ingredient.test(grid.get(x + y * GRID_SIZE))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchesShapeless(ShapelessArcaneRecipe recipe, List<ItemStack> grid) {
        List<TcIngredient> required = new ArrayList<>(recipe.ingredients());
        for (ItemStack stack : grid) {
            if (stack.isEmpty()) {
                continue;
            }
            boolean inRecipe = false;
            for (int index = 0; index < required.size(); index++) {
                if (required.get(index).test(stack)) {
                    required.remove(index);
                    inRecipe = true;
                    break;
                }
            }
            if (!inRecipe) {
                return false;
            }
        }
        return required.isEmpty();
    }
}
