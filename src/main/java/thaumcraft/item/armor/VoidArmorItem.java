package thaumcraft.item.armor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import thaumcraft.platform.ArmorSlot;
import thaumcraft.platform.TcArmorMaterial;
import thaumcraft.platform.Registration;
import net.minecraft.world.item.ItemStack;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.RunicArmor;
import thaumcraft.item.gear.WarpingGear;

public class VoidArmorItem extends ArmorItem implements Repairable, RunicArmor, WarpingGear {
    public VoidArmorItem(TcArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(Registration.armorMaterial(material), Registration.armorType(slot), properties);
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, owner, slotId, isSelected);
        ArmorSupport.selfRepair(stack, owner);
    }
}
