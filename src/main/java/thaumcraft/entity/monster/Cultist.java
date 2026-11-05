package thaumcraft.entity.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import thaumcraft.entity.boss.CultistLeader;
import thaumcraft.registry.ModItems;

public abstract class Cultist extends Monster {
    protected <T extends Cultist> Cultist(net.minecraft.world.entity.EntityType<? extends T> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createCultistAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof Cultist || entity instanceof CultistLeader || super.isAlliedTo(entity);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Cultist) && !(target instanceof CultistLeader) && super.canAttack(target);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int looting = source.getEntity() instanceof LivingEntity killer
            ? EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),
                killer
            )
            : 0;
        int roll = this.random.nextInt(10);
        if (roll == 0) {
            this.spawnAtLocation(new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()));
        } else if (roll == 1) {
            this.spawnAtLocation(new ItemStack(ModItems.VOID_SEED.get()));
        } else if (roll <= 3 + looting) {
            this.spawnAtLocation(new ItemStack(ModItems.GOLD_COIN.get()));
        }
        if (recentlyHit && this.random.nextInt(200) == 0) {
            this.spawnAtLocation(new ItemStack(ModItems.CRIMSON_RITES.get()));
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueOutput output = thaumcraft.compat.ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        if (this.hasRestriction() && (int) this.getRestrictRadius() > 0) {
            output.putInt("HomeRadius", (int) this.getRestrictRadius());
            output.putInt("HomeX", this.getRestrictCenter().getX());
            output.putInt("HomeY", this.getRestrictCenter().getY());
            output.putInt("HomeZ", this.getRestrictCenter().getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueInput input = thaumcraft.compat.ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        input.getInt("HomeRadius").ifPresent(radius -> this.restrictTo(
            new net.minecraft.core.BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0)),
            radius
        ));
    }
}
