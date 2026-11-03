package thaumcraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import thaumcraft.client.platform.GuiBlit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.menu.GolemMenu;

public final class GolemScreen extends AbstractContainerScreen<GolemMenu> {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/gui/guigolem.png");
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
    private static final String[] TARGET_NAMES = {"tc.golem.target.monsters", "tc.golem.target.animals", "tc.golem.target.players", "tc.golem.target.creepers"};
    private final GolemBase golem;
    private final int threat;

    public GolemScreen(GolemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        golem = menu.golem();
        threat = golem != null && golem.isAdvanced() && inventory.player.getRandom().nextInt(4) == 0
            ? inventory.player.getRandom().nextInt(9)
            : -1;
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        GuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }

    private void text(GuiGraphicsExtractor graphics, String key, int x, int y, int color) {
        graphics.text(font, Component.translatable(key), leftPos + x, topPos + y, color, true);
    }

    private void scaled(GuiGraphicsExtractor graphics, Component component, int x, int y, int color, float scale) {
        scaledLabel(graphics, component, leftPos + x, topPos + y, color, scale);
    }

    private void scaledLabel(GuiGraphicsExtractor graphics, Component component, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, component, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void swatch(GuiGraphicsExtractor graphics, int x, int y, int color) {
        if (color >= 0 && color < COLORS.length) {
            GuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + y, 0.0F, 176.0F, 6, 6, 256, 256, ARGB.opaque(COLORS[color]));
        }
    }

    private boolean toggled(int index) {
        return golem != null && golem.getToggles()[index];
    }

    private void toggle(GuiGraphicsExtractor graphics, int x, int y, int index) {
        blit(graphics, x, y, 8, toggled(index) ? 176 : 168, 8, 8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (golem == null) {
            return;
        }
        int core = golem.getCore();
        String key = threat >= 0 ? "golemthreat." + threat + ".text" : "golemblurb." + Math.max(0, core) + ".text";
        graphics.textWithWordWrap(font, Component.translatable(key), 80, 22, 110, TEXT, true);
        if (menu.maxScroll() > 0) {
            scaledLabel(graphics, Component.literal((menu.currentScroll() + 1) + "/" + (menu.maxScroll() + 1)), 161, 70, TEXT, 0.5F);
        }
        int count = Math.min(6, menu.filterSlots());
        for (int index = 0; index < count; index++) {
            int x = 96 + index / 2 * 28;
            int y = 4 + index % 2 * 31;
            if (inside(mouseX - leftPos, mouseY - topPos, x, y, 24, 12)) {
                int color = golem.getColors(index + menu.currentScroll() * 6);
                Component name = Component.translatable(color < 0 ? "tc.golem.anycolor" : "color.minecraft." + COLOR_NAMES[color]);
                graphics.text(font, name, 133 - font.width(name) / 2, -6, LABEL, true);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
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
            scaled(graphics, Component.translatable(toggled(0) ? "tc.golem.amount.any" : "tc.golem.amount.precise"), 66, 48, LABEL, 0.5F);
        }
        if (core == 8) {
            String[] names = {"tc.golem.toggle.block", "tc.golem.toggle.rightclick", "tc.golem.toggle.notsneaking"};
            String[] active = {"tc.golem.toggle.empty", "tc.golem.toggle.leftclick", "tc.golem.toggle.sneaking"};
            for (int index = 0; index < 3; index++) {
                toggle(graphics, 42, 40 + index * 10, index);
                scaled(graphics, Component.translatable(toggled(index) ? active[index] : names[index]), 53, 42 + index * 10, LABEL, 0.5F);
            }
        }
        if (golem.getUpgradeAmount(5) > 0 && (core == 0 || core == 1 || core == 2 || core == 8 || core == 10)) {
            int x = core == 10 ? 66 : 180;
            int y = core == 10 ? 12 : 0;
            for (int index = 0; index < 3; index++) {
                toggle(graphics, x, 24 + y + index * 10, 5 + index);
                String[] options = {"tc.golem.sort.oredict", "tc.golem.sort.damage", "tc.golem.sort.nbt"};
                scaled(graphics, Component.translatable(options[index]), x + 10, 26 + y + index * 10, toggled(5 + index) ? LABEL : 0xFF666666, 0.5F);
            }
        }
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, leftPos + 26, topPos + 8, leftPos + 76, topPos + 78, 45, 0.0625F, mouseX, mouseY, golem);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        if (golem != null) {
            double x = event.x() - leftPos;
            double y = event.y() - topPos;
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
        return super.mouseClicked(event, doubleClick);
    }
}
