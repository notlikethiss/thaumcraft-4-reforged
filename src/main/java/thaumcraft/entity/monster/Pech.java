package thaumcraft.entity.monster;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.entity.CombatUtils;
import thaumcraft.item.ManaBeanItem;
import thaumcraft.menu.PechTradeMenu;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;

public class Pech extends Monster implements RangedAttackMob, net.minecraft.world.MenuProvider {
    private static final EntityDataAccessor<Byte> DATA_TYPE = SynchedEntityData.defineId(Pech.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ANGER = SynchedEntityData.defineId(Pech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TAMED = SynchedEntityData.defineId(Pech.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Item, Integer> VALUED_ITEMS = valuedItems();
    private static final net.minecraft.resources.ResourceKey<Biome> EERIE = net.minecraft.resources.ResourceKey.create(Registries.BIOME, Thaumcraft.id("eerie"));
    private static final net.minecraft.resources.ResourceKey<Biome> MAGICAL_FOREST = net.minecraft.resources.ResourceKey.create(Registries.BIOME, Thaumcraft.id("magical_forest"));
    private static final net.minecraft.resources.ResourceKey<Biome> TAINTED_LAND = net.minecraft.resources.ResourceKey.create(Registries.BIOME, Thaumcraft.id("tainted_land"));

    private final ItemStack[] loot = new ItemStack[9];
    private boolean trading;
    private boolean updateCombatGoal;
    private float mumble;
    private @Nullable Goal combatGoal;

    public Pech(EntityType<? extends Pech> type, Level level) {
        super(type, level);
        for (int index = 0; index < this.loot.length; index++) {
            this.loot[index] = ItemStack.EMPTY;
        }
        this.setCanPickUpLoot(true);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 8.0F);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradingGoal());
        this.goalSelector.addGoal(2, new PechItemPickupGoal());
        this.combatGoal = new MeleeAttackGoal(this, 0.6, false);
        this.goalSelector.addGoal(3, this.combatGoal);
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.5, 0.6, player -> !this.isTamed()));
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 10, () -> true));
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return Pech.this.getAnger() > 0 && super.canUse();
            }
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.ARMOR, 2.0);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(0.6F, 1.8F).withEyeHeight(1.8F * 0.66F);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        int occupied = 0;
        for (ItemStack stack : this.loot) {
            if (!stack.isEmpty()) {
                occupied++;
            }
        }
        return occupied < 5;
    }

    public static boolean checkPechSpawnRules(
        EntityType<?> type,
        ServerLevelAccessor level,
        EntitySpawnReason spawnReason,
        BlockPos pos,
        RandomSource random
    ) {
        Holder<Biome> biome = level.getBiome(pos);
        if (!biome.is(Tags.Biomes.IS_MAGICAL) || biome.is(TAINTED_LAND)) {
            return false;
        }
        if (level.getLevel().dimension() != Level.OVERWORLD && !biome.is(MAGICAL_FOREST) && !biome.is(EERIE)) {
            return false;
        }
        AABB area = new AABB(pos).inflate(16.0);
        return level.getLevel().getEntitiesOfClass(Pech.class, area).size() < 4
            && Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) type, level, spawnReason, pos, random);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        return level instanceof ServerLevelAccessor serverLevel
            && checkPechSpawnRules(this.getType(), serverLevel, spawnReason, this.blockPosition(), this.random);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, (byte) PechType.NORMAL.id);
        builder.define(DATA_ANGER, 0);
        builder.define(DATA_TAMED, false);
    }

    public PechType getPechType() {
        return PechType.byId(this.entityData.get(DATA_TYPE));
    }

    public void setPechType(PechType type) {
        this.entityData.set(DATA_TYPE, (byte) type.id);
    }

    public int getAnger() {
        return this.entityData.get(DATA_ANGER);
    }

    private void setAnger(int anger) {
        this.entityData.set(DATA_ANGER, Math.max(0, Math.min(Short.MAX_VALUE, anger)));
    }

    public boolean isTamed() {
        return this.entityData.get(DATA_TAMED);
    }

    public void setTamed(boolean tamed) {
        this.entityData.set(DATA_TAMED, tamed);
        this.updateCombatGoal = true;
    }

    public boolean isTrading() {
        return this.trading;
    }

    public void setTrading(boolean trading) {
        this.trading = trading;
    }

    public ItemStack getLoot(int slot) {
        return this.loot[slot];
    }

    public void setLoot(int slot, ItemStack stack) {
        this.loot[slot] = stack.copy();
    }

    public boolean isValued(ItemStack stack) {
        return !stack.isEmpty() && (VALUED_ITEMS.containsKey(stack.getItem()) || this.getGreedValue(stack) > 0);
    }

    public int getValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int fixedValue = VALUED_ITEMS.getOrDefault(stack.getItem(), 0);
        return fixedValue > 0 ? fixedValue : Math.min(32, this.getGreedValue(stack));
    }

    private int getGreedValue(ItemStack stack) {
        AspectList aspects = AspectHelper.getObjectTags(stack);
        aspects = AspectHelper.getBonusTags(stack, aspects);
        return aspects.getAmount(Aspect.GREED);
    }

    public boolean canPickup(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!this.isTamed() && VALUED_ITEMS.containsKey(stack.getItem())) {
            return true;
        }
        for (ItemStack stored : this.loot) {
            if (stored.isEmpty() || (ItemStack.isSameItemSameComponents(stack, stored) && stack.getCount() + stored.getCount() <= stored.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    public ItemStack pickupItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (!this.isTamed() && this.isValued(stack)) {
            if (this.random.nextInt(10) < this.getValue(stack)) {
                this.setTamed(true);
                this.level().broadcastEntityEvent(this, (byte) 18);
                this.playSound(ModSounds.PECH_TRADE.get(), 0.4F, 1.0F);
            }
            stack.shrink(1);
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }
        for (int index = 0; index < this.loot.length && !stack.isEmpty(); index++) {
            ItemStack stored = this.loot[index];
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stack, stored) && stored.getCount() < stored.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), stored.getMaxStackSize() - stored.getCount());
                stored.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int index = 0; index < this.loot.length && !stack.isEmpty(); index++) {
            if (this.loot[index].isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.loot[index] = stack.copyWithCount(moved);
                stack.shrink(moved);
            }
        }
        return stack;
    }

    public boolean openTradeMenu(ServerPlayer player) {
        if (!this.isTamed() || this.isTrading() || this.distanceToSqr(player) > 64.0) {
            return false;
        }
        player.openMenu(this, buffer -> buffer.writeVarInt(this.getId()));
        return true;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(switch (this.getPechType()) {
            case NORMAL -> "entity.thaumcraft.pech";
            case WARRIOR -> "entity.thaumcraft.pech_mage";
            case ARCHER -> "entity.thaumcraft.pech_stalker";
        });
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PechTradeMenu(containerId, inventory, this.getId(), new thaumcraft.menu.PechTradeInventory(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && !(held.getItem() instanceof net.minecraft.world.item.NameTagItem)) {
            if (this.isTamed()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    this.openTradeMenu(serverPlayer);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason spawnReason,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
        this.addRandomEquipment();
        ItemStack held = this.getMainHandItem();
        if (held.getItem() instanceof WandItem) {
            this.setPechType(PechType.WARRIOR);
        } else if (held.getItem() instanceof BowItem) {
            this.setPechType(PechType.ARCHER);
        }
        this.setDropChance(EquipmentSlot.MAINHAND, this.getMainHandItem().getItem() instanceof WandItem ? 0.1F : 0.2F);
        if (!this.getMainHandItem().isEmpty() && !(this.getMainHandItem().getItem() instanceof WandItem)) {
            this.enchantSpawnedWeapon(level, this.random, difficulty);
        }
        this.setCanPickUpLoot(this.random.nextFloat() < 0.75F * tensionFactor(level, this.blockPosition()));
        return result;
    }

    private static float tensionFactor(ServerLevelAccessor accessor, BlockPos pos) {
        ServerLevel level = accessor.getLevel();
        float factor = 0.0F;
        boolean hard = level.getDifficulty() == Difficulty.HARD;
        if (level.hasChunkAt(pos)) {
            factor += Mth.clamp((float) level.getChunkAt(pos).getInhabitedTime() / 3600000.0F, 0.0F, 1.0F) * (hard ? 1.0F : 0.75F);
            factor += level.getMoonBrightness(pos) * 0.25F;
        }
        if (level.getDifficulty() == Difficulty.EASY || level.getDifficulty() == Difficulty.PEACEFUL) {
            factor *= level.getDifficulty().getId() / 2.0F;
        }
        return Mth.clamp(factor, 0.0F, hard ? 1.5F : 1.0F);
    }

    private void addRandomEquipment() {
        switch (this.random.nextInt(20)) {
            case 0, 12 -> {
                ItemStack wand = new ItemStack(ModItems.WAND.get());
                Wands.setFocus(wand, new ItemStack(ModItems.FOCUS_PECH.get()));
                Wands.addVis(wand, Aspect.EARTH, 2 + this.random.nextInt(6), true);
                Wands.addVis(wand, Aspect.ENTROPY, 2 + this.random.nextInt(6), true);
                Wands.addVis(wand, Aspect.WATER, 2 + this.random.nextInt(6), true);
                Wands.addVis(wand, Aspect.AIR, this.random.nextInt(4), true);
                Wands.addVis(wand, Aspect.FIRE, this.random.nextInt(4), true);
                Wands.addVis(wand, Aspect.ORDER, this.random.nextInt(4), true);
                this.setItemSlot(EquipmentSlot.MAINHAND, wand);
            }
            case 1 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            case 2, 4, 10, 11, 13 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            case 3 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
            case 5 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            case 6 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            case 7 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            case 8 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
            case 9 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            default -> { }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.getAnger() > 0) {
            this.setAnger(this.getAnger() - 1);
            if (this.getTarget() == null) {
                Player player = level.getNearestPlayer(this, 16.0);
                if (player != null) {
                    this.setTarget(player);
                }
            }
        }
        if (this.updateCombatGoal) {
            this.updateCombatGoal = false;
            this.updateCombatTasks();
        }
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
    }

    private void updateCombatTasks() {
        if (this.combatGoal != null) {
            this.goalSelector.removeGoal(this.combatGoal);
        }
        if (this.getMainHandItem().is(Items.BOW) || this.getMainHandItem().getItem() instanceof WandItem) {
            this.combatGoal = this.getMainHandItem().is(Items.BOW)
                ? new RangedAttackGoal(this, 0.6, 20, 50, 15.0F)
                : new RangedAttackGoal(this, 0.6, 20, 30, 15.0F);
        } else {
            this.combatGoal = new MeleeAttackGoal(this, 0.6, false);
        }
        this.goalSelector.addGoal(3, this.combatGoal);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (slot == EquipmentSlot.MAINHAND && !this.level().isClientSide()) {
            this.updateCombatGoal = true;
        }
    }

    private void becomeAngryAt(Player player) {
        if (this.getAnger() == 0) {
            this.level().broadcastEntityEvent(this, (byte) 19);
            this.playSound(ModSounds.PECH_CHARGE.get(), 0.4F, 1.0F);
        }
        this.setTarget(player);
        this.setAnger(400 + this.random.nextInt(400));
        this.setTamed(false);
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            for (Pech neighbor : level.getEntitiesOfClass(Pech.class, this.getBoundingBox().inflate(32.0, 16.0, 32.0))) {
                neighbor.becomeAngryAt(player);
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float distanceFactor) {
        ItemStack held = this.getMainHandItem();
        if (held.getItem() instanceof WandItem && this.level() instanceof ServerLevel level) {
            boolean nightshade = this.random.nextFloat() < 0.1F;
            thaumcraft.entity.projectile.PechBlast blast = new thaumcraft.entity.projectile.PechBlast(ModEntities.PECH_BLAST.get(), level);
            blast.strength = 1;
            blast.nightshade = nightshade;
            double dx = target.getX() + target.getDeltaMovement().x - this.getX();
            double dy = target.getY() + target.getEyeHeight() - 1.500000023841858 - this.getY();
            double dz = target.getZ() + target.getDeltaMovement().z - this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            blast.setOwner(this);
            blast.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
            CombatUtils.aim(blast, dx, dy + horizontal * 0.1, dz, 1.5F, 4.0F);
            this.playSound(ModSounds.ICE.get(), 0.4F, 1.0F + this.random.nextFloat() * 0.1F);
            level.addFreshEntity(blast);
        } else if (held.getItem() instanceof BowItem && this.level() instanceof ServerLevel level) {
            Arrow arrow = new Arrow(level, this, new ItemStack(Items.ARROW), held);
            Vec3 delta = target.position().add(target.getDeltaMovement()).subtract(this.position());
            delta = delta.add(0.0, target.getBbHeight() / 3.0 - this.getEyeHeight(), 0.0);
            arrow.shoot(delta.x, delta.y, delta.z, 1.6F, 14.0F - level.getDifficulty().getId() * 4.0F);
            int power = EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER), held);
            arrow.setBaseDamage(distanceFactor * 2.0 + this.random.nextGaussian() * 0.25 + level.getDifficulty().getId() * 0.11F + (power > 0 ? power * 0.5 + 0.5 : 0.0));
            level.addFreshEntity(arrow);
            this.playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.mumble > 0.0F) {
            this.mumble *= 0.75F;
        }
        if (this.getAnger() > 0 && this.level().isClientSide() && this.random.nextInt(15) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 0.8, this.getZ(), 0.0, 0.0, 0.0);
        }
        if (this.isTamed() && this.level().isClientSide() && this.random.nextInt(25) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.8, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 16) {
            this.mumble = (float) Math.PI;
        } else if (event == 17) {
            this.mumble = (float) (Math.PI * 2.0);
        } else if (event == 18) {
            for (int index = 0; index < 5; index++) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            }
        } else if (event == 19) {
            this.mumble = (float) (Math.PI * 2.0);
            for (int index = 0; index < 5; index++) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, this.getRandomX(1.0), this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            }
        } else {
            super.handleEntityEvent(event);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PECH_IDLE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public void playAmbientSound() {
        if (!this.level().isClientSide() && this.random.nextInt(3) == 0
            && !this.level().getEntitiesOfClass(Pech.class, this.getBoundingBox().inflate(4.0, 2.0, 4.0), pech -> pech != this).isEmpty()) {
            this.level().broadcastEntityEvent(this, (byte) 17);
            this.playSound(ModSounds.PECH_TRADE.get(), this.getSoundVolume(), this.getVoicePitch());
            return;
        }
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 16);
        }
        super.playAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return ModSounds.PECH_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PECH_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    public float getMumble() {
        return this.mumble;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : this.loot) {
            if (!stack.isEmpty() && this.random.nextFloat() < 0.88F) {
                this.spawnAtLocation(level, stack.copy(), 1.5F);
            }
        }
        for (int index = 0; index < 1 + this.getLootingLevel(source); index++) {
            if (this.random.nextBoolean()) {
                this.spawnAtLocation(level, ManaBeanItem.withAspect(Aspect.getPrimalAspects().get(this.random.nextInt(6))), 1.5F);
            }
        }
        int looting = this.getLootingLevel(source);
        if (this.random.nextInt(10) < 1 + looting) {
            this.spawnAtLocation(level, new ItemStack(ModItems.GOLD_COIN.get()), 1.5F);
        }
        if (recentlyHit && this.random.nextInt(200) - looting < 5) {
            this.spawnAtLocation(level, new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), 1.5F);
        }
    }

    private int getLootingLevel(net.minecraft.world.damagesource.DamageSource source) {
        return source.getEntity() instanceof net.minecraft.world.entity.LivingEntity killer
            ? net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                this.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                killer
            )
            : 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("PechType", this.entityData.get(DATA_TYPE));
        output.putInt("Anger", this.entityData.get(DATA_ANGER));
        output.putBoolean("Tamed", this.isTamed());
        output.store("Loot", ItemStack.OPTIONAL_CODEC.listOf(), List.of(this.loot));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_TYPE, input.getByteOr("PechType", (byte) 0));
        this.entityData.set(DATA_ANGER, input.getIntOr("Anger", 0));
        this.entityData.set(DATA_TAMED, input.getBooleanOr("Tamed", false));
        List<ItemStack> stored = input.read("Loot", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int index = 0; index < this.loot.length; index++) {
            this.loot[index] = index < stored.size() ? stored.get(index) : ItemStack.EMPTY;
        }
        this.updateCombatGoal = true;
    }

    private static Map<Item, Integer> valuedItems() {
        Map<Item, Integer> values = new java.util.IdentityHashMap<>();
        values.put(ModBlocks.MANA_BEAN.asItem(), 1);
        values.put(Items.GOLD_INGOT, 2);
        values.put(Items.GOLDEN_APPLE, 2);
        values.put(Items.ENDER_PEARL, 3);
        values.put(Items.DIAMOND, 4);
        values.put(Items.EMERALD, 5);
        return Map.copyOf(values);
    }

    public enum PechType {
        NORMAL(0),
        WARRIOR(1),
        ARCHER(2);

        private final int id;

        PechType(int id) {
            this.id = id;
        }

        private static PechType byId(int id) {
            return switch (id) {
                case 1 -> WARRIOR;
                case 2 -> ARCHER;
                default -> NORMAL;
            };
        }
    }

    private final class TradingGoal extends Goal {
        private TradingGoal() {
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Pech.this.isAlive() && Pech.this.isTamed() && Pech.this.trading && Pech.this.onGround() && !Pech.this.isInWater();
        }

        @Override
        public void start() {
            Pech.this.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void stop() {
            Pech.this.trading = false;
        }
    }

    private final class PechItemPickupGoal extends Goal {
        private @Nullable ItemEntity target;
        private int cooldown;

        private PechItemPickupGoal() {
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Pech.this.isTamed() && Pech.this.trading || Pech.this.tickCount % 20 != 0 || --this.cooldown > 0) {
                return false;
            }
            this.target = Pech.this.level().getEntitiesOfClass(ItemEntity.class, Pech.this.getBoundingBox().inflate(16.0), item -> {
                if (!Pech.this.canPickup(item.getItem())) {
                    return false;
                }
                return !(item.getOwner() instanceof Pech);
            }).stream().min(java.util.Comparator.comparingDouble(Pech.this::distanceToSqr)).orElse(null);
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive() && Pech.this.distanceToSqr(this.target) <= 256.0 && !Pech.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.cooldown = 0;
            if (this.target != null) {
                Pech.this.getNavigation().moveTo(this.target, 0.75);
            }
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            Pech.this.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            if (Pech.this.distanceToSqr(this.target) <= 1.5) {
                ItemStack stack = this.target.getItem();
                int before = stack.getCount();
                ItemStack remainder = Pech.this.pickupItem(stack.copy());
                int taken = before - remainder.getCount();
                if (taken > 0) {
                    stack.shrink(taken);
                    if (stack.isEmpty()) {
                        this.target.discard();
                    }
                    Pech.this.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.2F, 1.4F + Pech.this.random.nextFloat() * 0.4F);
                }
                this.target = null;
                this.cooldown = 4;
            } else if (--this.cooldown <= 0) {
                this.cooldown = 4;
                Pech.this.getNavigation().moveTo(this.target, 0.75);
            }
        }

        @Override
        public void stop() {
            this.target = null;
            Pech.this.getNavigation().stop();
        }
    }
}
