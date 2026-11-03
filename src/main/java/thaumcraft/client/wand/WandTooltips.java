package thaumcraft.client.wand;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WandTooltips {
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.##");

    private WandTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof WandItem wand)) {
            return;
        }
        Player player = event.getEntity();
        boolean shift = Screen.hasShiftDown();
        List<Component> lines = event.getToolTip();
        List<Component> vis = new ArrayList<>();
        MutableComponent average = Component.empty();
        if (WandKeys.VIS.has(stack)) {
            MutableComponent compact = Component.empty();
            int total = 0;
            int count = 0;
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                String amount = FORMAT.format(Wands.getVis(stack, aspect) / 100.0F);
                float modifier = wand.getConsumptionModifier(stack, player, aspect, false);
                total = (int)(total + modifier * 100.0F);
                count++;
                if (shift) {
                    vis.add(Component.literal(" ")
                        .append(Component.literal(aspect.getName()).withStyle(color(aspect)))
                        .append(Component.literal(" x " + amount + ", ").withStyle(ChatFormatting.RESET))
                        .append(Component.literal("(" + FORMAT.format(modifier * 100.0F) + "% ")
                            .append(Component.translatable("tc.vis.cost"))
                            .append(")")
                            .withStyle(ChatFormatting.ITALIC)));
                } else {
                    if (count > 1) {
                        compact.append(" | ");
                    }
                    compact.append(Component.literal(amount).withStyle(color(aspect)));
                }
            }
            if (!shift) {
                vis.add(compact);
                average = Component.literal(" (" + total / count + "% ").append(Component.translatable("tc.vis.costavg")).append(")");
            }
        }
        int position = Math.min(1, lines.size());
        lines.addAll(position, vis);
        lines.add(position, Component.translatable("item.capacity.text")
            .append(" " + Wands.getMaxVis(stack) / 100)
            .withStyle(ChatFormatting.GOLD)
            .append(average));
    }

    private static ChatFormatting color(Aspect aspect) {
        String code = aspect.getChatcolor();
        ChatFormatting formatting = code == null || code.isEmpty() ? null : ChatFormatting.getByCode(code.charAt(0));
        return formatting == null ? ChatFormatting.WHITE : formatting;
    }
}
