package thaumcraft.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public class BannerItem extends BlockItem {
    public BannerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Integer color = WardedKeys.BANNER_COLOR.get(stack);
        if (color != null && color >= 0 && color < ModBlocks.CANDLE_COLORS.length) {
            return Component.translatable("block.thaumcraft.banner." + ModBlocks.CANDLE_COLORS[color]);
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand().copy();
        InteractionResult result = super.place(context);
        if (!result.consumesAction() || context.getLevel().isClientSide()) {
            return result;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
            Direction side = context.getClickedFace();
            Player player = context.getPlayer();
            if (side.getAxis() == Direction.Axis.Y) {
                float yaw = player == null ? 0.0F : player.getYRot();
                banner.setFacing(Mth.floor((yaw + 180.0F) * 16.0F / 360.0F + 0.5) & 15);
            } else {
                banner.setOnWall(true);
                banner.setFacing(switch (side) {
                    case NORTH -> 8;
                    case WEST -> 4;
                    case EAST -> 12;
                    default -> 0;
                });
            }
            Aspect aspect = WardedKeys.BANNER_ASPECT.get(stack);
            if (aspect != null) {
                banner.setAspect(aspect);
            }
            Integer color = WardedKeys.BANNER_COLOR.get(stack);
            if (color != null) {
                banner.setColor(color);
            }
            banner.sync();
        }
        return result;
    }

    @SubscribeEvent
    static void addVariants(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != ModCreativeTabs.THAUMCRAFT.get()) {
            return;
        }
        for (int color = 0; color < 16; color++) {
            ItemStack banner = new ItemStack(ModBlocks.BANNER.get());
            WardedKeys.BANNER_COLOR.set(banner, color);
            event.accept(banner);
        }
    }
}
