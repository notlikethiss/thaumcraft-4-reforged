package thaumcraft.client.rei;

import me.shedaniel.rei.api.client.entry.filtering.base.BasicFilteringRule;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.ItemStack;
import thaumcraft.compat.rei.ArcaneDisplay;
import thaumcraft.compat.rei.CrucibleDisplay;
import thaumcraft.compat.rei.EnchantmentDisplay;
import thaumcraft.compat.rei.InfusionDisplay;
import thaumcraft.compat.rei.TcRei;
import thaumcraft.recipeview.RecipeCatalog;
import thaumcraft.registry.ModBlocks;

@REIPluginClient
public final class ThaumcraftReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new CrucibleCategory(), new ArcaneCategory(), new InfusionCategory.Infusion(), new InfusionCategory.Enchantment());
        registry.addWorkstations(TcRei.CRUCIBLE, EntryStacks.of(ModBlocks.CRUCIBLE.get()));
        registry.addWorkstations(TcRei.ARCANE, EntryStacks.of(ModBlocks.ARCANE_WORKTABLE.get()));
        registry.addWorkstations(TcRei.INFUSION, EntryStacks.of(ModBlocks.RUNIC_MATRIX.get()));
        registry.addWorkstations(TcRei.INFUSION_ENCHANTMENT, EntryStacks.of(ModBlocks.RUNIC_MATRIX.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        RecipeCatalog.crucible().forEach(recipe -> registry.add(new CrucibleDisplay(recipe)));
        RecipeCatalog.arcane().forEach(view -> registry.add(new ArcaneDisplay(view)));
        RecipeCatalog.infusion().forEach(recipe -> registry.add(new InfusionDisplay(recipe)));
        RecipeCatalog.enchantments(BasicDisplay.registryAccess()).forEach(view -> registry.add(new EnchantmentDisplay(view)));
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        for (ItemStack stack : RecipeCatalog.extraStacks()) {
            EntryStack<ItemStack> entry = EntryStacks.of(stack);
            if (!registry.alreadyContain(entry)) {
                registry.addEntry(entry);
            }
        }
    }

    @Override
    public void registerBasicEntryFiltering(BasicFilteringRule<?> rule) {
        rule.hide(() -> RecipeCatalog.hiddenStacks().stream().<EntryStack<?>>map(EntryStacks::of).toList());
    }
}
