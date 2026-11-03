package thaumcraft.entity.golem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.block.GolemFetterBlock;
import thaumcraft.item.golem.GolemPartItem;
import thaumcraft.menu.GolemMenu;
import thaumcraft.platform.Menus;
import thaumcraft.platform.GolemContainers;
import thaumcraft.registry.ModBlocks;

public class GolemBase extends PathfinderMob implements MenuProvider, GolemContainers.User {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CORE = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ADVANCED = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> UPGRADES = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DECORATION = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> COLORS = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TOGGLES = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<BlockPos> HOME = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.defineId(GolemBase.class, EntityDataSerializers.INT);
    private SimpleContainer filters = new SimpleContainer(1);
    private List<Marker> markers = List.of();
    private @Nullable UUID owner;
    private FluidStack fluidCarried = FluidStack.EMPTY;
    private @Nullable Aspect essentia;
    private int essentiaAmount;
    private int viewers;
    private int regenTimer;
    private final GolemContainers containers = new GolemContainers(this);
    public boolean inactive;
    public float bootup;
    public int action;
    public int leftArm;
    public int rightArm;
    public int healing;

    public GolemBase(EntityType<? extends GolemBase> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
            navigation.setCanFloat(true);
        }
    }

    @Override
    public GolemContainers containers() { return containers; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) containers.close();
        super.remove(reason);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 1)
            .add(Attributes.MOVEMENT_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 0).add(Attributes.STEP_HEIGHT, 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 1);
        builder.define(CORE, -1);
        builder.define(ADVANCED, false);
        builder.define(UPGRADES, "f");
        builder.define(DECORATION, "");
        builder.define(COLORS, "h");
        builder.define(TOGGLES, 0);
        builder.define(CARRIED, ItemStack.EMPTY);
        builder.define(DISPLAY, ItemStack.EMPTY);
        builder.define(HOME, BlockPos.ZERO);
        builder.define(FACING, Direction.UP.ordinal());
    }

    public GolemType getGolemType() { return GolemType.byId(entityData.get(TYPE)); }
    public int getCore() { return entityData.get(CORE); }
    public boolean isAdvanced() { return entityData.get(ADVANCED); }
    public String getGolemDecoration() { return entityData.get(DECORATION); }
    public BlockPos getHomePosition() { return entityData.get(HOME); }
    public Direction getHomeFacing() { return Direction.values()[Math.clamp(entityData.get(FACING), 0, 5)]; }
    public @Nullable UUID getOwnerUUID() { return owner; }
    public void setOwnerUUID(@Nullable UUID owner) { this.owner = owner; }
    public SimpleContainer inventory() { return filters; }
    public List<Marker> getMarkers() { return markers; }
    public void setMarkers(List<Marker> markers) { this.markers = List.copyOf(markers); }
    public void setHome(BlockPos home, Direction facing) { entityData.set(HOME, home.immutable()); entityData.set(FACING, facing.ordinal()); }
    public boolean isWorking() { return getCore() >= 0 && viewers == 0 && !inactive && isAlive(); }
    public void openConfiguration() { viewers++; }
    public void closeConfiguration() { viewers = Math.max(0, viewers - 1); }
    public boolean canOpen(Player player) { return isAlive() && distanceToSqr(player) <= 64; }

    public int getUpgradeAmount(int type) {
        int count = 0;
        for (char upgrade : entityData.get(UPGRADES).toCharArray()) if (Character.digit(upgrade, 16) == type) count++;
        return count;
    }

    public int getUpgrade(int slot) {
        String upgrades = entityData.get(UPGRADES);
        return slot < 0 || slot >= upgrades.length() || upgrades.charAt(slot) == 'f' ? -1 : Character.digit(upgrades.charAt(slot), 16);
    }

    public int getColors(int slot) {
        String colors = entityData.get(COLORS);
        return slot < 0 || slot >= colors.length() || colors.charAt(slot) == 'h' ? -1 : Character.digit(colors.charAt(slot), 16);
    }

    public void setColors(int slot, int color) {
        if (slot < 0 || slot >= filters.getContainerSize() || color < -1 || color > 15) return;
        StringBuilder colors = new StringBuilder(entityData.get(COLORS));
        colors.setCharAt(slot, color < 0 ? 'h' : Character.forDigit(color, 16));
        entityData.set(COLORS, colors.toString());
    }

    public boolean[] getToggles() {
        boolean[] toggles = new boolean[8];
        for (int index = 0; index < 8; index++) toggles[index] = (entityData.get(TOGGLES) & 1 << index) != 0;
        return toggles;
    }

    public void setToggle(int index, boolean value) {
        if (index >= 0 && index < 8) entityData.set(TOGGLES, value ? entityData.get(TOGGLES) | 1 << index : entityData.get(TOGGLES) & ~(1 << index));
    }

    public boolean checkOreDict() { return getToggles()[5]; }
    public boolean ignoreDamage() { return getToggles()[6]; }
    public boolean ignoreNBT() { return getToggles()[7]; }
    public int getGolemStrength() { return getGolemType().strength + getUpgradeAmount(1); }
    public int getCarryLimit() { int base = getGolemType().carry; return base + Math.min(16, Math.max(4, base)) * getUpgradeAmount(1); }
    public int getFluidCarryLimit() { return (int) Math.sqrt(getCarryLimit()) * 1000; }
    public int getAttackSpeed() { return isAdvanced() ? 18 : 20; }
    public float getRange() {
        float range = 16 + getUpgradeAmount(3) * 4;
        if (getGolemDecoration().contains("G")) range += Math.max(range * 0.1F, 1);
        if (isAdvanced()) range += Math.max(range * 0.2F, 2);
        return range;
    }

    public boolean isWithinHome(BlockPos pos) { return getHomePosition().distSqr(pos) < getRange() * getRange(); }
    public ItemStack getCarried() { return entityData.get(CARRIED); }
    public ItemStack getCarriedForDisplay() { return entityData.get(DISPLAY); }
    public void setCarried(ItemStack stack) { entityData.set(CARRIED, stack.isEmpty() ? ItemStack.EMPTY : stack.copy()); updateCarried(); }
    public int getCarrySpace() { return getCarried().isEmpty() ? getCarryLimit() : Math.max(0, Math.min(getCarryLimit(), getCarried().getMaxStackSize()) - getCarried().getCount()); }
    public FluidStack getFluidCarried() { return fluidCarried; }
    public void setFluidCarried(FluidStack fluid) { fluidCarried = fluid.copy(); updateCarried(); }
    public @Nullable Aspect getEssentia() { return essentia; }
    public int getEssentiaAmount() { return essentiaAmount; }
    public void setEssentia(@Nullable Aspect aspect, int amount) { essentia = amount > 0 ? aspect : null; essentiaAmount = Math.max(0, amount); updateCarried(); }

    public void updateCarried() {
        ItemStack display = getCarried().copy();
        if (display.isEmpty() && getCore() == 5) {
            display = new ItemStack(fluidCarried.isEmpty() ? net.minecraft.world.item.Items.BUCKET : fluidCarried.getFluid().getBucket());
            if (display.isEmpty()) display = new ItemStack(net.minecraft.world.item.Items.BUCKET);
        }
        if (display.isEmpty() && getCore() == 6) {
            display = new ItemStack(ModBlocks.FILLED_JAR.get());
            if (essentia != null && essentiaAmount > 0) thaumcraft.aspect.AspectKeys.ASPECTS.set(display, new thaumcraft.aspect.AspectList().add(essentia, (int) (64.0F * essentiaAmount / getCarryLimit())));
        }
        entityData.set(DISPLAY, display);
    }

    public boolean matchesFilter(ItemStack filter, ItemStack stack) {
        if (filter.isEmpty() || stack.isEmpty()) return false;
        boolean same = filter.is(stack.getItem());
        if (!same && checkOreDict()) same = sharesOreTag(filter, stack);
        if (!same || !ignoreDamage() && filter.getDamageValue() != stack.getDamageValue()) return false;
        if (ignoreNBT()) return true;
        ItemStack first = filter.copy();
        ItemStack second = stack.copy();
        if (ignoreDamage()) {
            first.remove(net.minecraft.core.component.DataComponents.DAMAGE);
            second.remove(net.minecraft.core.component.DataComponents.DAMAGE);
        }
        return first.getComponentsPatch().equals(second.getComponentsPatch());
    }

    public boolean sharesOreTag(ItemStack first, ItemStack second) {
        return first.getItem().builtInRegistryHolder().tags().anyMatch(tag -> {
            String namespace = tag.location().getNamespace();
            String path = tag.location().getPath();
            boolean common = namespace.equals("c") || namespace.equals("forge");
            boolean umbrella = java.util.Set.of("ingots", "nuggets", "ores", "gems", "dusts", "storage_blocks", "foods", "crops", "tools", "armors").contains(path);
            boolean legacyWood = namespace.equals("minecraft") && java.util.Set.of("planks", "logs", "wool").contains(path);
            return (common && !umbrella || legacyWood) && second.is(tag);
        });
    }

    public List<Integer> getColorsMatching(ItemStack stack) {
        List<Integer> colors = new ArrayList<>();
        for (int slot = 0; slot < filters.getContainerSize(); slot++) if (filters.isEmpty() || matchesFilter(filters.getItem(slot), stack)) colors.add(getColors(slot));
        return colors;
    }

    public GolemSnapshot snapshot() {
        List<Integer> upgrades = new ArrayList<>();
        for (int slot = 0; slot < entityData.get(UPGRADES).length(); slot++) upgrades.add(getUpgrade(slot));
        List<Integer> colors = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < filters.getContainerSize(); slot++) { colors.add(getColors(slot)); items.add(filters.getItem(slot).copy()); }
        return new GolemSnapshot(getCore(), isAdvanced(), upgrades, getGolemDecoration(), markers, ItemContainerContents.fromItems(items), colors, entityData.get(TOGGLES));
    }

    public void configure(GolemType type, GolemSnapshot state) {
        entityData.set(TYPE, type.ordinal());
        entityData.set(CORE, state.core());
        entityData.set(ADVANCED, state.advanced());
        entityData.set(DECORATION, state.decoration());
        StringBuilder upgrades = new StringBuilder();
        for (int slot = 0; slot < type.upgrades + (state.advanced() ? 1 : 0); slot++) {
            int upgrade = slot < state.upgrades().size() ? state.upgrades().get(slot) : -1;
            upgrades.append(upgrade < 0 ? 'f' : Character.forDigit(upgrade, 16));
        }
        entityData.set(UPGRADES, upgrades.toString());
        setMarkers(state.markers());
        entityData.set(TOGGLES, state.toggles());
        int slots = hasInventory(getCore()) ? (getCore() == 5 ? 1 + getUpgradeAmount(2) : 6 + getUpgradeAmount(2) * 6) : 1;
        filters = new SimpleContainer(slots);
        state.filters().copyInto(filters.getItems());
        StringBuilder colors = new StringBuilder();
        for (int slot = 0; slot < slots; slot++) { int color = slot < state.colors().size() ? state.colors().get(slot) : -1; colors.append(color < 0 ? 'h' : Character.forDigit(color, 16)); }
        entityData.set(COLORS, colors.toString());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.health + (state.decoration().contains("H") ? 5 : 0));
        getAttribute(Attributes.ARMOR).setBaseValue(type.armor + (state.decoration().contains("V") ? 1 : 0) + (state.decoration().contains("P") ? 4 : 0));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2 + getGolemStrength() + getUpgradeAmount(1) + (state.decoration().contains("M") ? 2 : 0));
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, type.ordinal() >= 5 ? 0 : 8);
        rebuildGoals();
        updateCarried();
    }

    public static boolean hasInventory(int core) { return core == 0 || core == 1 || core == 2 || core == 5 || core == 8; }
    public static boolean hasGUI(int core) { return hasInventory(core) || core == 4 || core == 10; }

    public void rebuildGoals() {
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        if (getCore() < 0) return;
        LogisticsGoals.install(this, goalSelector);
        WorkGoals.install(this, goalSelector, targetSelector);
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new ReturnHomeGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && isWorking();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (level().isClientSide() && filters != null && (accessor == CORE || accessor == UPGRADES || accessor == ADVANCED)) {
            int size = hasInventory(getCore()) ? (getCore() == 5 ? 1 + getUpgradeAmount(2) : 6 + getUpgradeAmount(2) * 6) : 1;
            if (filters.getContainerSize() != size) filters = new SimpleContainer(size);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            if (getUpgradeAmount(2) > 0) target.igniteForSeconds(getUpgradeAmount(2) * 4);
            if (getGolemDecoration().contains("V") && target instanceof net.minecraft.world.entity.LivingEntity living && owner != null) living.setLastHurtByPlayer(owner, 100);
            startRightArmTimer();
        }
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (isWorking()) {
            if (tickCount > 1 && (tickCount + getId()) % 2 != 0) {
                targetSelector.tick();
                goalSelector.tick();
            }
            super.customServerAiStep(level);
        }
        else { getNavigation().stop(); setTarget(null); }
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) containers.tick();
        var state = level().getBlockState(blockPosition().below());
        inactive = state.is(ModBlocks.GOLEM_FETTER.get()) && state.getValue(GolemFetterBlock.ACTIVE);
        float speed = getGolemType().speed * (getGolemDecoration().contains("B") ? 1.1F : 1) * (getGolemDecoration().contains("P") ? 0.88F : 1) * (1 + getUpgradeAmount(0) * 0.15F) * (isAdvanced() ? 1.1F : 1);
        if (isInWater() && getGolemType().ordinal() >= 5) speed *= 2;
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(isWorking() ? speed : 0);
        super.aiStep();
        if (action > 0) action--;
        if (leftArm > 0) leftArm--;
        if (rightArm > 0) rightArm--;
        if (healing > 0) healing--;
        if (!level().isClientSide()) {
            if (regenTimer-- <= 0) {
                regenTimer = (int) (getGolemType().regenDelay * (getGolemDecoration().contains("F") ? 0.66F : 1));
                if (getHealth() < getMaxHealth()) { heal(1); level().broadcastEntityEvent(this, (byte) 5); }
            }
            if (distanceToSqr(getHomePosition().getX(), getHomePosition().getY(), getHomePosition().getZ()) >= 2304 || isInWall()) returnHome();
        } else if (bootup > 0 && getCore() >= 0) bootup *= bootup / 33.1F;
    }

    private void returnHome() {
        BlockPos home = getHomePosition();
        if (!level().hasChunkAt(home)) return;
        for (int y = 1; y >= -1; y--) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = home.offset(x, y, z);
            if (level().getBlockState(pos.below()).isFaceSturdy(level(), pos.below(), Direction.UP) && level().noCollision(this, getBoundingBox().move(pos.getX() + 0.5 - getX(), pos.getY() - getY(), pos.getZ() + 0.5 - getZ()))) {
                setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5); getNavigation().stop(); return;
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || stack.getItem() instanceof thaumcraft.item.golem.GolemBellItem) return InteractionResult.PASS;
        if (stack.getItem() instanceof GolemPartItem part) {
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            GolemSnapshot old = snapshot();
            if (part.kind() == GolemPartItem.Kind.CORE && getCore() < 0 && part.index() >= 0) {
                configure(getGolemType(), new GolemSnapshot(part.index(), old.advanced(), old.upgrades(), old.decoration(), old.markers(), old.filters(), old.colors(), old.toggles()));
                level().broadcastEntityEvent(this, (byte) 7);
            } else if (part.kind() == GolemPartItem.Kind.UPGRADE && getUpgradeAmount(part.index()) < 2 && old.upgrades().contains(-1)) {
                List<Integer> upgrades = new ArrayList<>(old.upgrades()); upgrades.set(upgrades.indexOf(-1), part.index());
                configure(getGolemType(), new GolemSnapshot(old.core(), old.advanced(), upgrades, old.decoration(), old.markers(), old.filters(), old.colors(), old.toggles()));
            } else if (part.kind() == GolemPartItem.Kind.DECORATION && canDecorate(part.decoration())) {
                configure(getGolemType(), new GolemSnapshot(old.core(), old.advanced(), old.upgrades(), old.decoration() + part.decoration(), old.markers(), old.filters(), old.colors(), old.toggles()));
            } else return InteractionResult.PASS;
            stack.shrink(1);
            playSound(thaumcraft.registry.ModSounds.UPGRADE.get(), 0.5F, 1);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.COOKIE)) {
            if (!level().isClientSide()) { stack.shrink(1); heal(5); addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, Math.min(2400, hasEffect(net.minecraft.world.effect.MobEffects.SPEED) ? getEffect(net.minecraft.world.effect.MobEffects.SPEED).getDuration() + 600 : 600))); }
            return InteractionResult.SUCCESS;
        }
        if (hasGUI(getCore()) && !(stack.getItem() instanceof thaumcraft.wand.WandItem)) {
            if (player instanceof ServerPlayer server) Menus.openEntity(server, this, getId());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private boolean canDecorate(String decoration) {
        String current = getGolemDecoration();
        if (current.contains(decoration)) return false;
        for (String group : List.of("FH", "GV", "BP")) if (group.contains(decoration) && group.chars().anyMatch(c -> current.indexOf(c) >= 0)) return false;
        return true;
    }

    public ItemStack toItem(boolean dismantle) {
        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Thaumcraft.id(getGolemType().itemId())));
        GolemKeys.STATE.set(stack, dismantle ? new GolemSnapshot(-1, isAdvanced(), List.of(), "", List.of(), ItemContainerContents.EMPTY, List.of(), 0) : snapshot());
        if (!fluidCarried.isEmpty()) GolemKeys.FLUID.set(stack, java.util.Optional.of(net.neoforged.neoforge.fluids.FluidStackTemplate.fromNonEmptyStack(fluidCarried)));
        if (essentia != null && essentiaAmount > 0) {
            GolemKeys.ESSENTIA.set(stack, essentia.getTag());
            GolemKeys.ESSENTIA_AMOUNT.set(stack, essentiaAmount);
        }
        if (!dismantle && hasCustomName()) stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
        return stack;
    }

    public void dropCarried() {
        if (level() instanceof ServerLevel server && !getCarried().isEmpty()) { spawnAtLocation(server, getCarried()); setCarried(ItemStack.EMPTY); }
    }

    public void startActionTimer() { if (action == 0) { action = 6; level().broadcastEntityEvent(this, (byte) 4); } }
    public void startLeftArmTimer() { if (leftArm == 0) { leftArm = 5; level().broadcastEntityEvent(this, (byte) 6); } }
    public void startRightArmTimer() { if (rightArm == 0) { rightArm = 5; level().broadcastEntityEvent(this, (byte) 8); } }
    public int getActionTimer() { return 3 - Math.abs(action - 3); }
    @Override public void handleEntityEvent(byte event) { switch (event) { case 4 -> action = 6; case 5 -> healing = 5; case 6 -> leftArm = 5; case 7 -> bootup = 33; case 8 -> rightArm = 5; default -> super.handleEntityEvent(event); } }
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS)) return false;
        if (getGolemType() == GolemType.THAUMIUM && source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) amount *= 0.5F;
        var attacker = source.getDirectEntity();
        int entropy = getUpgradeAmount(5);
        if (attacker != null && attacker != this && entropy > 0 && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) attacker.hurtServer(level, damageSources().thorns(this), entropy * 2 + random.nextInt(entropy * 2));
        return super.hurtServer(level, source, amount);
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return thaumcraft.registry.ModSounds.CAMERACLACK.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return thaumcraft.registry.ModSounds.CAMERACLACK.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return thaumcraft.registry.ModSounds.CAMERACLACK.get(); }
    @Override protected float getSoundVolume() { return 0.1F; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean fireImmune() { return getGolemType().fireResist; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hit) { super.dropCustomDeathLoot(level, source, hit); dropCarried(); }
    @Override public Component getDisplayName() { return getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GolemMenu(id, inventory, getId()); }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("GolemType", getGolemType().ordinal());
        output.store("GolemState", GolemSnapshot.CODEC, snapshot());
        output.store("Home", BlockPos.CODEC, getHomePosition());
        output.putInt("HomeFacing", getHomeFacing().ordinal());
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
        output.store("Carried", ItemStack.OPTIONAL_CODEC, getCarried());
        output.store("Fluid", FluidStack.OPTIONAL_CODEC, fluidCarried);
        if (essentia != null) output.putString("Essentia", essentia.getTag());
        output.putInt("EssentiaAmount", essentiaAmount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        configure(GolemType.byId(input.getIntOr("GolemType", 1)), input.read("GolemState", GolemSnapshot.CODEC).orElse(GolemSnapshot.EMPTY));
        setHome(input.read("Home", BlockPos.CODEC).orElse(blockPosition()), Direction.values()[Math.clamp(input.getIntOr("HomeFacing", 1), 0, 5)]);
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        setCarried(input.read("Carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setFluidCarried(input.read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
        setEssentia(Aspect.getAspect(input.getStringOr("Essentia", "")), input.getIntOr("EssentiaAmount", 0));
    }

    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() { setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return isWorking() && distanceToSqr(getHomePosition().getX() + 0.5, getHomePosition().getY(), getHomePosition().getZ() + 0.5) > 5; }
        @Override public void start() { getNavigation().moveTo(getHomePosition().getX() + 0.5, getHomePosition().getY(), getHomePosition().getZ() + 0.5, 1); }
        @Override public boolean canContinueToUse() { return isWorking() && !getNavigation().isDone() && canUse(); }
    }
}
