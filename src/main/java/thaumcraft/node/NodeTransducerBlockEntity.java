package thaumcraft.node;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;

public class NodeTransducerBlockEntity extends TcBlockEntity {
    private int count = -1;
    private int status;

    public NodeTransducerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NODE_TRANSDUCER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NodeTransducerBlockEntity entity) {
        entity.tick(level);
    }

    public int getCount() {
        return count;
    }

    public int getStatus() {
        return status;
    }

    private boolean isAuraNode(BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.AURA_NODE.get());
    }

    private boolean isEnergizedNode(BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.ENERGIZED_AURA_NODE.get());
    }

    public boolean hasStabilizer() {
        BlockPos stabilizerPos = worldPosition.below(2);
        return !level.hasNeighborSignal(stabilizerPos) && level.getBlockState(stabilizerPos).getBlock() instanceof NodeStabilizerBlock;
    }

    private void syncServer() {
        if (level != null && !level.isClientSide()) {
            sync();
        }
    }

    private void tick(Level tickLevel) {
        boolean server = !tickLevel.isClientSide();
        BlockPos nodePos = worldPosition.below();
        if (count == -1) {
            checkStatus();
        }
        if (status == 1 && server && count >= 1000) {
            if (isAuraNode(nodePos) && tickLevel.getBlockEntity(nodePos) instanceof Node node) {
                AspectList base = node.getAspectsBase();
                NodeType type = node.getNodeType();
                NodeModifier modifier = node.getNodeModifier();
                String id = node.getId();
                tickLevel.setBlock(nodePos, ModBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
                if (tickLevel.getBlockEntity(nodePos) instanceof EnergizedNodeBlockEntity energized) {
                    energized.setNodeModifier(modifier);
                    energized.setNodeType(type);
                    energized.setAspects(base.copy());
                    energized.setId(id);
                    energized.setupNode();
                }
                checkStatus();
                tickLevel.blockEvent(worldPosition, getBlockState().getBlock(), 10, 10);
                sync();
            }
        }
        if (status == 2 && server && count <= 50) {
            if (tickLevel.getBlockEntity(nodePos) instanceof EnergizedNodeBlockEntity energized) {
                AspectList base = energized.getAuraBase();
                NodeType type = energized.getNodeType();
                NodeModifier modifier = energized.getNodeModifier();
                String id = energized.getId();
                tickLevel.setBlock(nodePos, ModBlocks.AURA_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
                if (tickLevel.getBlockEntity(nodePos) instanceof Node node) {
                    node.setNodeModifier(modifier);
                    node.setNodeType(type);
                    node.setAspects(base.copy());
                    node.setId(id);
                    for (Aspect aspect : node.getAspects().getAspects()) {
                        node.takeFromContainer(aspect, node.getAspects().getAmount(aspect));
                    }
                    if (node instanceof NodeBlockEntity restored) {
                        restored.sync();
                    }
                }
                tickLevel.blockEvent(worldPosition, getBlockState().getBlock(), 10, 10);
                status = 0;
                sync();
            }
        }
        boolean progressing = false;
        if (status != 0 && tickLevel.hasNeighborSignal(worldPosition)) {
            if (count < 1000) {
                count++;
                progressing = true;
                if (server && isAuraNode(nodePos) && tickLevel.getBlockEntity(nodePos) instanceof Node node) {
                    AspectList aspects = node.getAspects();
                    if (aspects.size() > 0) {
                        List<Aspect> list = aspects.getAspects();
                        node.takeFromContainer(list.get(tickLevel.getRandom().nextInt(list.size())), 1);
                        if (count % 5 == 0 || node.getAspects().visSize() == 0) {
                            tickLevel.sendBlockUpdated(nodePos, tickLevel.getBlockState(nodePos), tickLevel.getBlockState(nodePos), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        } else if (count > 0) {
            count--;
            progressing = true;
        }
        if (!server && progressing && count > 50) {
            var random = tickLevel.getRandom();
            if (random.nextBoolean()) thaumcraft.fx.Fx.get().nodeBolt(tickLevel, worldPosition.getX() + 0.25F + random.nextFloat() * 0.5F, worldPosition.getY() + 0.5F, worldPosition.getZ() + 0.25F + random.nextFloat() * 0.5F, worldPosition.getX() + 0.5F, worldPosition.getY() - 0.5F, worldPosition.getZ() + 0.5F);
            if (random.nextBoolean() && hasStabilizer()) thaumcraft.fx.Fx.get().nodeBolt(tickLevel, worldPosition.getX() + 0.25F + random.nextFloat() * 0.5F, worldPosition.getY() - 1.5F, worldPosition.getZ() + 0.25F + random.nextFloat() * 0.5F, worldPosition.getX() + 0.5F, worldPosition.getY() - 0.5F, worldPosition.getZ() + 0.5F);
        }
        if (count > 1000) {
            count = 1000;
        }
    }

    public void checkStatus() {
        if (level == null) {
            return;
        }
        BlockPos nodePos = worldPosition.below();
        if (count == -1) {
            count = 0;
        }
        if (status != 2 || count <= 50 || hasStabilizer() && isEnergizedNode(nodePos)) {
            if (level.hasNeighborSignal(worldPosition) && isAuraNode(nodePos) && hasStabilizer()) {
                status = 1;
                syncServer();
            } else if (isEnergizedNode(nodePos)) {
                status = 2;
                count = 1000;
                syncServer();
            } else {
                status = 0;
            }
        } else {
            EnergizedNodeBlock.explodify(level, nodePos);
            status = 0;
            count = 50;
            syncServer();
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 10 && param == 10) {
            if (level != null && level.isClientSide()) {
                thaumcraft.fx.Fx.get().shockBurst(level, worldPosition.getX() + 0.5, worldPosition.getY() - 0.5, worldPosition.getZ() + 0.5, 1.0F);
                level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() - 0.5, worldPosition.getZ() + 0.5, ModSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 0.5F, 1.0F, false);
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        status = input.getIntOr("status", 0);
        count = input.getIntOr("count", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("status", status);
        output.putInt("count", count);
    }
}
