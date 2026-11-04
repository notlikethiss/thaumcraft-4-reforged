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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.Thaumcraft;
import thaumcraft.client.recipeview.AspectGrid;
import thaumcraft.client.recipeview.InfusionLayout;
import thaumcraft.recipeview.EnchantmentView;
import thaumcraft.registry.ModBlocks;

final class EnchantmentCategory implements IRecipeCategory<EnchantmentView> {
    static final IRecipeType<EnchantmentView> TYPE = IRecipeType.create(Thaumcraft.id("infusion_enchantment"), EnchantmentView.class);

    private final IDrawable icon;

    EnchantmentCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(ModBlocks.RUNIC_MATRIX.get());
    }

    @Override
    public IRecipeType<EnchantmentView> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("recipe.type.infusionenchantment");
    }

    @Override
    public int getWidth() {
        return InfusionLayout.WIDTH;
    }

    @Override
    public int getHeight() {
        return InfusionLayout.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnchantmentView entry, IFocusGroup focuses) {
        builder.addInputSlot(InfusionLayout.CENTER_X - 8, InfusionLayout.CENTER_Y - 8).setStandardSlotBackground().add(new ItemStack(Items.BOOK));
        TcJei.components(builder, entry.recipe().components());
        builder.addOutputSlot(InfusionLayout.OUTPUT_X, InfusionLayout.OUTPUT_Y).setOutputSlotBackground().addItemStacks(entry.books());
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION).add(ModBlocks.RUNIC_MATRIX.get());
    }

    @Override
    public void draw(EnchantmentView entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        AspectGrid.draw(graphics, entry.recipe().aspects(), InfusionLayout.PER_ROW, InfusionLayout.WIDTH, 0, InfusionLayout.ASPECTS_Y);
        InfusionLayout.instability(graphics, entry.recipe().instability(), 0, 0);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, EnchantmentView entry, IRecipeSlotsView slots, double mouseX, double mouseY) {
        TcJei.aspectTooltip(tooltip, entry.recipe().aspects(), InfusionLayout.PER_ROW, InfusionLayout.WIDTH, InfusionLayout.ASPECTS_Y, mouseX, mouseY);
    }
}
