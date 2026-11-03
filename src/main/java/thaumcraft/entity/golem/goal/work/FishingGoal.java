package thaumcraft.entity.golem.goal.work;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemBobber;

public final class FishingGoal extends Goal {
    private record Entry(net.minecraft.world.item.Item item, int count, int weight, float maxDamagePercent, boolean enchantable) {
    }

    private static final List<Entry> JUNK = List.of(
        new Entry(Items.LEATHER_BOOTS, 1, 10, 0.9F, false),
        new Entry(Items.LEATHER, 1, 10, 0, false),
        new Entry(Items.BONE, 1, 10, 0, false),
        new Entry(Items.POTION, 1, 10, 0, false),
        new Entry(Items.STRING, 1, 5, 0, false),
        new Entry(Items.FISHING_ROD, 1, 2, 0.9F, false),
        new Entry(Items.BOWL, 1, 10, 0, false),
        new Entry(Items.STICK, 1, 5, 0, false),
        new Entry(Items.INK_SAC, 10, 5, 0, false),
        new Entry(Items.TRIPWIRE_HOOK, 1, 10, 0, false),
        new Entry(Items.ROTTEN_FLESH, 1, 10, 0, false)
    );
    private static final List<Entry> RARE = List.of(
        new Entry(Items.LILY_PAD, 1, 1, 0, false),
        new Entry(Items.NAME_TAG, 1, 1, 0, false),
        new Entry(Items.SADDLE, 1, 1, 0, false),
        new Entry(Items.BOW, 1, 1, 0.25F, true),
        new Entry(Items.FISHING_ROD, 1, 1, 0.25F, true),
        new Entry(Items.BOOK, 1, 1, 0, true)
    );
    private static final List<Entry> FISH = List.of(
        new Entry(Items.COD, 1, 60, 0, false),
        new Entry(Items.SALMON, 1, 25, 0, false),
        new Entry(Items.TROPICAL_FISH, 1, 2, 0, false),
        new Entry(Items.PUFFERFISH, 1, 13, 0, false)
    );

    private final GolemBase golem;
    private final int distance;
    private Vec3 target;
    private GolemBobber bobber;
    private float quality;
    private int count;

