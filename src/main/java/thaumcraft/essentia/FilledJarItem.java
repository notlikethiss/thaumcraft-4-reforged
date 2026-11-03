package thaumcraft.essentia;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectProvidingItem;
import thaumcraft.item.AspectTooltips;

public class FilledJarItem extends BlockItem implements AspectProvidingItem {
    private final Block jar;

    public FilledJarItem(Block jar, Properties properties) {
        super(jar, properties);
        this.jar = jar;
    }

    public Block jar() {
        return jar;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        AspectTooltips.append(stack, builder, " x ", true);
        Aspect filter = EssentiaKeys.FILTER.get(stack);
        if (filter != null) {
            Component name = AspectTooltips.isDiscovered(filter) ? Component.literal(filter.getName()) : Component.translatable("tc.aspect.unknown");
            builder.accept(name.copy().withStyle(ChatFormatting.DARK_PURPLE));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
