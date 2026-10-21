package thaumcraft.client.rei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import io.netty.buffer.ByteBuf;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.AspectList;

final class TcReiDisplay extends BasicDisplay {
    static final CategoryIdentifier<TcReiDisplay> CRUCIBLE = CategoryIdentifier.of(Thaumcraft.id("crucible"));
    static final CategoryIdentifier<TcReiDisplay> ARCANE = CategoryIdentifier.of(Thaumcraft.id("arcane"));
    static final CategoryIdentifier<TcReiDisplay> INFUSION = CategoryIdentifier.of(Thaumcraft.id("infusion"));
    static final CategoryIdentifier<TcReiDisplay> ENCHANTMENT = CategoryIdentifier.of(Thaumcraft.id("infusion_enchantment"));
    private static final Codec<Kind> KIND_CODEC = Codec.STRING.xmap(Kind::valueOf, Kind::name);
    private static final StreamCodec<ByteBuf, Kind> KIND_STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Kind::valueOf, Kind::name);

    static final DisplaySerializer<TcReiDisplay> SERIALIZER = DisplaySerializer.of(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(TcReiDisplay::getInputEntries),
            EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(TcReiDisplay::getOutputEntries),
            KIND_CODEC.fieldOf("kind").forGetter(TcReiDisplay::kind),
            AspectList.CODEC.fieldOf("aspects").forGetter(TcReiDisplay::aspects),
            Codec.INT.fieldOf("instability").forGetter(TcReiDisplay::instability),
            Codec.INT.fieldOf("grid_width").forGetter(TcReiDisplay::gridWidth),
            Codec.INT.fieldOf("grid_height").forGetter(TcReiDisplay::gridHeight)
        ).apply(instance, TcReiDisplay::decoded)),
        StreamCodec.composite(
            EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
            TcReiDisplay::getInputEntries,
            EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
            TcReiDisplay::getOutputEntries,
            KIND_STREAM_CODEC,
            TcReiDisplay::kind,
            AspectList.STREAM_CODEC,
            TcReiDisplay::aspects,
            ByteBufCodecs.VAR_INT,
            TcReiDisplay::instability,
            ByteBufCodecs.VAR_INT,
            TcReiDisplay::gridWidth,
            ByteBufCodecs.VAR_INT,
            TcReiDisplay::gridHeight,
            TcReiDisplay::decoded
        )
    );

    enum Kind {
        CRUCIBLE,
        ARCANE,
        INFUSION,
        ENCHANTMENT
    }

    private final CategoryIdentifier<TcReiDisplay> category;
    private final Kind kind;
    private final AspectList aspects;
    private final int instability;
    private final int gridWidth;
    private final int gridHeight;

    TcReiDisplay(
        CategoryIdentifier<TcReiDisplay> category,
        Kind kind,
        List<EntryIngredient> inputs,
        List<EntryIngredient> outputs,
        AspectList aspects,
        int instability,
        int gridWidth,
        int gridHeight
    ) {
        super(inputs, outputs);
        this.category = category;
        this.kind = kind;
        this.aspects = aspects.copy();
        this.instability = instability;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }

    Kind kind() {
        return kind;
    }

    AspectList aspects() {
        return aspects;
    }

    int instability() {
        return instability;
    }

    int gridWidth() {
        return gridWidth;
    }

    int gridHeight() {
        return gridHeight;
    }

    private static TcReiDisplay decoded(
        List<EntryIngredient> inputs,
        List<EntryIngredient> outputs,
        Kind kind,
        AspectList aspects,
        int instability,
        int gridWidth,
        int gridHeight
    ) {
        CategoryIdentifier<TcReiDisplay> category = switch (kind) {
            case CRUCIBLE -> CRUCIBLE;
            case ARCANE -> ARCANE;
            case INFUSION -> INFUSION;
            case ENCHANTMENT -> ENCHANTMENT;
        };
        return new TcReiDisplay(category, kind, inputs, outputs, aspects, instability, gridWidth, gridHeight);
    }

    static EntryIngredient ingredient(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return EntryIngredient.empty();
        }
        EntryIngredient.Builder builder = EntryIngredient.builder(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                builder.add(EntryStacks.of(stack));
            }
        }
        return builder.build();
    }

    static EntryIngredient ingredient(ItemStack stack) {
        return stack.isEmpty() ? EntryIngredient.empty() : EntryIngredient.of(EntryStacks.of(stack));
    }

    static EntryIngredient ingredientChoices(Collection<ItemStack> stacks) {
        return ingredient(new ArrayList<>(stacks));
    }
}
