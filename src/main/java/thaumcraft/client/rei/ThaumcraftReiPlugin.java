package thaumcraft.client.rei;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import thaumcraft.client.rei.TcReiDisplay.Kind;
import thaumcraft.crafting.ArcaneRecipe;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.InfusionEnchantmentRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.registry.ModBlocks;

@REIPluginClient
public final class ThaumcraftReiPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new TcReiCategory(TcReiDisplay.CRUCIBLE, "recipe.type.crucible", ModBlocks.CRUCIBLE.get(), Kind.CRUCIBLE));
        registry.add(new TcReiCategory(TcReiDisplay.ARCANE, "recipe.type.arcane", ModBlocks.ARCANE_WORKTABLE.get(), Kind.ARCANE));
        registry.add(new TcReiCategory(TcReiDisplay.INFUSION, "recipe.type.infusion", ModBlocks.RUNIC_MATRIX.get(), Kind.INFUSION));
        registry.add(new TcReiCategory(TcReiDisplay.ENCHANTMENT, "recipe.type.infusionenchantment", ModBlocks.RUNIC_MATRIX.get(), Kind.ENCHANTMENT));
        registry.addWorkstations(TcReiDisplay.CRUCIBLE, EntryStacks.of(ModBlocks.CRUCIBLE.get()));
        registry.addWorkstations(TcReiDisplay.ARCANE, EntryStacks.of(ModBlocks.ARCANE_WORKTABLE.get()));
        registry.addWorkstations(TcReiDisplay.INFUSION, EntryStacks.of(ModBlocks.RUNIC_MATRIX.get()));
        registry.addWorkstations(TcReiDisplay.ENCHANTMENT, EntryStacks.of(ModBlocks.RUNIC_MATRIX.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        TcRecipes.all(CrucibleRecipe.class).stream()
            .filter(recipe -> recipe.result().exists())
            .map(recipe -> new TcReiDisplay(
                TcReiDisplay.CRUCIBLE,
                Kind.CRUCIBLE,
                List.of(TcReiDisplay.ingredient(recipe.catalyst().displayStacks())),
                List.of(TcReiDisplay.ingredient(recipe.result().create())),
                recipe.aspects(),
                0,
                0,
                0
            ))
            .forEach(registry::add);

        for (ArcaneRecipe recipe : TcRecipes.all(ArcaneRecipe.class)) {
            if (!recipe.result().exists()) {
                continue;
            }
            List<EntryIngredient> inputs = new ArrayList<>();
            int gridWidth = 3;
            int gridHeight;
            if (recipe instanceof ShapedArcaneRecipe shaped) {
                gridWidth = shaped.width();
                gridHeight = shaped.height();
                for (TcIngredient ingredient : shaped.grid()) {
                    inputs.add(TcReiDisplay.ingredient(ingredient.displayStacks()));
                }
            } else {
                var ingredients = ((ShapelessArcaneRecipe) recipe).ingredients();
                gridHeight = (ingredients.size() + 2) / 3;
                for (TcIngredient ingredient : ingredients) {
                    inputs.add(TcReiDisplay.ingredient(ingredient.displayStacks()));
                }
            }
            registry.add(new TcReiDisplay(
                TcReiDisplay.ARCANE,
                Kind.ARCANE,
                inputs,
                List.of(TcReiDisplay.ingredient(recipe.result().create())),
                recipe.aspects(),
                0,
                gridWidth,
                gridHeight
            ));
        }

        TcRecipes.all(InfusionRecipe.class).stream()
            .filter(recipe -> recipe.result().exists())
            .map(recipe -> new TcReiDisplay(
                TcReiDisplay.INFUSION,
                Kind.INFUSION,
                infusionInputs(TcReiDisplay.ingredient(recipe.central().displayStacks()), recipe.components()),
                List.of(TcReiDisplay.ingredient(recipe.result().create())),
                recipe.aspects(),
                recipe.instability(),
                0,
                0
            ))
            .forEach(registry::add);

        for (InfusionEnchantmentRecipe recipe : TcRecipes.all(InfusionEnchantmentRecipe.class)) {
            List<ItemStack> books = enchantmentBooks(recipe);
            if (!books.isEmpty()) {
                registry.add(new TcReiDisplay(
                    TcReiDisplay.ENCHANTMENT,
                    Kind.ENCHANTMENT,
                    infusionInputs(TcReiDisplay.ingredient(new ItemStack(Items.BOOK)), recipe.components()),
                    List.of(TcReiDisplay.ingredientChoices(books)),
                    recipe.aspects(),
                    recipe.instability(),
                    0,
                    0
                ));
            }
        }
    }

    private static List<EntryIngredient> infusionInputs(EntryIngredient center, List<TcIngredient> components) {
        List<EntryIngredient> inputs = new ArrayList<>(components.size() + 1);
        inputs.add(center);
        for (TcIngredient component : components) {
            inputs.add(TcReiDisplay.ingredient(component.displayStacks()));
        }
        return inputs;
    }

    private static List<ItemStack> enchantmentBooks(InfusionEnchantmentRecipe recipe) {
        if (Minecraft.getInstance().level == null) {
            return List.of();
        }
        HolderLookup.RegistryLookup<Enchantment> lookup = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, recipe.enchantment())).orElse(null);
        if (holder == null) {
            return List.of();
        }
        List<ItemStack> books = new ArrayList<>();
        for (int level = 1; level <= holder.value().getMaxLevel(); level++) {
            books.add(EnchantmentHelper.createBook(new EnchantmentInstance(holder, level)));
        }
        return books;
    }
}
