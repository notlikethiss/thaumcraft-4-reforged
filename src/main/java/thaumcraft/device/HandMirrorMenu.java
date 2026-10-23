package thaumcraft.device;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import thaumcraft.registry.ModMenus;

public class HandMirrorMenu extends AbstractContainerMenu {
    public static final int INVENTORY_START = 1;
    public static final int HOTBAR_START = INVENTORY_START + 27;
    public static final int SLOT_END = HOTBAR_START + 9;

    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            inputChanged();
        }
    };
    private final @Nullable ItemStack mirror;
    private final Player player;
    private final int blockedSlot;

    public HandMirrorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public HandMirrorMenu(int containerId, Inventory inventory, @Nullable ItemStack mirror) {
        super(ModMenus.HAND_MIRROR.get(), containerId);
        this.mirror = mirror;
        this.player = inventory.player;
        this.blockedSlot = HOTBAR_START + inventory.getSelectedSlot();
        addSlot(new Slot(input, 0, 80, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof HandMirrorItem);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    public int getBlockedSlot() {
        return blockedSlot;
    }

    private void inputChanged() {
        ItemStack stack = input.getItem(0);
        if (mirror == null || stack.isEmpty() || player.level().isClientSide()) {
            return;
        }
        if (HandMirrorItem.transport(mirror, stack, player, player.level())) {
            input.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex == blockedSlot) {
            return;
        }
        if (containerInput == ContainerInput.SWAP && buttonNum == blockedSlot - HOTBAR_START) {
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == blockedSlot) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem() || slot.getItem().getItem() instanceof HandMirrorItem) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return mirror == null || player.getInventory().getSelectedItem().getItem() instanceof HandMirrorItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            ItemStack stack = input.removeItemNoUpdate(0);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
