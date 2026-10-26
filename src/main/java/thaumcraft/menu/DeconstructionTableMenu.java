package thaumcraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import thaumcraft.arcane.DeconstructionTableBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModMenus;
import thaumcraft.research.ResearchManager;

public class DeconstructionTableMenu extends AbstractContainerMenu {
    public static final int BUTTON_COLLECT = 1;
    private static final int DATA_BREAK_TIME = 0;
    private static final int DATA_ASPECT = 1;

    private final BlockPos pos;
    private final @Nullable DeconstructionTableBlockEntity table;
    private final ContainerData data;

    public DeconstructionTableMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.DECONSTRUCTION_TABLE.get(), containerId);
        this.pos = pos;
        this.table = inventory.player.level().getBlockEntity(pos) instanceof DeconstructionTableBlockEntity entity ? entity : null;
        Container container = table != null ? table : new SimpleContainer(1);
        this.data = table != null && !inventory.player.level().isClientSide() ? serverData(table) : new SimpleContainerData(2);
        addSlot(new SlotLimitedHasAspects(container, 0, 64, 16));
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

    private static ContainerData serverData(DeconstructionTableBlockEntity table) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index == DATA_BREAK_TIME) {
                    return table.getBreakTime();
                }
                Aspect aspect = table.getAspect();
                return aspect == null ? 0 : aspect.ordinal() + 1;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public BlockPos pos() {
        return pos;
    }

    public @Nullable DeconstructionTableBlockEntity table() {
        return table;
    }

    public int getBreakTime() {
        return data.get(DATA_BREAK_TIME);
    }

    public int getBreakTimeScaled(int scale) {
        return getBreakTime() * scale / DeconstructionTableBlockEntity.BREAK_TIME;
    }

    public @Nullable Aspect getAspect() {
        int value = data.get(DATA_ASPECT);
        if (value <= 0) {
            return null;
        }
        Aspect[] aspects = Aspect.values();
        return value <= aspects.length ? aspects[value - 1] : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_COLLECT && table != null && player instanceof ServerPlayer serverPlayer) {
            Aspect aspect = table.getAspect();
            if (aspect != null) {
                ResearchManager.addAspectPool(serverPlayer, aspect, 1);
                table.clearAspect();
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index != 0) {
            if (DeconstructionTableBlockEntity.hasAspects(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!moveItemStackTo(stack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37 && !moveItemStackTo(stack, 1, 28, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 37, false)) {
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
        return table != null && table.stillValid(player);
    }
}
