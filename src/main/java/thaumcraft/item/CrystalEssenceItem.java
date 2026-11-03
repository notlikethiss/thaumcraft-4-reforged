package thaumcraft.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;

public class CrystalEssenceItem extends AspectContainerItem {
    public CrystalEssenceItem(Properties properties) {
        super(properties, " x", true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        if (level.isClientSide()) {
            return;
        }
        if (!AspectKeys.ASPECTS.has(stack)) {
            Aspect[] aspects = Aspect.values();
            AspectTooltips.setSingle(stack, aspects[level.getRandom().nextInt(aspects.length)], 1);
        }
        super.inventoryTick(stack, level, owner, slotId, isSelected);
    }
}
