package thaumcraft.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.fx.Fx;
import thaumcraft.item.gear.Repairable;
import thaumcraft.network.BlockBubbleFxPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModSounds;

public class ElementalAxeItem extends Item implements Repairable {
    private static final int USE_DURATION = 72000;
    private static final double PULL_RANGE = 10.0;
    private static final double PULL_SPEED = 0.3;
    private static final double PULL_LIMIT = 0.35;
    private static final int RANGE_HORIZONTAL = 24;
    private static final int RANGE_VERTICAL = 48;
    private static final int BUBBLE_COLOR = 0x5555FF;

    public ElementalAxeItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_DURATION;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(PULL_RANGE))) {
            if (item.isRemoved() || FollowingItems.isFollowing(item)) {
                continue;
            }
            if (level.isClientSide()) {
                Fx.get().crucibleBubble(
                    level,
                    (float) item.getX() + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.125F,
                    (float) item.getY() + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.125F,
                    (float) item.getZ() + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.125F,
                    0.33F,
                    0.33F,
                    1.0F
                );
                continue;
            }
            Vec3 direction = new Vec3(item.getX() - player.getX(), item.getY() - player.getY() + player.getBbHeight() / 2.0F, item.getZ() - player.getZ());
            double length = direction.length();
            if (length == 0.0) {
                continue;
            }
            Vec3 pull = direction.scale(PULL_SPEED / length);
            Vec3 motion = item.getDeltaMovement().subtract(pull);
            item.setDeltaMovement(
                Mth.clamp(motion.x, -PULL_LIMIT, PULL_LIMIT),
                Mth.clamp(motion.y, -PULL_LIMIT, PULL_LIMIT),
                Mth.clamp(motion.z, -PULL_LIMIT, PULL_LIMIT)
            );
            item.needsSync = true;
        }
    }

    public static boolean breakFurthest(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState state) {
        if (player.isShiftKeyDown() || !state.is(BlockTags.LOGS) || ToolHarvest.isHarvesting()) {
            return false;
        }
        Block block = state.getBlock();
        BlockPos furthest = findFurthest(level, origin, block);
        boolean worked = ToolHarvest.destroyUnlessProtected(player, furthest, 10);
        ModNetwork.sendToNear(level, null, origin.getX(), origin.getY(), origin.getZ(), 32.0, new BlockBubbleFxPayload(origin, BUBBLE_COLOR));
        level.playSound(null, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5, ModSounds.BUBBLE.value(), SoundSource.BLOCKS, 0.15F, 1.0F);
        if (worked) {
            for (int offsetX = -3; offsetX <= 3; offsetX++) {
                for (int offsetY = -3; offsetY <= 3; offsetY++) {
                    for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
                        BlockPos around = furthest.offset(offsetX, offsetY, offsetZ);
                        BlockState aroundState = level.getBlockState(around);
                        if (!aroundState.isAir()) {
                            level.scheduleTick(around, aroundState.getBlock(), 150 + level.getRandom().nextInt(150));
                        }
                    }
                }
            }
        }
        return true;
    }

    private static BlockPos findFurthest(ServerLevel level, BlockPos origin, Block block) {
        BlockPos last = origin;
        double lastDistance = 0.0;
        boolean improved = true;
        while (improved) {
            improved = false;
            search:
            for (int offsetX = -2; offsetX <= 2; offsetX++) {
                for (int offsetY = 2; offsetY >= -2; offsetY--) {
                    for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                        if (Math.abs(last.getX() + offsetX - origin.getX()) > RANGE_HORIZONTAL
                            || Math.abs(last.getY() + offsetY - origin.getY()) > RANGE_VERTICAL
                            || Math.abs(last.getZ() + offsetZ - origin.getZ()) > RANGE_HORIZONTAL) {
                            return last;
                        }
                        BlockPos candidate = last.offset(offsetX, offsetY, offsetZ);
                        BlockState candidateState = level.getBlockState(candidate);
                        if (candidateState.is(block) && candidateState.is(BlockTags.LOGS) && candidateState.getDestroySpeed(level, candidate) >= 0.0F) {
                            double distance = candidate.distSqr(origin);
                            if (distance > lastDistance) {
                                lastDistance = distance;
                                last = candidate;
                                improved = true;
                                break search;
                            }
                        }
                    }
                }
            }
        }
        return last;
    }
}
