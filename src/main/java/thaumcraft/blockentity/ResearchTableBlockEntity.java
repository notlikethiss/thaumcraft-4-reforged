package thaumcraft.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.item.ResearchNotesItem;
import thaumcraft.item.ScribingToolsItem;
import thaumcraft.menu.ResearchTableMenu;
import thaumcraft.network.AspectCombinePayload;
import thaumcraft.network.AspectPlacePayload;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.HexGrid;
import thaumcraft.research.Knowledge;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchNote;
import thaumcraft.research.ResearchNoteKeys;
import thaumcraft.research.ResearchNotes;

public class ResearchTableBlockEntity extends TcBlockEntity implements Container, MenuProvider {
    public static final int SCRIBE_SLOT = 0;
    public static final int NOTE_SLOT = 1;
    private static final int RECALC_INTERVAL = 600;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private AspectList bonusAspects = new AspectList();
    private int nextRecalc;

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        serverTick(level, pos, state, table, null);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table, RandomSource random) {
        if (table.nextRecalc++ > RECALC_INTERVAL) {
            table.nextRecalc = 0;
            if (level instanceof ServerLevel serverLevel) {
                table.recalculateBonus(serverLevel, random == null ? serverLevel.getRandom() : random);
            }
            table.sync();
        }
    }

    public AspectList getBonusAspects() {
        return bonusAspects;
    }

    public @Nullable ResearchNote getNote() {
        ItemStack stack = items.get(NOTE_SLOT);
        if (stack.getItem() instanceof ResearchNotesItem) {
            return ResearchNoteKeys.NOTE.get(stack);
        }
        return null;
    }

    private static boolean consumeInk(ItemStack stack, boolean doit) {
        if (stack.getItem() instanceof ScribingToolsItem && stack.getDamageValue() < stack.getMaxDamage()) {
            if (doit) {
                stack.setDamageValue(stack.getDamageValue() + 1);
            }
            return true;
        }
        return false;
    }

    public boolean hasInk() {
        return consumeInk(items.get(SCRIBE_SLOT), false);
    }

    private static void playOrb(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    public void placeAspect(int q, int r, Optional<Aspect> aspect, ServerPlayer player) {
        if (level == null || level.isClientSide() || !consumeInk(items.get(SCRIBE_SLOT), false)) {
            return;
        }
        ItemStack noteStack = items.get(NOTE_SLOT);
        ResearchNote note = getNote();
        if (note == null || note.complete() || !(noteStack.getItem() instanceof ResearchNotesItem item) || item.isDiscovery()) {
            return;
        }
        HexGrid.Cell cell = new HexGrid.Cell(q, r);
        ResearchNote.Hex hex = HexGrid.index(note.hexes()).get(cell);
        if (hex == null) {
            return;
        }
        PlayerKnowledge knowledge = Knowledge.get(player);
        boolean researcherOne = ResearchManager.isComplete(player, "RESEARCHER1");
        boolean researcherTwo = ResearchManager.isComplete(player, "RESEARCHER2");
        ResearchNote.Hex replacement;
        if (aspect.isPresent()) {
            Aspect placed = aspect.get();
            if (hex.type() != ResearchNote.Hex.EMPTY || !knowledge.hasDiscoveredAspect(placed)) {
                return;
            }
            boolean fromPool = knowledge.getAspectPool(placed) > 0;
            if (!fromPool && bonusAspects.getAmount(placed) <= 0) {
                return;
            }
            if (researcherTwo && player.getRandom().nextFloat() < 0.1F) {
                playOrb(player);
            } else if (!fromPool) {
                bonusAspects.remove(placed, 1);
            } else if (!ResearchManager.spendAspectPool(player, placed, 1)) {
                return;
            }
            replacement = new ResearchNote.Hex(q, r, ResearchNote.Hex.PLACED, aspect);
        } else {
            if (hex.type() != ResearchNote.Hex.PLACED) {
                return;
            }
            float chance = player.getRandom().nextFloat();
            if (hex.aspect().isPresent() && (researcherOne && chance < 0.25F || researcherTwo && chance < 0.5F)) {
                playOrb(player);
                ResearchManager.addAspectPool(player, hex.aspect().get(), 1);
            }
            replacement = new ResearchNote.Hex(q, r, ResearchNote.Hex.EMPTY, Optional.empty());
        }
        List<ResearchNote.Hex> hexes = new ArrayList<>();
        for (ResearchNote.Hex existing : note.hexes()) {
            hexes.add(existing.q() == q && existing.r() == r ? replacement : existing);
        }
        ResearchNote updated = new ResearchNote(note.key(), note.color(), note.complete(), note.copies(), hexes);
        consumeInk(items.get(SCRIBE_SLOT), true);
        ResearchNote completed = HexGrid.complete(updated, knowledge::hasDiscoveredAspect);
        if (completed != null) {
            ItemStack discovery = new ItemStack(ModItems.DISCOVERY.get());
            ResearchNoteKeys.NOTE.set(discovery, completed);
            items.set(NOTE_SLOT, discovery);
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 1);
        } else {
            ResearchNoteKeys.NOTE.set(noteStack, updated);
        }
        sync();
    }

    public void duplicate(ServerPlayer player) {
        ItemStack stack = items.get(NOTE_SLOT);
        ResearchNote note = getNote();
        if (level == null || note == null || !note.complete() || stack.getCount() >= stack.getMaxStackSize()) {
            return;
        }
        int paper = ResearchNotes.findItem(player, Items.PAPER);
        int ink = ResearchNotes.findItem(player, Items.INK_SAC);
        ResearchItem research = ResearchCategories.getResearch(note.key());
        if (paper < 0 || ink < 0 || research == null || !ResearchManager.isComplete(player, "RESEARCHDUPE")) {
            return;
        }
        PlayerKnowledge knowledge = Knowledge.get(player);
        AspectList cost = HexGrid.requiredTags(research, note.copies());
        for (Aspect aspect : cost.getAspects()) {
            if (knowledge.getAspectPool(aspect) < cost.getAmount(aspect)) {
                return;
            }
        }
        for (Aspect aspect : cost.getAspects()) {
            ResearchManager.spendAspectPool(player, aspect, cost.getAmount(aspect));
        }
        player.getInventory().getItem(paper).shrink(1);
        player.getInventory().getItem(ink).shrink(1);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 1);
        ResearchNoteKeys.NOTE.set(stack, new ResearchNote(note.key(), note.color(), true, note.copies() + 1, note.hexes()));
        stack.grow(1);
        player.containerMenu.broadcastChanges();
        sync();
    }

    private static ResearchTableBlockEntity validated(BlockPos pos, Player player) {
        if (player.containerMenu instanceof ResearchTableMenu menu
            && menu.pos().equals(pos)
            && menu.stillValid(player)
            && player.level().getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
            return table;
        }
        return null;
    }

    public static void handlePlace(AspectPlacePayload payload, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchTableBlockEntity table = validated(payload.table(), player);
            if (table != null) {
                table.placeAspect(payload.q(), payload.r(), payload.aspect(), serverPlayer);
            }
        }
    }

    public static void handleCombine(AspectCombinePayload payload, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ResearchTableBlockEntity table = validated(payload.table(), player);
        if (table == null) {
            return;
        }
        Aspect combination = Aspect.combine(payload.first(), payload.second());
        PlayerKnowledge knowledge = Knowledge.get(serverPlayer);
        if (combination == null || !knowledge.hasDiscoveredAspect(payload.first()) || !knowledge.hasDiscoveredAspect(payload.second())) {
            return;
        }
        AspectList poolUse = new AspectList();
        AspectList bonusUse = new AspectList();
        Aspect[] inputs = {payload.first(), payload.second()};
        boolean[] flags = {payload.firstBonus(), payload.secondBonus()};
        for (int index = 0; index < 2; index++) {
            Aspect input = inputs[index];
            if (knowledge.getAspectPool(input) - poolUse.getAmount(input) > 0) {
                poolUse.merge(input, 1);
            } else if (flags[index] && table.bonusAspects.getAmount(input) - bonusUse.getAmount(input) > 0) {
                bonusUse.merge(input, 1);
            } else {
                return;
            }
        }
        for (Aspect aspect : poolUse.getAspects()) {
            ResearchManager.spendAspectPool(serverPlayer, aspect, poolUse.getAmount(aspect));
        }
        for (Aspect aspect : bonusUse.getAspects()) {
            table.bonusAspects.remove(aspect, bonusUse.getAmount(aspect));
        }
        if (bonusUse.size() > 0) {
            table.sync();
        }
        ResearchManager.checkAndSyncAspectKnowledge(serverPlayer, combination, 1);
    }

    private boolean attempt(Aspect aspect, int chance, RandomSource random) {
        if (bonusAspects.getAmount(aspect) < 1 && random.nextInt(chance) == 0) {
            bonusAspects.merge(aspect, 1);
            return true;
        }
        return false;
    }

    private static boolean isCircuits(Block block) {
        return block instanceof RedStoneWireBlock
            || block instanceof TorchBlock
            || block instanceof LeverBlock
            || block instanceof ButtonBlock
            || block instanceof DiodeBlock
            || block instanceof TripWireHookBlock
            || block instanceof TripWireBlock
            || block instanceof BaseRailBlock
            || block instanceof SkullBlock
            || block instanceof FlowerPotBlock
            || block instanceof PistonBaseBlock
            || block instanceof PistonHeadBlock
            || block instanceof MovingPistonBlock;
    }

    private static boolean isGround(Block block) {
        return block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.PODZOL || block == Blocks.ROOTED_DIRT || block == Blocks.FARMLAND || block == Blocks.DIRT_PATH;
    }

    private void recalculateBonus(ServerLevel serverLevel, RandomSource random) {
        BlockPos above = worldPosition.above();
        if (serverLevel.isDarkOutside()
            && serverLevel.getMaxLocalRawBrightness(above) < 4
            && !serverLevel.canSeeSky(above)
            && random.nextInt(20) == 0) {
            bonusAspects.merge(Aspect.ENTROPY, 1);
        }
        int height = worldPosition.getY() - serverLevel.getMinY();
        if (height > serverLevel.getHeight() * 0.5F && random.nextInt(20) == 0) {
            bonusAspects.merge(Aspect.AIR, 1);
        }
        if (height > 256.0F * 0.66F && random.nextInt(20) == 0) {
            bonusAspects.merge(Aspect.AIR, 1);
        }
        if (height > 256.0F * 0.75F && random.nextInt(20) == 0) {
            bonusAspects.merge(Aspect.AIR, 1);
        }
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = -8; y <= 8; y++) {
                    target.set(worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z);
                    if (serverLevel.isOutsideBuildHeight(target) || !serverLevel.hasChunkAt(target)) {
                        continue;
                    }
                    BlockState state = serverLevel.getBlockState(target);
                    Block block = state.getBlock();
                    boolean found;
                    if (block == ModBlocks.AIR_INFUSED_STONE.get()) {
                        found = attempt(Aspect.AIR, 20, random);
                    } else if (block == ModBlocks.AIR_CRYSTAL_CLUSTER.get()) {
                        found = attempt(Aspect.AIR, 10, random);
                    } else if (block instanceof BaseFireBlock || state.getFluidState().is(FluidTags.LAVA) || block == ModBlocks.FIRE_INFUSED_STONE.get()) {
                        found = attempt(Aspect.FIRE, 20, random);
                    } else if (block == ModBlocks.FIRE_CRYSTAL_CLUSTER.get()) {
                        found = attempt(Aspect.FIRE, 10, random);
                    } else if (isGround(block) || block == ModBlocks.EARTH_INFUSED_STONE.get()) {
                        found = attempt(Aspect.EARTH, 20, random);
                    } else if (block == ModBlocks.EARTH_CRYSTAL_CLUSTER.get()) {
                        found = attempt(Aspect.EARTH, 10, random);
                    } else if (block == Blocks.WATER) {
                        found = attempt(Aspect.WATER, 15, random);
                    } else if (block == ModBlocks.WATER_INFUSED_STONE.get()) {
                        found = attempt(Aspect.WATER, 20, random);
                    } else if (block == ModBlocks.WATER_CRYSTAL_CLUSTER.get()) {
                        found = attempt(Aspect.WATER, 10, random);
                    } else if (isCircuits(block) || block == ModBlocks.ORDER_INFUSED_STONE.get()) {
                        found = attempt(Aspect.ORDER, 20, random);
                    } else if (block == ModBlocks.ORDER_CRYSTAL_CLUSTER.get()) {
                        found = attempt(Aspect.ORDER, 10, random);
                    } else if (block == ModBlocks.ENTROPY_INFUSED_STONE.get()) {
                        found = attempt(Aspect.ENTROPY, 20, random);
                    } else if (block == ModBlocks.ENTROPY_CRYSTAL_CLUSTER.get()) {
                        found = attempt(Aspect.ENTROPY, 10, random);
                    } else {
                        found = false;
                    }
                    if (found) {
                        return;
                    }
                    if (block == Blocks.BOOKSHELF && random.nextInt(300) == 0) {
                        Aspect[] aspects = Aspect.values();
                        bonusAspects.merge(aspects[random.nextInt(aspects.length)], 1);
                        return;
                    }
                    if (block == ModBlocks.BRAIN_JAR.get() && random.nextInt(200) == 0) {
                        Aspect[] aspects = Aspect.values();
                        bonusAspects.merge(aspects[random.nextInt(aspects.length)], 1);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            if (level != null && level.isClientSide()) {
                level.playLocalSound(worldPosition, ModSounds.LEARN.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public void onContentsChanged() {
        if (level != null && !level.isClientSide()) {
            sync();
        } else {
            setChanged();
        }
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
            onContentsChanged();
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
        stack.limitSize(getMaxStackSize(stack));
        onContentsChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SCRIBE_SLOT) {
            return stack.getItem() instanceof ScribingToolsItem;
        }
        return stack.getItem() instanceof ResearchNotesItem item && !item.isDiscovery();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
            && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, items);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.research_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ResearchTableMenu(containerId, inventory, worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        nextRecalc = input.getIntOr("nextRecalc", 0);
        bonusAspects = input.read("bonusAspects", AspectList.CODEC).orElse(new AspectList());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
        output.putInt("nextRecalc", nextRecalc);
        output.store("bonusAspects", AspectList.CODEC, bonusAspects);
    }
}
