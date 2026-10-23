package thaumcraft.client.wand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.ClientNetwork;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.network.FocusChangePayload;
import thaumcraft.platform.Baubles;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusPouchItem;

public final class FocusRadial {
    private static final Identifier RADIAL = Thaumcraft.id("textures/misc/radial.png");
    private static final Identifier RADIAL2 = Thaumcraft.id("textures/misc/radial2.png");
    private static final TreeMap<String, ItemStack> FOCI = new TreeMap<>();
    private static final Map<String, Boolean> HOVER = new HashMap<>();
    private static final Map<String, Float> SCALE = new HashMap<>();
    private static float hudScale;
    private static long lastTime;
    private static boolean lastState;

    private FocusRadial() {
    }

    private static void collect(Player player) {
        FOCI.clear();
        HOVER.clear();
        SCALE.clear();
        for (ItemStack bauble : Baubles.stacks(player)) {
            if (bauble.getItem() instanceof FocusPouchItem) {
                FocusPouchItem.getInventory(bauble).forEach(FocusRadial::add);
            }
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            add(stack);
            if (stack.getItem() instanceof FocusPouchItem) {
                FocusPouchItem.getInventory(stack).forEach(FocusRadial::add);
            }
        }
    }

    private static void add(ItemStack stack) {
        if (stack.getItem() instanceof FocusItem focus) {
            String key = focus.getSortingHelper(stack);
            FOCI.put(key, stack.copy());
            SCALE.put(key, 1.0F);
            HOVER.put(key, false);
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !WandKeyHandler.radialActive && hudScale <= 0.0F) {
            return;
        }
        long time = Util.getMillis();
        if (!WandKeyHandler.radialActive) {
            if (minecraft.screen == null && lastState) {
                minecraft.mouseHandler.grabMouse();
                lastState = false;
            }
        } else {
            if (minecraft.screen != null) {
                WandKeyHandler.radialActive = false;
                WandKeyHandler.radialLock = true;
                return;
            }
            if (hudScale == 0.0F) {
                collect(player);
                if (!FOCI.isEmpty() && minecraft.mouseHandler.isMouseGrabbed()) {
                    minecraft.mouseHandler.releaseMouse();
                }
            }
        }
        draw(graphics, minecraft, player, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (time > lastTime) {
            for (String key : HOVER.keySet()) {
                if (HOVER.get(key)) {
                    if (!WandKeyHandler.radialActive && !WandKeyHandler.radialLock) {
                        ClientNetwork.sendToServer(new FocusChangePayload(key));
                        WandKeyHandler.radialLock = true;
                    }
                    if (SCALE.get(key) < 1.3F) {
                        SCALE.put(key, SCALE.get(key) + 0.025F);
                    }
                } else if (SCALE.get(key) > 1.0F) {
                    SCALE.put(key, SCALE.get(key) - 0.025F);
                }
            }
            hudScale += WandKeyHandler.radialActive ? 0.05F : -0.05F;
            if (hudScale > 1.0F) {
                hudScale = 1.0F;
            }
            if (hudScale < 0.0F) {
                hudScale = 0.0F;
                WandKeyHandler.radialLock = false;
            }
            lastTime = time + 5L;
            lastState = WandKeyHandler.radialActive;
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, Player player, float partialTick) {
        ItemStack wand = player.getMainHandItem();
        if (!Wands.isWand(wand) || FOCI.isEmpty()) {
            return;
        }
        double screenWidth = graphics.guiWidth();
        double screenHeight = graphics.guiHeight();
        int mouseX = (int) (minecraft.mouseHandler.xpos() * screenWidth / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * screenHeight / minecraft.getWindow().getScreenHeight());
        boolean click = minecraft.mouseHandler.isLeftPressed();
        ItemStack tooltip = ItemStack.EMPTY;
        float width = 16.0F + FOCI.size() * 2.5F;
        float spin = partialTick + player.tickCount % 720 / 2.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (screenWidth / 2.0), (float) (screenHeight / 2.0));
        ring(graphics, RADIAL, spin, width * 2.75F * hudScale);
        ring(graphics, RADIAL2, -spin, width * 2.55F * hudScale);
        if (Wands.getFocus(wand) != null) {
            graphics.item(Wands.getFocusItem(wand), -8, -8);
            int mx = (int) (mouseX - screenWidth / 2.0);
            int my = (int) (mouseY - screenHeight / 2.0);
            if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) {
                tooltip = Wands.getFocusItem(wand);
            }
        }
        graphics.pose().pushMatrix();
        graphics.pose().scale(hudScale, hudScale);
        float rotation = -90.0F * hudScale;
        float slice = 360.0F / FOCI.size();
        for (Map.Entry<String, ItemStack> entry : FOCI.entrySet()) {
            String key = entry.getKey();
            double x = Mth.cos(rotation / 180.0F * (float) Math.PI) * width;
            double y = Mth.sin(rotation / 180.0F * (float) Math.PI) * width;
            rotation += slice;
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) x, (float) y);
            float scale = SCALE.get(key);
            graphics.pose().scale(scale, scale);
            graphics.item(entry.getValue(), -8, -8);
            graphics.pose().popMatrix();
            if (!WandKeyHandler.radialLock && WandKeyHandler.radialActive) {
                int mx = (int) ((mouseX - screenWidth / 2.0) / Math.max(hudScale, 0.01F) - x);
                int my = (int) ((mouseY - screenHeight / 2.0) / Math.max(hudScale, 0.01F) - y);
                if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) {
                    HOVER.put(key, true);
                    tooltip = entry.getValue();
                    if (click) {
                        WandKeyHandler.radialActive = false;
                        WandKeyHandler.radialLock = true;
                        ClientNetwork.sendToServer(new FocusChangePayload(key));
                        break;
                    }
                } else {
                    HOVER.put(key, false);
                }
            }
        }
        graphics.pose().popMatrix();
        if (!tooltip.isEmpty()) {
            List<ClientTooltipComponent> lines = Screen.getTooltipFromItem(minecraft, tooltip).stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
            graphics.tooltip(minecraft.font, lines, -4, 20, DefaultTooltipPositioner.INSTANCE, null);
        }
        graphics.pose().popMatrix();
    }

    private static void ring(GuiGraphicsExtractor graphics, Identifier texture, float degrees, float size) {
        int extent = Math.max(1, Math.round(size));
        graphics.pose().pushMatrix();
        graphics.pose().rotate((float) Math.toRadians(degrees));
        GuiBlit.blit(graphics, texture, -extent / 2, -extent / 2, extent, extent, 0, 0, 256, 256, 256, 256, 0x80808080);
        graphics.pose().popMatrix();
    }
}
