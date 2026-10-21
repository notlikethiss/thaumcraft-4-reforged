package thaumcraft.client.rei;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;

final class TcReiCategory implements DisplayCategory<TcReiDisplay> {
    private final CategoryIdentifier<TcReiDisplay> id;
    private final Component title;
    private final ItemLike icon;
    private final TcReiDisplay.Kind kind;

    TcReiCategory(CategoryIdentifier<TcReiDisplay> id, String titleKey, ItemLike icon, TcReiDisplay.Kind kind) {
        this.id = id;
        this.title = Component.translatable(titleKey);
        this.icon = icon;
        this.kind = kind;
    }

    @Override
    public CategoryIdentifier<? extends TcReiDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }

    @Override
    public int getDisplayWidth(TcReiDisplay display) {
        return display.kind() == TcReiDisplay.Kind.CRUCIBLE ? 164 : 166;
    }

    @Override
    public int getDisplayHeight() {
        return switch (kind) {
            case CRUCIBLE -> 96;
            case ARCANE -> 116;
            case INFUSION, ENCHANTMENT -> 168;
        };
    }

    @Override
    public List<Widget> setupDisplay(TcReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        switch (kind) {
            case CRUCIBLE -> crucible(widgets, display, bounds);
            case ARCANE -> arcane(widgets, display, bounds);
            case INFUSION, ENCHANTMENT -> infusion(widgets, display, bounds);
        }
        addAspectCosts(widgets, display, bounds);
        return widgets;
    }

    private static void crucible(List<Widget> widgets, TcReiDisplay display, Rectangle bounds) {
        addInput(widgets, bounds.x + 32, bounds.y + 6, display, 0);
        widgets.add(Widgets.createArrow(new Point(bounds.x + 67, bounds.y + 7)));
        addOutput(widgets, bounds.x + 112, bounds.y + 6, display, 0);
    }

    private static void arcane(List<Widget> widgets, TcReiDisplay display, Rectangle bounds) {
        int offsetX = (3 - display.gridWidth()) * 9;
        int offsetY = (3 - display.gridHeight()) * 9;
        for (int index = 0; index < display.getInputEntries().size() && index < 9; index++) {
            int column = index % display.gridWidth();
            int row = index / display.gridWidth();
            addInput(widgets, bounds.x + 4 + offsetX + column * 18, bounds.y + 5 + offsetY + row * 18, display, index);
        }
        widgets.add(Widgets.createArrow(new Point(bounds.x + 75, bounds.y + 23)));
        addOutput(widgets, bounds.x + 127, bounds.y + 23, display, 0);
    }

    private static void infusion(List<Widget> widgets, TcReiDisplay display, Rectangle bounds) {
        List<EntryIngredient> inputs = display.getInputEntries();
        if (inputs.isEmpty()) {
            return;
        }
        int centerX = bounds.x + 71;
        int centerY = bounds.y + 52;
        addSlot(widgets, centerX - 9, centerY - 9, inputs.get(0), true);
        int components = inputs.size() - 1;
        for (int index = 0; index < components; index++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * index / Math.max(components, 1);
            int x = centerX + (int) (Math.cos(angle) * 43) - 9;
            int y = centerY + (int) (Math.sin(angle) * 43) - 9;
            addSlot(widgets, x, y, inputs.get(index + 1), true);
        }
        addOutput(widgets, bounds.x + 140, bounds.y + 7, display, 0);
    }

    private static void addInput(List<Widget> widgets, int x, int y, TcReiDisplay display, int index) {
        List<EntryIngredient> inputs = display.getInputEntries();
        if (index < inputs.size()) {
            addSlot(widgets, x, y, inputs.get(index), true);
        }
    }

    private static void addOutput(List<Widget> widgets, int x, int y, TcReiDisplay display, int index) {
        List<EntryIngredient> outputs = display.getOutputEntries();
        if (index < outputs.size()) {
            widgets.add(Widgets.createResultSlotBackground(new Point(x, y)));
            addSlot(widgets, x, y, outputs.get(index), false);
        }
    }

    private static void addSlot(List<Widget> widgets, int x, int y, EntryIngredient ingredient, boolean input) {
        var slot = Widgets.createSlot(new Point(x, y)).entries(ingredient);
        if (input) {
            slot.markInput();
        } else {
            slot.disableBackground().markOutput();
        }
        widgets.add(slot);
    }

    private static void addAspectCosts(List<Widget> widgets, TcReiDisplay display, Rectangle bounds) {
        List<Aspect> aspects = display.aspects().getAspectsSorted();
        int perRow = 5;
        int cellSize = 18;
        int iconY = switch (display.kind()) {
            case CRUCIBLE -> bounds.y + 30;
            case ARCANE -> bounds.y + 61;
            case INFUSION, ENCHANTMENT -> bounds.y + 103;
        };
        for (int index = 0; index < aspects.size(); index++) {
            int row = index / perRow;
            int column = index % perRow;
            int columnsInRow = Math.min(perRow, aspects.size() - row * perRow);
            int rowWidth = columnsInRow * cellSize;
            int x = bounds.getCenterX() - rowWidth / 2 + column * cellSize;
            int y = iconY + row * cellSize;
            Aspect aspect = aspects.get(index);
            int amount = display.aspects().getAmount(aspect);
            widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> AspectRenderer.drawTag(graphics, x, y, aspect, amount)));
        }
        if (display.kind() == TcReiDisplay.Kind.INFUSION || display.kind() == TcReiDisplay.Kind.ENCHANTMENT) {
            int level = Math.min(5, display.instability() / 2);
            Component instability = Component.translatable("tc.inst").append(Component.translatable("tc.inst." + level));
            widgets.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.y + 155), instability).noShadow().color(0xFF404040, 0xFFBBBBBB));
        }
    }
}
