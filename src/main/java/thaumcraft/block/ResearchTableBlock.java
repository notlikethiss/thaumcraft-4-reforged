package thaumcraft.block;

import thaumcraft.blockentity.TcBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.blockentity.ResearchTableBlockEntity;
import thaumcraft.platform.Menus;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;

public class ResearchTableBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final double[][] MASTER_BOXES = {{0, 12, 0, 16, 16, 16}, {2, 0, 2, 6, 12, 6}, {2, 0, 10, 6, 12, 14}, {4, 2, 6, 16, 6, 10}};
    private static final double[][] PART_BOXES = {{0, 12, 0, 16, 16, 16}, {10, 0, 2, 14, 12, 6}, {10, 0, 10, 14, 12, 14}, {0, 2, 6, 4, 6, 10}};

    public ResearchTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape shape(double[][] boxes, Direction direction) {
        VoxelShape shape = Shapes.empty();
        for (double[] box : boxes) {
            double[] first = rotate(box[0], box[2], direction);
            double[] second = rotate(box[3], box[5], direction);
            shape = Shapes.or(shape, Block.box(
                Math.min(first[0], second[0]), box[1], Math.min(first[1], second[1]),
                Math.max(first[0], second[0]), box[4], Math.max(first[1], second[1])
            ));
        }
        return shape;
    }

    private static double[] rotate(double x, double z, Direction direction) {
        return switch (direction) {
            case SOUTH -> new double[]{16 - z, x};
            case WEST -> new double[]{16 - x, 16 - z};
            case NORTH -> new double[]{z, 16 - x};
            default -> new double[]{x, z};
        };
    }

    public static VoxelShape masterShape(Direction direction) {
        return shape(MASTER_BOXES, direction);
    }

    public static VoxelShape partShape(Direction masterDirection) {
        return shape(PART_BOXES, masterDirection);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return masterShape(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.RESEARCH_TABLE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ResearchTableBlockEntity.serverTick(tickLevel, pos, tickState, (ResearchTableBlockEntity) entity);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (directionToNeighbour == state.getValue(FACING)
            && !(neighbourState.is(ModBlocks.RESEARCH_TABLE_PART.get()) && neighbourState.getValue(ResearchTablePartBlock.FACING) == directionToNeighbour.getOpposite())) {
            return ModBlocks.TABLE.get().defaultBlockState();
        }
        return super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Menus.open(serverPlayer, table, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.TABLE.get());
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        TcBlockEntity.beforeRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
