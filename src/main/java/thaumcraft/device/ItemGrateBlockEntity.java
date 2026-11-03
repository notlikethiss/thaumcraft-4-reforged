package thaumcraft.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.registry.ModBlockEntities;

public class ItemGrateBlockEntity extends TcBlockEntity implements WorldlyContainer {
    private static final int[] TOP_SLOTS = {0};
    private static final int[] NO_SLOTS = {};

    private ItemStack pending = ItemStack.EMPTY;

    public ItemGrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_GRATE.get(), pos, state);
    }

    public @Nullable WorldlyContainer exposedItems(@Nullable Direction side) {
        return this;
    }

    private boolean isOpen() {
        return !getBlockState().getValue(ItemGrateBlock.CLOSED);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return pending.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return pending;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = pending;
        pending = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        pending = stack;
        setChanged();
    }

    @Override
    public void setChanged() {
        if (!pending.isEmpty() && level != null && !level.isClientSide()) {
            ItemEntity dropped = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, pending.copy());
            dropped.setDeltaMovement(0.0, -0.1, 0.0);
            level.addFreshEntity(dropped);
            pending = ItemStack.EMPTY;
        }
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        pending = ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isOpen();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return isOpen() && side == Direction.UP ? TOP_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isOpen() && side == Direction.UP;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }
}
