package thaumcraft.client.jei;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.JeiWandSubtypes;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.recipeview.ArcaneView;
import thaumcraft.recipeview.EnchantmentView;
import thaumcraft.recipeview.RecipeCatalog;
import thaumcraft.registry.ModBlocks;

@JeiPlugin
public final class ThaumcraftJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThaumcraftJeiPlugin.class);

    @Override
    public Identifier getPluginUid() {
        return Thaumcraft.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        JeiWandSubtypes.register(registration);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(RecipeCatalog.extraStacks());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, RecipeCatalog.hiddenStacks());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new CrucibleCategory(helper),
            new ArcaneCategory(helper),
            new InfusionCategory(helper),
            new EnchantmentCategory(helper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<CrucibleRecipe> crucible = RecipeCatalog.crucible();
        List<ArcaneView> arcane = RecipeCatalog.arcane();
        List<InfusionRecipe> infusion = RecipeCatalog.infusion();
        List<EnchantmentView> enchantments = Minecraft.getInstance().level == null ? List.of()
            : RecipeCatalog.enchantments(Minecraft.getInstance().level.registryAccess());
        registration.addRecipes(CrucibleCategory.TYPE, crucible);
        registration.addRecipes(ArcaneCategory.TYPE, arcane);
        registration.addRecipes(InfusionCategory.TYPE, infusion);
        registration.addRecipes(EnchantmentCategory.TYPE, enchantments);
        LOGGER.info("JEI recipes: crucible {}, arcane {}, infusion {}, infusion enchantment {}", crucible.size(), arcane.size(), infusion.size(), enchantments.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(CrucibleCategory.TYPE, ModBlocks.CRUCIBLE.get());
        registration.addCraftingStation(ArcaneCategory.TYPE, ModBlocks.ARCANE_WORKTABLE.get());
        registration.addCraftingStation(InfusionCategory.TYPE, ModBlocks.RUNIC_MATRIX.get());
        registration.addCraftingStation(EnchantmentCategory.TYPE, ModBlocks.RUNIC_MATRIX.get());
    }
}
