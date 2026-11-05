package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeModifier;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchManager;
import thaumcraft.registry.EldritchContent;

public final class EldritchObjectItem extends Item {
    private final int kind;

    public EldritchObjectItem(int kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> builder, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        int[] lines = switch (this.kind) {
            case 0 -> new int[]{1};
            case 1 -> new int[]{2, 3};
            case 2 -> new int[]{4};
            case 3 -> new int[]{5, 6};
            default -> new int[0];
        };
        for (int line : lines) {
            builder.accept(net.minecraft.network.chat.Component.translatable("item.ItemEldritchObject.text." + line)
                .withStyle(line == 3 ? net.minecraft.ChatFormatting.DARK_BLUE : net.minecraft.ChatFormatting.DARK_PURPLE));
        }
        if (this.kind == 4) {
            builder.accept(net.minecraft.network.chat.Component.translatable("tc.creativeonly").withStyle(net.minecraft.ChatFormatting.ITALIC));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (this.kind == 1 && player instanceof ServerPlayer serverPlayer && !ResearchManager.isComplete(player, "CRIMSON")) {
            ResearchManager.complete(serverPlayer, "CRIMSON");
            level.playSound(null, player.blockPosition(), thaumcraft.registry.ModSounds.LEARN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.75F, 1.0F);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (this.kind == 4) {
            if (context.getClickedFace() != net.minecraft.core.Direction.UP || player.level().isClientSide()) {
                return InteractionResult.PASS;
            }
            BlockPos base = pos.above();
            for (int y = 0; y < 6; y++) {
                if (!level.getBlockState(base.above(y)).isAir()) {
                    return InteractionResult.FAIL;
                }
            }
            level.setBlock(base, EldritchContent.ELDRITCH_ALTAR.get().defaultBlockState(), 3);
            level.setBlock(base.above(2), EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
            for (int y = 3; y <= 6; y++) {
                level.setBlock(base.above(y), EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
            }
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (this.kind != 3 || !(blockEntity instanceof NodeBlockEntity node)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        context.getItemInHand().shrink(1);
        boolean research = ResearchManager.isComplete(player, "PRIMNODE");
        for (thaumcraft.aspect.Aspect aspect : node.getAspects().getAspects()) {
            int base = node.getNodeVisBase(aspect);
            if (aspect.isPrimal()) {
                node.setNodeVisBase(aspect, base - 2 + level.getRandom().nextInt(research ? 9 : 6));
            } else if (level.getRandom().nextBoolean()) {
                node.setNodeVisBase(aspect, base - 1);
            }
        }
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            int base = node.getNodeVisBase(aspect);
            int amount = level.getRandom().nextInt(research ? 4 : 3);
            if (amount > 0 && amount > base) {
                node.setNodeVisBase(aspect, amount);
                node.addToContainer(aspect, 1);
            }
        }
        if (node.getNodeModifier() == NodeModifier.FADING && level.getRandom().nextBoolean()) {
            node.setNodeModifier(NodeModifier.PALE);
        } else if (node.getNodeModifier() == NodeModifier.PALE && level.getRandom().nextBoolean()) {
            node.setNodeModifier(null);
        } else if (node.getNodeModifier() == null && level.getRandom().nextInt(5) == 0) {
            node.setNodeModifier(NodeModifier.BRIGHT);
        }
        node.sync();
        level.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3.0F + level.getRandom().nextFloat() * (research ? 3 : 5), true, Level.ExplosionInteraction.BLOCK);
        for (int i = 0; i < 33; i++) {
            BlockPos target = pos.offset(level.getRandom().nextInt(6) - level.getRandom().nextInt(6), level.getRandom().nextInt(6) - level.getRandom().nextInt(6), level.getRandom().nextInt(6) - level.getRandom().nextInt(6));
            if (level.getBlockState(target).isAir()) {
                level.setBlock(target, ((thaumcraft.alchemy.FluxBlock) (target.getY() < pos.getY() ? ModBlocks.FLUX_GOO.get() : ModBlocks.FLUX_GAS.get())).withLevel(8), 3);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
