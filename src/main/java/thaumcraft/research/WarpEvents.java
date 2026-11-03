package thaumcraft.research;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Config;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.effect.WarpEffectEvents;
import thaumcraft.item.armor.FortressArmorItem;
import thaumcraft.network.WarpEventPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModEffects;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class WarpEvents {
    private WarpEvents() {
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount > 0 && player.tickCount % 2000 == 0) {
            checkWarpEvent(player);
        }
        if (player.tickCount % 10 == 0 && player.hasEffect(ModEffects.DEATH_GAZE)) {
            checkDeathGaze(player);
        }
    }

    public static void checkWarpEvent(ServerPlayer player) {
        checkWarpEvent(player, player.level().getRandom());
    }

    static void checkWarpEvent(ServerPlayer player, RandomSource random) {
        if (shouldSkipWarpEvents(player, Config.wuss())) {
            return;
        }
        ServerLevel level = player.level();
        PlayerKnowledge knowledge = Knowledge.get(player);
        int actualWarp = Warp.get(player, WarpType.PERMANENT) + Warp.get(player, WarpType.STICKY);
        int warp = Math.max(0, Warp.total(player) + Warp.getWarpFromGear(player));
        int warpCounter = knowledge.getWarpCounter();

        if (warpCounter > 0 && warp > 0 && random.nextInt(100) <= Math.sqrt(warpCounter)) {
            int eventWarp = Math.min(100, (warp + warp + warpCounter) / 3);
            warpCounter = (int) (warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0));
            knowledge.setWarpCounter(warpCounter);
            Knowledge.sync(player);
            int effect = random.nextInt(eventWarp);
            if (FortressArmorItem.getMask(player) == 0) {
                effect -= 2 + random.nextInt(4);
            }
            ModNetwork.sendToPlayer(player, new WarpEventPayload(0));
            applyWarpEvent(player, eventWarp, effect);
            unlockWarpResearch(player, actualWarp);
        }

        if (Warp.get(player, WarpType.TEMPORARY) > 0) {
            Warp.set(player, Warp.get(player, WarpType.TEMPORARY) - 1, WarpType.TEMPORARY);
        }
    }

    static boolean shouldSkipWarpEvents(ServerPlayer player, boolean wussMode) {
        return wussMode || player.hasEffect(ModEffects.WARP_WARD);
    }

    static int scaledWarp(int warp, int counter) {
        return Math.min(100, (warp + warp + counter) / 3);
    }

    private static void applyWarpEvent(ServerPlayer player, int warp, int effect) {
        if (effect <= 0) {
            return;
        }
        int amplifier = Math.min(3, warp / 15);
        if (effect <= 4) {
            grantAspects(player, 1);
            whisper(player, 3);
        } else if (effect <= 8) {
            return;
        } else if (effect <= 12) {
            whisper(player, 11);
        } else if (effect <= 16) {
            applyEffect(player, ModEffects.VIS_EXHAUST, 5000, amplifier);
            whisper(player, 1);
        } else if (effect <= 20) {
            applyEffect(player, ModEffects.THAUMARHIA, Math.min(32000, 10 * warp), 0);
            whisper(player, 15);
        } else if (effect <= 24) {
            applyEffect(player, ModEffects.UNNATURAL_HUNGER, 5000, amplifier);
            whisper(player, 2);
        } else if (effect <= 28) {
            whisper(player, 12);
        } else if (effect <= 32) {
            spawnMist(player, 1);
        } else if (effect <= 36) {
            applyEffect(player, ModEffects.BLURRED_VISION, Math.min(32000, 10 * warp), 0);
        } else if (effect <= 40) {
            applyEffect(player, ModEffects.SUN_SCORNED, 5000, amplifier);
            whisper(player, 5);
        } else if (effect <= 44) {
            applyEffect(player, MobEffects.MINING_FATIGUE, 1200, amplifier);
            whisper(player, 9);
        } else if (effect <= 48) {
            applyEffect(player, ModEffects.INFECTIOUS_VIS_EXHAUST, 6000, amplifier);
            whisper(player, 1);
        } else if (effect <= 52) {
            applyEffect(player, MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0);
            whisper(player, 10);
        } else if (effect <= 56) {
            applyEffect(player, ModEffects.DEATH_GAZE, 6000, amplifier);
            whisper(player, 4);
        } else if (effect <= 60) {
            WarpEncounterHooks.spawnMindSpiders(player.level(), player, true, Math.min(50, warp));
            whisper(player, 7);
        } else if (effect <= 64) {
            whisper(player, 13);
        } else if (effect <= 68) {
            spawnMist(player, Math.min(8, warp / 30));
        } else if (effect <= 72) {
            applyEffect(player, MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0);
        } else if (effect == 76) {
            if (Warp.get(player, WarpType.STICKY) > 0) {
                Warp.add(player, -1, WarpType.STICKY);
            }
            whisper(player, 14);
        } else if (effect <= 80) {
            applyEffect(player, ModEffects.UNNATURAL_HUNGER, 6000, amplifier);
            whisper(player, 2);
        } else if (effect <= 84) {
            grantAspects(player, warp / 10);
            whisper(player, 3);
        } else if (effect <= 88) {
            return;
        } else if (effect <= 92) {
            WarpEncounterHooks.spawnMindSpiders(player.level(), player, false, Math.min(50, warp));
            whisper(player, 7);
        } else {
            spawnMist(player, Math.min(8, warp / 15));
        }
    }

    private static void spawnMist(ServerPlayer player, int guardians) {
        ModNetwork.sendToPlayer(player, new WarpEventPayload(1));
        if (guardians > 0) {
            WarpEncounterHooks.spawnGuardians(player.level(), player, guardians);
        }
        whisper(player, 6);
    }

    private static void grantAspects(ServerPlayer player, int times) {
        if (times <= 0) {
            return;
        }
        int amount = 1 + player.getRandom().nextInt(times);
        List<Aspect> primals = Aspect.getPrimalAspects();
        for (int index = 0; index < amount; index++) {
            ResearchManager.addAspectPool(player, primals.get(player.getRandom().nextInt(primals.size())), 1);
        }
    }

    private static void unlockWarpResearch(ServerPlayer player, int actualWarp) {
        if (actualWarp > 10 && !ResearchManager.isComplete(player, "BATHSALTS") && !ResearchManager.isComplete(player, "@BATHSALTS")) {
            whisper(player, 8);
            ResearchManager.complete(player, "@BATHSALTS");
        }
        if (actualWarp > 25 && !ResearchManager.isComplete(player, "ELDRITCHMINOR")) {
            grantAspects(player, 10);
            ResearchManager.complete(player, "ELDRITCHMINOR");
        }
        if (actualWarp > 50 && !ResearchManager.isComplete(player, "ELDRITCHMAJOR")) {
            grantAspects(player, 20);
            ResearchManager.complete(player, "ELDRITCHMAJOR");
        }
    }

    private static void whisper(ServerPlayer player, int message) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("warp.text." + message).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private static void applyEffect(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        boolean ambient = !effect.is(ModEffects.INFECTIOUS_VIS_EXHAUST);
        MobEffectInstance instance = new MobEffectInstance(effect, duration, amplifier, ambient, true, true);
        if (effect.is(ModEffects.VIS_EXHAUST)
            || effect.is(ModEffects.THAUMARHIA)
            || effect.is(ModEffects.SUN_SCORNED)
            || effect.is(ModEffects.INFECTIOUS_VIS_EXHAUST)
            || effect.is(ModEffects.DEATH_GAZE)
            || effect.is(ModEffects.UNNATURAL_HUNGER)) {
            WarpEffectEvents.addUncurableEffect(entity, instance);
        } else {
            entity.addEffect(instance);
        }
    }

    private static void checkDeathGaze(ServerPlayer player) {
        int amplifier = player.getEffect(ModEffects.DEATH_GAZE).getAmplifier();
        int range = Math.min(8 + amplifier * 3, 24);
        AABB area = player.getBoundingBox().inflate(range);
        Vec3 look = player.getViewVector(1.0F).normalize();
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive() && entity.isPickable())) {
            if (living.hasEffect(MobEffects.WITHER) || !player.hasLineOfSight(living)) {
                continue;
            }
            if (living instanceof Player && !player.level().isPvpAllowed()) {
                continue;
            }
            Vec3 target = new Vec3(living.getX(), living.getBoundingBox().minY + living.getBbHeight() / 2.0, living.getZ());
            if (!isWithinDeathGazeCone(look, player.getEyePosition(), target, range)) {
                continue;
            }
            living.setLastHurtByPlayer(player, 100);
            if (living instanceof Mob mob) {
                mob.setTarget(player);
            }
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
        }
    }

    static boolean isWithinDeathGazeCone(Vec3 look, Vec3 origin, Vec3 target, double range) {
        Vec3 direction = target.subtract(origin);
        double distance = direction.length();
        return distance > 0.0
            && look.dot(direction) < range
            && look.dot(direction.scale(1.0 / distance)) > Math.cos(0.75 / 2.0);
    }
}
