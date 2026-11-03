package thaumcraft.client.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import javax.annotation.Nullable;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.crafting.ArcaneRecipe;
import thaumcraft.crafting.CompoundRecipe;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.InfusionEnchantmentRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;

final class PageRecipes {
    private static final int XP_COLOR = 0xFF508850;
    private static final int ENCHANT_COLOR = 0xFF705090;

    private PageRecipes() {
    }

    private record Grid(boolean shaped, int width, int height, List<List<ItemStack>> slots, List<ItemStack> output) {
    }

    private static List<ItemStack> stacks(TcStack stack) {
        ItemStack created = stack.create();
        return created.isEmpty() ? List.of() : List.of(created);
    }

    private static List<List<ItemStack>> ingredients(List<TcIngredient> ingredients) {
        List<List<ItemStack>> slots = new ArrayList<>();
        for (TcIngredient ingredient : ingredients) {
            slots.add(ingredient.displayStacks());
        }
        return slots;
    }

    private static List<List<ItemStack>> resolve(List<Ingredient> ingredients) {
        List<List<ItemStack>> slots = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            slots.add(List.of(ingredient.getItems()));
        }
        return slots;
    }

    private static @Nullable Grid vanilla(PageRenderer renderer, CraftingRecipeRef reference) {
        if (renderer.minecraft.level == null) {
            return null;
        }
        RecipeHolder<?> holder = ClientRecipes.get(reference.recipe());
        if (holder == null) {
            return null;
        }
        RegistryAccess registries = renderer.minecraft.level.registryAccess();
        if (holder.value() instanceof ShapedRecipe shaped) {
            return new Grid(true, shaped.getWidth(), shaped.getHeight(), resolve(shaped.getIngredients()), List.of(shaped.getResultItem(registries)));
        }
        if (holder.value() instanceof ShapelessRecipe shapeless) {
            return new Grid(false, 3, 3, resolve(shapeless.getIngredients()), List.of(shapeless.getResultItem(registries)));
        }
        return null;
    }

    private static void grid(PageRenderer renderer, Grid grid, int x, int y) {
        if (grid.shaped()) {
            for (int column = 0; column < grid.width() && column < 3; column++) {
                for (int row = 0; row < grid.height() && row < 3; row++) {
                    int index = column + row * grid.width();
                    if (index < grid.slots().size()) {
                        renderer.slot(grid.slots().get(index), x + 16 + column * 32, y + row * 32, false, true);
                    }
                }
            }
        } else {
            for (int index = 0; index < grid.slots().size() && index < 9; index++) {
                renderer.slot(grid.slots().get(index), x + 16 + index % 3 * 32, y + index / 3 * 32, false, true);
            }
        }
    }

    static void crafting(PageRenderer renderer, int side, int x, int y, ResearchPage.Crafting page) {
        if (page.recipes().isEmpty() || !(TcRecipes.get(page.recipes().get(renderer.index(page.recipes().size()))) instanceof CraftingRecipeRef reference)) {
            return;
        }
        Grid grid = vanilla(renderer, reference);
        int start = side * 152;
        boolean shaped = grid == null || grid.shaped();
        renderer.title(shaped ? "recipe.type.workbench" : "recipe.type.workbenchshapeless", x + start, y);
        renderer.overlay(x + start, y, 2, 32, 60, 15, 52, 52, 1.0F);
        renderer.overlay(x + start, y, 20, 12, 20, 3, 16, 16, 1.0F);
        renderer.slot(grid == null ? stacks(reference.result()) : grid.output(), x + 48 + start, y + 32, true, false);
        if (grid != null) {
            grid(renderer, grid, x + start, y + 76);
        }
    }

    static void arcane(PageRenderer renderer, int side, int x, int y, ResearchPage.Arcane page) {
        if (page.recipes().isEmpty() || !(TcRecipes.get(page.recipes().get(renderer.index(page.recipes().size()))) instanceof ArcaneRecipe recipe)) {
            return;
        }
        int start = side * 152;
        renderer.overlay(x + start, y, 2, 27, 112, 15, 52, 52, 1.0F);
        renderer.overlay(x + start, y, 20, 7, 20, 3, 16, 16, 1.0F);
        renderer.overlay(x + start, y + 164, 0, 0, 68, 76, 12, 12, 0.4F);
        AspectList tags = recipe.aspects();
        if (tags.size() > 0) {
            renderer.costLine(tags, x + start, y + 172);
        }
        renderer.slot(stacks(recipe.result()), x + 48 + start, y + 22, true, false);
        renderer.title("recipe.type.arcane", x + start, y);
        if (recipe instanceof ShapedArcaneRecipe shaped) {
            for (int column = 0; column < shaped.width() && column < 3; column++) {
                for (int row = 0; row < shaped.height() && row < 3; row++) {
                    int index = column + row * shaped.width();
                    if (index < shaped.grid().size()) {
                        renderer.slot(shaped.grid().get(index).displayStacks(), x + start + 16 + column * 32, y + 66 + row * 32, false, true);
                    }
                }
            }
        } else if (recipe instanceof ShapelessArcaneRecipe shapeless) {
            List<TcIngredient> items = shapeless.ingredients();
            for (int index = 0; index < items.size() && index < 9; index++) {
                renderer.slot(items.get(index).displayStacks(), x + start + 16 + index % 3 * 32, y + 66 + index / 3 * 32, false, true);
            }
        }
    }

    static void crucible(PageRenderer renderer, int side, int x, int y, ResearchPage.Crucible page) {
        if (page.recipes().isEmpty() || !(TcRecipes.get(page.recipes().get(renderer.index(page.recipes().size()))) instanceof CrucibleRecipe recipe)) {
            return;
        }
        int start = side * 152;
        renderer.title("recipe.type.crucible", x + start, y);
        renderer.graphics.pose().pushPose();
        renderer.graphics.pose().translate(x + start, y + 28, 0.0F);
        renderer.graphics.pose().scale(2.0F, 2.0F, 1.0F);
        blit(renderer, 0, 0, 0, 3, 56, 17);
        blit(renderer, 0, 32, 0, 20, 56, 48);
        blit(renderer, 21, 24, 100, 84, 11, 13);
        renderer.graphics.pose().popPose();
        renderer.aspectRows(recipe.aspects(), 3, x + start + 28, y + 96 + 32, 1);
        renderer.slot(stacks(recipe.result()), x + 48 + start, y + 36, true, false);
        renderer.slot(recipe.catalyst().displayStacks(), x + 26 + start, y + 72, false, true);
    }

    static void smelting(PageRenderer renderer, int side, int x, int y, ResearchPage.Smelting page) {
        int start = side * 152;
        renderer.title("recipe.type.smelting", x + start, y);
        renderer.overlay(x + start, y + 28, 0, 0, 0, 192, 56, 64, 1.0F);
        renderer.slot(stacks(page.input()), x + 48 + start, y + 64, false, true);
        renderer.slot(stacks(page.output()), x + 48 + start, y + 144, true, false);
    }

    private static void blit(PageRenderer renderer, int drawX, int drawY, int u, int v, int width, int height) {
        GuiBlit.blit(renderer.graphics, PageRenderer.OVERLAY, drawX, drawY, width, height, u, v, width, height, 256, 256, 0xFFFFFFFF);
    }

    private static List<int[]> circle(int count) {
        List<int[]> coordinates = new ArrayList<>();
        float slice = 360 / count;
        float rotation = -90.0F;
        for (int index = 0; index < count; index++) {
            int offsetX = (int) (Mth.cos(rotation / 180.0F * Mth.PI) * 40.0F) - 8;
            int offsetY = (int) (Mth.sin(rotation / 180.0F * Mth.PI) * 40.0F) - 8;
            rotation += slice;
            coordinates.add(new int[] {offsetX, offsetY});
        }
        return coordinates;
    }

    private static void components(PageRenderer renderer, List<TcIngredient> components, int x, int y) {
        if (components.isEmpty()) {
            return;
        }
        List<int[]> coordinates = circle(components.size());
        for (int index = 0; index < components.size(); index++) {
            renderer.slot(components.get(index).displayStacks(), x + coordinates.get(index)[0], y + coordinates.get(index)[1], false, true);
        }
    }

    static void infusion(PageRenderer renderer, int side, int x, int y, ResearchPage.Infusion page) {
        if (page.recipes().isEmpty() || !(TcRecipes.get(page.recipes().get(renderer.index(page.recipes().size()))) instanceof InfusionRecipe recipe)) {
            return;
        }
        int start = side * 152;
        renderer.title("recipe.type.infusion", x + start, y);
        renderer.instability(recipe.instability(), x + start, y + 194);
        renderer.graphics.pose().pushPose();
        renderer.graphics.pose().translate(x + start, y + 20, 0.0F);
        renderer.graphics.pose().scale(2.0F, 2.0F, 1.0F);
        blit(renderer, 0, 0, 0, 3, 56, 17);
        blit(renderer, 0, 19, 200, 77, 60, 44);
        renderer.graphics.pose().popPose();
        renderer.aspectRows(recipe.aspects(), 5, x + start + 8, y + 164, 1);
        renderer.slot(stacks(recipe.result()), x + 48 + start, y + 28, true, false);
        renderer.slot(recipe.central().displayStacks(), x + 48 + start, y + 94, false, true);
        components(renderer, recipe.components(), x + 56 + start, y + 102);
    }

    static void infusionEnchantment(PageRenderer renderer, int side, int x, int y, ResearchPage.InfusionEnchantment page) {
        if (!(TcRecipes.get(page.recipe()) instanceof InfusionEnchantmentRecipe recipe)) {
            return;
        }
        ResourceLocation id = recipe.enchantment();
        Holder<Enchantment> holder = renderer.minecraft.level == null ? null : renderer.minecraft.level.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .get(ResourceKey.create(Registries.ENCHANTMENT, id))
            .orElse(null);
        int maxLevel = holder == null ? 1 : holder.value().getMaxLevel();
        int experience = holder == null ? 1 : Math.max(1, holder.value().getMinCost(1) / 3);
        int level = (int) (1L + System.currentTimeMillis() / 1000L % maxLevel);
        Component name = holder == null ? Component.translatable(id.toLanguageKey("enchantment")) : Component.literal(Enchantment.getFullname(holder, level).getString());
        int start = side * 152;
        renderer.title("recipe.type.infusionenchantment", x + start, y);
        renderer.instability(recipe.instability(), x + start, y + 194);
        renderer.centered(name, x + start + 56, y + 24, ENCHANT_COLOR);
        renderer.centered(Component.literal(experience * level + " levels"), x + start + 56, y + 40, XP_COLOR);
        renderer.graphics.pose().pushPose();
        renderer.graphics.pose().translate(x + start, y + 20, 0.0F);
        renderer.graphics.pose().scale(2.0F, 2.0F, 1.0F);
        blit(renderer, 0, 19, 200, 77, 60, 44);
        renderer.graphics.pose().popPose();
        renderer.aspectRows(recipe.aspects(), 5, x + start + 8, y + 164, level);
        components(renderer, recipe.components(), x + 56 + start, y + 102);
    }

    static void compound(PageRenderer renderer, int side, int x, int y, ResearchPage.Compound page) {
        if (!(TcRecipes.get(page.recipe()) instanceof CompoundRecipe recipe)) {
            return;
        }
        int dx = recipe.width();
        int dy = recipe.height();
        int dz = recipe.depth();
        int xoff = 64 - (dx * 16 + dz * 16) / 2;
        int yoff = -dy * 25;
        int start = side * 152;
        renderer.title("recipe.type.construct", x + start, y);
        AspectList aspects = recipe.aspects();
        if (aspects.size() > 0) {
            renderer.costLine(aspects, x + start, y + 182);
            renderer.overlay(x + start, y + 174, 0, 0, 68, 76, 12, 12, 0.4F);
        }
        float shrink = dy > 3 ? (dy - 3) * 0.2F : 0.0F;
        float originX = x + start + xoff * (1.0F + shrink);
        float originY = y + 108 + yoff * (1.0F - shrink);
        renderer.graphics.pose().pushPose();
        renderer.graphics.pose().translate(originX, originY, 0.0F);
        renderer.graphics.pose().scale(1.0F - shrink, 1.0F - shrink, 1.0F);
        renderer.graphics.pose().pushPose();
        renderer.graphics.pose().translate(-8 - xoff, -119 + Math.max(3 - dx, 3 - dz) * 8 + dx * 4 + dz * 4 + dy * 50, 0.0F);
        renderer.graphics.pose().scale(2.0F, 2.0F, 1.0F);
        GuiBlit.blit(renderer.graphics, PageRenderer.OVERLAY, 0, 0, 64, 44, 0, 72, 64, 44, 256, 256, 0x80FFFFFF);
        renderer.graphics.pose().popPose();
        List<TcStack> blocks = recipe.blocks();
        int count = 0;
        for (int layer = 0; layer < dy; layer++) {
            for (int depth = dz - 1; depth >= 0; depth--) {
                for (int column = dx - 1; column >= 0; column--) {
                    ItemStack stack = count < blocks.size() ? renderer.variant(stacks(blocks.get(count))) : ItemStack.EMPTY;
                    if (!stack.isEmpty()) {
                        renderer.graphics.renderItem(stack, column * 16 + depth * 16, -column * 8 + depth * 8 + layer * 50);
                    }
                    count++;
                }
            }
        }
        renderer.graphics.pose().popPose();
        count = 0;
        float size = 16.0F * (1.0F - shrink);
        for (int layer = 0; layer < dy; layer++) {
            for (int depth = dz - 1; depth >= 0; depth--) {
                for (int column = dx - 1; column >= 0; column--) {
                    int px = (int) (x + start + xoff * (1.0F + shrink) + column * 16 * (1.0F - shrink) + depth * 16 * (1.0F - shrink));
                    int py = (int) (y + 108 + yoff * (1.0F - shrink) - column * 8 * (1.0F - shrink) + depth * 8 * (1.0F - shrink) + layer * 50 * (1.0F - shrink));
                    ItemStack stack = count < blocks.size() ? renderer.variant(stacks(blocks.get(count))) : ItemStack.EMPTY;
                    if (!stack.isEmpty() && renderer.mouseX >= px && renderer.mouseY >= py && renderer.mouseX < px + size && renderer.mouseY < py + size) {
                        renderer.hover(stack, px, py, true);
                    }
                    count++;
                }
            }
        }
    }
}
