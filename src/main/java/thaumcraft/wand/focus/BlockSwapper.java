package thaumcraft.wand.focus;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.block.LootBlock;
import thaumcraft.network.BlockSparkleFxPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class BlockSwapper {
    private static final int SPARKLE_COLOR = 12632319;
    private static final Map<ResourceKey<Level>, ArrayDeque<Swap>> QUEUES = new HashMap<>();

    private record Swap(BlockPos pos, BlockState source, ItemStack target, int lifespan, ServerPlayer player, int wandSlot) {
    }

    private BlockSwapper() {
    }

    public static void add(ServerLevel level, BlockPos pos, BlockState source, ItemStack target, int lifespan, ServerPlayer player, int wandSlot) {
        if (source.isAir() || source.getDestroySpeed(level, pos) < 0.0F || samePlacement(source, target) || Block.byItem(target.getItem()) == Blocks.AIR) {
            return;
        }
        QUEUES.computeIfAbsent(level.dimension(), key -> new ArrayDeque<>()).offer(new Swap(pos, source, target.copyWithCount(1), lifespan, player, wandSlot));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WAND.get(), SoundSource.PLAYERS, 0.25F, 1.0F);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ArrayDeque<Swap> queue = QUEUES.get(level.dimension());
            if (queue != null) {
                tick(level, queue);
            }
        }
    }

    private static void tick(ServerLevel level, ArrayDeque<Swap> queue) {
        while (true) {
            Swap swap = queue.poll();
            if (swap == null) {
                return;
            }
            if (process(level, queue, swap)) {
                return;
            }
        }
    }

    private static boolean process(ServerLevel level, ArrayDeque<Swap> queue, Swap swap) {
        ServerPlayer player = swap.player();
        if (player.isRemoved() || player.level() != level) {
            return false;
        }
        BlockPos pos = swap.pos();
        BlockState state = level.getBlockState(pos);
        ItemStack wand = player.getInventory().getItem(swap.wandSlot());
        FocusItem focus = Wands.isWand(wand) ? Wands.getFocus(wand) : null;
        if (focus == null || !(wand.getItem() instanceof WandItem wandItem)) {
            return false;
        }
        ItemStack focusStack = Wands.getFocusItem(wand);
        if (!level.mayInteract(player, pos) || samePlacement(state, swap.target())) {
            return false;
        }
        if (!LootBlock.stateForStack(swap.target()).canSurvive(level, pos)) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit).isCanceled()) {
            return false;
        }
        if (!wandItem.consumeAllVis(wand, player, focus.getVisCost(focusStack), false, false)) {
            return false;
        }
        int slot = player.isCreative() ? 1 : carryingSlot(player, swap.target());
        if (state != swap.source() || slot < 0) {
            return false;
        }
        if (!player.isCreative()) {
            int fortune = Wands.getFocusTreasure(wand);
            boolean silk = focus.isUpgradedWith(focusStack, FocusUpgradeType.SILKTOUCH);
            player.getInventory().removeItem(slot, 1);
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, FocusUtils.harvestTool(level, fortune, silk));
            for (ItemStack drop : drops) {
                if (!player.getInventory().add(drop)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
                }
            }
            wandItem.consumeAllVis(wand, player, focus.getVisCost(focusStack), true, false);
        }
        Block block = Block.byItem(swap.target().getItem());
        BlockState placed = LootBlock.stateForStack(swap.target());
        level.setBlock(pos, placed, Block.UPDATE_ALL);
        block.setPlacedBy(level, pos, placed, player, swap.target());
        ModNetwork.sendToNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 32.0, new BlockSparkleFxPayload(pos, SPARKLE_COLOR));
        level.levelEvent(2001, pos, Block.getId(swap.source()));
        if (swap.lifespan() > 0) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos near = pos.offset(x, y, z);
                        if ((x != 0 || y != 0 || z != 0) && level.getBlockState(near) == swap.source() && FocusUtils.isBlockExposed(level, near)) {
                            queue.offer(new Swap(near, swap.source(), swap.target(), swap.lifespan() - 1, player, swap.wandSlot()));
                        }
                    }
                }
            }
        }
        return true;
    }

    private static int carryingSlot(Player player, ItemStack target) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && sameItemVariant(stack, target)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean samePlacement(BlockState state, ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        return block == state.getBlock() && (!(block instanceof LootBlock) || LootBlock.stateForStack(stack) == state);
    }

    private static boolean sameItemVariant(ItemStack first, ItemStack second) {
        if (!ItemStack.isSameItem(first, second)) {
            return false;
        }
        if (Block.byItem(first.getItem()) instanceof LootBlock && Block.byItem(second.getItem()) == Block.byItem(first.getItem())) {
            return LootBlock.stateForStack(first) == LootBlock.stateForStack(second);
        }
        return true;
    }
}
