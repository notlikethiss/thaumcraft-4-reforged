package thaumcraft.item.armor;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import thaumcraft.platform.ArmorSlot;
import thaumcraft.platform.TcArmorMaterial;
import thaumcraft.platform.Registration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.aspect.Aspect;
import thaumcraft.item.gear.Goggles;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.RunicArmor;
import thaumcraft.item.gear.WarpingGear;
import thaumcraft.node.Revealer;
import thaumcraft.wand.VisDiscountGear;

public class VoidRobeArmorItem extends ArmorItem implements Repairable, RunicArmor, VisDiscountGear, Goggles, Revealer, WarpingGear {
    public VoidRobeArmorItem(TcArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(Registration.armorMaterial(material), Registration.armorType(slot), properties);
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
        return 5;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity entity) {
        return ArmorSupport.slotOf(stack) == EquipmentSlot.HEAD;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity entity) {
        return ArmorSupport.slotOf(stack) == EquipmentSlot.HEAD;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> builder, TooltipFlag flag) {
        ArmorSupport.visDiscountLine(builder, getVisDiscount(stack, null, null));
        super.appendHoverText(stack, context, builder, flag);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, owner, slotId, isSelected);
        ArmorSupport.selfRepair(stack, owner);
    }
}
