package thaumcraft.wand.focus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.block.LootBlock;
import thaumcraft.wand.WandManager;
import thaumcraft.wand.Wands;

public class TradeFocus extends FocusItem implements Architect {
    private static final AspectList COST = new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.EARTH, 5).add(Aspect.ORDER, 5);
    private static final AspectList COST_SILK = new AspectList()
        .add(Aspect.AIR, 1)
        .add(Aspect.FIRE, 1)
        .add(Aspect.EARTH, 1)
        .add(Aspect.WATER, 1)
        .add(Aspect.ORDER, 1)
        .add(Aspect.ENTROPY, 1)
        .add(COST);

    public TradeFocus(Properties properties) {
        super(properties);
    }

    @Override
    public String getSortingHelper(ItemStack focus) {
        return "BT" + super.getSortingHelper(focus);
    }

    @Override
    public @Nullable Identifier getFocusTexture(ItemStack focus) {
        return Thaumcraft.id("item/focus_trade");
    }

    @Override
    public @Nullable Identifier getOrnamentTexture(ItemStack focus) {
        return Thaumcraft.id("item/focus_trade_orn");
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 8747923;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgradeType.SILKTOUCH) ? COST_SILK : COST;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0.0F;
    }

    private static @Nullable BlockHitResult target(Level level, Player player) {
        return FocusUtils.getTargetBlock(level, player, false, player.blockInteractionRange());
    }

    @Override
    public InteractionResult onFocusRightClick(ItemStack wand, Level level, Player player, @Nullable HitResult hit) {
        BlockHitResult target = target(level, player);
        if (target == null) {
            if (player.isShiftKeyDown() && level instanceof ServerLevel) {
                storePickedBlock(wand, ItemStack.EMPTY);
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = target.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel && level.getBlockEntity(pos) == null && canPick(state)) {
                storePickedBlock(wand, LootBlock.stackForState(state));
            }
            return InteractionResult.SUCCESS;
        }
        ItemStack picked = getPickedBlock(wand);
        if (picked.isEmpty() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) != null || state.is(FocusUtils.TAINT)) {
            return InteractionResult.SUCCESS;
        }
        int slot = player.getInventory().getSelectedSlot();
        if (isUpgradedWith(Wands.getFocusItem(wand), FocusUpgradeType.ARCHITECT)) {
            for (BlockPos block : getArchitectBlocks(wand, level, pos, target.getDirection(), player)) {
                BlockSwapper.add(serverLevel, block, level.getBlockState(block), picked, 0, serverPlayer, slot);
            }
        } else {
            BlockSwapper.add(serverLevel, pos, state, picked, 3 + Wands.getFocusEnlarge(wand), serverPlayer, slot);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onEntitySwing(LivingEntity entity, ItemStack wand) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof ServerPlayer player)) {
            return false;
        }
        if (player.getMainHandItem() != wand) {
            return false;
        }
        ItemStack picked = getPickedBlock(wand);
        BlockHitResult target = target(level, player);
        if (target != null && !picked.isEmpty()) {
            BlockPos pos = target.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) == null && !state.is(FocusUtils.TAINT)) {
                BlockSwapper.add(level, pos, state, picked, 0, player, player.getInventory().getSelectedSlot());
            }
        }
        return false;
    }

    public static boolean canPick(BlockState state) {
        return state.getBlock().asItem() instanceof BlockItem
            && !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && !state.hasProperty(BlockStateProperties.BED_PART);
    }

    public static void storePickedBlock(ItemStack wand, ItemStack picked) {
        if (picked.isEmpty()) {
            FocusKeys.PICKED.remove(wand);
        } else {
            FocusKeys.PICKED.set(wand, ItemStackTemplate.fromNonEmptyStack(picked));
        }
    }

    public static ItemStack getPickedBlock(ItemStack wand) {
        ItemStackTemplate picked = FocusKeys.PICKED.get(wand);
        return picked == null ? ItemStack.EMPTY : picked.create();
    }

    @Override
    public FocusUpgradeType @Nullable [] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4 -> new FocusUpgradeType[]{FocusUpgradeType.FRUGAL, FocusUpgradeType.ENLARGE};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.FRUGAL, FocusUpgradeType.ENLARGE, FocusUpgradeType.TREASURE, FocusUpgradeType.ARCHITECT};
            case 5 -> new FocusUpgradeType[]{FocusUpgradeType.FRUGAL, FocusUpgradeType.ENLARGE, FocusUpgradeType.SILKTOUCH};
            default -> null;
        };
    }

    @Override
    public int getMaxAreaSize(ItemStack focus) {
        return 3 + getUpgradeLevel(focus, FocusUpgradeType.ENLARGE) * 2;
    }

    @Override
    public List<BlockPos> getArchitectBlocks(ItemStack wand, Level level, BlockPos pos, Direction side, Player player) {
        BlockState state = level.getBlockState(pos);
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> checked = new HashSet<>();
        int face = side.get3DDataValue();
        if (face != 2 && face != 3) {
            checkNeighbours(level, pos, state, pos, face, WandManager.getAreaX(wand), WandManager.getAreaZ(wand), out, checked, player);
        } else {
            checkNeighbours(level, pos, state, pos, face, WandManager.getAreaZ(wand), WandManager.getAreaX(wand), out, checked, player);
        }
        return out;
    }

    private static void checkNeighbours(Level level, BlockPos origin, BlockState state, BlockPos pos, int side, int sizeX, int sizeZ, List<BlockPos> list, Set<BlockPos> checked, Player player) {
        if (!checked.add(pos)) {
            return;
        }
        switch (side) {
            case 0, 1 -> {
                if (Math.abs(pos.getX() - origin.getX()) > sizeX || Math.abs(pos.getZ() - origin.getZ()) > sizeZ) {
                    return;
                }
            }
            case 2, 3 -> {
                if (Math.abs(pos.getX() - origin.getX()) > sizeX || Math.abs(pos.getY() - origin.getY()) > sizeZ) {
                    return;
                }
            }
            default -> {
                if (Math.abs(pos.getY() - origin.getY()) > sizeX || Math.abs(pos.getZ() - origin.getZ()) > sizeZ) {
                    return;
                }
            }
        }
        BlockState current = level.getBlockState(pos);
        if (current == state
            && FocusUtils.isBlockExposed(level, pos)
            && !current.isAir()
            && current.getDestroySpeed(level, pos) >= 0.0F
            && level.mayInteract(player, pos)) {
            list.add(pos);
            for (Direction direction : Direction.values()) {
                if (direction.get3DDataValue() != side && direction.getOpposite().get3DDataValue() != side) {
                    checkNeighbours(level, origin, state, pos.relative(direction), side, sizeX, sizeZ, list, checked, player);
                }
            }
        }
    }

    @Override
    public boolean showAxis(ItemStack wand, Level level, Player player, Direction side, Direction.Axis axis) {
        int dim = WandManager.getAreaDim(wand);
        return switch (side.get3DDataValue()) {
            case 0, 1 -> axis == Direction.Axis.X && (dim == 0 || dim == 1) || axis == Direction.Axis.Z && (dim == 0 || dim == 2);
            case 2, 3 -> axis == Direction.Axis.Y && (dim == 0 || dim == 1) || axis == Direction.Axis.X && (dim == 0 || dim == 2);
            default -> axis == Direction.Axis.Y && (dim == 0 || dim == 1) || axis == Direction.Axis.Z && (dim == 0 || dim == 2);
        };
    }
}
