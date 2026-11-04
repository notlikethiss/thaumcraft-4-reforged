package thaumcraft.compat.rei;

import java.util.List;
import java.util.function.Function;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;

public final class TcRei {
    public static final CategoryIdentifier<CrucibleDisplay> CRUCIBLE = CategoryIdentifier.of(Thaumcraft.id("crucible"));
    public static final CategoryIdentifier<ArcaneDisplay> ARCANE = CategoryIdentifier.of(Thaumcraft.id("arcane"));
    public static final CategoryIdentifier<InfusionDisplay> INFUSION = CategoryIdentifier.of(Thaumcraft.id("infusion"));
    public static final CategoryIdentifier<EnchantmentDisplay> INFUSION_ENCHANTMENT = CategoryIdentifier.of(Thaumcraft.id("infusion_enchantment"));

    private TcRei() {
    }

    static EntryIngredient ingredient(TcIngredient ingredient) {
        return stacks(ingredient.displayStacks());
    }

    static EntryIngredient ingredient(TcStack stack) {
        return ingredient(stack.create());
    }

    static EntryIngredient ingredient(ItemStack stack) {
        return stack.isEmpty() ? EntryIngredient.empty() : EntryIngredients.of(stack);
    }

    static EntryIngredient stacks(List<ItemStack> stacks) {
        return stacks.isEmpty() ? EntryIngredient.empty() : EntryIngredients.ofItemStacks(stacks);
    }

    static <D extends Display> DisplaySerializer<D> byKey(Function<String, D> decode, Function<D, String> encode) {
        return new DisplaySerializer<>() {
            @Override
            public CompoundTag save(CompoundTag tag, D display) {
                tag.putString("key", encode.apply(display));
                return tag;
            }

            @Override
            public D read(CompoundTag tag) {
                String key = tag.getString("key");
                D display = decode.apply(key);
                if (display == null) {
                    throw new IllegalArgumentException("Unknown Thaumcraft recipe " + key);
                }
                return display;
            }
        };
    }
}
