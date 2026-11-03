package thaumcraft.client.fx;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;

final class TaintSwarmFx extends ParticleFx {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final List<Long> BUZZ_TIMES = new ArrayList<>();
    private final net.minecraft.world.level.Level level;
    private final Entity target;
    private final float speed;
    private final float turnSpeed;
    private int deathTimer;
    private float yaw;
    private float pitch;

    TaintSwarmFx(net.minecraft.world.level.Level level, Entity target, RandomSource random, float speed, float turnSpeed, float gravity) {
        super(
            random,
            target.getX() + (random.nextFloat() - random.nextFloat()) * 2.0F,
            target.getY() + (random.nextFloat() - random.nextFloat()) * 2.0F,
            target.getZ() + (random.nextFloat() - random.nextFloat()) * 2.0F,
            Integer.MAX_VALUE
        );
        this.level = level;
        this.target = target;
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.gravity = gravity;
        this.scale = random.nextFloat() * 0.5F + 1.0F;
        this.red = 0.8F + random.nextFloat() * 0.2F;
        this.green = random.nextFloat() * 0.4F;
        this.blue = 1.0F - random.nextFloat() * 0.2F;
        this.motionX = (random.nextFloat() - random.nextFloat()) * 0.2F;
        this.motionY = (random.nextFloat() - random.nextFloat()) * 0.2F;
        this.motionZ = (random.nextFloat() - random.nextFloat()) * 0.2F;
        this.yaw = 0.0F;
        this.pitch = 0.0F;
    }

    @Override
    public void tick() {
        prevX = x;
        prevY = y;
        prevZ = z;
        age++;
        boolean targetAlive = target != null && !target.isRemoved() && (!(target instanceof LivingEntity living) || living.deathTime == 0);
        if (!targetAlive) {
            deathTimer++;
            motionY -= gravity / 2.0F;
            if (deathTimer > 50) {
                dead = true;
            }
        } else {
            motionY += gravity;
        }

        pushOutOfBlocks();
        x += motionX;
        y += motionY;
        z += motionZ;
        motionX *= 0.985;
        motionY *= 0.985;
        motionZ *= 0.985;

        if (targetAlive) {
            boolean hurt = target instanceof LivingEntity living && living.hurtTime > 0;
            double dx = target.getX() - x;
            double dz = target.getZ() - z;
            double dy = target.getBoundingBox().getCenter().y - y;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            float wantedYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float wantedPitch = (float) (-Math.atan2(dy, horizontalDistance) * 180.0 / Math.PI);
            float yawStep = turnSpeed / 2.0F + random.nextInt(Math.max(1, (int) (turnSpeed / 2.0F)));
            float pitchStep = turnSpeed / 2.0F + random.nextInt(Math.max(1, (int) (turnSpeed / 2.0F)));
            if (xDistanceSquaredToTarget() > target.getBbWidth() && !hurt) {
                yaw = updateRotation(yaw, wantedYaw, yawStep);
                pitch = updateRotation(pitch, wantedPitch, pitchStep);
            } else {
                yaw = updateRotation(yaw, wantedYaw, -yawStep);
                pitch = updateRotation(pitch, wantedPitch, -pitchStep);
            }
            double headingX = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            double headingZ = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            double headingY = -Mth.sin(pitch * Mth.DEG_TO_RAD);
            double length = Math.sqrt(headingX * headingX + headingY * headingY + headingZ * headingZ);
            if (length > 1.0E-7) {
                headingX /= length;
                headingY /= length;
                headingZ /= length;
            }
            motionX = (headingX + random.nextGaussian() * 0.0075F * 15.0F) * speed;
            motionY = (headingY + random.nextGaussian() * 0.0075F * 15.0F) * speed;
            motionZ = (headingZ + random.nextGaussian() * 0.0075F * 15.0F) * speed;
        }
        tickBuzz();
    }

    private void tickBuzz() {
        long now = System.nanoTime();
        if (BUZZ_TIMES.size() < 3 && random.nextInt(50) == 0) {
            var player = Minecraft.getInstance().player;
            if (player != null && player.distanceToSqr(x, y, z) <= 64.0) {
                level.playLocalSound(BlockPos.containing(x, y, z), ModSounds.FLY.get(), SoundSource.HOSTILE, 0.03F,
                    0.5F + random.nextFloat() * 0.4F, false);
                BUZZ_TIMES.add(now + 1_500_000L);
            }
        }
        if (BUZZ_TIMES.size() >= 3 && BUZZ_TIMES.getFirst() < now) {
            BUZZ_TIMES.removeFirst();
        }
    }

    private double xDistanceSquaredToTarget() {
        double dx = x - target.getX();
        double dy = y - target.getY();
        double dz = z - target.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private void pushOutOfBlocks() {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(ModBlocks.FIBROUS_TAINT.get()) || !level.getFluidState(pos).isEmpty() || state.getCollisionShape(level, pos).isEmpty()) {
            return;
        }
        double localX = x - pos.getX();
        double localY = y - pos.getY();
        double localZ = z - pos.getZ();
        Direction nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Direction direction : DIRECTIONS) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockState(neighborPos).isSolidRender(level, neighborPos)) {
                continue;
            }
            double distance = switch (direction) {
                case WEST -> localX;
                case EAST -> 1.0 - localX;
                case DOWN -> localY;
                case UP -> 1.0 - localY;
                case NORTH -> localZ;
                case SOUTH -> 1.0 - localZ;
            };
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = direction;
            }
        }
        if (nearest == null) {
            return;
        }
        float push = random.nextFloat() * 0.05F + 0.025F;
        float drift = (random.nextFloat() - random.nextFloat()) * 0.1F;
        switch (nearest) {
            case WEST -> {
                motionX = -push;
                motionY = motionZ = drift;
            }
            case EAST -> {
                motionX = push;
                motionY = motionZ = drift;
            }
            case DOWN -> {
                motionY = -push;
                motionX = motionZ = drift;
            }
            case UP -> {
                motionY = push;
                motionX = motionZ = drift;
            }
            case NORTH -> {
                motionZ = -push;
                motionX = motionY = drift;
            }
            case SOUTH -> {
                motionZ = push;
                motionX = motionY = drift;
            }
        }
    }

    private static float updateRotation(float current, float wanted, float maxChange) {
        float difference = Mth.wrapDegrees(wanted - current);
        if (difference > maxChange) {
            difference = maxChange;
        }
        if (difference < -maxChange) {
            difference = -maxChange;
        }
        return current + difference;
    }

    @Override
    public int blendMode() {
        return 771;
    }

    @Override
    public void render(QuadSink sink, float partialTick, Vec3 camera, float cameraYaw, float cameraPitch) {
        float pulse = Mth.sin(age / 3.0F) * 0.25F + 1.0F;
        float fade = 0.75F * Math.max(0.0F, (50.0F - deathTimer) / 50.0F);
        float originalGreen = green;
        float originalBlue = blue;
        if (target instanceof LivingEntity living && living.hurtTime > 0) {
            green *= 0.5F;
            blue *= 0.5F;
        }
        float u0 = (7 + age % 8) / 16.0F;
        billboard(sink, partialTick, camera, cameraYaw, cameraPitch, 0.1F * scale * pulse, u0, 0.25F, u0 + CELL, 0.25F + CELL, fade);
        green = originalGreen;
        blue = originalBlue;
    }
}
