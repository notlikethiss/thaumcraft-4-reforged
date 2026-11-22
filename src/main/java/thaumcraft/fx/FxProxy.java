package thaumcraft.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface FxProxy {
    default void wardingStone(Level level, BlockPos pos) {
    }

    default void beamPulse(Level level, Entity source, Entity target, int color) {
    }

    default void hungryNode(Level level, BlockPos pos) {
    }

    default void nodeBolt(Level level, float x, float y, float z, float targetX, float targetY, float targetZ) {
    }

    default void visDrain(Level level, BlockPos drainer, BlockPos source, int primal) {
    }

    default void blockSparkle(Level level, BlockPos pos, int color, int count) {
    }

    default void crucibleBoil(Level level, BlockPos pos, float fluidHeight, int aspectAmount, int count) {
    }

    default void crucibleBubble(Level level, float x, float y, float z, float red, float green, float blue) {
    }

    default void crucibleFroth(Level level, float x, float y, float z) {
    }

    default void crucibleFrothDown(Level level, float x, float y, float z) {
    }

    default void ventParticles(Level level, double x, double y, double z, double motionX, double motionY, double motionZ, int color, float scale) {
    }

    default void essentiaSource(Level level, BlockPos pos, int dx, int dy, int dz, int color) {
    }

    default void essentiaTrail(Level level, BlockPos from, BlockPos to, int count, int color, float scale) {
    }

    default void infusionItemParticles(Level level, double x, double y, double z, BlockPos target, ItemStack stack) {
    }

    default void infusionBlockParticles(Level level, double x, double y, double z, BlockPos target, BlockState state) {
    }

    default void infusionSparkle(Level level, double x, double y, double z, BlockPos target, boolean alternate) {
    }

    default void blockRunes(Level level, double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
    }

    default void sparkle(Level level, float x, float y, float z, float size, int color, float gravity) {
    }

    default void shockBolt(Level level, Entity shooter, double x, double y, double z) {
    }

    default void shockZap(Level level, Entity source, Entity target) {
    }

    default void shockBurst(Level level, double x, double y, double z, float size) {
    }

    default void fireOrbTrail(Level level, double x, double y, double z, float scale) {
    }

    default void alumentumTrail(Level level, Entity projectile) {
    }

    default void eldritchOrbBurst(Level level, Entity orb) {
    }

    default void golemOrbBurst(Level level, double x, double y, double z, float size) {
    }

    default void purifyingFluid(Level level, BlockPos pos, int fluidLevel, RandomSource random) {
    }

    default void liquidDeath(Level level, BlockPos pos, int fluidLevel, RandomSource random) {
    }

    default void primalOrbTrail(Level level, Entity orb) {
    }

    default void primalOrbBurst(Level level, double x, double y, double z) {
    }

    default void pechBlastTrail(Level level, Entity blast) {
    }

    default void pechBlastBurst(Level level, double x, double y, double z) {
    }

    default void smokeSpiral(Level level, double x, double y, double z, float radius, int start, int minY, int color) {
    }

    default void oreScan(Level level, BlockPos center, int range, long until) {
    }

    default void blockBubble(Level level, BlockPos pos, int color) {
    }

    default void excavationBeam(Level level, Entity source, double x, double y, double z, int impact) {
    }

    default void excavationCrack(Level level, Entity source, BlockPos pos, BlockState state, int progress) {
    }

    default void focalManipulatorSparkle(Level level, double x, double y, double z) {
    }

    default void nitorParticles(Level level, BlockPos pos) {
    }

    default void nitorSparkle(Level level, BlockPos pos) {
    }

    default void crystalSpark(Level level, double x, double y, double z, float size, int color) {
    }

    default void mirrorTransfer(Level level, BlockPos pos, net.minecraft.core.Direction facing) {
    }

    default void sinisterNode(Level level, BlockPos pos) {
    }

    default void taintSwarm(Level level, Entity target, int maxParticles, float speed, float turnSpeed, float gravity) {
    }
}
