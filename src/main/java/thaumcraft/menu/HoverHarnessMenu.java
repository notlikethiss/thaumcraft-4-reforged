package thaumcraft.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.item.armor.Hover;
import thaumcraft.item.armor.HoverHarnessItem;
import thaumcraft.registry.ModMenus;

public class HoverHarnessMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;

    private final Player player;
    private final ItemStack armor;
    private final int blockSlot;
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return Hover.isFuel(stack);
        }
    };

    public HoverHarnessMenu(int containerId, Inventory inventory) {
        super(ModMenus.HOVER_HARNESS.get(), containerId);
        this.player = inventory.player;
        this.armor = inventory.getSelectedItem();
        this.blockSlot = inventory.getSelectedSlot() + 28;
        addSlot(new Slot(input, INPUT_SLOT, 80, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Hover.isFuel(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            int hotbarIndex = column;
            addSlot(new Slot(inventory, column, 8 + column * 18, 142) {
                @Override
                public boolean mayPickup(Player picker) {
                    return hotbarIndex + 28 != blockSlot && super.mayPickup(picker);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return hotbarIndex + 28 != blockSlot && super.mayPlace(stack);
                }
            });
        }
        if (!player.level().isClientSide() && armor.getItem() instanceof HoverHarnessItem) {
            input.setItem(INPUT_SLOT, Hover.getJar(armor));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == blockSlot) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == INPUT_SLOT) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!Hover.isFuel(stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player clicker) {
        if (slotIndex == blockSlot) {
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, clicker);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide() || player.getInventory().getSelectedItem() == armor;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack jar = input.removeItemNoUpdate(INPUT_SLOT);
        if (player.getInventory().getSelectedItem() == armor) {
            Hover.setJar(armor, jar);
        } else if (!jar.isEmpty()) {
            player.getInventory().placeItemBackInInventory(jar);
        }
    }
}
