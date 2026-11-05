package thaumcraft.compat.rei;

import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import thaumcraft.recipeview.ArcaneView;
import thaumcraft.recipeview.RecipeCatalog;

public final class ArcaneDisplay extends BasicDisplay {
    public static final DisplaySerializer<ArcaneDisplay> SERIALIZER = TcRei.byKey(key -> {
        ArcaneView view = RecipeCatalog.arcane(key);
        return view == null ? null : new ArcaneDisplay(view);
    }, display -> display.view.key());

    private final ArcaneView view;

    public ArcaneDisplay(ArcaneView view) {
        super(view.ingredients().stream().map(TcRei::ingredient).toList(), List.of(TcRei.ingredient(view.result())), Optional.empty());
        this.view = view;
    }

    public ArcaneView view() {
        return view;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return TcRei.ARCANE;
    }

    @Override
    public DisplaySerializer<ArcaneDisplay> getSerializer() {
        return SERIALIZER;
    }
}
