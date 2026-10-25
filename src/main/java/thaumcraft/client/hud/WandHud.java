package thaumcraft.client.hud;

import java.text.DecimalFormat;
import thaumcraft.aspect.AspectList;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandManager;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.TradeFocus;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.ClientConfig;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.client.platform.HudVisibility;
import thaumcraft.wand.WandVis;
import thaumcraft.wand.Wands;

public final class WandHud {
    private static final Identifier TEXTURE = Thaumcraft.id("textures/gui/hud.png");
    private static final long SAMPLE_INTERVAL = 1000L;
    private static final Map<Integer, WandVis> OLD_VALUES = new HashMap<>();
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("#####.##");
    private static final DecimalFormat COOLDOWN_FORMAT = new DecimalFormat("#######.#");
    private static long nextSample;

    private WandHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || HudVisibility.hidden()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!Wands.isWand(stack)) {
            return;
        }
        long time = Util.getMillis();
        int slot = player.getInventory().getSelectedSlot();
        WandVis current = Wands.getAllVisData(stack);
        WandVis old = OLD_VALUES.get(slot);
        if (old == null) {
            OLD_VALUES.put(slot, current);
            old = current;
        } else if (nextSample <= time) {
            OLD_VALUES.put(slot, current);
            nextSample = time + SAMPLE_INTERVAL;
        }
        int max = Wands.getMaxVis(stack);
        boolean sneaking = player.isShiftKeyDown();
        FocusItem focus = Wands.getFocus(stack);
        ItemStack focusStack = Wands.getFocusItem(stack);
        AspectList focusCost = focus == null ? null : focus.getVisCost(focusStack);
        boolean dialBottom = ClientConfig.dialBottom();
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, dialBottom ? graphics.guiHeight() - 32 : 0);
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        blit(graphics, 0, 0, 0, 0, 64, 64, 0xFFFFFFFF);
        graphics.pose().popMatrix();
        graphics.pose().translate(16.0F, 16.0F);
        int count = 0;
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            int amount = current.get(aspect);
            graphics.pose().pushMatrix();
            if (!dialBottom) {
                graphics.pose().rotate((float) Math.toRadians(90.0F));
            }
            graphics.pose().rotate((float) Math.toRadians(-15 + count * 24));
            graphics.pose().translate(0.0F, -32.0F);
            graphics.pose().scale(0.5F, 0.5F);
            int level = max <= 0 ? 0 : (int) (30.0F * amount / max);
            blit(graphics, -4, 35 - level, 104, 0, 8, level, 0xCC000000 | aspect.getColor());
            blit(graphics, -8, -3, 72, 0, 16, 42, 0xFFFFFFFF);
            int shift = 0;
            boolean costs = focusCost != null && focusCost.getAmount(aspect) > 0;
            if (costs) {
                blit(graphics, -4, -8, 136, 0, 8, 8, 0xFFFFFFFF);
                shift = 8;
            }
            int previous = old.get(aspect);
            if (previous > amount) {
                blit(graphics, -4, -8 - shift, 128, 0, 8, 8, 0xFFFFFFFF);
            } else if (previous < amount) {
                blit(graphics, -4, -8 - shift, 120, 0, 8, 8, 0xFFFFFFFF);
            }
            if (sneaking) {
                graphics.pose().pushMatrix();
                graphics.pose().rotate((float) Math.toRadians(-90.0F));
                graphics.text(minecraft.font, String.valueOf(amount / 100), -32, -4, 0xFFFFFFFF, true);
                graphics.pose().popMatrix();
                if (costs) {
                    float modifier = ((WandItem) stack.getItem()).getConsumptionModifier(stack, player, aspect, false);
                    graphics.pose().pushMatrix();
                    graphics.pose().rotate((float) Math.toRadians(-90.0F));
                    graphics.text(minecraft.font, COST_FORMAT.format(focusCost.getAmount(aspect) * modifier / 100.0F), 8, -4, 0xFFFFFFFF, true);
                    graphics.pose().popMatrix();
                }
            }
            graphics.pose().popMatrix();
            count++;
        }
        if (focus != null) {
            ItemStack picked = focus instanceof TradeFocus ? TradeFocus.getPickedBlock(stack) : ItemStack.EMPTY;
            if (!picked.isEmpty()) {
                renderTrade(graphics, minecraft, player, picked);
            } else {
                graphics.item(focusStack, -8, -8);
                float cooldown = WandManager.getCooldown(player);
                if (cooldown > 0.0F) {
                    graphics.pose().pushMatrix();
                    graphics.pose().scale(0.5F, 0.5F);
                    String seconds = COOLDOWN_FORMAT.format(cooldown) + "s";
                    graphics.text(minecraft.font, seconds, -minecraft.font.width(seconds) / 2, -4, 0xFFFFFFFF, true);
                    graphics.pose().popMatrix();
                }
            }
        }
        graphics.pose().popMatrix();
    }

    private static void renderTrade(GuiGraphicsExtractor graphics, Minecraft minecraft, Player player, ItemStack picked) {
        int amount = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (ItemStack.isSameItem(stack, picked)) {
                amount += stack.getCount();
            }
        }
        graphics.item(picked, -8, -8);
        String text = String.valueOf(amount);
        int width = minecraft.font.width(text);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, -minecraft.font.lineHeight);
        graphics.pose().scale(0.5F, 0.5F);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if ((a == 0 || b == 0) && (a != 0 || b != 0)) {
                    graphics.text(minecraft.font, text, a + 16 - width, b + 24, 0xFF000000, false);
                }
            }
        }
        graphics.text(minecraft.font, text, 16 - width, 24, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();
    }

    private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        GuiBlit.blit(graphics, TEXTURE, x, y, width, height, u, v, width, height, 256, 256, argb);
    }
}
