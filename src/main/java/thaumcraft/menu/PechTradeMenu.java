package thaumcraft.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import thaumcraft.entity.monster.Pech;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEnchantments;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModMenus;
import thaumcraft.registry.ModSounds;

public class PechTradeMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 5;
    private static final int PLAYER_START = 5;
    private static final int SLOT_END = 41;

    private final @Nullable Pech pech;
    private final SimpleContainer tradeInventory;

    public PechTradeMenu(int containerId, Inventory inventory, int pechEntityId) {
        this(containerId, inventory, pechEntityId, new SimpleContainer(5));
    }

    public PechTradeMenu(int containerId, Inventory inventory, int pechEntityId, PechTradeInventory serverInventory) {
        this(containerId, inventory, pechEntityId, (SimpleContainer) serverInventory);
    }

    private PechTradeMenu(int containerId, Inventory inventory, int pechEntityId, SimpleContainer tradeInventory) {
        super(ModMenus.PECH_TRADE.get(), containerId);
        this.pech = inventory.player.level().getEntity(pechEntityId) instanceof Pech entity ? entity : null;
        this.tradeInventory = tradeInventory;
        if (this.pech != null) {
            this.pech.setTrading(true);
        }
        addSlot(new Slot(tradeInventory, INPUT_SLOT, 36, 29));
        for (int index = 0; index < 4; index++) {
            addSlot(new OutputSlot(tradeInventory, OUTPUT_START + index, 106 + index % 2 * 18, 20 + index / 2 * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    public @Nullable Pech pech() {
        return pech;
    }

    public boolean canTrade() {
        if (pech == null || !pech.isTamed() || tradeInventory.getItem(INPUT_SLOT).isEmpty() || !pech.isValued(tradeInventory.getItem(INPUT_SLOT))) {
            return false;
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (!tradeInventory.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !(player instanceof ServerPlayer) || pech == null || !stillValid(player) || !canTrade()) {
            return false;
        }
        ItemStack input = tradeInventory.getItem(INPUT_SLOT);
        TradePlan plan = createPlan(pech, pech.getValue(input), pech.getRandom(), pech.level().registryAccess());
        if (plan == null) {
            return false;
        }
        if (plan.betray()) {
            pech.setTamed(false);
        }
        for (int slot = 0; slot < plan.remainingLoot().size(); slot++) {
            pech.setLoot(slot, plan.remainingLoot().get(slot));
        }
        input.shrink(1);
        for (int index = 0; index < plan.outputs().size(); index++) {
            tradeInventory.setItem(OUTPUT_START + index, plan.outputs().get(index));
        }
        tradeInventory.setChanged();
        if (pech.level() instanceof ServerLevel level) {
            level.playSound(null, pech.blockPosition(), ModSounds.PECH_TRADE.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.4F, 1.0F);
        }
        return true;
    }

    private static @Nullable TradePlan createPlan(Pech pech, int value, RandomSource random, RegistryAccess registryAccess) {
        boolean betray = random.nextInt(100) <= value / 2;
        if (random.nextInt(5) == 0) {
            value += random.nextInt(3);
        } else if (random.nextBoolean()) {
            value -= random.nextInt(3);
        }
        List<ItemStack> outputs = new ArrayList<>(4);
        List<ItemStack> remainingLoot = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            remainingLoot.add(pech.getLoot(slot).copy());
        }
        while (value > 0) {
            int amount = Math.min(5, Math.max((value + 1) / 2, random.nextInt(value) + 1));
            value -= amount;
            ItemStack reward;
            if (amount == 1 && random.nextBoolean() && hasLoot(remainingLoot)) {
                List<Integer> occupied = new ArrayList<>();
                for (int slot = 0; slot < remainingLoot.size(); slot++) {
                    if (!remainingLoot.get(slot).isEmpty()) {
                        occupied.add(slot);
                    }
                }
                int slot = occupied.get(random.nextInt(occupied.size()));
                reward = remainingLoot.get(slot).copyWithCount(1);
                remainingLoot.get(slot).shrink(1);
            } else if (amount >= 4 && random.nextBoolean()) {
                reward = dungeonReward(random, registryAccess);
            } else {
                List<ItemStack> offers = tradeOffers(pech.getPechType(), amount, registryAccess);
                if (offers.isEmpty()) {
                    return null;
                }
                reward = offers.get(random.nextInt(offers.size()));
            }
            if (!insertReward(outputs, reward)) {
                return null;
            }
        }
        return new TradePlan(List.copyOf(outputs), List.copyOf(remainingLoot), betray);
    }

    private static boolean hasLoot(List<ItemStack> loot) {
        for (ItemStack stack : loot) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean insertReward(List<ItemStack> outputs, ItemStack reward) {
        ItemStack remaining = reward.copy();
        for (ItemStack output : outputs) {
            if (ItemStack.isSameItemSameComponents(output, remaining) && output.getCount() < output.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), output.getMaxStackSize() - output.getCount());
                output.grow(moved);
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        if (outputs.size() < 4) {
            outputs.add(remaining);
            return true;
        }
        return false;
    }

    private static ItemStack dungeonReward(RandomSource random, RegistryAccess registryAccess) {
        List<WeightedOffer> offers = dungeonOffers();
        WeightedOffer selected = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            selected = offers.get(random.nextInt(offers.size()));
            if (selected.weight() <= 5 && selected.maximumGenerate() <= 1) {
                break;
            }
        }
        if (selected == null) {
            return ItemStack.EMPTY;
        }
        if (selected.stack().is(Items.ENCHANTED_BOOK)) {
            return EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK), 30, registryAccess, Optional.empty());
        }
        return selected.stack().copy();
    }

    private static List<WeightedOffer> dungeonOffers() {
        List<WeightedOffer> offers = new ArrayList<>();
        offers.add(new WeightedOffer(new ItemStack(Items.SADDLE), 10, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.IRON_INGOT), 10, 4));
        offers.add(new WeightedOffer(new ItemStack(Items.BREAD), 10, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.WHEAT), 10, 4));
        offers.add(new WeightedOffer(new ItemStack(Items.GUNPOWDER), 10, 4));
        offers.add(new WeightedOffer(new ItemStack(Items.STRING), 10, 4));
        offers.add(new WeightedOffer(new ItemStack(Items.BUCKET), 10, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.GOLDEN_APPLE), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.REDSTONE), 10, 4));
        offers.add(new WeightedOffer(new ItemStack(Items.MUSIC_DISC_13), 10, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.MUSIC_DISC_CAT), 10, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.NAME_TAG), 10, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.GOLDEN_HORSE_ARMOR), 2, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.IRON_HORSE_ARMOR), 5, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.DIAMOND_HORSE_ARMOR), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(Items.ENCHANTED_BOOK), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.COMMON_LOOT_BAG.get()), 5, 3));
        offers.add(new WeightedOffer(new ItemStack(ModItems.THAUMIUM_INGOT.get()), 5, 3));
        offers.add(new WeightedOffer(new ItemStack(ModItems.AMBER.get()), 5, 3));
        offers.add(new WeightedOffer(new ItemStack(ModItems.UNCOMMON_LOOT_BAG.get()), 4, 2));
        offers.add(new WeightedOffer(new ItemStack(ModItems.MUNDANE_AMULET.get()), 4, 2));
        offers.add(new WeightedOffer(new ItemStack(ModItems.MUNDANE_RING.get()), 4, 2));
        offers.add(new WeightedOffer(new ItemStack(ModItems.MUNDANE_BELT.get()), 4, 2));
        offers.add(new WeightedOffer(new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), 4, 2));
        offers.add(new WeightedOffer(new ItemStack(ModItems.RARE_LOOT_BAG.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.THAUMONOMICON.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.THAUMIUM_SWORD.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.THAUMIUM_PICKAXE.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.THAUMIUM_AXE.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.THAUMIUM_HOE.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.PROTECTION_RING.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.APPRENTICE_RING_AIR.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.APPRENTICE_RING_EARTH.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.APPRENTICE_RING_FIRE.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.APPRENTICE_RING_WATER.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.APPRENTICE_RING_ORDER.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.APPRENTICE_RING_ENTROPY.get()), 1, 1));
        offers.add(new WeightedOffer(new ItemStack(ModItems.VIS_AMULET.get()), 1, 1));
        return offers;
    }

    public static List<ItemStack> tradeOffers(Pech.PechType type, int value, RegistryAccess registryAccess) {
        List<ItemStack> result = new ArrayList<>();
        switch (type) {
            case NORMAL -> {
                switch (value) {
                    case 1 -> result.addAll(List.of(
                        new ItemStack(ModBlocks.MANA_BEAN.get()),
                        new ItemStack(ModItems.NATIVE_IRON_CLUSTER.get()),
                        new ItemStack(ModItems.NATIVE_GOLD_CLUSTER.get()),
                        new ItemStack(ModItems.NATIVE_CINNABAR_CLUSTER.get()),
                        new ItemStack(ModItems.NATIVE_COPPER_CLUSTER.get()),
                        new ItemStack(ModItems.NATIVE_TIN_CLUSTER.get()),
                        new ItemStack(ModItems.NATIVE_SILVER_CLUSTER.get()),
                        new ItemStack(ModItems.NATIVE_LEAD_CLUSTER.get())
                    ));
                    case 2 -> result.addAll(List.of(new ItemStack(Items.BLAZE_ROD), new ItemStack(ModBlocks.GREATWOOD_SAPLING.get()), legacyPotion(8201), legacyPotion(8194)));
                    case 3 -> result.addAll(List.of(new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), new ItemStack(Items.GOLDEN_APPLE), legacyPotion(8265), legacyPotion(8262)));
                    case 4 -> result.add(new ItemStack(ModItems.THAUMIUM_PICKAXE.get()));
                    case 5 -> result.addAll(List.of(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), new ItemStack(ModBlocks.SILVERWOOD_SAPLING.get()), new ItemStack(ModBlocks.SILVERWOOD_SAPLING.get())));
                }
            }
            case WARRIOR -> {
                switch (value) {
                    case 1 -> result.addAll(List.of(
                        new ItemStack(ModBlocks.MANA_BEAN.get()),
                        new ItemStack(ModItems.AIR_SHARD.get()),
                        new ItemStack(ModItems.FIRE_SHARD.get()),
                        new ItemStack(ModItems.WATER_SHARD.get()),
                        new ItemStack(ModItems.EARTH_SHARD.get()),
                        new ItemStack(ModItems.ORDER_SHARD.get()),
                        new ItemStack(ModItems.ENTROPY_SHARD.get()),
                        new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get())
                    ));
                    case 2 -> result.addAll(List.of(new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), legacyPotion(8193), legacyPotion(8261)));
                    case 3 -> result.addAll(List.of(new ItemStack(Items.GOLDEN_APPLE), enchantmentBook(registryAccess, ModEnchantments.HASTE, 1), legacyPotion(8225), legacyPotion(8229)));
                    case 4 -> result.addAll(List.of(
                        new ItemStack(ModBlocks.AIR_CRYSTAL_CLUSTER.get()),
                        new ItemStack(ModBlocks.FIRE_CRYSTAL_CLUSTER.get()),
                        new ItemStack(ModBlocks.WATER_CRYSTAL_CLUSTER.get()),
                        new ItemStack(ModBlocks.EARTH_CRYSTAL_CLUSTER.get()),
                        new ItemStack(ModBlocks.ORDER_CRYSTAL_CLUSTER.get()),
                        new ItemStack(ModBlocks.ENTROPY_CRYSTAL_CLUSTER.get()),
                        new ItemStack(ModBlocks.MIXED_CRYSTAL_CLUSTER.get())
                    ));
                    case 5 -> result.addAll(List.of(
                        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
                        enchantmentBook(registryAccess, ModEnchantments.REPAIR, 1),
                        new ItemStack(ModItems.FOCUS_POUCH.get()),
                        new ItemStack(ModItems.FOCUS_PECH.get()),
                        new ItemStack(ModItems.VIS_AMULET.get())
                    ));
                }
            }
            case ARCHER -> {
                switch (value) {
                    case 1 -> {
                        result.add(new ItemStack(ModBlocks.MANA_BEAN.get()));
                        int index = 0;
                        for (var candle : ModBlocks.CANDLES.values()) {
                            if (index++ == 15) {
                                break;
                            }
                            result.add(new ItemStack(candle.get()));
                        }
                    }
                    case 2 -> result.addAll(List.of(
                        new ItemStack(Items.GHAST_TEAR), legacyPotion(8194), legacyPotion(8201),
                        enchantmentBook(registryAccess, net.minecraft.world.item.enchantment.Enchantments.POWER, 1)
                    ));
                    case 3 -> result.addAll(List.of(
                        new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()),
                        legacyPotion(8270), legacyPotion(8225), new ItemStack(Items.GOLDEN_APPLE)
                    ));
                    case 4 -> result.add(new ItemStack(ModItems.THAUMIUM_BOOTS.get()));
                    case 5 -> result.addAll(List.of(
                        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
                        new ItemStack(ModItems.PROTECTION_RING.get()),
                        enchantmentBook(registryAccess, net.minecraft.world.item.enchantment.Enchantments.FLAME, 1),
                        enchantmentBook(registryAccess, net.minecraft.world.item.enchantment.Enchantments.INFINITY, 1)
                    ));
                }
            }
        }
        return result;
    }

    private static ItemStack legacyPotion(int metadata) {
        Holder<MobEffect> effect = switch (metadata) {
            case 8201, 8265 -> MobEffects.STRENGTH;
            case 8194 -> MobEffects.SPEED;
            case 8262 -> MobEffects.NIGHT_VISION;
            case 8193, 8225 -> MobEffects.REGENERATION;
            case 8261, 8229 -> MobEffects.INSTANT_HEALTH;
            case 8270 -> MobEffects.INVISIBILITY;
            default -> throw new IllegalArgumentException("Unsupported Pech potion: " + metadata);
        };
        int duration = switch (metadata) {
            case 8201 -> 3600;
            case 8194 -> 3600;
            case 8265, 8262, 8270 -> 9600;
            case 8193 -> 900;
            case 8261, 8229 -> 1;
            case 8225 -> 450;
            default -> throw new IllegalArgumentException("Unsupported Pech potion: " + metadata);
        };
        int amplifier = metadata == 8225 || metadata == 8229 ? 1 : 0;
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(effect, duration, amplifier)), Optional.empty()));
        return stack;
    }

    private static ItemStack enchantmentBook(RegistryAccess registryAccess, net.minecraft.resources.ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> enchantment = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.enchant(enchantment, level);
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return pech != null && pech.isAlive() && pech.isTamed() && player.distanceToSqr(pech) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            for (int index = 0; index < OUTPUT_END; index++) {
                ItemStack stack = tradeInventory.removeItemNoUpdate(index);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
            if (pech != null) {
                pech.setTrading(false);
            }
        }
    }

    private record TradePlan(List<ItemStack> outputs, List<ItemStack> remainingLoot, boolean betray) {
    }

    private record WeightedOffer(ItemStack stack, int weight, int maximumGenerate) {
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
