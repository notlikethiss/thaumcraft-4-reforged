package thaumcraft.device;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import thaumcraft.alchemy.BellowsBlock;
import thaumcraft.alchemy.BellowsBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.vis.VisNet;

public class InfernalFurnaceBlockEntity extends TcBlockEntity {
    public static final int SLOTS = 32;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int cookTime;
    private int maxCookTime;
    private int speedyTime;

    public InfernalFurnaceBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.INFERNAL_FURNACE.get(), pos, state);
    }

    protected InfernalFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getCookTime() {
        return cookTime;
    }

    public int getMaxCookTime() {
        return maxCookTime;
    }

    public int getSpeedyTime() {
        return speedyTime;
    }

    public void addSpeedyTime(int ticks) {
        speedyTime += ticks;
        setChanged();
    }

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    public Direction getFacing() {
        return getBlockState().getValue(InfernalFurnaceBlock.FACING);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfernalFurnaceBlockEntity furnace) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean cooked = false;
        if (furnace.cookTime > 0) {
            furnace.cookTime--;
            cooked = true;
        }
        if (cooked && furnace.speedyTime > 0) {
            furnace.speedyTime--;
        }
        if (furnace.speedyTime <= 0) {
            furnace.speedyTime = VisNet.drainVis(level, pos, Aspect.FIRE, 5);
        }
        if (furnace.maxCookTime == 0) {
            furnace.maxCookTime = furnace.calcCookTime();
        }
        if (furnace.cookTime > furnace.maxCookTime) {
            furnace.cookTime = furnace.maxCookTime;
        }
        if (furnace.cookTime == 0 && cooked) {
            for (int slot = 0; slot < SLOTS; slot++) {
                ItemStack stack = furnace.items.get(slot);
                if (!stack.isEmpty()) {
                    ItemStack result = furnace.smeltingResult(serverLevel, stack);
                    if (!result.isEmpty()) {
                        furnace.ejectItem(serverLevel, result.copy(), stack);
                        level.blockEvent(pos, state.getBlock(), InfernalFurnaceBlock.EVENT_LAVA, 0);
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            furnace.items.set(slot, ItemStack.EMPTY);
                        }
                        break;
                    }
                }
            }
            furnace.setChanged();
        }
        if (furnace.cookTime == 0 && !cooked) {
            for (int slot = 0; slot < SLOTS; slot++) {
                if (!furnace.items.get(slot).isEmpty() && furnace.canSmelt(serverLevel, slot)) {
                    furnace.maxCookTime = furnace.calcCookTime();
                    furnace.cookTime = furnace.maxCookTime;
                    furnace.setChanged();
                    break;
                }
            }
        }
    }

    private int getBellows() {
        int bellows = 0;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }
            BlockPos target = worldPosition.relative(direction, 2);
            BlockState state = level.getBlockState(target);
            if (state.getBlock() instanceof BellowsBlock
                && state.getValue(BellowsBlock.FACING) == direction.getOpposite()
                && level.getBlockEntity(target) instanceof BellowsBlockEntity
                && !level.hasNeighborSignal(target)) {
                bellows++;
            }
        }
        return Math.min(3, bellows);
    }

    private int calcCookTime() {
        return (speedyTime > 0 ? 80 : 140) - 20 * getBellows();
    }

    private ItemStack smeltingResult(ServerLevel serverLevel, ItemStack stack) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), serverLevel);
        return recipe.map(holder -> holder.value().assemble(new SingleRecipeInput(stack))).orElse(ItemStack.EMPTY);
    }

    private float smeltingExperience(ServerLevel serverLevel, ItemStack stack) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), serverLevel);
        return recipe.map(holder -> holder.value().experience()).orElse(0.0F);
    }

    private boolean canSmelt(ServerLevel serverLevel, int slot) {
        ItemStack stack = items.get(slot);
        return !stack.isEmpty() && !smeltingResult(serverLevel, stack).isEmpty();
    }

    public boolean addItemsToInventory(ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItem(existing, stack) && existing.getCount() + stack.getCount() <= stack.getMaxStackSize()) {
                existing.grow(stack.getCount());
                if (!canSmelt(serverLevel, slot)) {
                    destroyItem(serverLevel, slot);
                }
                setChanged();
                return true;
            }
            if (existing.isEmpty()) {
                items.set(slot, stack.copyWithCount(Math.min(stack.getCount(), 64)));
                if (!canSmelt(serverLevel, slot)) {
                    destroyItem(serverLevel, slot);
                }
                setChanged();
                return true;
            }
        }
        return false;
    }

    private void destroyItem(ServerLevel serverLevel, int slot) {
        items.set(slot, ItemStack.EMPTY);
        RandomSource random = serverLevel.getRandom();
        serverLevel.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
        serverLevel.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + random.nextFloat(), worldPosition.getY() + 1, worldPosition.getZ() + random.nextFloat(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    public void ejectItem(ServerLevel serverLevel, ItemStack result, ItemStack source) {
        RandomSource random = serverLevel.getRandom();
        Direction facing = getFacing();
        int facingX = facing.getStepX();
        int facingZ = facing.getStepZ();
        int bellows = getBellows();
        float lx = 0.5F + facingX * 1.2F;
        float lz = 0.5F + facingZ * 1.2F;
        float mx = facingX == 0 ? (random.nextFloat() - random.nextFloat()) * 0.03F : facingX * 0.13F;
        float mz = facingZ == 0 ? (random.nextFloat() - random.nextFloat()) * 0.03F : facingZ * 0.13F;
        ItemStack bit = result.copy();
        ItemEntity entity = new ItemEntity(serverLevel, worldPosition.getX() + lx, worldPosition.getY() + 0.4F, worldPosition.getZ() + lz, result);
        entity.setDeltaMovement(mx, 0.0, mz);
        serverLevel.addFreshEntity(entity);
        Item bonusItem = SmeltingBonuses.get(source);
        if (bonusItem != null) {
            int count = 0;
            if (bellows == 0) {
                if (random.nextInt(4) == 0) {
                    count++;
                }
            } else {
                for (int index = 0; index < bellows; index++) {
                    if (random.nextFloat() < 0.44F) {
                        count++;
                    }
                }
            }
            if (count > 0) {
                mx = facingX == 0 ? (random.nextFloat() - random.nextFloat()) * 0.03F : facingX * 0.13F;
                mz = facingZ == 0 ? (random.nextFloat() - random.nextFloat()) * 0.03F : facingZ * 0.13F;
                ItemEntity bonus = new ItemEntity(serverLevel, worldPosition.getX() + lx, worldPosition.getY() + 0.4F, worldPosition.getZ() + lz, new ItemStack(bonusItem, count));
                bonus.setDeltaMovement(mx, 0.0, mz);
                serverLevel.addFreshEntity(bonus);
            }
        }
        int amount = bit.getCount();
        float experience = smeltingExperience(serverLevel, source);
        if (experience == 0.0F) {
            amount = 0;
        } else if (experience < 1.0F) {
            int whole = Mth.floor(amount * experience);
            if (whole < Mth.ceil(amount * experience) && random.nextFloat() < amount * experience - whole) {
                whole++;
            }
            amount = whole;
        }
        while (amount > 0) {
            int split = ExperienceOrb.getExperienceValue(amount);
            amount -= split;
            ExperienceOrb orb = new ExperienceOrb(serverLevel, worldPosition.getX() + lx, worldPosition.getY() + 0.4F, worldPosition.getZ() + lz, split);
            mx = facingX == 0 ? (random.nextFloat() - random.nextFloat()) * 0.025F : facingX * 0.13F;
            mz = facingZ == 0 ? (random.nextFloat() - random.nextFloat()) * 0.025F : facingZ * 0.13F;
            orb.setDeltaMovement(mx, 0.0, mz);
            serverLevel.addFreshEntity(orb);
        }
    }

    private boolean releaseBlaze = true;

    public void suppressBlaze() {
        this.releaseBlaze = false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
            items.clear();
            Vec3 spawn = new Vec3(pos.getX() + 0.5F, pos.getY() + 1.0F, pos.getZ() + 0.5F);
            if (this.releaseBlaze && EntityType.BLAZE.create(serverLevel, EntitySpawnReason.TRIGGERED) instanceof LivingEntity blaze) {
                blaze.snapTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
                blaze.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6000, 2));
                blaze.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 12000, 0));
                serverLevel.addFreshEntity(blaze);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        cookTime = input.getIntOr("CookTime", 0);
        speedyTime = input.getIntOr("SpeedyTime", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("CookTime", cookTime);
        output.putInt("SpeedyTime", speedyTime);
    }
}
