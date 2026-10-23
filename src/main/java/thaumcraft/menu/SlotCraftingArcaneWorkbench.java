package thaumcraft.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ArcaneCrafting;
import thaumcraft.wand.WandItem;

public class SlotCraftingArcaneWorkbench extends Slot {
    private final Container craftMatrix;
    private final Player player;
    private int removeCount;

    public SlotCraftingArcaneWorkbench(Player player, Container craftMatrix, Container result, int slot, int x, int y) {
        super(result, slot, x, y);
        this.player = player;
        this.craftMatrix = craftMatrix;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (hasItem()) {
            removeCount += Math.min(amount, getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack picked, int count) {
        removeCount += count;
        checkTakeAchievements(picked);
    }

    @Override
    protected void onSwapCraft(int count) {
        removeCount += count;
    }

    @Override
    protected void checkTakeAchievements(ItemStack carried) {
        if (removeCount > 0) {
            carried.onCraftedBy(player, removeCount);
            EventHooks.firePlayerCraftingEvent(player, carried, craftMatrix);
        }
        if (container instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(player, gridStacks());
        }
        removeCount = 0;
    }

    private List<ItemStack> gridStacks() {
        List<ItemStack> grid = new ArrayList<>(ArcaneWorkbenchBlockEntity.GRID_SLOTS);
        for (int index = 0; index < ArcaneWorkbenchBlockEntity.GRID_SLOTS; index++) {
            grid.add(craftMatrix.getItem(index));
        }
        return grid;
    }

    @Override
    public void onTake(Player taker, ItemStack carried) {
        checkTakeAchievements(carried);
        List<ItemStack> grid = gridStacks();
        ArcaneCrafting.Match arcane = ArcaneCrafting.find(grid, player);
        AspectList aspects = arcane == null ? new AspectList() : arcane.aspects();
        ItemStack wand = craftMatrix.getItem(ArcaneWorkbenchBlockEntity.WAND_SLOT);
        if (aspects.size() > 0 && wand.getItem() instanceof WandItem wandItem) {
            wandItem.consumeAllVisCrafting(wand, player, aspects, true);
            if (craftMatrix instanceof ArcaneWorkbenchBlockEntity workbench) {
                workbench.markWandChanged();
            }
        }
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(ArcaneCrafting.GRID_SIZE, ArcaneCrafting.GRID_SIZE, grid);
        CraftingInput input = positioned.input();
        int left = positioned.left();
        int top = positioned.top();
        NonNullList<ItemStack> remaining = CraftingRecipe.defaultCraftingReminder(input);
        if (player.level() instanceof ServerLevel serverLevel) {
            Optional<RecipeHolder<CraftingRecipe>> vanilla = serverLevel.recipeAccess().getRecipeFor(RecipeType.CRAFTING, positioned.input(), serverLevel);
            if (vanilla.isPresent()) {
                CommonHooks.setCraftingPlayer(player);
                try {
                    remaining = vanilla.get().value().getRemainingItems(input);
                } finally {
                    CommonHooks.setCraftingPlayer(null);
                }
            }
        }
        if (arcane != null && arcane.result().is(thaumcraft.registry.ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get().asItem()) && grid.get(4).is(thaumcraft.registry.EldritchContent.PRIMORDIAL_PEARL.get())) {
            remaining.set(1 - left + (1 - top) * input.width(), grid.get(4).copyWithCount(1));
        }
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int slot = x + left + (y + top) * ArcaneCrafting.GRID_SIZE;
                ItemStack current = craftMatrix.getItem(slot);
                ItemStack replacement = remaining.get(x + y * input.width());
                if (!current.isEmpty()) {
                    craftMatrix.removeItem(slot, 1);
                    current = craftMatrix.getItem(slot);
                }
                if (!replacement.isEmpty()) {
                    if (current.isEmpty()) {
                        craftMatrix.setItem(slot, replacement);
                    } else if (ItemStack.isSameItemSameComponents(current, replacement)) {
                        replacement.grow(current.getCount());
                        craftMatrix.setItem(slot, replacement);
                    } else if (!player.getInventory().add(replacement)) {
                        player.drop(replacement, false);
                    }
                }
            }
        }
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
