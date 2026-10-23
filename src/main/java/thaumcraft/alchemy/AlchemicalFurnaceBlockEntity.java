package thaumcraft.alchemy;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.crafting.CrucibleCrafting;
import thaumcraft.menu.AlchemicalFurnaceMenu;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModItems;

public class AlchemicalFurnaceBlockEntity extends TcBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int MAX_VIS = 50;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int DATA_COOK_TIME = 0;
    public static final int DATA_BURN_TIME = 1;
    public static final int DATA_ITEM_BURN_TIME = 2;
    public static final int DATA_VIS = 3;
    public static final int DATA_SMELT_TIME = 4;

    private static final int[] SLOTS_BOTTOM = {FUEL_SLOT};
    private static final int[] SLOTS_TOP = {};
    private static final int[] SLOTS_SIDES = {INPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private AspectList aspects = new AspectList();
    private int smeltTime = 100;
    private int bellows = -1;
    private boolean speedBoost;
    private int burnTime;
    private int currentItemBurnTime;
    private int cookTime;
    private int count;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_COOK_TIME -> cookTime;
                case DATA_BURN_TIME -> burnTime;
                case DATA_ITEM_BURN_TIME -> currentItemBurnTime;
                case DATA_VIS -> aspects.visSize();
                case DATA_SMELT_TIME -> smeltTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public AlchemicalFurnaceBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ALCHEMICAL_FURNACE.get(), pos, state);
    }

    protected AlchemicalFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public @Nullable WorldlyContainer exposedItems(@Nullable Direction side) {
        return this;
    }

    public ContainerData getData() {
        return data;
    }

    public AspectList getAspects() {
        return aspects;
    }

    public int getVis() {
        return aspects.visSize();
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getComparatorSignal() {
        int vis = aspects.visSize();
        return (int) Math.floor(vis / (float) MAX_VIS * 14.0F) + (vis > 0 ? 1 : 0);
    }

    public void resetBellows() {
        bellows = -1;
    }

    public static boolean isFuel(Level level, ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
    }

    private int getBurnDuration(ServerLevel serverLevel, ItemStack fuel) {
        return fuel.getBurnTime(RecipeType.SMELTING, serverLevel.fuelValues());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalFurnaceBlockEntity furnace) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean wasBurning = furnace.burnTime > 0;
        boolean dirty = false;
        furnace.count++;
        if (furnace.burnTime > 0) {
            furnace.burnTime--;
        }
        if (furnace.bellows < 0) {
            furnace.bellows = BellowsBlockEntity.getBellows(level, pos, Direction.values());
        }
        if (furnace.count % (furnace.speedBoost ? 20 : 40) == 0 && furnace.aspects.size() > 0) {
            furnace.feedAlembics(level, pos);
        }
        if (furnace.burnTime == 0 && furnace.canSmelt()) {
            ItemStack fuel = furnace.items.get(FUEL_SLOT);
            furnace.currentItemBurnTime = furnace.getBurnDuration(serverLevel, fuel);
            furnace.burnTime = furnace.currentItemBurnTime;
            if (furnace.burnTime > 0) {
                dirty = true;
                furnace.speedBoost = fuel.is(ModItems.ALUMENTUM.get());
                ItemStackTemplate remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty() && remainder != null) {
                    furnace.items.set(FUEL_SLOT, remainder.create());
                }
            }
        }
        if (furnace.burnTime > 0 && furnace.canSmelt()) {
            furnace.cookTime++;
            if (furnace.cookTime >= furnace.smeltTime) {
                furnace.cookTime = 0;
                furnace.smeltItem();
                dirty = true;
            }
        } else {
            furnace.cookTime = 0;
        }
        if (wasBurning != furnace.burnTime > 0) {
            dirty = true;
        }
        boolean lit = furnace.burnTime > 0;
        boolean hasVis = furnace.aspects.visSize() > 0;
        if (state.getValue(AlchemicalFurnaceBlock.LIT) != lit || state.getValue(AlchemicalFurnaceBlock.HAS_VIS) != hasVis) {
            level.setBlock(pos, state.setValue(AlchemicalFurnaceBlock.LIT, lit).setValue(AlchemicalFurnaceBlock.HAS_VIS, hasVis), Block.UPDATE_CLIENTS);
        }
        if (dirty) {
            furnace.setChanged();
        }
    }

    private void feedAlembics(Level level, BlockPos pos) {
        AspectList excluded = new AspectList();
        int deep = 0;
        while (deep < 5) {
            deep++;
            if (!(level.getBlockEntity(pos.above(deep)) instanceof AlembicBlockEntity alembic)) {
                break;
            }
            Aspect aspect = alembic.getAspect();
            if (aspect != null && alembic.getAmount() < alembic.getMaxAmount() && aspects.getAmount(aspect) > 0) {
                takeFromContainer(aspect, 1);
                alembic.addToContainer(aspect, 1);
                excluded.merge(aspect, 1);
                sync();
            }
        }
        deep = 0;
        while (deep < 5) {
            deep++;
            if (!(level.getBlockEntity(pos.above(deep)) instanceof AlembicBlockEntity alembic)) {
                break;
            }
            if (alembic.getAspect() == null || alembic.getAmount() == 0) {
                Aspect taken = null;
                if (alembic.getAspectFilter() == null) {
                    taken = takeRandomAspect(excluded, level);
                } else if (takeFromContainer(alembic.getAspectFilter(), 1)) {
                    taken = alembic.getAspectFilter();
                }
                if (taken != null) {
                    alembic.addToContainer(taken, 1);
                    sync();
                    break;
                }
            }
        }
    }

    private boolean canSmelt() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        AspectList tags = CrucibleCrafting.dissolveTags(input);
        if (tags.isEmpty()) {
            return false;
        }
        int visSize = tags.visSize();
        if (visSize > MAX_VIS - aspects.visSize()) {
            return false;
        }
        smeltTime = (int) (visSize * 10 * (1.0F - 0.125F * bellows));
        return true;
    }

    private void smeltItem() {
        if (!canSmelt()) {
            return;
        }
        ItemStack input = items.get(INPUT_SLOT);
        AspectList tags = CrucibleCrafting.dissolveTags(input);
        for (Aspect aspect : tags.getAspects()) {
            aspects.add(aspect, tags.getAmount(aspect));
        }
        input.shrink(1);
        sync();
    }

    private @Nullable Aspect takeRandomAspect(AspectList excluded, Level level) {
        if (aspects.size() <= 0) {
            return null;
        }
        AspectList temp = aspects.copy();
        for (Aspect aspect : excluded.getAspects()) {
            temp.remove(aspect);
        }
        if (temp.size() <= 0) {
            return null;
        }
        List<Aspect> candidates = temp.getAspects();
        Aspect tag = candidates.get(level.getRandom().nextInt(candidates.size()));
        aspects.remove(tag, 1);
        return tag;
    }

    public boolean takeFromContainer(Aspect tag, int amount) {
        if (aspects.getAmount(tag) >= amount) {
            aspects.remove(tag, amount);
            return true;
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, this);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alchemyfurnace");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AlchemicalFurnaceMenu(containerId, inventory, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT && CrucibleCrafting.hasDissolveTags(stack)) {
            return true;
        }
        return slot == FUEL_SLOT && level != null && isFuel(level, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_BOTTOM : side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN || slot != FUEL_SLOT || stack.is(Items.BUCKET);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        burnTime = input.getIntOr("BurnTime", 0);
        currentItemBurnTime = input.getIntOr("BurnTotal", 0);
        cookTime = input.getIntOr("CookTime", 0);
        speedBoost = input.getBooleanOr("speedBoost", false);
        aspects = input.read("Aspects", AspectList.CODEC).orElseGet(AspectList::new);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnTotal", currentItemBurnTime);
        output.putInt("CookTime", cookTime);
        output.putBoolean("speedBoost", speedBoost);
        output.store("Aspects", AspectList.CODEC, aspects);
    }
}
