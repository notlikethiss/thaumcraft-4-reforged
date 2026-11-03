package thaumcraft.item.tool;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import thaumcraft.platform.Registration;
import thaumcraft.item.gear.TcMaterials;
import net.minecraft.world.item.ItemStack;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.SelfRepair;
import thaumcraft.item.gear.WarpingGear;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.TooltipFlag;

public class CrimsonBladeItem extends SwordItem implements Repairable, WarpingGear {
    private static final int WARP = 2;

    public CrimsonBladeItem(Properties properties) {
        super(Registration.tier(TcMaterials.CRIMSON_VOID_TOOL), properties);
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
    public void postHurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        super.postHurtEnemy(stack, mob, attacker);
        SelfRepair.applyEffect(mob, attacker, new MobEffectInstance(MobEffects.WEAKNESS, 60));
        SelfRepair.applyEffect(mob, attacker, new MobEffectInstance(MobEffects.HUNGER, 120));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> builder, TooltipFlag flag) {
        builder.add(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, builder, flag);
    }
}
