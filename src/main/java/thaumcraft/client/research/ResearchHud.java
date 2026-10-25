package thaumcraft.client.research;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import thaumcraft.client.ClientConfig;
import thaumcraft.Thaumcraft;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.client.platform.BrowserDraw;
import thaumcraft.client.platform.GuiBlit;
import thaumcraft.client.platform.HudVisibility;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ResearchHud {
    private static final Identifier THAUMONOMICON = Thaumcraft.id("textures/item/thaumonomicon.png");

    private ResearchHud() {
    }

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Thaumcraft.id("research_notifications"), ResearchHud::render);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (HudVisibility.hidden()) {
            return;
        }
        long time = ResearchNotifications.now();
        if (!ResearchNotifications.getListAndUpdate(time).isEmpty()) {
            renderNotifications(graphics, time);
        }
        if (!ResearchNotifications.getAspectListAndUpdate(time).isEmpty()) {
            renderAspects(graphics, time);
        }
    }

    public static void renderNotifications(GuiGraphicsExtractor graphics, long time) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long delay = ClientConfig.notificationDelay();
        List<ResearchNotifications.Notification> notifications = ResearchNotifications.getListAndUpdate(time);
        int tickCount = minecraft.player == null ? 0 : minecraft.player.tickCount;
        float shift = -8.0F;
        for (int entry = 0; entry < notifications.size() && entry < ClientConfig.notificationMax(); entry++) {
            ResearchNotifications.Notification notification = notifications.get(entry);
            int size = font.width(notification.text()) / 2;
            int alpha = 255;
            boolean last = entry == notifications.size() - 1;
            if (last && notification.created() > time) {
                alpha = 255 - (int) ((float) (notification.created() - time) / (delay / 4) * 240.0F);
            }
            if (notification.expire() < time + delay) {
                alpha = (int) (255.0F - (float) (time + delay - notification.expire()) / delay * 240.0F);
                shift = -8.0F * (alpha / 255.0F);
            }
            int textAlpha = alpha / 2;
            if (textAlpha > 3) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(width - size - 10, height - entry * 8 + shift);
                graphics.pose().scale(0.5F, 0.5F);
                graphics.text(font, notification.text(), -4, -8, ARGB.color(textAlpha, 0xFFFFFF), true);
                graphics.pose().popMatrix();
            }
            if (notification.image() != null) {
                int color = ARGB.color(Math.max(0, Math.min(255, alpha / 2)), notification.color());
                graphics.pose().pushMatrix();
                graphics.pose().translate(width - 9, height - entry * 8 + shift - 6.0F);
                graphics.pose().scale(0.5F, 0.5F);
                BrowserDraw.full(graphics, notification.image(), 0, 0, 16, 16, color);
                graphics.pose().popMatrix();
            }
            if (last && notification.created() > time) {
                float scale = (float) (notification.created() - time) / (delay / 4);
                alpha = 255 - (int) (scale * 240.0F);
                float cube = (1.0F - scale) * (1.0F - scale) * (1.0F - scale);
                float particleAlpha = Math.max(0.0F, Math.min(1.0F, 0.5F - alpha / 511.0F));
                graphics.pose().pushMatrix();
                graphics.pose().translate(width - 5 - 8.0F * scale - cube * size * 3.0F, height - entry * 8 + shift - 2.0F - 8.0F * scale);
                graphics.pose().scale(scale, scale);
                int particleU = 16 * ((tickCount + entry * 3) % 16);
                GuiBlit.blit(graphics, AspectRenderer.PARTICLES, 0, 0, 16, 16, particleU, 80, 16, 16, 256, 256, ARGB.colorFromFloat(particleAlpha, 1.0F, 1.0F, 1.0F));
                graphics.pose().popMatrix();
            }
        }
    }

    public static void renderAspects(GuiGraphicsExtractor graphics, long time) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float mainAlpha = 0.0F;
        for (ResearchNotifications.AspectNotification notification : ResearchNotifications.getAspectListAndUpdate(time)) {
            if (notification.created() > time) {
                continue;
            }
            int startX = (int) (width * notification.startX());
            int startY = (int) (height * notification.startY());
            int bezierX = (int) (width * (0.25F + notification.startX()));
            int bezierY = (int) (height * notification.startY());
            double progress = (double) (time - notification.created()) / (notification.expire() - notification.created());
            double x = (1.0 - progress) * (1.0 - progress) * startX + 2.0 * (1.0 - progress) * progress * bezierX + progress * progress * width;
            double y = (1.0 - progress) * (1.0 - progress) * startY + 2.0 * (1.0 - progress) * progress * bezierY + progress * progress * -8;
            float alpha = 1.0F;
            if (progress < 0.3F) {
                alpha = (float) (progress / 0.3F);
            } else if (progress > 0.66F) {
                alpha = (float) (1.0 - (progress - 0.66F) / 0.34F);
            }
            mainAlpha = Math.max(mainAlpha, alpha);
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) x, (float) y);
            graphics.pose().scale(alpha, alpha);
            int color = ARGB.color(Math.round(Math.max(0.0F, Math.min(1.0F, alpha * 0.66F)) * 255.0F), notification.aspect().getColor());
            BrowserDraw.full(graphics, notification.aspect().getImage(), 0, 0, 19, 19, color);
            graphics.pose().popMatrix();
        }
        if (mainAlpha > 0.0F) {
            BrowserDraw.full(graphics, THAUMONOMICON, width - 16, 0, 16, 16, ARGB.colorFromFloat(Math.min(1.0F, mainAlpha), 1.0F, 1.0F, 1.0F));
        }
    }
}
