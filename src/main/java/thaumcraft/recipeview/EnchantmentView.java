package thaumcraft.recipeview;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import thaumcraft.crafting.InfusionEnchantmentRecipe;

public record EnchantmentView(InfusionEnchantmentRecipe recipe, List<ItemStack> books) {
    public EnchantmentView {
        books = List.copyOf(books);
    }
}
