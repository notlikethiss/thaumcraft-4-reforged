package thaumcraft.entity.boss;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import thaumcraft.entity.ai.AttackOnCollideGoal;
import thaumcraft.entity.ai.CultistHurtByTargetGoal;
import thaumcraft.entity.ai.LongRangeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import thaumcraft.entity.monster.Cultist;
import thaumcraft.entity.projectile.GolemOrb;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModEnchantments;
import thaumcraft.registry.ModSounds;

public class CultistLeader extends ThaumcraftBoss implements RangedAttackMob {
    private static final String[] TITLES = {"Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse", "Dodorol", "Ebardo", "Ferrando", "Fertus", "Guillen", "Larpe", "Obano", "Zelipe"};
    private int title;

    public CultistLeader(EntityType<? extends CultistLeader> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 125.0).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LongRangeAttackGoal(this, 16.0, 1.0, 30, 40, 24.0F));
        this.goalSelector.addGoal(3, new AttackOnCollideGoal(this, 1.1, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new CultistHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level && this.hasLineOfSight(target)) {
            level.addFreshEntity(GolemOrb.create(level, this, target, true));
            this.playSound(ModSounds.ELDRITCH_GUARDIAN_ATTACK.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.1F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level && this.tickCount % 20 == 0) {
            for (Cultist cultist : level.getEntitiesOfClass(Cultist.class, this.getBoundingBox().inflate(8.0))) {
                if (!cultist.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)) {
                    cultist.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 60, 1), this);
                }
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Cultist) && !(target instanceof CultistLeader) && super.canAttack(target);
    }

    @Override
    public boolean considersEntityAsAlly(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Cultist || entity instanceof CultistLeader || super.considersEntityAsAlly(entity);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.world.entity.@org.jspecify.annotations.Nullable SpawnGroupData data) {
        var result = super.finalizeSpawn(level, difficulty, reason, data);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PRAETOR_HELM.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PRAETOR_CHESTPLATE.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PRAETOR_GREAVES.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_CULT_BOOTS.get()));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(difficulty.getDifficulty() == Difficulty.EASY ? ModItems.VOID_SWORD.get() : ModItems.CRIMSON_BLADE.get()));
        if (level instanceof ServerLevel serverLevel) {
            float tension = tensionFactor(serverLevel, this.blockPosition());
            if (this.random.nextFloat() < 0.5F * tension) {
                int enchantmentLevel = (int) (7.0F + tension * this.random.nextInt(22));
                ItemStack weapon = this.getMainHandItem();
                var enchantmentRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                Stream<Holder<Enchantment>> allowedEnchantments = Stream.of(
                    enchantmentRegistry.getOrThrow(Enchantments.SHARPNESS),
                    enchantmentRegistry.getOrThrow(Enchantments.SMITE),
                    enchantmentRegistry.getOrThrow(Enchantments.BANE_OF_ARTHROPODS),
                    enchantmentRegistry.getOrThrow(Enchantments.KNOCKBACK),
                    enchantmentRegistry.getOrThrow(Enchantments.FIRE_ASPECT),
                    enchantmentRegistry.getOrThrow(Enchantments.LOOTING),
                    enchantmentRegistry.getOrThrow(Enchantments.UNBREAKING),
                    enchantmentRegistry.getOrThrow(ModEnchantments.REPAIR)
                );
                this.setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(this.random, weapon, enchantmentLevel, allowedEnchantments));
            }
        }
        this.title = this.random.nextInt(TITLES.length);
        return result;
    }

    private static float tensionFactor(ServerLevel level, net.minecraft.core.BlockPos pos) {
        float tension = 0.0F;
        boolean hard = level.getDifficulty() == Difficulty.HARD;
        if (level.hasChunkAt(pos)) {
            tension += Mth.clamp((float) level.getChunkAt(pos).getInhabitedTime() / 3600000.0F, 0.0F, 1.0F) * (hard ? 1.0F : 0.75F);
            tension += level.getMoonBrightness(pos) * 0.25F;
        }
        if (level.getDifficulty() == Difficulty.EASY || level.getDifficulty() == Difficulty.PEACEFUL) {
            tension *= level.getDifficulty().getId() / 2.0F;
        }
        return Mth.clamp(tension, 0.0F, hard ? 1.5F : 1.0F);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        this.spawnAtLocation(level, new ItemStack(ModItems.RARE_LOOT_BAG.get()));
    }

    @Override
    public void generateName() {
        int modifier = thaumcraft.entity.monster.ChampionModifier.get(this);
        if (modifier >= 0) {
            this.setCustomName(net.minecraft.network.chat.Component.translatable(this.getType().getDescriptionId(), this.getTitle(), net.minecraft.network.chat.Component.translatable("champion.mod." + thaumcraft.entity.monster.ChampionModifier.name(modifier))));
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Title", this.title);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.title = Math.floorMod(input.getIntOr("Title", 0), TITLES.length);
    }

    public String getTitle() {
        return TITLES[Math.floorMod(this.title, TITLES.length)];
    }
}
