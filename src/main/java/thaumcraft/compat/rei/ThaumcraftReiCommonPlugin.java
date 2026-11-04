package thaumcraft.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REIServerPlugin;
import me.shedaniel.rei.forge.REIPluginCommon;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.Wands;

@REIPluginCommon
public final class ThaumcraftReiCommonPlugin implements REIServerPlugin {
    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(TcRei.CRUCIBLE, CrucibleDisplay.SERIALIZER);
        registry.register(TcRei.ARCANE, ArcaneDisplay.SERIALIZER);
        registry.register(TcRei.INFUSION, InfusionDisplay.SERIALIZER);
        registry.register(TcRei.INFUSION_ENCHANTMENT, EnchantmentDisplay.SERIALIZER);
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register((context, stack) -> Wands.getParts(stack).hashCode(), ModItems.WAND.get());
    }
}
