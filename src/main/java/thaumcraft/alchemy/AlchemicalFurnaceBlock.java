package thaumcraft.alchemy;

import thaumcraft.blockentity.TcBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;
import thaumcraft.platform.Menus;
import thaumcraft.registry.ModBlockEntities;

public class AlchemicalFurnaceBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty HAS_VIS = BooleanProperty.create("has_vis");

    public AlchemicalFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(HAS_VIS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, HAS_VIS);
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(LIT) ? 12 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof AlchemicalFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Menus.open(serverPlayer, furnace, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof AlchemicalFurnaceBlockEntity furnace) {
            furnace.resetBellows();
        }
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AlchemicalFurnaceBlockEntity furnace) {
            return furnace.getComparatorSignal();
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemicalFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.ALCHEMICAL_FURNACE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> AlchemicalFurnaceBlockEntity.serverTick(tickLevel, pos, tickState, (AlchemicalFurnaceBlockEntity) entity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        TcBlockEntity.beforeRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
