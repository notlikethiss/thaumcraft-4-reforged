package thaumcraft.alchemy;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;

public class AdvancedAlchemicalFurnaceBlock extends Block implements EntityBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 12);
    private static final VoxelShape CORE_COLLISION = Block.box(0, 0, 0, 16, 11.2, 16);

    public enum Part implements StringRepresentable {
        CORE("core"),
        NOZZLE("nozzle"),
        FRAME_ALEMBIC("frame_alembic"),
        FRAME_EDGE("frame_edge"),
        FRAME_CORNER("frame_corner");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public AdvancedAlchemicalFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.CORE).setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, LIGHT);
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(LIGHT);
    }

    public BlockState partState(Part part) {
        return defaultBlockState().setValue(PART, part);
    }

    public static Block revertBlock(Part part) {
        return switch (part) {
            case CORE -> ModBlocks.ALCHEMICAL_FURNACE.get();
            case NOZZLE, FRAME_CORNER -> ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get();
            case FRAME_ALEMBIC -> ModBlocks.ALEMBIC.get();
            case FRAME_EDGE -> ModBlocks.ALCHEMICAL_CONSTRUCT.get();
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == Part.CORE) {
            boolean living = context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity;
            if (!living) {
                return CORE_COLLISION;
            }
        }
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || state.getValue(PART) != Part.CORE || !(entity instanceof ItemEntity item)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof AdvancedAlchemicalFurnaceBlockEntity furnace && furnace.process(item.getItem())) {
            ItemStack stack = item.getItem();
            stack.shrink(1);
            RandomSource random = level.getRandom();
            level.playSound(null, item.getX(), item.getY(), item.getZ(), ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2F, 1.0F + random.nextFloat() * 0.4F);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(revertBlock(state.getValue(PART)).asItem()));
    }

    @Override
    protected void onRemove(BlockState state, Level worldLevel, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock()) || !(worldLevel instanceof ServerLevel level)) {
            super.onRemove(state, worldLevel, pos, newState, movedByPiston);
            return;
        }
        if (state.getValue(PART) == Part.CORE) {
            revertAround(level, pos, false);
        } else {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos candidate = pos.offset(x, y, z);
                        BlockState candidateState = level.getBlockState(candidate);
                        if (candidateState.is(this) && candidateState.getValue(PART) == Part.CORE) {
                            revertAround(level, candidate, true);
                            super.onRemove(state, level, pos, newState, movedByPiston);
                            return;
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void revertAround(ServerLevel level, BlockPos core, boolean includeCore) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos target = core.offset(x, y, z);
                    BlockState targetState = level.getBlockState(target);
                    if (targetState.is(this)) {
                        level.setBlock(target, revertBlock(targetState.getValue(PART)).defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        if (includeCore) {
            level.setBlock(core, ModBlocks.ALCHEMICAL_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AdvancedAlchemicalFurnaceNozzleBlockEntity nozzle && nozzle.getFurnace() != null) {
            AdvancedAlchemicalFurnaceBlockEntity furnace = nozzle.getFurnace();
            float ratio = (float) furnace.getVis() / furnace.getMaxVis();
            return (int) Math.floor(ratio * 14.0F) + (furnace.getVis() > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (state.getValue(PART)) {
            case CORE -> new AdvancedAlchemicalFurnaceBlockEntity(pos, state);
            case NOZZLE -> new AdvancedAlchemicalFurnaceNozzleBlockEntity(pos, state);
            default -> null;
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(PART) != Part.CORE || type != ModBlockEntities.ADVANCED_ALCHEMICAL_FURNACE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> AdvancedAlchemicalFurnaceBlockEntity.serverTick(tickLevel, pos, tickState, (AdvancedAlchemicalFurnaceBlockEntity) entity);
    }
}
