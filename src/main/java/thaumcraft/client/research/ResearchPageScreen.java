package thaumcraft.client.research;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;

public class ResearchPageScreen extends Screen {
    private static final Deque<HistoryEntry> HISTORY = new ArrayDeque<>();
    private static final int PANE_WIDTH = 256;
    private static final int PANE_HEIGHT = 181;

    private final ResearchItem research;
    private final List<DisplayPage> pages;
    private final PageRenderer renderer;
    private int page;
    private long lastCycle;

    private record DisplayPage(ResearchPage source, List<BookText.Line> lines) {
    }

    private record HistoryEntry(String research, int page) {
    }

    public ResearchPageScreen(ResearchItem research, int page) {
        super(Component.empty());
        this.research = research;
        Minecraft minecraft = Minecraft.getInstance();
        this.renderer = new PageRenderer(minecraft, research);
        this.pages = buildPages(minecraft, research);
        this.page = Math.max(0, page - page % 2);
    }

    public static void open(ResearchItem item) {
        HISTORY.clear();
        Minecraft.getInstance().setScreen(new ResearchPageScreen(item, 0));
    }

    private static List<DisplayPage> buildPages(Minecraft minecraft, ResearchItem research) {
        List<ResearchPage> sources = new ArrayList<>();
        for (ResearchPage candidate : research.pages()) {
            if (!(candidate instanceof ResearchPage.ConcealedText concealed) || ResearchManager.isComplete(minecraft.player, concealed.research())) {
                sources.add(candidate);
            }
        }
        if (research.key().equals("ASPECTS")) {
            AspectList known = Knowledge.get(minecraft.player).aspects();
            AspectList group = new AspectList();
            int count = 0;
            for (Aspect aspect : known.getAspectsSorted()) {
                group.add(aspect, known.getAmount(aspect));
                if (++count == 4) {
                    sources.add(ResearchPage.aspects(group));
                    group = new AspectList();
                    count = 0;
                }
            }
            if (count > 0) {
                sources.add(ResearchPage.aspects(group));
            }
        }
        List<DisplayPage> pages = new ArrayList<>();
        for (ResearchPage source : sources) {
            String key = switch (source) {
                case ResearchPage.Text text -> text.text();
                case ResearchPage.ConcealedText text -> text.text();
                default -> null;
            };
            if (key == null) {
                pages.add(new DisplayPage(source, List.of()));
                continue;
            }
            String raw = PageRenderer.lang(key);
            List<BookText.Line> lines = BookText.layout(minecraft.font, raw.isEmpty() ? key : raw, PageRenderer.TEXT_WIDTH);
            int height = PANE_HEIGHT + 10;
            int firstHeight = pages.isEmpty() ? height - 25 : height;
            for (List<BookText.Line> textPage : BookText.paginate(lines, firstHeight, height)) {
                pages.add(new DisplayPage(source, textPage));
            }
        }
        return pages;
    }

    private static int pageIndex(ResearchItem target, ResearchPage page) {
        List<DisplayPage> built = buildPages(Minecraft.getInstance(), target);
        for (int index = 0; index < built.size(); index++) {
            if (built.get(index).source().equals(page)) {
                return index;
            }
        }
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() || minecraft.options.keyInventory.matches(event)) {
            HISTORY.clear();
            ResearchBrowserScreen.open();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderer.begin(graphics, mouseX, mouseY);
        renderer.resetFrame();
        int left = (width - PANE_WIDTH) / 2;
        int top = (height - PANE_HEIGHT) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate((width - PANE_WIDTH * 1.3F) / 2.0F, (height - PANE_HEIGHT * 1.3F) / 2.0F);
        graphics.pose().scale(1.3F, 1.3F);
        GuiBlit.blit(graphics, BookText.BOOK, 0, 0, PANE_WIDTH, PANE_HEIGHT, 0, 0, PANE_WIDTH, PANE_HEIGHT, 256, 256, 0xFFFFFFFF);
        graphics.pose().popMatrix();
        if (lastCycle < System.currentTimeMillis()) {
            renderer.cycle++;
            lastCycle = System.currentTimeMillis() + 1000L;
        }
        for (int index = page; index <= page + 1 && index < pages.size(); index++) {
            drawPage(graphics, pages.get(index), index % 2, left, top);
        }
        renderer.flushTooltip();
        float bob = Mth.sin(minecraft.player.tickCount / 3.0F) * 0.2F + 0.1F;
        if (!HISTORY.isEmpty()) {
            drawScaled(graphics, left + 118, top + 189, 38, 202, 20, 12, bob);
            if (renderer.hovering(left + 118, top + 189, 20, 12)) {
                graphics.text(font, Component.translatable("recipe.return"), mouseX, mouseY, 0xFFFFFFFF, true);
            }
        }
        if (page > 0) {
            drawScaled(graphics, left - 16, top + 190, 0, 184, 12, 8, bob);
        }
        if (page < pages.size() - 2) {
            drawScaled(graphics, left + 262, top + 190, 12, 184, 12, 8, bob);
        }
    }

