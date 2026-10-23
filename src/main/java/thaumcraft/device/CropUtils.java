package thaumcraft.device;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.Thaumcraft;
import thaumcraft.block.ManaPodBlock;
import thaumcraft.registry.ModBlocks;

public final class CropUtils {
    public static final TagKey<Block> LAMP_BLACKLIST = TagKey.create(Registries.BLOCK, Thaumcraft.id("lamp_blacklist"));

    private static final List<Predicate<BlockState>> STANDARD = new ArrayList<>();
    private static final List<Predicate<BlockState>> CLICKABLE = new ArrayList<>();
    private static final List<Predicate<BlockState>> STACKED = new ArrayList<>();

    static {
        addStandardCrop(state -> state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN));
        addStandardCrop(state -> state.is(ModBlocks.MANA_POD.get()) && state.getValue(ManaPodBlock.AGE) == 7);
        addStackedCrop(state -> state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS));
    }

    private CropUtils() {
    }

    public static void addStandardCrop(Predicate<BlockState> grown) {
        STANDARD.add(grown);
    }

    public static void addClickableCrop(Predicate<BlockState> grown) {
        CLICKABLE.add(grown);
    }

    public static void addStackedCrop(Predicate<BlockState> grown) {
        STACKED.add(grown);
    }

    public static boolean isStandardCrop(BlockState state) {
        return matches(STANDARD, state);
    }

    public static boolean isClickableCrop(BlockState state) {
        return matches(CLICKABLE, state);
    }

    public static boolean isStackedCrop(BlockState state) {
        return matches(STACKED, state);
    }

    private static boolean matches(List<Predicate<BlockState>> list, BlockState state) {
        for (Predicate<BlockState> predicate : list) {
            if (predicate.test(state)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGrownCrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        BlockState below = level.getBlockState(pos.below());
        return block instanceof BonemealableBlock bonemealable
                && !(block instanceof StemBlock)
                && !bonemealable.isValidBonemealTarget(level, pos, state)
            || block instanceof CropBlock crop && crop.isMaxAge(state) && !listedByBlock(block)
            || block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE
            || block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE
            || isStandardCrop(state)
            || isClickableCrop(state)
            || isStackedCrop(state) && below.is(block);
    }

    private static boolean listedByBlock(Block block) {
        return isStandardCrop(block.defaultBlockState()) || isClickableCrop(block.defaultBlockState()) || isStackedCrop(block.defaultBlockState());
    }

    public static boolean isPlant(BlockState state) {
        Block block = state.getBlock();
        return (block instanceof BonemealableBlock && !(block instanceof GrassBlock))
            || block instanceof VegetationBlock
            || block instanceof CactusBlock
            || block instanceof SugarCaneBlock
            || block instanceof CocoaBlock
            || block instanceof ManaPodBlock;
    }

    public static boolean doesLampGrow(BlockState state) {
        return !state.is(LAMP_BLACKLIST);
    }
}
