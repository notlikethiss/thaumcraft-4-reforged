package thaumcraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.alchemy.AlchemicalFurnaceBlockEntity;
import thaumcraft.crafting.CrucibleCrafting;
import thaumcraft.registry.ModMenus;

public class AlchemicalFurnaceMenu extends AbstractContainerMenu {
    private static final int FURNACE_SLOTS = 2;
    private static final int INVENTORY_START = 2;
    private static final int HOTBAR_START = 29;
    private static final int END = 38;

    private final BlockPos pos;
    private final Container container;
    private final ContainerData data;
    private final AlchemicalFurnaceBlockEntity furnace;

    public AlchemicalFurnaceMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ALCHEMICAL_FURNACE.get(), containerId);
        this.pos = pos;
        this.furnace = inventory.player.level().getBlockEntity(pos) instanceof AlchemicalFurnaceBlockEntity entity ? entity : null;
        this.container = furnace != null ? furnace : new SimpleContainer(FURNACE_SLOTS);
        this.data = furnace != null && !inventory.player.level().isClientSide() ? furnace.getData() : new SimpleContainerData(5);
        addSlot(new Slot(container, AlchemicalFurnaceBlockEntity.INPUT_SLOT, 80, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CrucibleCrafting.hasDissolveTags(stack);
            }
        });
        addSlot(new Slot(container, AlchemicalFurnaceBlockEntity.FUEL_SLOT, 80, 48));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public int getCookTime() {
        return data.get(AlchemicalFurnaceBlockEntity.DATA_COOK_TIME);
    }

    public int getBurnTime() {
        return data.get(AlchemicalFurnaceBlockEntity.DATA_BURN_TIME);
    }

    public int getItemBurnTime() {
        return data.get(AlchemicalFurnaceBlockEntity.DATA_ITEM_BURN_TIME);
    }

    public int getVis() {
        return data.get(AlchemicalFurnaceBlockEntity.DATA_VIS);
    }

    public int getSmeltTime() {
        return data.get(AlchemicalFurnaceBlockEntity.DATA_SMELT_TIME);
    }

    public int getCookProgressScaled(int scale) {
        int smeltTime = Math.max(1, getSmeltTime());
        return getCookTime() * scale / smeltTime;
    }

    public int getContentsScaled(int scale) {
        return getVis() * scale / AlchemicalFurnaceBlockEntity.MAX_VIS;
    }

    public int getBurnTimeRemainingScaled(int scale) {
        int total = getItemBurnTime() == 0 ? 200 : getItemBurnTime();
        return getBurnTime() * scale / total;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index >= FURNACE_SLOTS) {
            if (AlchemicalFurnaceBlockEntity.isFuel(player.level(), stack)) {
                if (!moveItemStackTo(stack, 1, 2, false) && !moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (CrucibleCrafting.hasDissolveTags(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INVENTORY_START, END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return furnace != null && furnace.stillValid(player);
    }
}
