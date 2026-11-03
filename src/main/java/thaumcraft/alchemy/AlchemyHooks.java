package thaumcraft.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.EssentiaKeys;
import thaumcraft.entity.monster.ThaumicSlime;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.taint.TaintSpread;

public final class AlchemyHooks {
    private AlchemyHooks() {
    }

    public static boolean isNitor(BlockGetter level, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.NITOR_LIGHT.get());
    }

    public static boolean spawnThaumicSlime(ServerLevel level, BlockPos pos, int size) {
        return spawnThaumicSlime(level, Vec3.atBottomCenterOf(pos), size);
    }

    public static boolean spawnThaumicSlime(ServerLevel level, Vec3 position, int size) {
        ThaumicSlime slime = ModEntities.THAUMIC_SLIME.get().create(level);
        if (slime == null) {
            return false;
        }
        slime.setSlimeSize(size);
        slime.setPos(position);
        return level.addFreshEntity(slime);
    }

    public static boolean taintFromFlux(ServerLevel level, BlockPos pos) {
        return TaintSpread.taintFromFlux(level, pos);
    }

    public static boolean isThaumicSlime(Entity entity) {
        return entity instanceof ThaumicSlime;
    }

    public static int getSlimeSize(Entity entity) {
        return entity instanceof ThaumicSlime slime ? slime.getSlimeSize() : 0;
    }

    public static void setSlimeSize(Entity entity, int size) {
        if (entity instanceof ThaumicSlime slime) {
            slime.setSlimeSize(size);
        }
    }

    public static @Nullable Aspect getJarFilter(ItemStack jar) {
        return EssentiaKeys.FILTER.get(jar);
    }
}
