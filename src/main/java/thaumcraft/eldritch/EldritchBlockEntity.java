package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.blockentity.TcBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.EldritchEntities;
import thaumcraft.registry.ModSounds;

public final class EldritchBlockEntity extends TcBlockEntity {
    private int timer;
    private int count;
    private boolean triggered;
    private int eyes;
    private boolean spawner;
    private boolean open;
    private boolean spawnedClerics;
    private byte spawnType;
    private int venting;

    public EldritchBlockEntity(BlockPos pos, BlockState state) {
        super(EldritchContent.ELDRITCH_BLOCK_ENTITY.get(), pos, state);
        this.count = state.getBlock() instanceof EldritchBlock block && block.kind() == 10 ? 20 : 150;
    }

    public static EldritchBlockEntity create(BlockPos pos, BlockState state) {
        return new EldritchBlockEntity(pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EldritchBlockEntity entity) {
        if (level.isClientSide() && ((EldritchBlock) state.getBlock()).kind() == 9) {
            entity.tickVent(level, pos, state);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int kind = ((EldritchBlock) state.getBlock()).kind();
        if (kind == 0) {
            entity.tickAltar(serverLevel, pos);
        } else if (kind == 1 && state.getValue(EldritchBlock.PART) == DoubleBlockHalf.LOWER) {
            entity.tickObelisk(serverLevel, pos);
        } else if (kind == 8) {
            entity.tickLock(serverLevel, pos);
        } else if (kind == 9) {
            entity.tickCrabSpawner(serverLevel, pos, state);
        } else if (kind == 10) {
            entity.tickTrap(serverLevel, pos);
        }
    }

    private void tickObelisk(ServerLevel level, BlockPos pos) {
        if (this.timer++ % 20 != 0) {
            return;
        }
        AABB bounds = new AABB(pos).inflate(6.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity instanceof thaumcraft.entity.EldritchMob)) {
            if (!living.hasEffect(MobEffects.REGENERATION)) {
                living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0, true, false));
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
            }
        }
    }

    private void tickAltar(ServerLevel level, BlockPos pos) {
        if (!this.spawner || this.timer++ < 80 || this.timer % 40 != 0) {
            return;
        }
        if (this.spawnType == 0) {
            if (!this.spawnedClerics) {
                this.spawnClerics(level, pos);
            } else {
                this.spawnGuards(level, pos);
            }
        } else if (this.spawnType == 1) {
            this.spawnGuardian(level, pos);
        }
    }

    private void spawnClerics(ServerLevel level, BlockPos pos) {
        int success = 0;
        int[][] offsets = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] offset : offsets) {
            BlockPos spawnPos = pos.offset(offset[0], 0, offset[1]);
            if (!hasSolidTop(level, spawnPos.below())) {
                continue;
            }
            thaumcraft.entity.monster.CultistCleric cleric = thaumcraft.registry.ModEntities.CULTIST_CLERIC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (cleric == null) {
                continue;
            }
            cleric.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            if (validSpawn(level, cleric)) {
                cleric.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, (SpawnGroupData) null);
                cleric.setHomeTo(pos, 8);
                cleric.setRitualist(true);
                if (level.addFreshEntity(cleric)) {
                    success++;
                }
            }
        }
        if (success > 2) {
            this.spawnedClerics = true;
            this.setChanged();
        }
    }

    private void spawnGuards(ServerLevel level, BlockPos pos) {
        AABB bounds = new AABB(pos).inflate(24.0, 16.0, 24.0);
        if (level.getEntitiesOfClass(thaumcraft.entity.monster.CultistCleric.class, bounds).isEmpty()) {
            this.spawner = false;
            this.setChanged();
            return;
        }
        if (level.getEntitiesOfClass(thaumcraft.entity.monster.Cultist.class, bounds).size() >= 8) {
            return;
        }
        thaumcraft.entity.monster.CultistKnight knight = thaumcraft.registry.ModEntities.CULTIST_KNIGHT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (knight == null) {
            return;
        }
        BlockPos spawnPos = randomOffset(level, pos, 4, 10, 0, 3);
        knight.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        if (hasSolidTop(level, spawnPos.below()) && validSpawn(level, knight)) {
            knight.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, (SpawnGroupData) null);
            knight.setHomeTo(pos, 16);
            level.addFreshEntity(knight);
        }
    }

    private void spawnGuardian(ServerLevel level, BlockPos pos) {
        thaumcraft.entity.monster.eldritch.EldritchGuardian guardian = thaumcraft.registry.ModEntities.ELDRITCH_GUARDIAN.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guardian == null) {
            return;
        }
        BlockPos spawnPos = randomOffset(level, pos, 4, 10, 0, 3);
        guardian.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        if (hasSolidTop(level, spawnPos.below()) && guardian.checkSpawnRules(level, EntitySpawnReason.NATURAL) && validSpawn(level, guardian)) {
            guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, (SpawnGroupData) null);
            guardian.setHomeTo(pos, 16);
            level.addFreshEntity(guardian);
        }
    }

    private static BlockPos randomOffset(ServerLevel level, BlockPos pos, int min, int max, int yMin, int yMax) {
        int x = (level.getRandom().nextInt(max - min + 1) + min) * (level.getRandom().nextInt(3) - 1);
        int y = (level.getRandom().nextInt(yMax - yMin + 1) + yMin) * (level.getRandom().nextInt(3) - 1);
        int z = (level.getRandom().nextInt(max - min + 1) + min) * (level.getRandom().nextInt(3) - 1);
        return pos.offset(x, y, z);
    }

    private static boolean hasSolidTop(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean validSpawn(ServerLevel level, Mob mob) {
        return level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox());
    }

    private void tickLock(ServerLevel level, BlockPos pos) {
        if (!this.triggered) {
            return;
        }
        if (++this.timer % 5 == 0) {
            level.playSound(null, pos, ModSounds.PUMP.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (this.timer > 100) {
            level.playSound(null, pos, ModSounds.ICE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            this.triggered = false;
            this.timer = 0;
            EldritchEntities.BossKind[] bosses = {
                EldritchEntities.BossKind.GOLEM,
                EldritchEntities.BossKind.WARDEN,
                EldritchEntities.BossKind.CULTIST_PORTAL,
                EldritchEntities.BossKind.TAINTACLE_GIANT,
            };
            int next = thaumcraft.world.dim.MazeHandler.nextBoss(level, level.getRandom());
            EldritchEntities.BossKind kind = bosses[Math.floorMod(next, bosses.length)];
            thaumcraft.world.dim.MazeHandler.generateBossRoom(level, pos, kind);
            if (kind == EldritchEntities.BossKind.TAINTACLE_GIANT) {
                EldritchEntities.spawnTaintEncounter(level, thaumcraft.world.dim.MazeHandler.arenaCenter(level, pos));
            } else {
                BlockPos bossPos = thaumcraft.world.dim.MazeHandler.bossSpawnPos(level, pos, kind);
                EldritchEntities.spawnBoss(level, bossPos, kind, thaumcraft.world.dim.MazeHandler.arenaCenter(level, pos).above(2));
            }
            String message = switch (kind) {
                case GOLEM -> "golem";
                case WARDEN -> "warden";
                case CULTIST_PORTAL -> "crimson";
                case TAINTACLE_GIANT -> "taint";
            };
            for (var player : level.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >= 300.0) continue;
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.boss." + message));
            }
            level.removeBlock(pos, false);
        }
    }

    private void tickTrap(ServerLevel level, BlockPos pos) {
        if (this.count-- > 0) {
            return;
        }
        this.count = 10 + level.getRandom().nextInt(25);
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            player.hurt(level.damageSources().magic(), 2.0F);
            if (level.getRandom().nextBoolean() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                thaumcraft.research.Warp.add(serverPlayer, 1 + level.getRandom().nextInt(2), thaumcraft.research.WarpType.TEMPORARY);
            }
            thaumcraft.platform.ModNetwork.sendToNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 32.0,
                new thaumcraft.network.NodeZapPayload(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                    (float) player.getX(), (float) player.getEyeY(), (float) player.getZ()));
        }
    }

    private void tickCrabSpawner(ServerLevel level, BlockPos pos, BlockState state) {
        this.count--;
        if (this.count < 0) {
            this.count = 50 + level.getRandom().nextInt(50);
            return;
        }
        if (this.count == 15 && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16.0, false) != null
            && level.getEntitiesOfClass(thaumcraft.entity.monster.eldritch.EldritchCrab.class, new AABB(pos).inflate(32.0)).size() <= 5) {
            level.blockEvent(pos, state.getBlock(), 1, 0);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GENERIC_EXTINGUISH_FIRE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        if (this.count != 0 || level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16.0, false) == null
            || level.getEntitiesOfClass(thaumcraft.entity.monster.eldritch.EldritchCrab.class, new AABB(pos).inflate(32.0)).size() > 5) {
            return;
        }
        this.count = 150 + level.getRandom().nextInt(100);
        Direction direction = state.getValue(EldritchBlock.FACING);
        thaumcraft.entity.monster.eldritch.EldritchCrab crab = thaumcraft.registry.ModEntities.ELDRITCH_CRAB.get().create(level, EntitySpawnReason.TRIGGERED);
        if (crab != null) {
            BlockPos spawnPos = pos.relative(direction);
            crab.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);
            crab.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, (SpawnGroupData) null);
            crab.setHelm(false);
            crab.setDeltaMovement(direction.getStepX() * 0.2, direction.getStepY() * 0.2, direction.getStepZ() * 0.2);
            level.addFreshEntity(crab);
            level.playSound(null, pos, ModSounds.GORE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    private void tickVent(Level level, BlockPos pos, BlockState state) {
        int particles = this.venting > 0 ? 3 : (level.getRandom().nextInt(20) == 0 ? 1 : 0);
        if (this.venting > 0) {
            this.venting--;
        }
        Direction direction = state.getValue(EldritchBlock.FACING);
        var random = level.getRandom();
        for (int i = 0; i < particles; i++) {
            thaumcraft.fx.Fx.get().ventParticles(level,
                pos.getX() + 0.5 + 0.15F - random.nextFloat() * 0.3F + direction.getStepX() / 2.1F,
                pos.getY() + 0.5 + 0.15F - random.nextFloat() * 0.3F + direction.getStepY() / 2.1F,
                pos.getZ() + 0.5 + 0.15F - random.nextFloat() * 0.3F + direction.getStepZ() / 2.1F,
                direction.getStepX() / 3.0F + 0.1F - random.nextFloat() * 0.2F,
                direction.getStepY() / 3.0F + 0.1F - random.nextFloat() * 0.2F,
                direction.getStepZ() / 3.0F + 0.1F - random.nextFloat() * 0.2F, 10061994, 2.0F);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            this.venting = 20;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public boolean trigger() {
        if (!this.triggered) {
            this.triggered = true;
            this.timer = 0;
            this.sync();
            return true;
        }
        return false;
    }

    public boolean addEye() {
        if (this.eyes >= 4) {
            return false;
        }
        if (this.eyes >= 2) {
            this.spawner = true;
            this.spawnType = 1;
        }
        this.eyes++;
        if (this.level instanceof ServerLevel serverLevel) {
            ServerLevel outer = serverLevel.getServer().getLevel(thaumcraft.world.dim.OuterLands.OUTER_LANDS);
            if (outer != null) {
                thaumcraft.world.dim.MazeHandler.ensureMaze(outer, this.worldPosition);
            }
        }
        this.sync();
        return true;
    }

    public int eyeCount() {
        return this.eyes;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean triggered() {
        return this.triggered;
    }

    public int timer() {
        return this.timer;
    }

    public void setOpen(boolean value) {
        this.open = value;
        this.sync();
    }

    public void setSpawner(boolean value) {
        this.spawner = value;
        this.sync();
    }

    public void setSpawnType(byte value) {
        this.spawnType = value;
        this.sync();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.timer = input.getIntOr("timer", 0);
        this.count = input.getIntOr("count", this.getBlockState().getBlock() instanceof EldritchBlock block && block.kind() == 10 ? 20 : 150);
        this.triggered = input.getBooleanOr("triggered", false);
        this.eyes = input.getIntOr("eyes", 0);
        this.spawner = input.getBooleanOr("spawner", false);
        this.open = input.getBooleanOr("open", false);
        this.spawnedClerics = input.getBooleanOr("spawnedClerics", false);
        this.spawnType = input.getByteOr("spawnType", (byte) 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("timer", this.timer);
        output.putInt("count", this.count);
        output.putBoolean("triggered", this.triggered);
        output.putInt("eyes", this.eyes);
        output.putBoolean("spawner", this.spawner);
        output.putBoolean("open", this.open);
        output.putBoolean("spawnedClerics", this.spawnedClerics);
        output.putByte("spawnType", this.spawnType);
    }
}
