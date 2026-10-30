package thaumcraft.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import thaumcraft.Thaumcraft;
import thaumcraft.client.recipeview.AspectGrid;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.registry.ModBlocks;

final class CrucibleCategory implements IRecipeCategory<CrucibleRecipe> {
    static final IRecipeType<CrucibleRecipe> TYPE = IRecipeType.create(Thaumcraft.id("crucible"), CrucibleRecipe.class);
    private static final int WIDTH = 150;
    private static final int PER_ROW = 8;
    private static final int ASPECTS_Y = 30;

    private final IGuiHelper helper;
    private final IDrawable icon;

    CrucibleCategory(IGuiHelper helper) {
        this.helper = helper;
        this.icon = helper.createDrawableItemLike(ModBlocks.CRUCIBLE.get());
    }

    @Override
    public IRecipeType<CrucibleRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("recipe.type.crucible");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return ASPECTS_Y + 18 * 2 + 2;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleRecipe recipe, IFocusGroup focuses) {
        TcJei.accept(builder.addInputSlot(30, 4).setStandardSlotBackground(), recipe.catalyst());
        TcJei.accept(builder.addOutputSlot(100, 4).setOutputSlotBackground(), recipe.result());
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION).add(ModBlocks.CRUCIBLE.get());
    }

    @Override
    public void draw(CrucibleRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        helper.getRecipeArrow().draw(graphics, 62, 5);
        AspectGrid.draw(graphics, recipe.aspects(), PER_ROW, WIDTH, 0, ASPECTS_Y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, CrucibleRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        TcJei.aspectTooltip(tooltip, recipe.aspects(), PER_ROW, WIDTH, ASPECTS_Y, mouseX, mouseY);
    }
}
