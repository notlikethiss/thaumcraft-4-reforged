package thaumcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import thaumcraft.client.platform.GuiBlit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.TravelingTrunk;
import thaumcraft.menu.TravelingTrunkMenu;

public final class TravelingTrunkScreen extends AbstractContainerScreen<TravelingTrunkMenu> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/gui/guitrunkbase.png");
    private final TravelingTrunk trunk;

    public TravelingTrunkScreen(TravelingTrunkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 200;
        trunk = menu.trunk();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (trunk == null) {
            return;
        }
        Component label = trunk.hasCustomName() ? trunk.getCustomName() : Component.translatable("entity.trunk.guiname");
        graphics.pose().pushPose();
        graphics.pose().translate(8, 2, 0);
        graphics.pose().scale(0.5F, 0.5F, 1);
        graphics.drawString(font, label, 0, 0, 0xFFC0C0C0, true);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

        GuiBlit.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (trunk == null) {
            return;
        }
        int health = Math.clamp(Math.round(trunk.getHealth() / trunk.getMaxHealth() * 39.0F), 0, 39);
        if (health > 0) {
            GuiBlit.blit(graphics, TEXTURE, leftPos + 134, topPos + 2, 176, 16, health, 6, 256, 256);
        }
        if (trunk.getUpgrade() == 1) {
            GuiBlit.blit(graphics, TEXTURE, leftPos, topPos + 80, 0, 206, imageWidth, 27, 256, 256);
        }
        if (trunk.getStay()) {
            GuiBlit.blit(graphics, TEXTURE, leftPos + 112, topPos, 176, 0, 10, 10, 256, 256);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (trunk != null && inside(mouseX - leftPos, mouseY - topPos, 112, 0, 10, 10)) {
            if (minecraft.gameMode != null) {
                if (minecraft.player != null) {
                    minecraft.player.sendSystemMessage(Component.translatable(trunk.getStay() ? "entity.trunk.move" : "entity.trunk.stay"));
                }
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }

    @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
    public static final class TrunkLifecycle {
        private TrunkLifecycle() {
        }

        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.screen instanceof TravelingTrunkScreen screen && screen.trunk != null && !screen.trunk.isAlive()) {
                if (minecraft.player != null) minecraft.player.closeContainer();
            }
        }
    }
}
