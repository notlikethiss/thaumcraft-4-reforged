package thaumcraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModItems;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerEquip {
    private static final int SETTLED_TICKS = 5;
    private static final int SUPPRESS_TICKS = 5;

    private static @Nullable ItemStack lastHeld;
    private static int heldTicks;
    private static boolean wasUsing;
    private static int suppressTicks;

    private ThaumometerEquip() {
    }

    public static float hidden(float raw) {
        return suppressTicks > 0 ? 0.0F : raw;
    }

    @SubscribeEvent
    static void clientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack held = player == null ? null : player.getMainHandItem();
        if (held == null || !held.is(ModItems.THAUMOMETER.get()) || held != lastHeld) {
            lastHeld = held != null && held.is(ModItems.THAUMOMETER.get()) ? held : null;
            heldTicks = 0;
            wasUsing = false;
            suppressTicks = 0;
            return;
        }
        boolean using = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND;
        if (using && !wasUsing && heldTicks >= SETTLED_TICKS) {
            suppressTicks = SUPPRESS_TICKS;
        } else if (suppressTicks > 0) {
            suppressTicks--;
        }
        wasUsing = using;
        heldTicks++;
    }
}
