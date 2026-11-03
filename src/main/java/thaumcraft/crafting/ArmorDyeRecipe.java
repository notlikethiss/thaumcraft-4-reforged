package thaumcraft.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
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
            if (stack.has(DataComponents.EQUIPPABLE)) {
                if (!isTarget(stack) || hasTarget) {
                    return false;
                }
                hasTarget = true;
            } else if (stack.has(DataComponents.DYE)) {
                hasDye = true;
            } else {
                return false;
            }
        }
        return hasTarget && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        List<DyeColor> dyes = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.has(DataComponents.EQUIPPABLE)) {
                if (!isTarget(stack) || !target.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                target = stack;
            } else if (stack.has(DataComponents.DYE)) {
                dyes.add(stack.get(DataComponents.DYE));
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (target.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int color = RobeColor.get(target);
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
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
