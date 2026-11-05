package thaumcraft.recipeview;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jspecify.annotations.Nullable;
import thaumcraft.crafting.ArcaneRecipe;
import thaumcraft.crafting.ArcaneSceptreRecipe;
import thaumcraft.crafting.ArcaneWandRecipe;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.DynamicArcaneRecipe;
import thaumcraft.crafting.InfusionEnchantmentRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.WandCap;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.WandRod;
import thaumcraft.wand.Wands;

public final class RecipeCatalog {
    private RecipeCatalog() {
    }

    public static List<CrucibleRecipe> crucible() {
        return TcRecipes.all(CrucibleRecipe.class).stream().filter(recipe -> recipe.result().exists()).toList();
    }

    public static List<ArcaneView> arcane() {
        List<ArcaneView> views = new ArrayList<>(TcRecipes.all(ArcaneRecipe.class).stream()
            .filter(recipe -> recipe.result().exists()).map(ArcaneView::of).toList());
        views.addAll(wandRecipes());
        return views;
    }

    public static List<InfusionRecipe> infusion() {
        return TcRecipes.all(InfusionRecipe.class).stream().filter(recipe -> recipe.result().exists()).toList();
    }

    public static List<EnchantmentView> enchantments(HolderLookup.Provider registries) {
        List<EnchantmentView> views = new ArrayList<>();
        for (InfusionEnchantmentRecipe recipe : TcRecipes.all(InfusionEnchantmentRecipe.class)) {
            EnchantmentView view = enchantment(recipe, registries);
            if (view != null) {
                views.add(view);
            }
        }
        return views;
    }

    public static @Nullable CrucibleRecipe crucible(String key) {
        return TcRecipes.get(key) instanceof CrucibleRecipe recipe && recipe.result().exists() ? recipe : null;
    }

    public static @Nullable ArcaneView arcane(String key) {
        for (ArcaneView view : arcane()) {
            if (view.key().equals(key)) {
                return view;
            }
        }
        return null;
    }

    public static @Nullable InfusionRecipe infusion(String key) {
        return TcRecipes.get(key) instanceof InfusionRecipe recipe && recipe.result().exists() ? recipe : null;
    }

    public static @Nullable EnchantmentView enchantment(String key, HolderLookup.Provider registries) {
        return TcRecipes.get(key) instanceof InfusionEnchantmentRecipe recipe ? enchantment(recipe, registries) : null;
    }

    public static List<ItemStack> extraStacks() {
        List<ItemStack> wands = new ArrayList<>();
        wands.add(Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false));
        for (ArcaneView view : wandRecipes()) {
            wands.add(view.result());
        }
        return wands;
    }

    public static List<ItemStack> hiddenStacks() {
        return List.of(new ItemStack(ModBlocks.NODE_JAR_ITEM.get()));
    }

    private static @Nullable EnchantmentView enchantment(InfusionEnchantmentRecipe recipe, HolderLookup.Provider registries) {
        Holder<Enchantment> holder = registries.lookupOrThrow(Registries.ENCHANTMENT)
            .get(ResourceKey.create(Registries.ENCHANTMENT, recipe.enchantment())).orElse(null);
        if (holder == null) {
            return null;
        }
        List<ItemStack> books = new ArrayList<>();
        for (int level = 1; level <= holder.value().getMaxLevel(); level++) {
            books.add(EnchantmentHelper.createBook(new EnchantmentInstance(holder, level)));
        }
        return new EnchantmentView(recipe, books);
    }

    private static List<ArcaneView> wandRecipes() {
        List<ArcaneView> recipes = new ArrayList<>();
        for (WandCap cap : WandRegistry.caps()) {
            for (WandRod rod : WandRegistry.rods()) {
                ItemStack capStack = new ItemStack(BuiltInRegistries.ITEM.getValue(cap.item()));
                ItemStack rodStack = new ItemStack(BuiltInRegistries.ITEM.getValue(rod.item()));
                if (capStack.isEmpty() || rodStack.isEmpty()) {
                    continue;
                }
                for (boolean sceptre : new boolean[]{false, true}) {
                    List<TcIngredient> ingredients = new ArrayList<>();
                    List<ItemStack> grid = new ArrayList<>();
                    for (int slot = 0; slot < 9; slot++) {
                        ingredients.add(TcIngredient.EMPTY);
                        grid.add(ItemStack.EMPTY);
                    }
                    int[] caps = sceptre ? new int[]{1, 5, 6} : new int[]{2, 6};
                    for (int slot : caps) {
                        ingredients.set(slot, TcIngredient.item(cap.item().toString()));
                        grid.set(slot, capStack.copy());
                    }
                    ingredients.set(4, TcIngredient.item(rod.item().toString()));
                    grid.set(4, rodStack);
                    if (sceptre) {
                        TcIngredient charm = TcIngredient.item("thaumcraft:primal_charm");
                        ingredients.set(2, charm);
                        grid.set(2, new ItemStack(ModItems.PRIMAL_CHARM.get()));
                    }
                    DynamicArcaneRecipe recipe = sceptre ? new ArcaneSceptreRecipe() : new ArcaneWandRecipe();
                    ItemStack result = recipe.result(grid);
                    if (!result.isEmpty()) {
                        String key = (sceptre ? "sceptre:" : "wand:") + cap.tag() + ":" + rod.tag();
                        recipes.add(new ArcaneView(key, ingredients, 3, false, result, recipe.aspects(grid)));
                    }
                }
            }
        }
        return recipes;
    }
}
