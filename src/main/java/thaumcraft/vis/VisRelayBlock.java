package thaumcraft.vis;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.WandInteractable;

public class VisRelayBlock extends Block implements EntityBlock, WandInteractable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.DOWN, Block.box(5, 8, 5, 11, 16, 11),
        Direction.UP, Block.box(5, 0, 5, 11, 8, 11),
        Direction.NORTH, Block.box(5, 5, 8, 11, 11, 16),
        Direction.SOUTH, Block.box(5, 5, 0, 11, 11, 8),
        Direction.WEST, Block.box(8, 5, 5, 16, 11, 11),
        Direction.EAST, Block.box(0, 5, 5, 8, 11, 11)
    );

    public VisRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VisRelayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.VIS_RELAY.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> VisRelayBlockEntity.tick(tickLevel, pos, tickState, (VisRelayBlockEntity) entity);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && !hasSupport(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    protected boolean hasSupport(BlockState state, Level level, BlockPos pos) {
        return !level.isEmptyBlock(pos.relative(state.getValue(FACING).getOpposite()));
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        int shard = shardColor(stack.getItem());
        if (shard < 0 || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VisRelayBlockEntity relay) {
            byte color = (byte) shard;
            relay.setColor(color != relay.getColor() && color != 6 ? color : (byte) -1);
        }
        return InteractionResult.SUCCESS;
    }

    private static int shardColor(Item item) {
        if (item == ModItems.AIR_SHARD.get()) {
            return 0;
        }
        if (item == ModItems.FIRE_SHARD.get()) {
            return 1;
        }
        if (item == ModItems.WATER_SHARD.get()) {
            return 2;
        }
        if (item == ModItems.EARTH_SHARD.get()) {
            return 3;
        }
        if (item == ModItems.ORDER_SHARD.get()) {
            return 4;
        }
        if (item == ModItems.ENTROPY_SHARD.get()) {
            return 5;
        }
        if (item == ModItems.BALANCED_SHARD.get()) {
            return 6;
        }
        return -1;
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack wand, Direction side) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VisRelayBlockEntity relay) {
            relay.cycleColor();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }
}
