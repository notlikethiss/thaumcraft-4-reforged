package thaumcraft.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import thaumcraft.entity.TaintedMob;
import thaumcraft.registry.ModEffects;

public final class EffectHooks {
    private EffectHooks() {
    }

    public static void applyVisExhaust(LivingEntity entity, int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(ModEffects.VIS_EXHAUST, duration, amplifier, false, true, true));
    }

    public static void applyTaintPoison(LivingEntity entity, int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON, duration, amplifier, false, true, true));
    }

    public static boolean isTainted(LivingEntity entity) {
        return entity instanceof TaintedMob;
    }
}
