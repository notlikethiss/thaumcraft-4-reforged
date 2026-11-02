package thaumcraft.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;
import thaumcraft.registry.ModItems;

public class BrainyZombie extends Zombie {
    public BrainyZombie(EntityType<? extends BrainyZombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
            .add(Attributes.MAX_HEALTH, 25.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 5.0);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason spawnReason,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
        return result;
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        return level.getDifficulty() != Difficulty.PEACEFUL && allowedBiome(level.getBiome(this.blockPosition())) && super.checkSpawnRules(level, spawnReason);
    }

    public static boolean checkBrainyZombieSpawnRules(
        EntityType<? extends Monster> type,
        ServerLevelAccessor level,
        EntitySpawnReason spawnReason,
        BlockPos pos,
        RandomSource random
    ) {
        return allowedBiome(level.getBiome(pos)) && Monster.checkMonsterSpawnRules(type, level, spawnReason, pos, random);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        for (int index = 0; index < 3; index++) {
            if (this.random.nextBoolean()) {
                this.spawnAtLocation(level, new ItemStack(Items.ROTTEN_FLESH));
            }
        }
        int looting = 0;
        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity killer) {
            looting = EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),
                killer
            );
        }
        if (this.random.nextInt(10) - looting <= 4) {
            this.spawnAtLocation(level, new ItemStack(ModItems.ZOMBIE_BRAIN.get()), 1.5F);
        }
    }

    private static boolean allowedBiome(Holder<Biome> biome) {
        return !biome.is(BiomeTags.IS_NETHER) && !biome.is(Biomes.MUSHROOM_FIELDS);
    }
}
