package thaumcraft.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;
import thaumcraft.item.armor.RobeColor;

public abstract class ArmorDyeRecipe extends CustomRecipe {
    private static final float[][] FLEECE_COLORS = {
        {1.0F, 1.0F, 1.0F},
        {0.85F, 0.5F, 0.2F},
        {0.7F, 0.3F, 0.85F},
        {0.4F, 0.6F, 0.85F},
        {0.9F, 0.9F, 0.2F},
        {0.5F, 0.8F, 0.1F},
        {0.95F, 0.5F, 0.65F},
        {0.3F, 0.3F, 0.3F},
        {0.6F, 0.6F, 0.6F},
        {0.3F, 0.5F, 0.6F},
        {0.5F, 0.25F, 0.7F},
        {0.2F, 0.3F, 0.7F},
        {0.4F, 0.3F, 0.2F},
        {0.4F, 0.5F, 0.2F},
        {0.6F, 0.2F, 0.2F},
        {0.1F, 0.1F, 0.1F}
    };

    protected ArmorDyeRecipe() {
        super(CraftingBookCategory.EQUIPMENT);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    protected abstract boolean isTarget(ItemStack stack);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasTarget = false;
        boolean hasDye = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ArmorItem) {
                if (!isTarget(stack) || hasTarget) {
                    return false;
                }
                hasTarget = true;
            } else if (stack.getItem() instanceof DyeItem) {
                hasDye = true;
            } else {
                return false;
            }
        }
        return hasTarget && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack target = ItemStack.EMPTY;
        List<DyeColor> dyes = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ArmorItem) {
                if (!isTarget(stack) || !target.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                target = stack;
            } else if (stack.getItem() instanceof DyeItem) {
                dyes.add(((DyeItem) stack.getItem()).getDyeColor());
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (target.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int color = RobeColor.get(target);
        int red = FastColor.ARGB32.red(color);
        int green = FastColor.ARGB32.green(color);
        int blue = FastColor.ARGB32.blue(color);
        int intensity = Math.max(red, Math.max(green, blue));
        int count = 1;
        for (DyeColor dye : dyes) {
            float[] fleece = FLEECE_COLORS[dye.getId()];
            int dyeRed = (int) (fleece[0] * 255.0F);
            int dyeGreen = (int) (fleece[1] * 255.0F);
            int dyeBlue = (int) (fleece[2] * 255.0F);
            intensity += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
            red += dyeRed;
            green += dyeGreen;
            blue += dyeBlue;
            count++;
        }
        red /= count;
        green /= count;
        blue /= count;
        float averageIntensity = (float) intensity / count;
        float resultIntensity = Math.max(red, Math.max(green, blue));
        red = (int) (red * averageIntensity / resultIntensity);
        green = (int) (green * averageIntensity / resultIntensity);
        blue = (int) (blue * averageIntensity / resultIntensity);
        ItemStack result = target.copyWithCount(1);
        RobeColor.set(result, (red << 16) + (green << 8) + blue);
        return result;
    }
}
