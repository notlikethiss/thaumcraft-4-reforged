package thaumcraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.research.ResearchManager;

public class PrimalCharmItem extends Item {
    public PrimalCharmItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        if (level.isClientSide()) {
            return;
        }
        super.inventoryTick(stack, level, owner, slotId, isSelected);
        if (owner instanceof ServerPlayer player
            && level.getRandom().nextInt(20000) == 42
            && !ResearchManager.isComplete(player, "FOCUSPRIMAL")
            && !ResearchManager.isComplete(player, "@FOCUSPRIMAL")) {
            player.sendSystemMessage(Component.translatable("tc.primalcharm.trigger").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            ResearchManager.complete(player, "@FOCUSPRIMAL");
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RandomSource random = RandomSource.create(stack.hashCode() + System.currentTimeMillis() / 6000L);
        if (random.nextInt(200) < 25) {
            tooltip.add(Component.translatable("tc.primalcharm." + random.nextInt(5)).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
