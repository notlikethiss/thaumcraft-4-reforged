package thaumcraft.client.research;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.platform.ClientNetwork;
import thaumcraft.fx.Fx;
import thaumcraft.network.ScanPayload;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.ScanClientHooks;
import thaumcraft.research.ScanKind;
import thaumcraft.research.ScanManager;

public final class ThaumometerClient implements ScanClientHooks.Scanner {
    private static final int SCAN_TICKS = 20;

    public record Hit(int kind, BlockPos pos, int entityId, ScanManager.Target target) {
        boolean sameAs(@Nullable Hit other) {
            return other != null && kind == other.kind && pos.equals(other.pos) && entityId == other.entityId;
        }
    }

    private @Nullable Hit startScan;
    private int scanTicks;
    private boolean attempted;

    public static void register() {
        ScanClientHooks.setScanner(new ThaumometerClient());
        ScanClientHooks.setLocalPlayer(() -> Minecraft.getInstance().player);
    }

    @Override
    public void useTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (entity != Minecraft.getInstance().player) {
            return;
        }
        Player player = (Player) entity;
        Hit scan = doScan(level, player);
        if (scan == null || !scan.sameAs(startScan)) {
            startScan = scan;
            scanTicks = 0;
            attempted = false;
        }
        if (scan != null) {
            runes(level, scan);
        }
        if (scan == null || attempted) {
            return;
        }
        scanTicks++;
        if (scanTicks % 2 == 0) {
            level.playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.CAMERATICKS.get(),
                SoundSource.PLAYERS,
                0.2F,
                0.45F + level.getRandom().nextFloat() * 0.1F,
                false
            );
        }
        if (scanTicks >= SCAN_TICKS) {
            attempted = true;
            if (ScanManager.validScan(scan.target().aspects(), player)) {
                ClientNetwork.sendToServer(new ScanPayload(scan.kind(), scan.pos(), scan.entityId()));
            }
        }
    }

    @Override
    public void stopUsing(LivingEntity entity) {
        if (entity != Minecraft.getInstance().player) {
            return;
        }
        startScan = null;
        scanTicks = 0;
        attempted = false;
    }

    private static void runes(Level level, Hit scan) {
        RandomSource random = level.getRandom();
        float red = 0.3F + random.nextFloat() * 0.7F;
        float blue = 0.3F + random.nextFloat() * 0.7F;
        if (scan.kind() == ScanManager.KIND_ENTITY) {
            Entity entity = level.getEntity(scan.entityId());
            if (entity != null) {
                Fx.get().blockRunes(level, entity.getX() - 0.5, entity.getY() + entity.getEyeHeight() / 2.0F, entity.getZ() - 0.5,
                    red, 0.0F, blue, (int) (entity.getBbHeight() * 15.0F), 0.03F);
            }
            return;
        }
        BlockPos pos = scan.pos();
        Fx.get().blockRunes(level, pos.getX(), pos.getY() + 0.25, pos.getZ(), red, 0.0F, blue, 15, 0.03F);
    }

    private @Nullable Hit doScan(Level level, Player player) {
        return fresh(player, lookAt(level, player));
    }

    public static @Nullable Hit lookAt(Level level, Player player) {
        Entity pointed = pointedEntity(level, player);
        if (pointed != null) {
            ScanManager.Target target = ScanManager.resolveEntity(pointed);
            return new Hit(ScanManager.KIND_ENTITY, BlockPos.ZERO, pointed.getId(), target);
        }
        HitResult result = ScanManager.pickBlock(level, player);
        if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            ScanManager.Target target = ScanManager.resolveBlock(level, pos);
            if (target == null) {
                return null;
            }
            int kind = target.kind() == ScanKind.PHENOMENON ? ScanManager.KIND_NODE : ScanManager.KIND_BLOCK;
            return new Hit(kind, pos, 0, target);
        }
        return null;
    }

    private static @Nullable Hit fresh(Player player, @Nullable Hit hit) {
        if (hit == null || hit.target() == null || ScanManager.hasBeenScanned(player, hit.target())) {
            return null;
        }
        return hit;
    }

    private static @Nullable Entity pointedEntity(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(ScanManager.ENTITY_RANGE));
        AABB area = player.getBoundingBox().expandTowards(player.getLookAngle().scale(ScanManager.ENTITY_RANGE)).inflate(1.0);
        List<Entity> entities = level.getEntities(player, area, entity -> !entity.isSpectator());
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (entity.distanceTo(player) < 0.5F) {
                continue;
            }
            ClipContext context = new ClipContext(eye, entity.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            if (level.clip(context).getType() != HitResult.Type.MISS) {
                continue;
            }
            AABB box = entity.getBoundingBox().inflate(Math.max(0.8, entity.getPickRadius()));
            if (box.contains(eye)) {
                return entity;
            }
            double distance = box.clip(eye, end).map(eye::distanceTo).orElse(Double.MAX_VALUE);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }
}
