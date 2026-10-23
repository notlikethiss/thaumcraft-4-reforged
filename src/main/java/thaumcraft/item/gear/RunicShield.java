package thaumcraft.item.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Config;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.infusion.InfusionHooks;
import thaumcraft.item.bauble.RunicAmuletItem;
import thaumcraft.item.bauble.RunicGirdleItem;
import thaumcraft.item.bauble.RunicRingItem;
import thaumcraft.network.RunicChargePayload;
import thaumcraft.network.ShieldFxPayload;
import thaumcraft.platform.Baubles;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.WandManager;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class RunicShield {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        private int charge;
        private int lastCharge = -1;
        private long nextCycle;
        private int rechargeDelay;
        private boolean dirty = true;
        private boolean active;
        private int max;
        private int charged;
        private int kinetic;
        private int healing;
        private int emergency;
        private final Map<Integer, Long> cooldowns = new HashMap<>();
    }

    private RunicShield() {
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), key -> new State());
    }

    public static void markDirty(Player player) {
        if (!player.level().isClientSide()) {
            state(player).dirty = true;
        }
    }

    public static int getCharge(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.charge;
    }

    private static void send(ServerPlayer player, int charge, int max) {
        ModNetwork.sendToPlayer(player, new RunicChargePayload(charge, max));
    }

    private static void recalculate(ServerPlayer player, State state) {
        int max = 0;
        int charged = 0;
        int kinetic = 0;
        int healing = 0;
        int emergency = 0;
        state.dirty = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof RunicArmor) {
                max += InfusionHooks.getFinalCharge(stack);
            }
        }
        for (ItemStack stack : Baubles.stacks(player)) {
            if (!(stack.getItem() instanceof RunicArmor)) {
                continue;
            }
            if (stack.getItem() instanceof RunicRingItem ring) {
                if (ring.getVariant() == 2) {
                    charged++;
                } else if (ring.getVariant() == 3) {
                    healing++;
                }
            } else if (stack.getItem() instanceof RunicAmuletItem amulet && amulet.getVariant() == 1) {
                emergency++;
            } else if (stack.getItem() instanceof RunicGirdleItem girdle && girdle.getVariant() == 1) {
                kinetic++;
            }
            max += InfusionHooks.getFinalCharge(stack);
        }
        if (max > 0) {
            state.active = true;
            state.max = max;
            state.charged = charged;
            state.kinetic = kinetic;
            state.healing = healing;
            state.emergency = emergency;
            if (state.charge > max) {
                state.charge = max;
                send(player, max, max);
            }
        } else {
            state.active = false;
            state.charge = 0;
            send(player, 0, 0);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        if (state.dirty || player.tickCount % 40 == 0) {
            recalculate(player, state);
        }
        if (state.rechargeDelay > 0) {
            state.rechargeDelay--;
            return;
        }
        if (!state.active) {
            return;
        }
        long time = System.currentTimeMillis();
        if (state.charge > state.max) {
            state.charge = state.max;
        } else if (state.charge < state.max
            && state.nextCycle < time
            && WandManager.consumeVisFromInventory(player, new AspectList().add(Aspect.AIR, Config.shieldCost()).add(Aspect.EARTH, Config.shieldCost()))) {
            state.nextCycle = time + Config.shieldRecharge() - state.charged * 500L;
            state.charge++;
        }
        if (state.lastCharge != state.charge) {
            send(player, state.charge, state.max);
            state.lastCharge = state.charge;
        }
    }

    private static boolean ignored(DamageSource source) {
        return source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.STARVE);
    }

    private static boolean ready(State state, int key, long time, long cooldown) {
        Long until = state.cooldowns.get(key);
        if (until != null && until >= time) {
            return false;
        }
        state.cooldowns.put(key, time + cooldown);
        return true;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ignored(event.getSource())) {
            return;
        }
        State state = STATES.get(player.getUUID());
        if (state == null || !state.active || state.charge <= 0) {
            return;
        }
        DamageSource source = event.getSource();
        if (player.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN) && event.getAmount() <= player.lastHurt) {
            return;
        }
        int target = source.getEntity() != null ? source.getEntity().getId() : -1;
        if (source.is(DamageTypeTags.IS_FALL)) {
            target = -2;
        }
        if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_STALACTITE)) {
            target = -3;
        }
        ServerLevel level = player.level();
        ModNetwork.sendToNear(level, null, player.getX(), player.getY(), player.getZ(), 64.0, new ShieldFxPayload(player.getId(), target));
        long time = System.currentTimeMillis();
        int charge = state.charge;
        float amount = event.getAmount();
        if (charge > amount) {
            charge = (int) (charge - amount);
            event.setAmount(0.0F);
        } else {
            event.setAmount(amount - charge);
            charge = 0;
        }
        if (charge <= 0 && state.kinetic > 0 && ready(state, 2, time, 20000L)) {
            level.explode(player, player.getX(), player.getY() + player.getBbHeight() / 2.0F, player.getZ(), 1.5F + state.kinetic * 0.5F, false, Level.ExplosionInteraction.NONE);
        }
        if (charge <= 0 && state.healing > 0 && ready(state, 3, time, 20000L)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, state.healing));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RUNIC_SHIELD_EFFECT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (charge <= 0 && state.emergency > 0 && ready(state, 4, time, 60000L)) {
            charge = Math.min(state.max, 8 * state.emergency);
            state.dirty = true;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RUNIC_SHIELD_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (charge <= 0) {
            state.rechargeDelay = Config.shieldWait();
        }
        state.charge = charge;
        send(player, charge, state.max);
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