    public FishingGoal(GolemBase golem) {
        this.golem = golem;
        this.distance = Mth.ceil(golem.getRange() / 2.0F);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!golem.isWorking() || target != null || count > 0 || golem.tickCount % 5 != 0 || !golem.getNavigation().isDone()) return false;
        if (bobber != null && bobber.isAlive()) bobber.discard();
        BlockPos water = findWater();
        if (water == null) return false;
        target = new Vec3(water.getX(), water.getY(), water.getZ());
        quality = calculateQuality(water);
        golem.level().playSound(null, golem.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 0.5F, 0.4F / (golem.getRandom().nextFloat() * 0.4F + 0.8F));
        bobber = new GolemBobber(golem.level(), golem);
        bobber.setPos(golem.getX(), golem.getY(), golem.getZ());
        double dx = water.getX() + 0.5 - golem.getX();
        double dy = water.getY() + 1.0 - golem.getY();
        double dz = water.getZ() + 0.5 - golem.getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        bobber.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(length) * 0.08, dz * 0.1);
        return golem.level().addFreshEntity(bobber);
    }

    @Override
    public boolean canContinueToUse() {
        return golem.isWorking() && bobber != null && bobber.isAlive() && target != null && count-- > 0;
    }

    @Override
    public void start() {
        count = 300 + golem.getRandom().nextInt(200);
        golem.startRightArmTimer();
    }

    @Override
    public void tick() {
        if (target == null || !(golem.level() instanceof ServerLevel server)) return;
        golem.getLookControl().setLookAt(target.x + 0.5, target.y + 1.0, target.z + 0.5, 30, 30);
        float chance = quality + golem.getGolemStrength() * 1.5E-4F;
        if (golem.getRandom().nextFloat() >= chance) return;
        golem.startRightArmTimer();
        int catches = 1;
        if (golem.getUpgradeAmount(0) > 0 && golem.getRandom().nextInt(10) < golem.getUpgradeAmount(0)) catches++;
        for (int index = 0; index < catches; index++) {
            ItemStack stack = getFishingResult(server);
            if (golem.getUpgradeAmount(2) > 0) {
                var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
                var recipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, server);
                if (recipe.isPresent()) stack = recipe.get().value().assemble(input, server.registryAccess());
            }
            ItemEntity item = new ItemEntity(server, target.x + 0.5, target.y + 1.0, target.z + 0.5, stack);
            if (golem.getUpgradeAmount(2) > 0) item.igniteForSeconds(2);
            item.setPickUpDelay(20);
            double dx = golem.getX() + golem.getRandom().nextFloat() - golem.getRandom().nextFloat() - target.x + 0.5;
            double dy = golem.getY() - target.y + 1.0;
            double dz = golem.getZ() + golem.getRandom().nextFloat() - golem.getRandom().nextFloat() - target.z + 0.5;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(length) * 0.08, dz * 0.1);
            server.addFreshEntity(item);
        }
        if (bobber != null && bobber.isAlive()) {
            bobber.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.15F, 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.4F);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, bobber.getX(), bobber.getY() + 0.5, bobber.getZ(), 20 + golem.getRandom().nextInt(20), 0.1, 0, 0.1, 0);
            bobber.discard();
        }
        target = null;
    }

    @Override
    public void stop() {
        if (bobber != null && bobber.isAlive()) bobber.discard();
        bobber = null;
        target = null;
        count = -1;
    }

    private BlockPos findWater() {
        BlockPos home = golem.getRestrictCenter();
        for (int attempt = 0; attempt < distance * 2; attempt++) {
            int x = home.getX() + golem.getRandom().nextInt(1 + distance * 2) - distance;
            int y = home.getY() + golem.getRandom().nextInt(1 + distance) - distance / 2;
            int z = home.getZ() + golem.getRandom().nextInt(1 + distance * 2) - distance;
            BlockPos pos = new BlockPos(x, y, z);
            if (golem.level().getBlockState(pos).getFluidState().is(Fluids.WATER) && golem.level().isEmptyBlock(pos.above())) return pos;
        }
        return null;
    }

    private float calculateQuality(BlockPos pos) {
        float value = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (golem.level().getBlockState(side).getFluidState().is(Fluids.WATER) && golem.level().isEmptyBlock(side.above())) {
                value += 3.0E-5F;
                if (golem.level().canSeeSky(side.above())) value += 3.0E-5F;
                for (int depth = 1; depth <= 3; depth++) {
                    if (golem.level().getBlockState(side.below(depth)).getFluidState().is(Fluids.WATER)) value += 1.5E-5F;
                }
            }
        }
        return value;
    }

    private ItemStack getFishingResult(ServerLevel server) {
        float junkChance = 0.1F - golem.getUpgradeAmount(5) * 0.025F;
        float rareChance = 0.05F + golem.getUpgradeAmount(4) * 0.0125F;
        BlockPos pos = BlockPos.containing(target);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (golem.level().getBlockState(side).getFluidState().is(Fluids.WATER) && golem.level().isEmptyBlock(side.above())) {
                junkChance -= 0.005F;
                rareChance += 0.00125F;
                if (golem.level().canSeeSky(side.above())) {
                    junkChance -= 0.005F;
                    rareChance += 0.00125F;
                }
                for (int depth = 1; depth <= 3; depth++) {
                    if (golem.level().getBlockState(side.below(depth)).getFluidState().is(Fluids.WATER)) rareChance += 0.001F;
                }
            }
        }
        junkChance = Mth.clamp(junkChance, 0, 1);
        rareChance = Mth.clamp(rareChance, 0, 1);
        float roll = golem.getRandom().nextFloat();
        if (roll < junkChance) return pick(server, JUNK);
        if ((roll -= junkChance) < rareChance) return pick(server, RARE);
        return pick(server, FISH);
    }

    private ItemStack pick(ServerLevel level, List<Entry> entries) {
        int total = entries.stream().mapToInt(Entry::weight).sum();
        int roll = golem.getRandom().nextInt(total);
        Entry entry = entries.getLast();
        for (Entry candidate : entries) {
            roll -= candidate.weight();
            if (roll < 0) {
                entry = candidate;
                break;
            }
        }
        ItemStack stack = new ItemStack(entry.item(), entry.count());
        if (entry.maxDamagePercent() > 0 && stack.isDamageableItem()) {
            int upper = (int) (entry.maxDamagePercent() * stack.getMaxDamage());
            int damage = stack.getMaxDamage() - golem.getRandom().nextInt(golem.getRandom().nextInt(upper) + 1);
            if (damage > upper) damage = upper;
            if (damage < 1) damage = 1;
            stack.setDamageValue(damage);
        }
        if (entry.enchantable()) stack = EnchantmentHelper.enchantItem(golem.getRandom(), stack, 30, level.registryAccess(), Optional.empty());
        return stack;
    }
}
