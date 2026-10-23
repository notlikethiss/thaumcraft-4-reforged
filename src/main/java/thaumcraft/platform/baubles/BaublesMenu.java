package thaumcraft.platform.baubles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.platform.Bauble;
import thaumcraft.platform.BaubleType;
import thaumcraft.platform.Baubles;

public class BaublesMenu extends AbstractCraftingMenu {
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_START = 1;
    public static final int CRAFT_END = 5;
    public static final int ARMOR_START = 5;
    public static final int ARMOR_END = 9;
    public static final int BAUBLE_START = 9;
    public static final int BAUBLE_END = 13;
    public static final int INVENTORY_START = 13;
    public static final int INVENTORY_END = 40;
    public static final int HOTBAR_START = 40;
    public static final int HOTBAR_END = 49;

    private static final Map<EquipmentSlot, Identifier> EMPTY_ARMOR_ICONS = Map.of(
        EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
        EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
        EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
        EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    );
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Player owner;
    private final BaubleInventory baubles;

    public BaublesMenu(int containerId, Inventory inventory) {
        super(BaubleMenus.EXPANDED.get(), containerId, 2, 2);
        this.owner = inventory.player;
        this.baubles = BaubleAttachments.inventory(owner);
        addResultSlot(owner, 144, 36);
        addCraftingGridSlots(106, 26);
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            EquipmentSlot equipmentSlot = ARMOR_SLOTS[index];
            addSlot(new BaubleArmorSlot(inventory, owner, equipmentSlot, 39 - index, 8, 8 + index * 18, EMPTY_ARMOR_ICONS.get(equipmentSlot)));
        }
        addSlot(new BaubleSlot(baubles, BaubleType.AMULET, 0, 80, 8));
        addSlot(new BaubleSlot(baubles, BaubleType.RING, 1, 80, 26));
        addSlot(new BaubleSlot(baubles, BaubleType.RING, 2, 80, 44));
        addSlot(new BaubleSlot(baubles, BaubleType.BELT, 3, 80, 62));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (owner.level() instanceof ServerLevel level && container == craftSlots) {
            CraftingInput input = craftSlots.asCraftInput();
            ServerPlayer serverPlayer = (ServerPlayer) owner;
            ItemStack result = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, (RecipeHolder<CraftingRecipe>) null);
            if (recipe.isPresent() && resultSlots.setRecipeUsed(serverPlayer, recipe.get())) {
                ItemStack assembled = recipe.get().value().assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
            resultSlots.setItem(0, result);
            setRemoteSlot(0, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        resultSlots.clearContent();
        if (!player.level().isClientSide()) {
            clearContainer(player, craftSlots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private boolean moveToFreeBaubleSlot(ItemStack stack, ItemStack original) {
        if (!(original.getItem() instanceof Bauble bauble)) {
            return false;
        }
        BaubleType type = bauble.getBaubleType(original);
        for (int slotIndex = BAUBLE_START; slotIndex < BAUBLE_END; slotIndex++) {
            Slot target = slots.get(slotIndex);
            if (Baubles.slotType(slotIndex - BAUBLE_START) == type && !target.hasItem() && target.mayPlace(original)) {
                return moveItemStackTo(stack, slotIndex, slotIndex + 1, false);
            }
        }
        return false;
    }

    private boolean canMoveToFreeBaubleSlot(ItemStack original) {
        if (!(original.getItem() instanceof Bauble bauble)) {
            return false;
        }
        BaubleType type = bauble.getBaubleType(original);
        for (int slotIndex = BAUBLE_START; slotIndex < BAUBLE_END; slotIndex++) {
            Slot target = slots.get(slotIndex);
            if (Baubles.slotType(slotIndex - BAUBLE_START) == type && !target.hasItem() && target.mayPlace(original)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(clicked);
            if (slotIndex == RESULT_SLOT) {
                if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex < BAUBLE_END) {
                if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !slots.get(8 - equipmentSlot.getIndex()).hasItem()) {
                int position = 8 - equipmentSlot.getIndex();
                if (!moveItemStackTo(stack, position, position + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (canMoveToFreeBaubleSlot(clicked)) {
                if (!moveToFreeBaubleSlot(stack, clicked)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, clicked);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (slotIndex == RESULT_SLOT) {
                player.drop(stack, false);
            }
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public Slot getResultSlot() {
        return slots.get(RESULT_SLOT);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return slots.subList(CRAFT_START, CRAFT_END);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return owner;
    }
}
