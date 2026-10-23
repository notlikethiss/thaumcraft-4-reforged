package thaumcraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.ClientConfig;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.ResearchTableBlockEntity;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.client.platform.ClientNetwork;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.client.platform.GuiLines;
import thaumcraft.client.research.ResearchHud;
import thaumcraft.client.research.ResearchNotifications;
import thaumcraft.menu.ResearchTableMenu;
import thaumcraft.network.AspectCombinePayload;
import thaumcraft.network.AspectPlacePayload;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.HexGrid;
import thaumcraft.research.Knowledge;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchNote;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/gui/guiresearchtable2.png");
    private static final Identifier HEX = Thaumcraft.id("textures/gui/hex1.png");
    private static final Identifier HEX_HIGHLIGHT = Thaumcraft.id("textures/gui/hex2.png");
    private static final Identifier PARCHMENT = Thaumcraft.id("textures/misc/parchment3.png");
    private static final Identifier SCRIPT = Thaumcraft.id("textures/misc/script.png");
    private static final int HEX_SIZE = 9;
    private static final int GRID_X = 169;
    private static final int GRID_Y = 83;

    private final RandomSource random = RandomSource.create();
    private final Map<HexGrid.Cell, Rune> runes = new HashMap<>();
    private int page;
    private int lastPage;
    private long combineCooldown;
    private long lastRuneCheck;
    private Aspect select1;
    private Aspect select2;
    private Aspect draggedAspect;

    private record Rune(int q, int r, long start, long decay, int rune) {
    }

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 255, 255);
    }

    private PlayerKnowledge knowledge() {
        return Knowledge.get(minecraft.player);
    }

    private AspectList bonus() {
        ResearchTableBlockEntity table = menu.table();
        return table == null ? new AspectList() : table.getBonusAspects();
    }

    private ResearchNote note() {
        ResearchTableBlockEntity table = menu.table();
        return table == null ? null : table.getNote();
    }

    private boolean researcherOne() {
        return ResearchManager.isComplete(minecraft.player, "RESEARCHER1");
    }

    private boolean researcherTwo() {
        return ResearchManager.isComplete(minecraft.player, "RESEARCHER2");
    }

    private boolean canDuplicate(ResearchNote note) {
        return note != null && note.complete() && ResearchManager.isComplete(minecraft.player, "RESEARCHDUPE");
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        double offsetX = mouseX - x;
        double offsetY = mouseY - y;
        return offsetX >= 0 && offsetY >= 0 && offsetX < width && offsetY < height;
    }

    private void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight, int color) {
        GuiBlit.blit(graphics, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight, color);
    }

    private void blitSheet(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int u, int v) {
        blit(graphics, TEXTURE, x, y, width, height, u, v, 256, 256, 0xFFFFFFFF);
    }

    private void play(SoundEvent sound, float volume, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void playClick() {
        play(ModSounds.CAMERACLACK.get(), 0.4F, 1.0F);
    }

    private void playAspect() {
        play(ModSounds.HHOFF.get(), 0.2F, 1.0F + random.nextFloat() * 0.1F);
    }

    private void playCombine() {
        play(ModSounds.HHON.get(), 0.3F, 1.0F);
    }

    private void playWrite() {
        play(ModSounds.WRITE.get(), 0.2F, 1.0F);
    }

    private void playErase() {
        play(ModSounds.ERASE.get(), 0.2F, 1.0F + random.nextFloat() * 0.1F);
    }

    private void playScroll() {
        play(ModSounds.KEY.get(), 0.3F, 1.0F);
    }

    private List<Aspect> visibleAspects() {
        List<Aspect> sorted = knowledge().aspects().getAspectsSorted();
        int from = page * 5;
        int to = Math.min(sorted.size(), from + 25);
        return from >= sorted.size() ? List.of() : sorted.subList(from, to);
    }

    private Aspect aspectAt(double mouseX, double mouseY, boolean ignoreZero) {
        AspectList pool = knowledge().aspects();
        List<Aspect> visible = visibleAspects();
        for (int drawn = 0; drawn < visible.size(); drawn++) {
            Aspect aspect = visible.get(drawn);
            int x = leftPos + 10 + drawn / 5 * 16;
            int y = topPos + 40 + drawn % 5 * 16;
            if ((ignoreZero || pool.getAmount(aspect) > 0 || bonus().getAmount(aspect) > 0) && inside(mouseX, mouseY, x, y, 16, 16)) {
                return aspect;
            }
        }
        return null;
    }

    private void drawOrb(GuiGraphicsExtractor graphics, double x, double y) {
        int count = minecraft.player.tickCount;
        float red = 0.7F + Mth.sin((float) ((count + x) / 10.0)) * 0.15F + 0.15F;
        float green = 0.7F + Mth.sin((float) ((count + x + y) / 11.0)) * 0.15F + 0.15F;
        float blue = 0.7F + Mth.sin((float) ((count + y) / 12.0)) * 0.15F + 0.15F;
        drawOrb(graphics, x, y, ARGB.colorFromFloat(1.0F, Math.min(red, 1.0F), Math.min(green, 1.0F), Math.min(blue, 1.0F)));
    }

    private void drawOrb(GuiGraphicsExtractor graphics, double x, double y, int color) {
        int part = minecraft.player.tickCount % 8;
        if (ClientConfig.colorBlind()) {
            color = ARGB.colorFromFloat(1.0F, ARGB.redFloat(color) / 1.8F, ARGB.greenFloat(color) / 1.8F, ARGB.blueFloat(color) / 1.8F);
        }
        blit(graphics, AspectRenderer.PARTICLES, (int) x, (int) y, 16, 16, 128 + part * 16, 128, 256, 256, ARGB.opaque(color));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        blitSheet(graphics, leftPos, topPos, 255, 167, 0, 0);
        blitSheet(graphics, leftPos + 40, topPos + 167, 184, 88, 0, 166);
        if (page < lastPage) {
            blitSheet(graphics, leftPos + 51, topPos + 121, 24, 8, 208, 208);
        }
        if (page > 0) {
            blitSheet(graphics, leftPos + 27, topPos + 121, 24, 8, 184, 208);
        }
        boolean ready = select1 != null && select2 != null;
        if (ready && combineCooldown < System.nanoTime()) {
            blitSheet(graphics, leftPos + 35, topPos + 139, 32, 16, 184, 184);
            drawOrb(graphics, leftPos + 43, topPos + 139);
        } else if (ready) {
            blitSheet(graphics, leftPos + 35, topPos + 139, 32, 16, 184, 184);
            blitSheet(graphics, leftPos + 35, topPos + 139, 32, 16, 184, 168);
        }
        ResearchNote note = note();
        if (canDuplicate(note)) {
            blitSheet(graphics, leftPos + 37, topPos + 5, 24, 24, 232, 200);
        }
        drawAspects(graphics);
        drawSheet(graphics, mouseX, mouseY, note);
    }

    private void drawAspects(GuiGraphicsExtractor graphics) {
        AspectList pool = knowledge().aspects();
        AspectList bonus = bonus();
        lastPage = (pool.size() - 20) / 5;
        List<Aspect> visible = visibleAspects();
        for (int drawn = 0; drawn < visible.size(); drawn++) {
            Aspect aspect = visible.get(drawn);
            boolean faded = pool.getAmount(aspect) <= 0 && bonus.getAmount(aspect) <= 0;
            AspectRenderer.drawTag(graphics, leftPos + 10 + drawn / 5 * 16, topPos + 40 + drawn % 5 * 16, aspect, pool.getAmount(aspect), bonus.getAmount(aspect), faded ? 0.33F : 1.0F, false);
        }
        if (select1 != null && pool.getAmount(select1) <= 0 && bonus.getAmount(select1) <= 0) {
            select1 = null;
        }
        if (select2 != null && pool.getAmount(select2) <= 0 && bonus.getAmount(select2) <= 0) {
            select2 = null;
        }
        if (select1 != null) {
            AspectRenderer.drawTag(graphics, leftPos + 13, topPos + 139, select1, 0.0F);
        }
        if (select2 != null) {
            AspectRenderer.drawTag(graphics, leftPos + 71, topPos + 139, select2, 0.0F);
        }
    }

    private void drawLine(GuiGraphicsExtractor graphics, double x, double y, double x2, double y2) {
        float alpha = Mth.clamp(0.3F + Mth.sin((float) (minecraft.player.tickCount + x)) * 0.3F + 0.3F, 0.0F, 1.0F);
        int color = ARGB.colorFromFloat(alpha, 0.0F, 0.6F, 0.8F);
        GuiLines.strip(graphics, new float[] {(float) x, (float) x2}, new float[] {(float) y, (float) y2}, new int[] {color, color}, true);
    }

    private void drawRune(GuiGraphicsExtractor graphics, double x, double y, int rune, float alpha) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().rotate((float) -Math.PI / 2.0F);
        GuiBlit.blit(graphics, SCRIPT, -5, -5, 10, 10, rune * 16, 0, 16, 16, 256, 16, ARGB.colorFromFloat(alpha, 0.0F, 0.0F, 0.0F));
        graphics.pose().popMatrix();
    }

    private void drawSheet(GuiGraphicsExtractor graphics, int mouseX, int mouseY, ResearchNote note) {
        if (note == null || note.key().isEmpty()) {
            runes.clear();
            return;
        }
        PlayerKnowledge knowledge = knowledge();
        GuiBlit.blit(graphics, PARCHMENT, leftPos + 94, topPos + 8, 150, 150, 0, 0, 150, 150, 256, 256, 0xFFFFFFFF);
        Map<HexGrid.Cell, ResearchNote.Hex> hexes = HexGrid.index(note.hexes());
        long time = System.currentTimeMillis();
        if (lastRuneCheck < time) {
            lastRuneCheck = time + 250L;
            HexGrid.Cell cell = HexGrid.fromPixel(random.nextInt(120) - 60, random.nextInt(120) - 60, HEX_SIZE);
            if (!runes.containsKey(cell) && !hexes.containsKey(cell)) {
                runes.put(cell, new Rune(cell.q(), cell.r(), time, lastRuneCheck + 15000L + random.nextInt(10000), random.nextInt(16)));
            }
        }
        runes.values().removeIf(rune -> rune.decay() < time);
        for (Rune rune : runes.values()) {
            HexGrid.Cell cell = new HexGrid.Cell(rune.q(), rune.r());
            float progress = (float) (time - rune.start()) / (float) (rune.decay() - rune.start());
            float alpha = 0.5F;
            if (progress < 0.25F) {
                alpha = progress * 2.0F;
            } else if (progress > 0.5F) {
                alpha = 1.0F - progress;
            }
            drawRune(graphics, leftPos + GRID_X + cell.pixelX(HEX_SIZE), topPos + GRID_Y + cell.pixelY(HEX_SIZE), rune.rune(), alpha * 0.66F);
        }
        HexGrid.Cell hovered = HexGrid.fromPixel(mouseX - (leftPos + GRID_X), mouseY - (topPos + GRID_Y), HEX_SIZE);
        HexGrid.Links links = HexGrid.links(note.hexes(), knowledge::hasDiscoveredAspect);
        for (HexGrid.Cell[] line : links.lines()) {
            drawLine(
                graphics,
                leftPos + GRID_X + line[0].pixelX(HEX_SIZE),
                topPos + GRID_Y + line[0].pixelY(HEX_SIZE),
                leftPos + GRID_X + line[1].pixelX(HEX_SIZE),
                topPos + GRID_Y + line[1].pixelY(HEX_SIZE)
            );
        }
        if (!note.complete()) {
            for (Map.Entry<HexGrid.Cell, ResearchNote.Hex> entry : hexes.entrySet()) {
                HexGrid.Cell cell = entry.getKey();
                int centerX = (int) (leftPos + GRID_X + cell.pixelX(HEX_SIZE));
                int centerY = (int) (topPos + GRID_Y + cell.pixelY(HEX_SIZE));
                if (entry.getValue().type() != ResearchNote.Hex.GIVEN) {
                    if (cell.equals(hovered)) {
                        blit(graphics, HEX_HIGHLIGHT, centerX - 8, centerY - 8, 16, 16, 0, 0, 32, 32, 0xB0FFFFFF);
                    }
                    GuiBlit.blit(graphics, HEX, centerX - 8, centerY - 8, 16, 16, 0, 0, 32, 32, 32, 32, 0x40FFFFFF);
                } else {
                    drawOrb(graphics, centerX - 8, centerY - 8);
                }
            }
        }
        for (Map.Entry<HexGrid.Cell, ResearchNote.Hex> entry : hexes.entrySet()) {
            ResearchNote.Hex hex = entry.getValue();
            if (hex.aspect().isEmpty()) {
                continue;
            }
            Aspect aspect = hex.aspect().get();
            int x = (int) (leftPos + 161 + entry.getKey().pixelX(HEX_SIZE));
            int y = (int) (topPos + 75 + entry.getKey().pixelY(HEX_SIZE));
            if (!knowledge.hasDiscoveredAspect(aspect)) {
                GuiBlit.blit(graphics, AspectRenderer.UNKNOWN, x, y, 16, 16, 0, 0, 32, 32, 32, 32, 0x80000000);
            } else if (hex.type() == ResearchNote.Hex.GIVEN || links.highlight().contains(entry.getKey())) {
                AspectRenderer.drawTag(graphics, x, y, aspect, 0.0F, 0, 1.0F, false);
            } else if (hex.type() == ResearchNote.Hex.PLACED) {
                AspectRenderer.drawTag(graphics, x, y, aspect, 0.0F, 0, 0.66F, true);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ResearchHud.renderNotifications(graphics, ResearchNotifications.now());
        ResearchNote note = note();
        if (canDuplicate(note) && inside(mouseX, mouseY, leftPos + 37, topPos + 5, 24, 24)) {
            drawCopyCost(graphics, note);
        }
        drawAspectTooltips(graphics, mouseX, mouseY);
        ResearchTableBlockEntity table = menu.table();
        if (note != null && (table == null || !table.hasInk())) {
            Component first = Component.translatable("tile.researchtable.noink.0");
            Component second = Component.translatable("tile.researchtable.noink.1");
            int half = Math.max(font.width(first), font.width(second)) / 2;
            graphics.setTooltipForNextFrame(font, List.of(first, second), Optional.empty(), leftPos + 157 - half, topPos + 84);
        }
        if (draggedAspect != null) {
            graphics.nextStratum();
            drawOrb(graphics, mouseX - 8, mouseY - 8, draggedAspect.getColor());
        }
    }

    private void drawCopyCost(GuiGraphicsExtractor graphics, ResearchNote note) {
        ResearchItem research = ResearchCategories.getResearch(note.key());
        if (research == null) {
            return;
        }
        graphics.nextStratum();
        blitSheet(graphics, leftPos + 100, topPos + 21, 48, 16, 184, 224);
        AspectList cost = HexGrid.requiredTags(research, note.copies());
        int count = 0;
        for (Aspect aspect : cost.getAspectsSorted()) {
            AspectRenderer.drawTag(graphics, leftPos + 148 + count * 16, topPos + 21, aspect, cost.getAmount(aspect));
            count++;
        }
        graphics.text(font, Component.translatable("tc.research.copy"), leftPos + 100, topPos + 12, 0xFFFFFFFF, true);
    }

    private void drawAspectTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Aspect hovered = null;
        List<Aspect> visible = visibleAspects();
        for (int drawn = 0; drawn < visible.size(); drawn++) {
            if (inside(mouseX, mouseY, leftPos + 10 + drawn / 5 * 16, topPos + 40 + drawn % 5 * 16, 16, 16)) {
                hovered = visible.get(drawn);
                break;
            }
        }
        if (hovered == null && select1 != null && inside(mouseX, mouseY, leftPos + 13, topPos + 139, 16, 16)) {
            hovered = select1;
        }
        if (hovered == null && select2 != null && inside(mouseX, mouseY, leftPos + 71, topPos + 139, 16, 16)) {
            hovered = select2;
        }
        if (hovered == null) {
            return;
        }
        graphics.setTooltipForNextFrame(font, List.of(Component.literal(hovered.getName()), hovered.getDescription()), Optional.empty(), mouseX, mouseY - 8);
        if (researcherOne() && !hovered.isPrimal() && hovered.getComponents() != null) {
            graphics.nextStratum();
            AspectRenderer.drawBack(graphics, mouseX + 6, mouseY + 6);
            AspectRenderer.drawBack(graphics, mouseX + 24, mouseY + 6);
            AspectRenderer.drawTag(graphics, mouseX + 8, mouseY + 8, hovered.getComponents()[0], 0.0F);
            AspectRenderer.drawTag(graphics, mouseX + 26, mouseY + 8, hovered.getComponents()[1], 0.0F);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && handleClick(event)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleClick(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (inside(mouseX, mouseY, leftPos + 10, topPos + 40, 80, 80)) {
            Aspect aspect = aspectAt(mouseX, mouseY, false);
            if (aspect != null) {
                playAspect();
                draggedAspect = aspect;
                return true;
            }
        }
        if (combineCooldown > System.nanoTime()) {
            return false;
        }
        if (inside(mouseX, mouseY, leftPos + 35, topPos + 139, 32, 16) && select1 != null && select2 != null) {
            combineCooldown = System.nanoTime() + 200000000L;
            playClick();
            playCombine();
            sendCombine(select1, select2);
            return true;
        }
        if (page > 0 && inside(mouseX, mouseY, leftPos + 27, topPos + 121, 24, 8)) {
            page--;
            playScroll();
            return true;
        }
        if (page < lastPage && inside(mouseX, mouseY, leftPos + 51, topPos + 121, 24, 8)) {
            page++;
            playScroll();
            return true;
        }
        if (select1 != null && inside(mouseX, mouseY, leftPos + 11, topPos + 137, 16, 16)) {
            select1 = null;
            playAspect();
            return true;
        }
        if (select2 != null && inside(mouseX, mouseY, leftPos + 71, topPos + 137, 16, 16)) {
            select2 = null;
            playAspect();
            return true;
        }
        ResearchNote note = note();
        if (note != null) {
            HexGrid.Cell cell = HexGrid.fromPixel(mouseX - (leftPos + GRID_X), mouseY - (topPos + GRID_Y), HEX_SIZE);
            ResearchNote.Hex hex = HexGrid.index(note.hexes()).get(cell);
            if (hex != null && hex.type() == ResearchNote.Hex.PLACED) {
                playCombine();
                playErase();
                ClientNetwork.sendToServer(new AspectPlacePayload(menu.pos(), cell.q(), cell.r(), Optional.empty()));
                return true;
            }
            if (canDuplicate(note) && inside(mouseX, mouseY, leftPos + 37, topPos + 5, 24, 24)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchTableMenu.BUTTON_DUPLICATE);
                playClick();
                return true;
            }
        }
        if (event.hasShiftDown() && researcherTwo()) {
            Aspect aspect = aspectAt(mouseX, mouseY, true);
            if (aspect != null && !aspect.isPrimal() && aspect.getComponents() != null) {
                AspectList pool = knowledge().aspects();
                AspectList bonus = bonus();
                Aspect first = aspect.getComponents()[0];
                Aspect second = aspect.getComponents()[1];
                if ((pool.getAmount(first) > 0 || bonus.getAmount(first) > 0) && (pool.getAmount(second) > 0 || bonus.getAmount(second) > 0)) {
                    draggedAspect = null;
                    playCombine();
                    sendCombine(first, second);
                    return true;
                }
            }
        }
        return false;
    }

    private void sendCombine(Aspect first, Aspect second) {
        AspectList bonus = bonus();
        ClientNetwork.sendToServer(new AspectCombinePayload(menu.pos(), first, second, bonus.getAmount(first) > 0, bonus.getAmount(second) > 0));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && draggedAspect != null) {
            handleDrop(event.x(), event.y());
            draggedAspect = null;
        }
        return super.mouseReleased(event);
    }

    private void handleDrop(double mouseX, double mouseY) {
        ResearchNote note = note();
        if (note != null) {
            HexGrid.Cell cell = HexGrid.fromPixel(mouseX - (leftPos + GRID_X), mouseY - (topPos + GRID_Y), HEX_SIZE);
            ResearchNote.Hex hex = HexGrid.index(note.hexes()).get(cell);
            if (hex != null && hex.type() == ResearchNote.Hex.EMPTY) {
                playCombine();
                playWrite();
                ClientNetwork.sendToServer(new AspectPlacePayload(menu.pos(), cell.q(), cell.r(), Optional.of(draggedAspect)));
                return;
            }
        }
        double offsetX = mouseX - (leftPos + 20);
        double offsetY = mouseY - (topPos + 146);
        if (offsetX >= -16 && offsetY >= -16 && offsetX < 16 && offsetY < 16) {
            playAspect();
            select1 = draggedAspect;
            return;
        }
        offsetX = mouseX - (leftPos + 79);
        offsetY = mouseY - (topPos + 146);
        if (offsetX >= -16 && offsetY >= -16 && offsetX < 16 && offsetY < 16) {
            playAspect();
            select2 = draggedAspect;
            return;
        }
        if (aspectAt(mouseX, mouseY, false) == draggedAspect) {
            if (select1 == null) {
                select1 = draggedAspect;
            } else if (select2 == null) {
                select2 = draggedAspect;
            }
        }
    }
}
