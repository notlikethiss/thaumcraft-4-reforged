package thaumcraft.compat.rei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.function.Function;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
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

    static <D extends Display> DisplaySerializer<D> byKey(Function<String, @Nullable D> decode, Function<D, String> encode) {
        MapCodec<D> codec = Codec.STRING.fieldOf("key").flatXmap(key -> {
            D display = decode.apply(key);
            return display == null ? DataResult.error(() -> "Unknown Thaumcraft recipe " + key) : DataResult.success(display);
        }, display -> DataResult.success(encode.apply(display)));
        StreamCodec<RegistryFriendlyByteBuf, D> streamCodec = ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast().map(key -> {
            D display = decode.apply(key);
            if (display == null) {
                throw new DecoderException("Unknown Thaumcraft recipe " + key);
            }
            return display;
        }, encode);
        return DisplaySerializer.of(codec, streamCodec);
    }
}
