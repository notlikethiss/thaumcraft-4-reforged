package thaumcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModSounds;
import thaumcraft.taint.TaintBiomes;

public class EtherealBloomBlockEntity extends TcBlockEntity {
    private int counter;
    private int growthCounter;

    public EtherealBloomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETHEREAL_BLOOM.get(), pos, state);
    }

    public int getCounter() {
        return this.counter;
    }

    public int getGrowthCounter() {
        return this.growthCounter;
    }

    public static void tick(Level level, BlockPos pos, EtherealBloomBlockEntity bloom) {
        if (bloom.counter == 0) {
            bloom.counter = level.getRandom().nextInt(100);
        }
        bloom.counter++;
        if (level.isClientSide()) {
            if (bloom.growthCounter == 0) {
                level.playLocalSound(pos, ModSounds.ROOTS.get(), SoundSource.BLOCKS, 1.0F, 0.6F, false);
            }
            bloom.growthCounter++;
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        if (bloom.counter % 20 != 0) {
            bloom.growthCounter++;
            return;
        }
        int x = serverLevel.getRandom().nextInt(8) - serverLevel.getRandom().nextInt(8);
        int z = serverLevel.getRandom().nextInt(8) - serverLevel.getRandom().nextInt(8);
        BlockPos target = pos.offset(x, 0, z);
        if (pos.distToCenterSqr(target.getX() + 0.5, pos.getY(), target.getZ() + 0.5) <= 81.0) {
            TaintBiomes.purify(serverLevel, target);
        }
        bloom.growthCounter++;
    }
}
