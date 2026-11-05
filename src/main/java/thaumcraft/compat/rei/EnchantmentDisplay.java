package thaumcraft.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.recipeview.EnchantmentView;
import thaumcraft.recipeview.RecipeCatalog;

public final class EnchantmentDisplay extends BasicDisplay {
    public static final DisplaySerializer<EnchantmentDisplay> SERIALIZER = TcRei.byKey(key -> {
        EnchantmentView view = RecipeCatalog.enchantment(key, BasicDisplay.registryAccess());
        return view == null ? null : new EnchantmentDisplay(view);
    }, display -> display.view.recipe().key());

    private final EnchantmentView view;

    public EnchantmentDisplay(EnchantmentView view) {
        super(inputs(view), List.of(TcRei.stacks(view.books())), Optional.empty());
        this.view = view;
    }

    private static List<EntryIngredient> inputs(EnchantmentView view) {
        List<EntryIngredient> inputs = new ArrayList<>();
        inputs.add(TcRei.ingredient(new ItemStack(Items.BOOK)));
        view.recipe().components().forEach(component -> inputs.add(TcRei.ingredient(component)));
        return inputs;
    }

    public EnchantmentView view() {
        return view;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return TcRei.INFUSION_ENCHANTMENT;
    }

    @Override
    public DisplaySerializer<EnchantmentDisplay> getSerializer() {
        return SERIALIZER;
    }
}
