package thaumcraft.device;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;

public class ArcaneKeyItem extends Item {
    private final int keyType;

    public ArcaneKeyItem(Properties properties, int keyType) {
        super(properties);
        this.keyType = keyType;
    }

    public int getKeyType() {
        return keyType;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return WardedKeys.KEY_LINK.has(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        boolean door = state.is(ModBlocks.ARCANE_DOOR.get());
        if (!door && !state.is(ModBlocks.ARCANE_PRESSURE_PLATE.get())) {
            return InteractionResult.SUCCESS;
        }
        int type = door ? 0 : 1;
        int below = door && state.getValue(ArcaneDoorBlock.HALF) == DoubleBlockHalf.UPPER ? -1 : 0;
        BlockPos target = pos.above(below);
        BlockPos other = pos.above(door ? below == 0 ? 1 : 0 : 0);
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, target);
        if (owned == null) {
            return InteractionResult.SUCCESS;
        }
        UUID id = player.getUUID();
        WardedKeys.KeyLink link = WardedKeys.KEY_LINK.get(stack);
        if (link == null) {
            if (owned.isOwner(id) || owned.hasAccess(id, OwnedBlockEntity.GOLD) && keyType == OwnedBlockEntity.IRON) {
                ItemStack bound = new ItemStack(this);
                WardedKeys.KEY_LINK.set(bound, new WardedKeys.KeyLink(target, type));
                if (!level.isClientSide()) {
                    if (!player.getInventory().add(bound)) {
                        level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), bound));
                    }
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (!level.isClientSide()) {
                    message(player, type == 0 ? "tc.key1" : "tc.key2");
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.KEY.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
                }
            }
        } else if (!owned.isOwner(id) && !owned.hasAccess(id, keyType) && !owned.hasAccess(id, OwnedBlockEntity.GOLD) && target.equals(link.pos())) {
            owned.addAccess(keyType, id);
            if (type == 0) {
                OwnedBlockEntity otherHalf = OwnedBlockEntity.at(level, other);
                if (otherHalf != null) {
                    otherHalf.addAccess(keyType, id);
                }
            }
            if (!level.isClientSide()) {
                String suffix = keyType == 0 ? "" : type == 0 ? "tc.key4" : "tc.key6";
                Component text = Component.translatable(type == 0 ? "tc.key3" : "tc.key5");
                if (!suffix.isEmpty()) {
                    text = text.copy().append(Component.translatable(suffix));
                }
                player.sendSystemMessage(text.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.KEY.get(), SoundSource.BLOCKS, 1.0F, 1.1F);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        } else if (!level.isClientSide()) {
            message(player, target.equals(link.pos()) ? "tc.key8" : "tc.key7");
        }
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        WardedKeys.KeyLink link = WardedKeys.KEY_LINK.get(stack);
        if (link != null) {
            BlockPos pos = link.pos();
            builder.accept(styled(Component.translatable("tc.key9")));
            builder.accept(styled(Component.translatable(link.type() == 0 ? "tc.key10" : "tc.key11")));
            builder.accept(styled(Component.literal("x " + pos.getX() + ", z " + pos.getZ() + ", y " + pos.getY())));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }

    private static Component styled(Component component) {
        return component.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
    }
}
