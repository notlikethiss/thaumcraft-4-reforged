package thaumcraft.item.tool;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShovelItem;
import thaumcraft.platform.Registration;
import thaumcraft.item.gear.TcMaterials;
import net.minecraft.world.item.ItemStack;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.SelfRepair;
import thaumcraft.item.gear.WarpingGear;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class VoidShovelItem extends ShovelItem implements Repairable, WarpingGear {
    private static final int WARP = 1;

    public VoidShovelItem(Properties properties) {
        super(Registration.tier(TcMaterials.VOID_TOOL), properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        SelfRepair.tick(stack, owner);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return WARP;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof LivingEntity target) {
            SelfRepair.applyEffect(target, player, new MobEffectInstance(MobEffects.WEAKNESS, 80));
        }
        return false;
    }
}
