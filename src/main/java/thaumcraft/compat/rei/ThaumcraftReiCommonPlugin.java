package thaumcraft.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.forge.REIPluginCommon;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.Wands;

@REIPluginCommon
public final class ThaumcraftReiCommonPlugin implements REICommonPlugin {
    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(Thaumcraft.id("crucible"), CrucibleDisplay.SERIALIZER);
        registry.register(Thaumcraft.id("arcane"), ArcaneDisplay.SERIALIZER);
        registry.register(Thaumcraft.id("infusion"), InfusionDisplay.SERIALIZER);
        registry.register(Thaumcraft.id("infusion_enchantment"), EnchantmentDisplay.SERIALIZER);
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register((context, stack) -> Wands.getParts(stack).hashCode(), ModItems.WAND.get());
    }
}
