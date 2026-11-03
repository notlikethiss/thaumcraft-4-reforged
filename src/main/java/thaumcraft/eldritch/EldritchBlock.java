package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.registry.EldritchContent;

public class EldritchBlock extends Block implements EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> PART = EnumProperty.create("part", DoubleBlockHalf.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private final int kind;

    public EldritchBlock(int kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }

    public int kind() {
        return this.kind;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        if (this.kind == 0 || this.kind == 1 || this.kind == 3 || this.kind == 10) {
            return RenderShape.INVISIBLE;
        }
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
        if (this.kind == 5 || this.kind == 10) {
            return 1 + level.getRandom().nextInt(4);
        }
        return this.kind == 9 ? 6 + level.getRandom().nextInt(5) : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean movedByPiston) {
        super.onRemove(state, level, pos, nextState, movedByPiston);
        if (state.is(nextState.getBlock())) return;
        if (this.kind >= 4) {
            return;
        }
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
            if (level.getBlockState(nearby).getBlock() instanceof EldritchBlock block && block.kind < 4) {
                level.removeBlock(nearby, false);
            }
        }
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, Level.ExplosionInteraction.NONE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        if (this.kind == 8 || this.kind == 9) {
            state = state.setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (this.kind != 0 && this.kind != 1 && this.kind != 3 && this.kind != 8 && this.kind != 9 && this.kind != 10) {
            return null;
        }
        if (this.kind == 1 && state.getValue(PART) == DoubleBlockHalf.UPPER) {
            return null;
        }
        return EldritchBlockEntity.create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != EldritchContent.ELDRITCH_BLOCK_ENTITY.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> EldritchBlockEntity.tick(tickLevel, pos, tickState, (EldritchBlockEntity) entity);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (this.kind == 0 && stack.is(EldritchContent.ELDRITCH_EYE.get()) && !player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof EldritchBlockEntity altar && altar.eyeCount() < 4) {
                if (!level.isClientSide() && altar.addEye()) {
                    stack.shrink(1);
                    level.playSound(null, pos, thaumcraft.registry.ModSounds.CRYSTAL.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, 1.0F);
                }
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
        }
        if (this.kind == 8 && stack.is(EldritchContent.RUNED_TABLET.get())) {
            if (level.getBlockEntity(pos) instanceof EldritchBlockEntity lock) {
                if (!level.isClientSide() && lock.trigger()) {
                    stack.shrink(1);
                    level.playSound(null, pos, thaumcraft.registry.ModSounds.RUNIC_SHIELD_CHARGE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

}
