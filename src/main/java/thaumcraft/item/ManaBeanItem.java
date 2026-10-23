package thaumcraft.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.block.ManaPodBlock;
import thaumcraft.blockentity.ManaPodBlockEntity;
import thaumcraft.platform.Registration;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchManager;

public class ManaBeanItem extends Item {
    private static final List<Holder<MobEffect>> POTION_EFFECTS = List.of(
        MobEffects.SPEED,
        MobEffects.SLOWNESS,
        MobEffects.HASTE,
        MobEffects.MINING_FATIGUE,
        MobEffects.STRENGTH,
        MobEffects.INSTANT_HEALTH,
        MobEffects.INSTANT_DAMAGE,
        MobEffects.JUMP_BOOST,
        MobEffects.NAUSEA,
        MobEffects.REGENERATION,
        MobEffects.RESISTANCE,
        MobEffects.FIRE_RESISTANCE,
        MobEffects.WATER_BREATHING,
        MobEffects.INVISIBILITY,
        MobEffects.BLINDNESS,
        MobEffects.NIGHT_VISION,
        MobEffects.HUNGER,
        MobEffects.WEAKNESS,
        MobEffects.POISON,
        MobEffects.WITHER,
        MobEffects.HEALTH_BOOST,
        MobEffects.ABSORPTION,
        MobEffects.SATURATION
    );
    private static final int POTION_SLOTS = 32;

    public ManaBeanItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return Registration.food(properties, 1, 0.5F, true, 0.5F);
    }

    public static ItemStack withAspect(Aspect aspect) {
        ItemStack stack = new ItemStack(ModBlocks.MANA_BEAN.get());
        AspectKeys.ASPECTS.set(stack, new AspectList().add(aspect, 1));
        return stack;
    }

    public static @Nullable Aspect getAspect(ItemStack stack) {
        AspectList list = AspectKeys.ASPECTS.get(stack);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getAspects().get(0);
    }

    public static void awardAspectPool(ServerPlayer player, Aspect aspect, int amount) {
        ResearchManager.addAspectPool(player, aspect, amount);
    }

    private static Aspect randomAspect(RandomSource random) {
        Aspect[] aspects = Aspect.values();
        return aspects[random.nextInt(aspects.length)];
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Aspect aspect = getAspect(stack);
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            int slot = level.getRandom().nextInt(POTION_SLOTS);
            if (slot >= 1 && slot <= POTION_EFFECTS.size()) {
                Holder<MobEffect> effect = POTION_EFFECTS.get(slot - 1);
                if (effect.value().isInstantenous()) {
                    effect.value().applyInstantenousEffect(serverLevel, player, player, player, 2, 3.0);
                } else {
                    player.addEffect(new MobEffectInstance(effect, 160 + level.getRandom().nextInt(80), 0));
                }
            }
            if (level.getRandom().nextFloat() < 0.25F && aspect != null) {
                awardAspectPool(player, aspect, 1);
            }
        }
        return result;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (getAspect(stack) == null) {
            AspectKeys.ASPECTS.set(stack, new AspectList().add(randomAspect(level.getRandom()), 1));
        }
        super.inventoryTick(stack, level, owner, slot);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        if (getAspect(stack) == null) {
            AspectKeys.ASPECTS.set(stack, new AspectList().add(randomAspect(player.getRandom()), 1));
        }
        super.onCraftedBy(stack, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        AspectList list = AspectKeys.ASPECTS.get(stack);
        if (list != null) {
            for (Aspect aspect : list.getAspectsSorted()) {
                builder.accept(Component.empty().append(aspect.getDescription()).append(" x" + list.getAmount(aspect)));
            }
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos logPos = context.getClickedPos();
        if (context.getClickedFace() != Direction.DOWN || !ManaPodBlock.canHangFrom(level, logPos)) {
            return InteractionResult.PASS;
        }
        if (player != null && !player.mayUseItemAt(logPos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        BlockPos podPos = logPos.below();
        if (!level.isEmptyBlock(podPos)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            BlockState pod = ModBlocks.MANA_POD.get().defaultBlockState();
            level.setBlock(podPos, pod, Block.UPDATE_CLIENTS);
            Aspect aspect = getAspect(context.getItemInHand());
            if (aspect != null && level.getBlockEntity(podPos) instanceof ManaPodBlockEntity entity) {
                entity.setAspect(aspect);
            }
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
