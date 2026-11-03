package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import javax.annotation.Nullable;
import thaumcraft.registry.ModEntities;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;

public final class TaintMobFactory {
    private TaintMobFactory() {
    }

    public static @Nullable Entity createReplacement(LivingEntity original) {
        EntityType<? extends Entity> replacementType;
        if (original instanceof Chicken) {
            replacementType = ModEntities.TAINTED_CHICKEN.get();
        } else if (original instanceof Cow) {
            replacementType = ModEntities.TAINTED_COW.get();
        } else if (original instanceof Creeper) {
            replacementType = ModEntities.TAINTED_CREEPER.get();
        } else if (original instanceof Pig) {
            replacementType = ModEntities.TAINTED_PIG.get();
        } else if (original instanceof Sheep) {
            replacementType = ModEntities.TAINTED_SHEEP.get();
        } else if (original instanceof Villager) {
            replacementType = ModEntities.TAINTED_VILLAGER.get();
        } else {
            return null;
        }
        Entity replacement = replacementType.create(original.level());
        if (replacement == null) {
            return null;
        }
        replacement.absMoveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), 0.0F);
        replacement.setDeltaMovement(original.getDeltaMovement());
        replacement.setCustomName(original.getCustomName());
        replacement.setCustomNameVisible(original.isCustomNameVisible());
        return replacement;
    }

}
