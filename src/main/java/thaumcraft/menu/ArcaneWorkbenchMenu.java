package thaumcraft.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ArcaneCrafting;
import thaumcraft.registry.ModMenus;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandVis;
import thaumcraft.wand.Wands;

public class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int WAND_SLOT = 1;
    public static final int GRID_START = 2;
    public static final int INVENTORY_START = 11;
    public static final int HOTBAR_START = 38;
    public static final int SLOT_END = 47;

    private final BlockPos pos;
    private final Player player;
    private final Container table;
    private final @Nullable ArcaneWorkbenchBlockEntity workbench;
    private final ResultContainer resultContainer = new ResultContainer();
    private WandVis lastWandVis = WandVis.EMPTY;
    private List<ItemStack> cachedGrid = List.of();
    private AspectList cachedAspects = new AspectList();

    public ArcaneWorkbenchMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ARCANE_WORKBENCH.get(), containerId);
        this.pos = pos;
        this.player = inventory.player;
        this.workbench = player.level().getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity entity ? entity : null;
        this.table = workbench != null ? workbench : new SimpleContainer(ArcaneWorkbenchBlockEntity.SLOT_COUNT);
        addSlot(new SlotCraftingArcaneWorkbench(player, table, resultContainer, 0, 160, 64));
        addSlot(new SlotLimitedByWand(table, ArcaneWorkbenchBlockEntity.WAND_SLOT, 160, 24));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(table, column + row * 3, 40 + column * 24, 40 + row * 24));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        }
        if (workbench != null) {
            workbench.addMenu(this);
        }
        if (player instanceof ServerPlayer) {
            resultContainer.setItem(0, computeResult());
            lastWandVis = wandVis();
        }
    }

    public BlockPos pos() {
        return pos;
    }

    public @Nullable ArcaneWorkbenchBlockEntity workbench() {
        return workbench;
    }

    public ItemStack getWand() {
        return table.getItem(ArcaneWorkbenchBlockEntity.WAND_SLOT);
    }

    public AspectList getMatchedAspects() {
        List<ItemStack> grid = gridStacks();
        if (!ItemStack.listMatches(grid, cachedGrid)) {
            cachedGrid = grid.stream().map(ItemStack::copy).toList();
            cachedAspects = ArcaneCrafting.findAspects(grid, player);
        }
        return cachedAspects;
    }

    private List<ItemStack> gridStacks() {
        List<ItemStack> grid = new ArrayList<>(ArcaneWorkbenchBlockEntity.GRID_SLOTS);
        for (int index = 0; index < ArcaneWorkbenchBlockEntity.GRID_SLOTS; index++) {
            grid.add(table.getItem(index));
        }
        return grid;
    }

    private WandVis wandVis() {
        ItemStack wand = getWand();
        return wand.isEmpty() ? WandVis.EMPTY : Wands.getAllVisData(wand);
    }

    private ItemStack computeResult() {
        if (!(player.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> grid = gridStacks();
        CraftingInput input = CraftingInput.ofPositioned(ArcaneCrafting.GRID_SIZE, ArcaneCrafting.GRID_SIZE, grid).input();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        ItemStack result = ItemStack.EMPTY;
        if (recipe.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = recipe.get();
            if (resultContainer.setRecipeUsed(serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        if (result.isEmpty()) {
            ItemStack wand = getWand();
            if (wand.getItem() instanceof WandItem wandItem) {
                ArcaneCrafting.Match match = ArcaneCrafting.find(grid, player);
                if (match != null && wandItem.consumeAllVisCrafting(wand, player, match.aspects(), false)) {
                    result = match.result();
                }
            }
        }
        return result;
    }

    public void onCraftMatrixChanged() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack result = computeResult();
        lastWandVis = wandVis();
        resultContainer.setItem(0, result);
        setRemoteSlot(RESULT_SLOT, result);
        if (serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, result));
        }
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer) {
            WandVis current = wandVis();
            if (!current.equals(lastWandVis)) {
                lastWandVis = current;
                onCraftMatrixChanged();
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (workbench != null) {
            workbench.removeMenu(this);
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player clicker) {
        int button = buttonNum;
        if (containerInput == ContainerInput.THROW) {
            button = 1;
        }
        if ((slotIndex == RESULT_SLOT || slotIndex == WAND_SLOT) && button > 0) {
            button = 0;
        }
        super.clicked(slotIndex, button, containerInput, clicker);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != table && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean wandCandidate = Wands.isWand(stack) && !Wands.isStaff(stack);
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index >= INVENTORY_START && index < HOTBAR_START) {
            if (wandCandidate) {
                if (!moveItemStackTo(stack, WAND_SLOT, WAND_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, HOTBAR_START, SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < SLOT_END) {
            if (wandCandidate) {
                if (!moveItemStackTo(stack, WAND_SLOT, WAND_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INVENTORY_START, SLOT_END, false)) {
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
        if (index == RESULT_SLOT) {
            player.drop(stack, false);
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return workbench != null && workbench.stillValid(player);
    }
}
