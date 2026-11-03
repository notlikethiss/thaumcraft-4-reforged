package thaumcraft.wand;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import thaumcraft.item.InteractionResults;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.wand.focus.Architect;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;

public class WandItem extends Item implements Architect {
    private static final Pattern PLACEHOLDER = Pattern.compile("%%?(CAP|ROD|OBJ)");
    private static final String DEFAULT_TEMPLATE = "%CAP %ROD %OBJ";
    private static final ItemAttributeModifiers STAFF_MODIFIERS = ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        .build();

    public WandItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return WandKeys.PARTS.apply(properties.stacksTo(1).rarity(Rarity.UNCOMMON), WandParts.DEFAULT);
    }

    public float getConsumptionModifier(ItemStack stack, @Nullable Player player, Aspect aspect, boolean crafting) {
        float modifier = Wands.getCap(stack).getCostModifier(aspect);
        if (Wands.getFocus(stack) != null && !crafting) {
            modifier -= Wands.getFocusFrugal(stack) / 10.0F;
        }
        if (player != null) {
            modifier -= WandManager.getTotalVisDiscount(player, aspect);
        }
        if (Wands.isSceptre(stack)) {
            modifier -= 0.1F;
        }
        return Math.max(modifier, 0.1F);
    }

    public boolean consumeVis(ItemStack stack, Player player, Aspect aspect, int amount, boolean crafting) {
        int cost = (int)(amount * getConsumptionModifier(stack, player, aspect, crafting));
        int current = Wands.getVis(stack, aspect);
        if (current >= cost) {
            Wands.storeVis(stack, aspect, current - cost);
            return true;
        }
        return false;
    }

    public boolean consumeAllVisCrafting(ItemStack stack, Player player, AspectList aspects, boolean doit) {
        if (aspects == null || aspects.size() == 0) {
            return false;
        }
        AspectList scaled = new AspectList();
        for (Aspect aspect : aspects.getAspects()) {
            scaled.add(aspect, aspects.getAmount(aspect) * 100);
        }
        return consumeAllVis(stack, player, scaled, doit, true);
    }

    public boolean consumeAllVis(ItemStack stack, Player player, AspectList aspects, boolean doit, boolean crafting) {
        if (aspects == null || aspects.size() == 0) {
            return false;
        }
        AspectList cost = new AspectList();
        for (Aspect aspect : aspects.getAspects()) {
            cost.add(aspect, (int)(aspects.getAmount(aspect) * getConsumptionModifier(stack, player, aspect, crafting)));
        }
        for (Aspect aspect : cost.getAspects()) {
            if (Wands.getVis(stack, aspect) < cost.getAmount(aspect)) {
                return false;
            }
        }
        if (doit && !player.level().isClientSide()) {
            for (Aspect aspect : cost.getAspects()) {
                Wands.storeVis(stack, aspect, Wands.getVis(stack, aspect) - cost.getAmount(aspect));
            }
        }
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        String template = Language.getInstance().getOrDefault("item.thaumcraft.wand", DEFAULT_TEMPLATE);
        if (!PLACEHOLDER.matcher(template).find()) {
            template = DEFAULT_TEMPLATE;
        }
        MutableComponent name = Component.empty();
        Matcher matcher = PLACEHOLDER.matcher(template);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                name.append(Component.literal(template.substring(last, matcher.start())));
            }
            name.append(partName(stack, matcher.group(1)));
            last = matcher.end();
        }
        if (last < template.length()) {
            name.append(Component.literal(template.substring(last)));
        }
        return name;
    }

    private static Component partName(ItemStack stack, String placeholder) {
        return switch (placeholder) {
            case "CAP" -> Component.translatable("item.Wand." + Wands.getCap(stack).tag() + ".cap");
            case "ROD" -> Component.translatable("item.Wand." + Wands.getRod(stack).textureTag() + ".rod");
            default -> Component.translatable(
                Wands.isStaff(stack) ? "item.Wand.staff.obj" : Wands.isSceptre(stack) ? "item.Wand.sceptre.obj" : "item.Wand.wand.obj"
            );
        };
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return Wands.isStaff(stack) ? STAFF_MODIFIERS : ItemAttributeModifiers.EMPTY;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        if (!level.isClientSide() && owner instanceof Player player) {
            RodUpdate update = Wands.getRod(stack).onUpdate();
            if (update != null) {
                update.onUpdate(stack, player);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WandInteractable interactable) {
            InteractionResult result = interactable.onWandUseOn(level, pos, player, stack, context.getClickedFace());
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        if (level.getBlockEntity(pos) instanceof WandInteractable interactable) {
            InteractionResult result = interactable.onWandUseOn(level, pos, player, stack, context.getClickedFace());
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        if (WandTriggers.hasTrigger(state)) {
            return WandTriggers.perform(level, stack, player, pos, context.getClickedFace()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResults.holder(handleUse(level, player, hand), player.getItemInHand(hand), level);
    }

    private InteractionResult handleUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        boolean blockHit = hit.getType() == HitResult.Type.BLOCK;
        if (blockHit) {
            BlockPos pos = hit.getBlockPos();
            InteractionResult result = InteractionResult.PASS;
            if (level.getBlockState(pos).getBlock() instanceof WandInteractable interactable) {
                result = interactable.onWandUse(level, pos, player, stack);
            }
            if (result == InteractionResult.PASS && level.getBlockEntity(pos) instanceof WandInteractable interactable) {
                result = interactable.onWandUse(level, pos, player, stack);
            }
            if (result != InteractionResult.PASS) {
                if (WandKeys.TARGET.has(stack)) {
                    player.startUsingItem(hand);
                    return InteractionResult.CONSUME;
                }
                return result;
            }
        }
        FocusItem focus = Wands.getFocus(stack);
        if (focus != null && !WandManager.isOnCooldown(player)) {
            WandManager.setCooldown(player, focus.getActivationCooldown(Wands.getFocusItem(stack)));
            InteractionResult result = focus.onFocusRightClick(stack, level, player, blockHit ? hit : null);
            if (result == InteractionResult.CONSUME) {
                player.startUsingItem(hand);
            }
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        WandInteractable target = getObjectInUse(stack, level);
        if (target != null) {
            target.onUsingWandTick(stack, player, ticksRemaining);
            return;
        }
        FocusItem focus = Wands.getFocus(stack);
        if (WandKeys.TARGET.has(stack) || focus == null) {
            WandKeys.TARGET.remove(stack);
            entity.stopUsingItem();
        } else if (!WandManager.isOnCooldown(player)) {
            WandManager.setCooldown(player, focus.getActivationCooldown(Wands.getFocusItem(stack)));
            focus.onUsingFocusTick(stack, player, ticksRemaining);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (entity instanceof Player player) {
            WandInteractable target = getObjectInUse(stack, level);
            if (target != null) {
                target.onWandStoppedUsing(stack, level, player, remainingTime);
            } else {
                FocusItem focus = Wands.getFocus(stack);
                if (focus != null) {
                    focus.onPlayerStoppedUsingFocus(stack, level, player, remainingTime);
                }
            }
        }
        WandKeys.TARGET.remove(stack);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        FocusItem focus = Wands.getFocus(stack);
        if (focus != null && !WandManager.isOnCooldown(entity)) {
            WandManager.setCooldown(entity, focus.getActivationCooldown(Wands.getFocusItem(stack)));
            return focus.onEntitySwing(entity, stack);
        }
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        ItemStack focusStack = Wands.getFocusItem(stack);
        return focusStack.isEmpty() ? super.getDestroySpeed(stack, state) : focusStack.getItem().getDestroySpeed(focusStack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        ItemStack focusStack = Wands.getFocusItem(stack);
        return !focusStack.isEmpty() && focusStack.getItem().isCorrectToolForDrops(focusStack, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> builder, TooltipFlag flag) {
        ItemStack focusStack = Wands.getFocusItem(stack);
        if (!focusStack.isEmpty()) {
            builder.add(focusStack.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
        }
    }

    @Override
    public boolean showAxis(ItemStack stack, Level level, Player player, Direction side, Direction.Axis axis) {
        FocusItem focus = Wands.getFocus(stack);
        return focus instanceof Architect architect
            && focus.isUpgradedWith(Wands.getFocusItem(stack), FocusUpgradeType.ARCHITECT)
            && architect.showAxis(stack, level, player, side, axis);
    }

    @Override
    public List<BlockPos> getArchitectBlocks(ItemStack stack, Level level, BlockPos pos, Direction side, Player player) {
        FocusItem focus = Wands.getFocus(stack);
        if (focus instanceof Architect architect && focus.isUpgradedWith(Wands.getFocusItem(stack), FocusUpgradeType.ARCHITECT)) {
            return architect.getArchitectBlocks(stack, level, pos, side, player);
        }
        return List.of();
    }

    private static @Nullable WandInteractable getObjectInUse(ItemStack stack, Level level) {
        BlockPos pos = WandKeys.TARGET.get(stack);
        if (pos == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof WandInteractable interactable ? interactable : null;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }
}
