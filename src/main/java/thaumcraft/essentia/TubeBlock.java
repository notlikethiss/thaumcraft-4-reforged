package thaumcraft.essentia;

import thaumcraft.blockentity.TcBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.block.ItemInteractions;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.WandItem;

public class TubeBlock extends Block implements EntityBlock {
    public enum Kind {
        TUBE,
        VALVE,
        FILTER,
        RESTRICT,
        ONEWAY
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final Kind kind;

    public TubeBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(TubeArms.noArms(stateDefinition.any()).setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TubeArms.DOWN, TubeArms.UP, TubeArms.NORTH, TubeArms.SOUTH, TubeArms.WEST, TubeArms.EAST);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case TUBE -> new TubeBlockEntity(pos, state);
            case VALVE -> new ValveTubeBlockEntity(pos, state);
            case FILTER -> new FilterTubeBlockEntity(pos, state);
            case RESTRICT -> new RestrictTubeBlockEntity(pos, state);
            case ONEWAY -> new OnewayTubeBlockEntity(pos, state);
        };
    }

    private BlockEntityType<?> entityType() {
        return switch (kind) {
            case TUBE -> ModBlockEntities.TUBE.get();
            case VALVE -> ModBlockEntities.VALVE_TUBE.get();
            case FILTER -> ModBlockEntities.FILTER_TUBE.get();
            case RESTRICT -> ModBlockEntities.RESTRICT_TUBE.get();
            case ONEWAY -> ModBlockEntities.ONEWAY_TUBE.get();
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != entityType()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> TubeBlockEntity.tick(tickLevel, pos, tickState, (TubeBlockEntity) entity);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return TubeArms.withArms(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide()) {
            TubeArms.refresh(level, pos);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(TubeArms.property(directionToNeighbour), TubeArms.compute(level, pos, state, directionToNeighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isHoldingItem(ModItems.WAND.get()) && level.getBlockEntity(pos) instanceof TubeBlockEntity tube) {
            return TubeArms.traceShape(tube.traceBoxes());
        }
        return TubeArms.shape(state, 0.375, 0.625);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TubeArms.shape(state, 0.375, 0.625);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (kind == Kind.VALVE) {
            if (stack.getItem() instanceof WandItem || stack.is(asItem())) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            return ItemInteractions.skipping(toggleValve(level, pos));
        }
        if (kind == Kind.FILTER) {
            return ItemInteractions.skipping(filterInteract(level, pos, player, stack, hitResult.getDirection()));
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (kind == Kind.VALVE) {
            return toggleValve(level, pos);
        }
        if (kind == Kind.FILTER) {
            return filterInteract(level, pos, player, ItemStack.EMPTY, hitResult.getDirection());
        }
        return InteractionResult.PASS;
    }

    private InteractionResult toggleValve(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ValveTubeBlockEntity valve) {
            if (!level.isClientSide()) {
                valve.setAllowFlow(!valve.isAllowFlow());
                valve.playSqueek(level);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult filterInteract(Level level, BlockPos pos, Player player, ItemStack held, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof FilterTubeBlockEntity filter)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && filter.getAspectFilter() != null) {
            if (!level.isClientSide()) {
                filter.setAspectFilter(null);
                filter.sync();
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.addFreshEntity(new ItemEntity(
                    level,
                    pos.getX() + 0.5F + side.getStepX() / 3.0F,
                    pos.getY() + 0.5F,
                    pos.getZ() + 0.5F + side.getStepZ() / 3.0F,
                    new ItemStack(ModItems.JAR_LABEL.get())
                ));
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.isEmpty() && filter.getAspectFilter() == null && held.is(ModItems.JAR_LABEL.get())) {
            AspectList labelAspects = AspectKeys.ASPECTS.get(held);
            if (labelAspects != null && !labelAspects.isEmpty() && !level.isClientSide()) {
                Aspect aspect = labelAspects.getAspects().get(0);
                filter.setAspectFilter(aspect);
                held.consume(1, player);
                filter.sync();
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        TcBlockEntity.beforeRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
