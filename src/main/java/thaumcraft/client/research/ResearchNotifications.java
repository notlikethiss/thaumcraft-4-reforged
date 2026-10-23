package thaumcraft.client.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.ClientConfig;
import thaumcraft.aspect.Aspect;
import thaumcraft.network.AspectDiscoveryPayload;
import thaumcraft.network.AspectPoolPayload;
import thaumcraft.network.ResearchCompletePayload;
import thaumcraft.network.WarpMessagePayload;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;

public final class ResearchNotifications {

    private static final RandomSource RANDOM = RandomSource.create();
    private static List<Notification> notifications = new ArrayList<>();
    private static List<AspectNotification> aspectNotifications = new ArrayList<>();
    private static long lastSound;

    public record Notification(String text, @Nullable Identifier image, long expire, long created, int color) {
    }

    public record AspectNotification(Aspect aspect, float startX, float startY, long created, long expire) {
    }

    private ResearchNotifications() {
    }

    public static long now() {
        return Util.getNanos() / 1000000L;
    }

    public static void addNotification(String text) {
        addNotification(text, null, 0xFFFFFF);
    }

    public static void addNotification(String text, Aspect aspect) {
        addNotification(text, aspect.getImage(), aspect.getColor());
    }

    public static void addNotification(String text, @Nullable Identifier image, int color) {
        long time = now();
        long timeBonus = notifications.isEmpty() ? ClientConfig.notificationDelay() / 2 : 0L;
        notifications.add(new Notification(text, image, time + ClientConfig.notificationDelay() + timeBonus, time + ClientConfig.notificationDelay() / 4, color));
    }

    public static void addAspectNotification(Aspect aspect) {
        long time = now() + RANDOM.nextInt(1000);
        float x = 0.4F + RANDOM.nextFloat() * 0.2F;
        float y = 0.4F + RANDOM.nextFloat() * 0.2F;
        aspectNotifications.add(new AspectNotification(aspect, x, y, time, time + 1500L));
    }

    public static List<Notification> getListAndUpdate(long time) {
        List<Notification> updated = new ArrayList<>();
        boolean first = true;
        for (Notification notification : notifications) {
            if (notification.expire() >= time) {
                if (!first) {
                    updated.add(new Notification(notification.text(), notification.image(), time + ClientConfig.notificationDelay(), notification.created(), notification.color()));
                } else {
                    updated.add(notification);
                }
            }
            first = false;
        }
        notifications = updated;
        return updated;
    }

    public static List<AspectNotification> getAspectListAndUpdate(long time) {
        List<AspectNotification> updated = new ArrayList<>();
        for (AspectNotification notification : aspectNotifications) {
            if (notification.expire() >= time) {
                updated.add(notification);
            }
        }
        aspectNotifications = updated;
        return updated;
    }

    public static void researchComplete(ResearchCompletePayload payload, Player player) {
        String key = payload.key();
        if (key == null || key.isEmpty()) {
            return;
        }
        if (key.startsWith("@")) {
            addNotification("§a" + Component.translatable("tc.addclue").getString());
            play(player, ModSounds.LEARN.get(), 0.2F, 1.0F + RANDOM.nextFloat() * 0.1F);
        } else {
            ResearchItem item = ResearchCategories.getResearch(key);
            if (item != null && !item.isVirtual()) {
                Minecraft.getInstance().getToastManager().addToast(new ResearchToast(item));
                ResearchBrowserScreen.lastX = item.column();
                ResearchBrowserScreen.lastY = item.row();
                ResearchBrowserScreen.highlight(key);
                ResearchBrowserScreen.highlight(item.category());
            }
        }
        if (Minecraft.getInstance().screen instanceof ResearchBrowserScreen browser) {
            browser.updateResearch();
        }
    }

    public static void aspectPool(AspectPoolPayload payload, Player player) {
        if (payload.amount() <= 0) {
            return;
        }
        Aspect aspect = payload.aspect();
        String text = Component.translatable("tc.addaspectpool", payload.amount()).getString().replace("%n", aspect.getName());
        addNotification(text, aspect);
        for (int count = 0; count < payload.amount(); count++) {
            addAspectNotification(aspect);
        }
        if (System.currentTimeMillis() > lastSound) {
            play(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F, 0.9F + RANDOM.nextFloat() * 0.2F);
            lastSound = System.currentTimeMillis() + 100L;
        }
    }

    public static void aspectDiscovery(AspectDiscoveryPayload payload, Player player) {
        Aspect aspect = payload.aspect();
        String text = Component.translatable("tc.addaspectdiscovery").getString().replace("%n", aspect.getName());
        addNotification("§6" + text, aspect);
        play(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.2F, 0.5F + RANDOM.nextFloat() * 0.2F);
        ResearchBrowserScreen.highlight("ASPECTS");
        ResearchBrowserScreen.highlight("BASICS");
    }

    public static void warpMessage(WarpMessagePayload payload, Player player) {
        int type = payload.warpType();
        int data = payload.amount();
        if (data == 0) {
            return;
        }
        if (type == 0 && data > 0) {
            addNotification(Component.translatable("tc.addwarp").getString());
            play(player, ModSounds.WHISPERS.get(), 0.5F, 1.0F);
        } else if (type == 1) {
            if (data < 0) {
                addNotification(Component.translatable("tc.removewarpsticky").getString());
            } else {
                addNotification(Component.translatable("tc.addwarpsticky").getString());
                play(player, ModSounds.WHISPERS.get(), 0.5F, 1.0F);
            }
        } else if (data > 0) {
            addNotification(Component.translatable("tc.addwarptemp").getString());
        }
    }

    private static void play(Player player, SoundEvent sound, float volume, float pitch) {
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }
}
