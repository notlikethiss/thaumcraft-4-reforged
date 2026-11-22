package thaumcraft.client.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.alchemy.CrucibleBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.geom.LightningBolt;
import thaumcraft.client.platform.ParticleSprites;
import thaumcraft.client.platform.WorldFxRenderer;
import thaumcraft.entity.monster.taint.TaintSwarm;
import thaumcraft.fx.FxProxy;

public final class ClientFx implements FxProxy {
    private static final int EXCAVATION_COLOR = 65382;
    private final Map<Integer, WandBeamFx> beams = new HashMap<>();
    private final Map<UUID, List<TaintSwarmFx>> taintSwarmParticles = new HashMap<>();

    @Override
    public void beamPulse(Level level, Entity source, Entity target, int color) {
        BeamFx beam = new BeamFx(
            level,
            source.getX(),
            source.getY() + source.getEyeHeight(),
            source.getZ(),
            target,
            (color >> 16 & 255) / 255.0F,
            (color >> 8 & 255) / 255.0F,
            (color & 255) / 255.0F,
            20
        );
        beam.setBlendMode(771);
        beam.setWidth(2.5F);
        beam.setType(1);
        beam.setReverse(true);
        beam.setPulse(true);
        WorldFxRenderer.add(beam);
    }

    @Override
    public void hungryNode(Level level, BlockPos pos) {
        var random = level.getRandom();
        int x = pos.getX() + random.nextInt(16) - random.nextInt(16);
        int y = pos.getY() + random.nextInt(16) - random.nextInt(16);
        int z = pos.getZ() + random.nextInt(16) - random.nextInt(16);
        if (!level.hasChunkAt(new BlockPos(x, pos.getY(), z))) return;
        y = Math.min(y, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(net.minecraft.world.phys.Vec3.atCenterOf(pos), new net.minecraft.world.phys.Vec3(x + 0.5, y + 0.5, z + 0.5), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || pos.distSqr(hit.getBlockPos()) >= 256) return;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        var sprite = ParticleSprites.block(state);
        if (sprite == null) return;
        var source = hit.getBlockPos();
        WorldFxRenderer.add(new BoreParticleFx(random, source.getX() + random.nextFloat(), source.getY() + random.nextFloat(), source.getZ() + random.nextFloat(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sprite));
    }

    @Override
    public void nodeBolt(Level level, float x, float y, float z, float targetX, float targetY, float targetZ) {
        BoltRenderer.add(new LightningBolt(x, y, z, targetX, targetY, targetZ, level.getRandom().nextLong(), 10, 4.0F, 5).setType(0).defaultFractal());
    }

    @Override
    public void visDrain(Level level, BlockPos drainer, BlockPos source, int primal) {
        List<Aspect> primals = Aspect.getPrimalAspects();
        if (primal < 0 || primal >= primals.size()) {
            return;
        }
        RandomSource random = level.getRandom();
        WorldFxRenderer.add(new VisSparkleFx(
            random,
            source.getX() + 0.4 + random.nextFloat() * 0.2F,
            source.getY() + 0.4 + random.nextFloat() * 0.2F,
            source.getZ() + 0.4 + random.nextFloat() * 0.2F,
            drainer.getX() + random.nextFloat(),
            drainer.getY() + random.nextFloat(),
            drainer.getZ() + random.nextFloat(),
            primals.get(primal).getColor()
        ));
    }

    public static int particleCount(int base) {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        if (status == ParticleStatus.MINIMAL) {
            return 0;
        }
        return status == ParticleStatus.DECREASED ? base : base * 2;
    }

    @Override
    public void taintSwarm(Level level, Entity target, int maxParticles, float speed, float turnSpeed, float gravity) {
        if (!level.isClientSide() || target.isRemoved()) {
            return;
        }
        taintSwarmParticles.values().forEach(particles -> particles.removeIf(TaintSwarmFx::isDead));
        taintSwarmParticles.values().removeIf(List::isEmpty);
        int targetCount = target instanceof TaintSwarm ? Math.max(particleCount(maxParticles), 10) : maxParticles;
        if (targetCount <= 0) {
            return;
        }
        List<TaintSwarmFx> particles = taintSwarmParticles.computeIfAbsent(target.getUUID(), ignored -> new ArrayList<>());
        if (particles.size() < targetCount) {
            TaintSwarmFx particle = new TaintSwarmFx(level, target, level.getRandom(), speed, turnSpeed, gravity);
            particles.add(particle);
            WorldFxRenderer.add(particle);
        }
    }

    @Override
    public void blockSparkle(Level level, BlockPos pos, int color, int count) {
        RandomSource random = level.getRandom();
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        for (int index = 0; index < particleCount(count); index++) {
            if (color == -9999) {
                red = 0.33F + random.nextFloat() * 0.67F;
                green = 0.33F + random.nextFloat() * 0.67F;
                blue = 0.33F + random.nextFloat() * 0.67F;
            }
            GenericFx fx = new GenericFx(
                random,
                pos.getX() - 0.1F + random.nextFloat() * 1.2F,
                pos.getY() - 0.1F + random.nextFloat() * 1.2F,
                pos.getZ() - 0.1F + random.nextFloat() * 1.2F,
                0.0,
                random.nextFloat() * 0.02,
                0.0,
                false,
                112,
                9,
                1,
                5 + random.nextInt(8),
                random.nextInt(10),
                0.7F + random.nextFloat() * 0.4F
            );
            fx.color(red - 0.2F + random.nextFloat() * 0.4F, green - 0.2F + random.nextFloat() * 0.4F, blue - 0.2F + random.nextFloat() * 0.4F);
            fx.alpha(0.9F);
            WorldFxRenderer.add(fx);
        }
    }

    @Override
    public void crucibleBoil(Level level, BlockPos pos, float fluidHeight, int aspectAmount, int count) {
        RandomSource random = level.getRandom();
        List<Aspect> aspects = level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible ? crucible.getAspects().getAspects() : List.of();
        for (int index = 0; index < particleCount(1); index++) {
            BubbleFx fx = new BubbleFx(
                random,
                pos.getX() + 0.2F + random.nextFloat() * 0.6F,
                pos.getY() + 0.1F + fluidHeight,
                pos.getZ() + 0.2F + random.nextFloat() * 0.6F,
                3
            );
            if (aspects.isEmpty()) {
                fx.color(1.0F, 1.0F, 1.0F);
            } else {
                int color = aspects.get(random.nextInt(aspects.size())).getColor();
                fx.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
            }
            fx.bubbleSpeed(0.003 * count);
            WorldFxRenderer.add(fx);
        }
    }

    @Override
    public void crucibleBubble(Level level, float x, float y, float z, float red, float green, float blue) {
        BubbleFx fx = new BubbleFx(level.getRandom(), x, y, z, 1);
        fx.color(red, green, blue);
        WorldFxRenderer.add(fx);
    }

    @Override
    public void crucibleFroth(Level level, float x, float y, float z) {
        BubbleFx fx = new BubbleFx(level.getRandom(), x, y, z, -4);
        fx.color(0.5F, 0.5F, 0.7F);
        fx.froth();
        WorldFxRenderer.add(fx);
    }

    @Override
    public void crucibleFrothDown(Level level, float x, float y, float z) {
        BubbleFx fx = new BubbleFx(level.getRandom(), x, y, z, -4);
        fx.color(0.5F, 0.5F, 0.7F);
        fx.froth2();
        WorldFxRenderer.add(fx);
    }

    @Override
    public void ventParticles(Level level, double x, double y, double z, double motionX, double motionY, double motionZ, int color, float scale) {
        WorldFxRenderer.add(new VentFx(level.getRandom(), x, y, z, motionX, motionY, motionZ, color, scale));
    }

    @Override
    public void essentiaSource(Level level, BlockPos pos, int dx, int dy, int dz, int color) {
        EssentiaSourceTracker.add(pos, pos.offset(-dx, -dy, -dz), color);
    }

    @Override
    public void essentiaTrail(Level level, BlockPos from, BlockPos to, int count, int color, float scale) {
        WorldFxRenderer.add(new EssentiaTrailFx(
            level.getRandom(),
            from.getX() + 0.5,
            from.getY() + 0.5,
            from.getZ() + 0.5,
            to.getX() + 0.5,
            to.getY() + 0.5,
            to.getZ() + 0.5,
            count,
            color,
            scale
        ));
    }

    @Override
    public void infusionItemParticles(Level level, double x, double y, double z, BlockPos target, ItemStack stack) {
        RandomSource random = level.getRandom();
        TextureAtlasSprite sprite = ParticleSprites.item(level, stack, random);
        if (sprite == null) {
            return;
        }
        BoreParticleFx fx = new BoreParticleFx(random, x, y, z, target.getX() + 0.5, target.getY() - 0.5, target.getZ() + 0.5, sprite);
        fx.motion((float) random.nextGaussian() * 0.03F, (float) random.nextGaussian() * 0.03F, (float) random.nextGaussian() * 0.03F);
        WorldFxRenderer.add(fx);
    }

    @Override
    public void infusionBlockParticles(Level level, double x, double y, double z, BlockPos target, BlockState state) {
        TextureAtlasSprite sprite = ParticleSprites.block(state);
        if (sprite == null) {
            return;
        }
        WorldFxRenderer.add(new BoreParticleFx(level.getRandom(), x, y, z, target.getX() + 0.5, target.getY() - 0.5, target.getZ() + 0.5, sprite));
    }

    @Override
    public void infusionSparkle(Level level, double x, double y, double z, BlockPos target, boolean alternate) {
        RandomSource random = level.getRandom();
        BoreSparkleFx fx = new BoreSparkleFx(random, x, y, z, target.getX() + 0.5, target.getY() - 0.5, target.getZ() + 0.5);
        if (alternate) {
            fx.color(0.2F, 0.6F + random.nextFloat() * 0.3F, 0.3F);
        } else {
            fx.color(0.4F + random.nextFloat() * 0.2F, 0.2F, 0.6F + random.nextFloat() * 0.3F);
        }
        WorldFxRenderer.add(fx);
    }

    @Override
    public void blockRunes(Level level, double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
        WorldFxRenderer.add(new BlockRunesFx(level.getRandom(), x + 0.5, y + 0.5, z + 0.5, red, green, blue, duration, gravity));
    }

    private static float jitter(RandomSource random, float amount) {
        return (random.nextFloat() - random.nextFloat()) * amount;
    }

    private static void wisp(Level level, double x, double y, double z, float size, int type, boolean shrink, float gravity) {
        WispFx fx = new WispFx(level.getRandom(), x, y, z, size, type, null);
        fx.shrink(shrink);
        fx.gravity(gravity);
        WorldFxRenderer.add(fx);
    }

    private static void wispTo(Level level, double x, double y, double z, double targetX, double targetY, double targetZ, float size, int type, boolean shrink, float gravity) {
        WispFx fx = new WispFx(level.getRandom(), x, y, z, size, type, null);
        fx.moveTo(targetX, targetY, targetZ);
        fx.shrink(shrink);
        fx.gravity(gravity);
        WorldFxRenderer.add(fx);
    }

    private static void bolt(Level level, double x, double y, double z, double targetX, double targetY, double targetZ) {
        BoltRenderer.add(new LightningBolt(x, y, z, targetX, targetY, targetZ, level.getRandom().nextLong(), 6, 0.5F, 8).defaultFractal().setType(2).setWidth(0.125F));
    }

    @Override
    public void sparkle(Level level, float x, float y, float z, float size, int color, float gravity) {
        RandomSource random = level.getRandom();
        if (random.nextInt(6) < particleCount(2)) {
            SparkleFx fx = new SparkleFx(random, x, y, z, size, color, 6);
            fx.gravity(gravity);
            WorldFxRenderer.add(fx);
        }
    }

    @Override
    public void alumentumTrail(Level level, Entity projectile) {
        RandomSource random = level.getRandom();
        double x = projectile.getX();
        double y = projectile.getY();
        double z = projectile.getZ();
        for (int index = 0; index < particleCount(3); index++) {
            wisp(level, x + jitter(random, 0.12F), y + jitter(random, 0.12F), z + jitter(random, 0.12F), 0.45F, 7, true, 0.0F);
        }
        for (int index = 0; index < particleCount(6); index++) {
            WispFx fx = new WispFx(random, x + jitter(random, 0.18F), y + jitter(random, 0.18F), z + jitter(random, 0.18F), 0.22F, 4, null);
            fx.moveTo(x - projectile.getDeltaMovement().x * 2.0, y - projectile.getDeltaMovement().y * 2.0, z - projectile.getDeltaMovement().z * 2.0);
            fx.gravity(0.0F);
            WorldFxRenderer.add(fx);
        }
        if (particleCount(1) > 0) {
            SparkleFx sparkle = new SparkleFx(random, x, y, z, 0.7F, 7, 1);
            sparkle.gravity(0.0F);
            WorldFxRenderer.add(sparkle);
        }
    }

    @Override
    public void eldritchOrbBurst(Level level, Entity orb) {
        RandomSource random = level.getRandom();
        double x = orb.getX();
        double y = orb.getY();
        double z = orb.getZ();
        for (int index = 0; index < 30; index++) {
            float dx = jitter(random, 0.3F);
            float dy = jitter(random, 0.3F);
            float dz = jitter(random, 0.3F);
            wispTo(level, x + dx, y + dy, z + dz, x + dx * 8.0, y + dy * 8.0, z + dz * 8.0, 0.3F, 5, true, 0.02F);
        }
    }

    @Override
    public void golemOrbBurst(Level level, double x, double y, double z, float size) {
        WorldFxRenderer.add(new BurstFx(level.getRandom(), x, y, z, size));
    }

    @Override
    public void purifyingFluid(Level level, BlockPos pos, int fluidLevel, RandomSource random) {
        BubbleFx bubble = new BubbleFx(
            random,
            pos.getX() + random.nextFloat(),
            pos.getY() + 0.125F * (8 - fluidLevel),
            pos.getZ() + random.nextFloat(),
            0
        );
        bubble.color(1.0F, 1.0F, 1.0F).alpha(0.25F);
        WorldFxRenderer.add(bubble);
        if (random.nextInt(25) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.25F, 0.8F + random.nextFloat() * 0.4F, false);
        }
    }

    @Override
    public void liquidDeath(Level level, BlockPos pos, int fluidLevel, RandomSource random) {
        BubbleFx bubble = new BubbleFx(
            random,
            pos.getX() + random.nextFloat(),
            pos.getY() + 0.1F + 0.225F * fluidLevel,
            pos.getZ() + random.nextFloat(),
            0
        );
        bubble.color(0.65F, 0.1F, 0.8F);
        bubble.alpha(0.8F);
        bubble.scaleTo(0.075F + random.nextFloat() * 0.075F);
        WorldFxRenderer.add(bubble);
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.25F, 0.7F + random.nextFloat() * 0.3F, false);
        }
    }

    @Override
    public void shockBolt(Level level, Entity shooter, double x, double y, double z) {
        boolean local = shooter == Minecraft.getInstance().player;
        float yaw = shooter.getYRot() / 180.0F * (float) Math.PI;
        double px = shooter.getX() - Mth.cos(yaw) * 0.06F;
        double py = local ? shooter.getEyeY() - 0.06F : shooter.getY() + shooter.getBbHeight() / 2.0F + 0.25;
        double pz = shooter.getZ() - Mth.sin(yaw) * 0.06F;
        Vec3 look = shooter.getViewVector(1.0F);
        bolt(level, px + look.x * 0.3, py + look.y * 0.3, pz + look.z * 0.3, x, y, z);
    }

    @Override
    public void shockZap(Level level, Entity source, Entity target) {
        bolt(
            level,
            source.getX(),
            source.getY() + source.getBbHeight() / 2.0F,
            source.getZ(),
            target.getX(),
            target.getY() + target.getBbHeight() / 2.0F,
            target.getZ()
        );
    }

    @Override
    public void shockBurst(Level level, double x, double y, double z, float size) {
        WorldFxRenderer.add(new BurstFx(level.getRandom(), x, y, z, size));
    }

    @Override
    public void fireOrbTrail(Level level, double x, double y, double z, float scale) {
        RandomSource random = level.getRandom();
        GenericFx fx = new GenericFx(random, x, y, z, 0.0, 0.0, 0.0, false, 151, 9, 1, 7 + random.nextInt(5), 0, scale);
        fx.color(1.0F, 1.0F, 1.0F);
        fx.alpha(0.8F);
        WorldFxRenderer.add(fx);
    }

    @Override
    public void primalOrbTrail(Level level, Entity orb) {
        RandomSource random = level.getRandom();
        wisp(level, orb.getX() + jitter(random, 0.2F), orb.getY() + jitter(random, 0.2F), orb.getZ() + jitter(random, 0.2F), 0.1F, random.nextInt(6), true, 0.0F);
    }

    @Override
    public void primalOrbBurst(Level level, double x, double y, double z) {
        RandomSource random = level.getRandom();
        for (int a = 0; a < 6; a++) {
            for (int b = 0; b < 6; b++) {
                float fx = jitter(random, 0.5F);
                float fy = jitter(random, 0.5F);
                float fz = jitter(random, 0.5F);
                wispTo(level, x + fx, y + fy, z + fz, x + fx * 10.0F, y + fy * 10.0F, z + fz * 10.0F, 0.4F, b, true, 0.05F);
            }
        }
    }

    @Override
    public void pechBlastTrail(Level level, Entity blast) {
        RandomSource random = level.getRandom();
        for (int a = 0; a < 3; a++) {
            wisp(level, blast.getX() + jitter(random, 0.2F), blast.getY() + jitter(random, 0.2F), blast.getZ() + jitter(random, 0.2F), 0.3F, 3, true, 0.02F);
            wisp(
                level,
                (blast.getX() + blast.xo) / 2.0 + jitter(random, 0.2F),
                (blast.getY() + blast.yo) / 2.0 + jitter(random, 0.2F),
                (blast.getZ() + blast.zo) / 2.0 + jitter(random, 0.2F),
                0.3F,
                2,
                true,
                0.02F
            );
            sparkle(level, (float) blast.getX() + jitter(random, 0.1F), (float) blast.getY() + jitter(random, 0.1F), (float) blast.getZ() + jitter(random, 0.1F), 1.5F, 5, 0.0F);
        }
    }

    @Override
    public void pechBlastBurst(Level level, double x, double y, double z) {
        RandomSource random = level.getRandom();
        int[] types = {3, 2, 0};
        for (int a = 0; a < 9; a++) {
            for (int type : types) {
                float fx = jitter(random, 0.3F);
                float fy = jitter(random, 0.3F);
                float fz = jitter(random, 0.3F);
                wispTo(level, x + fx, y + fy, z + fz, x + fx * 8.0F, y + fy * 8.0F, z + fz * 8.0F, 0.3F, type, true, 0.02F);
            }
        }
    }

    @Override
    public void smokeSpiral(Level level, double x, double y, double z, float radius, int start, int minY, int color) {
        WorldFxRenderer.add(new SmokeSpiralFx(level.getRandom(), x, y, z, radius, start, minY, color));
    }

    @Override
    public void oreScan(Level level, BlockPos center, int range, long until) {
        OreScanFx.start(level, center, range, until);
    }

    @Override
    public void blockBubble(Level level, BlockPos pos, int color) {
        RandomSource random = level.getRandom();
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (int a = 0; a < particleCount(1); a++) {
            crucibleBubble(level, x, y + random.nextFloat(), z + random.nextFloat(), red, green, blue);
            crucibleBubble(level, x + 1, y + random.nextFloat(), z + random.nextFloat(), red, green, blue);
            crucibleBubble(level, x + random.nextFloat(), y + random.nextFloat(), z, red, green, blue);
            crucibleBubble(level, x + random.nextFloat(), y + random.nextFloat(), z + 1, red, green, blue);
        }
    }

    @Override
    public void excavationBeam(Level level, Entity source, double x, double y, double z, int impact) {
        WandBeamFx beam = beams.get(source.getId());
        if (beam != null && beam.isFor(source)) {
            beam.retarget(x, y, z);
        } else {
            beam = new WandBeamFx(
                level,
                source,
                x,
                y,
                z,
                (EXCAVATION_COLOR >> 16 & 255) / 255.0F,
                (EXCAVATION_COLOR >> 8 & 255) / 255.0F,
                (EXCAVATION_COLOR & 255) / 255.0F,
                8
            );
            beam.setType(2);
            beam.setReverse(false);
            WorldFxRenderer.add(beam);
            beams.put(source.getId(), beam);
        }
        beam.setEndMod(impact > 0 ? 2.0F : 0.0F);
        beam.setImpact(impact);
        beams.values().removeIf(WandBeamFx::isDead);
    }

    @Override
    public void excavationCrack(Level level, Entity source, BlockPos pos, BlockState state, int progress) {
        level.destroyBlockProgress(source.getId(), pos, progress);
    }

    @Override
    public void focalManipulatorSparkle(Level level, double x, double y, double z) {
        RandomSource random = level.getRandom();
        GenericFx fx = new GenericFx(random, x, y, z, 0.0, 0.0, 0.0, false, 112, 9, 1, 6 + random.nextInt(5), 0, 0.7F + random.nextFloat() * 0.4F);
        fx.color(0.5F + random.nextFloat() * 0.4F, 1.0F - random.nextFloat() * 0.4F, 1.0F - random.nextFloat() * 0.4F);
        fx.alpha(0.8F);
        WorldFxRenderer.add(fx);
    }

    @Override
    public void wardingStone(Level level, BlockPos pos) {
        DeviceFx.wardingStone(level, pos);
    }

    @Override
    public void nitorParticles(Level level, BlockPos pos) {
        DeviceFx.nitorParticles(level, pos);
    }

    @Override
    public void nitorSparkle(Level level, BlockPos pos) {
        DeviceFx.nitorSparkle(level, pos);
    }

    @Override
    public void crystalSpark(Level level, double x, double y, double z, float size, int color) {
        DeviceFx.crystalSpark(level, x, y, z, size, color);
    }

    @Override
    public void mirrorTransfer(Level level, BlockPos pos, net.minecraft.core.Direction facing) {
        RandomSource random = level.getRandom();
        for (int index = 0; index < particleCount(1); index++) {
            level.addParticle(
                net.minecraft.core.particles.ColorParticleOption.create(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, net.minecraft.util.ARGB.colorFromFloat(0.5F, 0.0F, 0.0F, 0.0F)),
                pos.getX() + 0.33 + random.nextFloat() * 0.33F - facing.getStepX() / 2.0,
                pos.getY() + 0.33 + random.nextFloat() * 0.33F - facing.getStepY() / 2.0,
                pos.getZ() + 0.33 + random.nextFloat() * 0.33F - facing.getStepZ() / 2.0,
                facing.getStepX() * 0.05,
                facing.getStepY() * 0.05,
                facing.getStepZ() * 0.05
            );
        }
    }

    @Override
    public void sinisterNode(Level level, BlockPos pos) {
        thaumcraft.client.render.device.SinisterNodes.add(level.dimension(), pos);
    }
}