    private void drawScaled(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width / 2.0F, y + height / 2.0F);
        graphics.pose().scale(1.0F + scale, 1.0F + scale);
        GuiBlit.blit(graphics, BookText.BOOK, -width / 2, -height / 2, width, height, u, v, width, height, 256, 256, 0xFFFFFFFF);
        graphics.pose().popMatrix();
    }

    private void drawPage(GuiGraphicsExtractor graphics, DisplayPage display, int side, int x, int y) {
        ResearchPage node = display.source();
        if (page == 0 && side == 0) {
            GuiBlit.blit(graphics, BookText.BOOK, x + 4, y - 13, 96, 4, 24, 184, 96, 4, 256, 256, 0xFFFFFFFF);
            GuiBlit.blit(graphics, BookText.BOOK, x + 4, y + 4, 96, 4, 24, 184, 96, 4, 256, 256, 0xFFFFFFFF);
            Component name = BookText.uniform(Component.translatable(research.nameKey()).getString());
            int nameWidth = font.width(name);
            if (nameWidth <= 130) {
                graphics.text(font, name, x + 52 - nameWidth / 2, y - 6, 0xFF303030, false);
            } else {
                float scale = 130.0F / nameWidth;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + 52 - nameWidth / 2 * scale, y - 6.0F * scale);
                graphics.pose().scale(scale, scale);
                graphics.text(font, name, 0, 0, 0xFF303030, false);
                graphics.pose().popMatrix();
            }
            y += 25;
        }
        switch (node) {
            case ResearchPage.Text text -> renderer.text(side, x, y - 10, display.lines());
            case ResearchPage.ConcealedText text -> renderer.text(side, x, y - 10, display.lines());
            case ResearchPage.Aspects aspects -> renderer.aspects(side, x - 8, y - 8, aspects.aspects());
            case ResearchPage.Crucible crucible -> PageRecipes.crucible(renderer, side, x - 4, y - 8, crucible);
            case ResearchPage.Crafting crafting -> PageRecipes.crafting(renderer, side, x - 4, y - 8, crafting);
            case ResearchPage.Arcane arcane -> PageRecipes.arcane(renderer, side, x - 4, y - 8, arcane);
            case ResearchPage.Compound compound -> PageRecipes.compound(renderer, side, x - 4, y - 8, compound);
            case ResearchPage.Infusion infusion -> PageRecipes.infusion(renderer, side, x - 4, y - 8, infusion);
            case ResearchPage.InfusionEnchantment enchantment -> PageRecipes.infusionEnchantment(renderer, side, x - 4, y - 8, enchantment);
            case ResearchPage.Smelting smelting -> PageRecipes.smelting(renderer, side, x - 4, y - 8, smelting);
            case ResearchPage.Image image -> {
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int left = (width - PANE_WIDTH) / 2;
        int top = (height - PANE_HEIGHT) / 2;
        if (page < pages.size() - 2 && inside(mouseX, mouseY, left + 261, top + 189, 14, 10)) {
            turn(2);
            return true;
        }
        if (page >= 2 && inside(mouseX, mouseY, left - 17, top + 189, 14, 10)) {
            turn(-2);
            return true;
        }
        if (!HISTORY.isEmpty() && inside(mouseX, mouseY, left + 118, top + 189, 20, 12)) {
            playPage();
            HistoryEntry entry = HISTORY.pop();
            ResearchItem previous = ResearchCategories.getResearch(entry.research());
            if (previous == null) {
                ResearchBrowserScreen.open();
            } else {
                minecraft.setScreen(new ResearchPageScreen(previous, entry.page()));
            }
            return true;
        }
        for (PageRenderer.Hit hit : renderer.hits) {
            if (inside(mouseX, mouseY, hit.x(), hit.y(), 16, 16)) {
                playPage();
                HISTORY.push(new HistoryEntry(research.key(), page));
                ResearchItem target = hit.reference().research();
                minecraft.setScreen(new ResearchPageScreen(target, pageIndex(target, hit.reference().page())));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        int dx = mouseX - x;
        int dy = mouseY - y;
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }

    private void turn(int delta) {
        page += delta;
        lastCycle = 0L;
        renderer.cycle = -1;
        playPage();
    }

    private void playPage() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 1.0F, 0.66F));
    }
}
