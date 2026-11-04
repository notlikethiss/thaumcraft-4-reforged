package thaumcraft.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import thaumcraft.Thaumcraft;
import thaumcraft.client.recipeview.AspectGrid;
import thaumcraft.recipeview.ArcaneView;
import thaumcraft.registry.ModBlocks;

final class ArcaneCategory implements IRecipeCategory<ArcaneView> {
    static final RecipeType<ArcaneView> TYPE = new RecipeType<>(Thaumcraft.id("arcane"), ArcaneView.class);
    private static final int WIDTH = 130;
    private static final int PER_ROW = 7;
    private static final int COST_Y = 60;

    private final IGuiHelper helper;
    private final IDrawable icon;

    ArcaneCategory(IGuiHelper helper) {
        this.helper = helper;
        this.icon = helper.createDrawableItemLike(ModBlocks.ARCANE_WORKTABLE.get());
    }

    @Override
    public RecipeType<ArcaneView> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("recipe.type.arcane");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return COST_Y + 20;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcaneView recipe, IFocusGroup focuses) {
        for (int index = 0; index < recipe.ingredients().size() && index < 9; index++) {
            TcJei.accept(builder.addInputSlot(index % recipe.width() * 18, index / recipe.width() * 18).setStandardSlotBackground(), recipe.ingredients().get(index));
        }
        if (recipe.shapeless()) {
            builder.setShapeless();
        }
        builder.addOutputSlot(104, 18).setOutputSlotBackground().addItemStack(recipe.result());
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemLike(ModBlocks.ARCANE_WORKTABLE.get());
    }

    @Override
    public void draw(ArcaneView recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        helper.getRecipeArrow().draw(graphics, 70, 19);
        AspectGrid.draw(graphics, recipe.aspects(), PER_ROW, WIDTH, 0, COST_Y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ArcaneView recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        TcJei.aspectTooltip(tooltip, recipe.aspects(), PER_ROW, WIDTH, COST_Y, mouseX, mouseY);
    }
}
