package thaumcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import thaumcraft.client.platform.GuiBlit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.menu.GolemMenu;

public final class GolemScreen extends AbstractContainerScreen<GolemMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/guigolem.png");
    private static final int TEXT = 0xFFDDDDDD;
    private static final int LABEL = 0xFFFDFDFD;
    private static final int[] COLORS = {
        0xFFF0F0F0, 0xFFEB8844, 0xFFC354CD, 0xFF6689D3, 0xFFDECF2A, 0xFF41CD34, 0xFFD88198, 0xFF434343,
        0xFFA0A0A0, 0xFF287697, 0xFF7B2FBE, 0xFF253192, 0xFF51301A, 0xFF3B511A, 0xFFB3312C, 0xFF1E1B1B
    };
    private static final String[] COLOR_NAMES = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final int[] TARGET_TOGGLES = {1, 2, 3, 4};
    private static final String[] TARGET_NAMES = {"Monsters", "Animals", "Players", "Creepers"};
    private final GolemBase golem;
    private final int threat;

    public GolemScreen(GolemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        golem = menu.golem();
        threat = golem != null && golem.isAdvanced() && inventory.player.getRandom().nextInt(4) == 0
            ? inventory.player.getRandom().nextInt(9)
            : -1;
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        GuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }

    private void text(GuiGraphics graphics, String value, int x, int y, int color) {
        graphics.drawString(font, Component.literal(value), leftPos + x, topPos + y, color, true);
    }

    private void scaled(GuiGraphics graphics, String value, int x, int y, int color, float scale) {
        Component component = Component.literal(value);
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + x, topPos + y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, component, 0, 0, color, true);
        graphics.pose().popPose();
    }

    private void swatch(GuiGraphics graphics, int x, int y, int color) {
        if (color >= 0 && color < COLORS.length) {
            GuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + y, 0, 176, 6, 6, 256, 256, FastColor.ARGB32.opaque(COLORS[color]));
        }
    }

    private boolean toggled(int index) {
        return golem != null && golem.getToggles()[index];
    }

    private void toggle(GuiGraphics graphics, int x, int y, int index) {
        blit(graphics, x, y, 8, toggled(index) ? 176 : 168, 8, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(-leftPos, -topPos, 0);
        if (golem == null) {
            graphics.pose().popPose();
            return;
        }
        int core = golem.getCore();
        String key = threat >= 0 ? "golemthreat." + threat + ".text" : "golemblurb." + Math.max(0, core) + ".text";
        graphics.drawWordWrap(font, Component.translatable(key), leftPos + 80, topPos + 22, 110, TEXT);
        if (menu.maxScroll() > 0) {
            scaled(graphics, (menu.currentScroll() + 1) + "/" + (menu.maxScroll() + 1), 161, 70, TEXT, 0.5F);
        }
        int count = Math.min(6, menu.filterSlots());
        for (int index = 0; index < count; index++) {
            int x = 96 + index / 2 * 28;
            int y = 4 + index % 2 * 31;
            if (inside(mouseX - leftPos, mouseY - topPos, x, y, 24, 12)) {
                int color = golem.getColors(index + menu.currentScroll() * 6);
                Component name = Component.translatable(color < 0 ? "gui.thaumcraft.any_color" : "color.minecraft." + COLOR_NAMES[color]);
                graphics.drawString(font, name, leftPos + 133 - font.width(name) / 2, topPos - 6, LABEL, true);
            }
        }
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

        blit(graphics, 0, 0, 0, 0, imageWidth, imageHeight);
        if (golem == null) {
            return;
        }
        int core = golem.getCore();
        int slots = menu.filterSlots() == 0 ? 0 : golem.inventory().getContainerSize();
        if (slots > 0) {
            for (int index = 0; index < Math.min(6, slots); index++) {
                int slot = index + menu.currentScroll() * 6;
                blit(graphics, 96 + index / 2 * 28, 12 + index % 2 * 31, 184, golem.getGolemType().ordinal() * 24, 24, 24);
                if (golem.getUpgradeAmount(4) > 0) {
                    blit(graphics, 96 + index / 2 * 28, 4 + index % 2 * 31, 72, 168, 24, 12);
                    swatch(graphics, 105 + index / 2 * 28, 7 + index % 2 * 31, golem.getColors(slot));
                }
            }
            if (slots > 6) {
                blit(graphics, 111, 68, 0, menu.currentScroll() > 0 ? 200 : 208, 24, 8);
                blit(graphics, 135, 68, 24, menu.currentScroll() < menu.maxScroll() ? 200 : 208, 24, 8);
            }
        }
        if (core == 4 && golem.getUpgradeAmount(4) > 0) {
            for (int index = 0; index < TARGET_TOGGLES.length; index++) {
                blit(graphics, 104, 5 + index * 16, 8, toggled(TARGET_TOGGLES[index]) ? 168 : 176, 8, 8);
                text(graphics, TARGET_NAMES[index], 122, 6 + index * 16, 0xFFFFCCCC);
            }
        }
        if (core == 0) {
            toggle(graphics, 62, 54, 0);
            scaled(graphics, toggled(0) ? "Any amount" : "Precise amount", 66, 48, LABEL, 0.5F);
        }
        if (core == 8) {
            String[] names = {"Block", "Right click", "Not sneaking"};
            String[] active = {"Empty space", "Left click", "Sneaking"};
            for (int index = 0; index < 3; index++) {
                toggle(graphics, 42, 40 + index * 10, index);
                scaled(graphics, toggled(index) ? active[index] : names[index], 53, 42 + index * 10, LABEL, 0.5F);
            }
        }
        if (golem.getUpgradeAmount(5) > 0 && (core == 0 || core == 1 || core == 2 || core == 8 || core == 10)) {
            int x = core == 10 ? 66 : 180;
            int y = core == 10 ? 12 : 0;
            for (int index = 0; index < 3; index++) {
                toggle(graphics, x, 24 + y + index * 10, 5 + index);
                String[] options = {"Use Ore dictionary", "Ignore item damage", "Ignore NBT values"};
                scaled(graphics, options[index], x + 10, 26 + y + index * 10, toggled(5 + index) ? LABEL : 0xFF666666, 0.5F);
            }
        }
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 26, topPos + 8, leftPos + 76, topPos + 78, 45, 0.0625F, mouseX, mouseY, golem);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }

    private boolean sendButton(int button) {
        if (minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (golem != null) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            int slots = menu.filterSlots() == 0 ? 0 : golem.inventory().getContainerSize();
            if (slots > 0 && slots > 6) {
                if (inside(x, y, 111, 68, 24, 8)) return sendButton(66);
                if (inside(x, y, 135, 68, 24, 8)) return sendButton(67);
            }
            if (golem.getUpgradeAmount(4) > 0 && golem.getCore() == 4) {
                for (int index = 0; index < 4; index++) if (inside(x, y, 102, 3 + index * 16, 12, 12)) return sendButton(51 + index);
            }
            if (golem.getCore() == 0 && inside(x, y, 62, 54, 8, 8)) return sendButton(50);
            if (golem.getCore() == 8) {
                for (int index = 0; index < 3; index++) if (inside(x, y, 42, 40 + index * 10, 8, 8)) return sendButton(50 + index);
            }
            if (golem.getUpgradeAmount(5) > 0) {
                int tx = golem.getCore() == 10 ? 66 : 180;
                int ty = golem.getCore() == 10 ? 12 : 0;
                for (int index = 0; index < 3; index++) if (inside(x, y, tx, 24 + ty + index * 10, 8, 8)) return sendButton(55 + index);
            }
            if (golem.getUpgradeAmount(4) > 0) {
                for (int index = 0; index < Math.min(6, slots); index++) {
                    int x0 = 96 + index / 2 * 28;
                    int y0 = 4 + index % 2 * 31;
                    if (inside(x, y, x0 + 7, y0 + 2, 8, 8)) return sendButton(index + menu.currentScroll() * 6);
                    if (inside(x, y, x0 + 17, y0 + 2, 8, 8)) return sendButton(slots + index + menu.currentScroll() * 6);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
