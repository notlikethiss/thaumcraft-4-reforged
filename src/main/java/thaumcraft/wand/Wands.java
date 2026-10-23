package thaumcraft.wand;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;

public final class Wands {
    private Wands() {
    }

    public static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get());
    }

    public static ItemStack create(WandCap cap, WandRod rod, boolean sceptre) {
        ItemStack stack = new ItemStack(ModItems.WAND.get());
        WandKeys.PARTS.set(stack, new WandParts(cap.tag(), rod.tag(), sceptre));
        return stack;
    }

    public static WandParts getParts(ItemStack stack) {
        return WandKeys.PARTS.getOrDefault(stack, WandParts.DEFAULT);
    }

    public static WandCap getCap(ItemStack stack) {
        return getParts(stack).getCap();
    }

    public static WandRod getRod(ItemStack stack) {
        return getParts(stack).getRod();
    }

    public static boolean isSceptre(ItemStack stack) {
        return getParts(stack).sceptre();
    }

    public static boolean isStaff(ItemStack stack) {
        return getRod(stack).staff();
    }

    public static boolean hasRunes(ItemStack stack) {
        return getRod(stack).runes();
    }

    public static int getMaxVis(ItemStack stack) {
        return getRod(stack).capacity() * (isSceptre(stack) ? 150 : 100);
    }

    public static WandVis getAllVisData(ItemStack stack) {
        return WandKeys.VIS.getOrDefault(stack, WandVis.EMPTY);
    }

    public static AspectList getAllVis(ItemStack stack) {
        return getAllVisData(stack).toAspectList();
    }

    public static AspectList getAspectsWithRoom(ItemStack stack) {
        AspectList result = new AspectList();
        int max = getMaxVis(stack);
        WandVis vis = getAllVisData(stack);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            if (vis.get(aspect) < max) {
                result.add(aspect, 1);
            }
        }
        return result;
    }

    public static int getVis(ItemStack stack, Aspect aspect) {
        return getAllVisData(stack).get(aspect);
    }

    public static void storeVis(ItemStack stack, Aspect aspect, int amount) {
        WandKeys.VIS.set(stack, getAllVisData(stack).with(aspect, amount));
    }

    public static void storeAllVis(ItemStack stack, AspectList list) {
        WandVis vis = getAllVisData(stack);
        for (Aspect aspect : list.getAspects()) {
            vis = vis.with(aspect, list.getAmount(aspect));
        }
        WandKeys.VIS.set(stack, vis);
    }

    public static int addVis(ItemStack stack, Aspect aspect, int amount, boolean doit) {
        return addRealVis(stack, aspect, amount * 100, doit) / 100;
    }

    public static int addRealVis(ItemStack stack, Aspect aspect, int amount, boolean doit) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int max = getMaxVis(stack);
        int store = getVis(stack, aspect) + amount;
        int leftover = Math.max(store - max, 0);
        if (doit) {
            storeVis(stack, aspect, Math.min(store, max));
        }
        return leftover;
    }

    public static void fill(ItemStack stack) {
        int max = getMaxVis(stack);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            storeVis(stack, aspect, max);
        }
    }

    public static @Nullable FocusItem getFocus(ItemStack stack) {
        ItemStackTemplate focus = WandKeys.FOCUS.get(stack);
        return focus != null && focus.item().value() instanceof FocusItem item ? item : null;
    }

    public static ItemStack getFocusItem(ItemStack stack) {
        ItemStackTemplate focus = WandKeys.FOCUS.get(stack);
        return focus == null ? ItemStack.EMPTY : focus.create();
    }

    public static void setFocus(ItemStack stack, ItemStack focus) {
        if (focus.isEmpty()) {
            WandKeys.FOCUS.remove(stack);
        } else {
            WandKeys.FOCUS.set(stack, ItemStackTemplate.fromNonEmptyStack(focus.copyWithCount(1)));
        }
    }

    public static int getFocusUpgrade(ItemStack stack, FocusUpgradeType type) {
        FocusItem focus = getFocus(stack);
        return focus == null ? 0 : focus.getUpgradeLevel(getFocusItem(stack), type);
    }

    public static int getFocusPotency(ItemStack stack) {
        return getFocus(stack) == null ? 0 : getFocusUpgrade(stack, FocusUpgradeType.POTENCY) + (hasRunes(stack) ? 1 : 0);
    }

    public static int getFocusTreasure(ItemStack stack) {
        return getFocusUpgrade(stack, FocusUpgradeType.TREASURE);
    }

    public static int getFocusFrugal(ItemStack stack) {
        return getFocusUpgrade(stack, FocusUpgradeType.FRUGAL);
    }

    public static int getFocusEnlarge(ItemStack stack) {
        return getFocusUpgrade(stack, FocusUpgradeType.ENLARGE);
    }

    public static int getFocusExtend(ItemStack stack) {
        return getFocusUpgrade(stack, FocusUpgradeType.EXTEND);
    }
}
