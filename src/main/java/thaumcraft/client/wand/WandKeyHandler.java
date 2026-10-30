package thaumcraft.client.wand;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.ClientNetwork;
import thaumcraft.item.armor.Hover;
import thaumcraft.item.golem.GolemBellItem;
import thaumcraft.network.BellResetPayload;
import thaumcraft.network.FocusChangePayload;
import thaumcraft.network.HoverTogglePayload;
import thaumcraft.network.MiscTogglePayload;
import thaumcraft.wand.WandManager;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WandKeyHandler {
    public static final KeyMapping CHANGE_FOCUS = new KeyMapping("key.thaumcraft.change_focus", InputConstants.KEY_F, "key.categories.misc");
    public static final KeyMapping HOVER_HARNESS = new KeyMapping("key.thaumcraft.hover_harness", InputConstants.KEY_H, "key.categories.misc");
    public static final KeyMapping MISC_TOGGLE = new KeyMapping("key.thaumcraft.misc_toggle", InputConstants.KEY_G, "key.categories.misc");

    static boolean radialActive;
    static boolean radialLock;
    private static net.minecraft.client.player.LocalPlayer movementPlayer;
    private static boolean forward;
    private static boolean pressedF;
    private static boolean pressedH;
    private static boolean pressedG;

    private WandKeyHandler() {
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CHANGE_FOCUS);
        event.register(HOVER_HARNESS);
        event.register(MISC_TOGGLE);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer player = minecraft.player;
        if (player == null) {
            movementPlayer = null;
            return;
        }
        boolean movingForward = player.input.forwardImpulse > 0.0F;
        if (movementPlayer != player || forward != movingForward) {
            movementPlayer = player;
            forward = movingForward;
            ClientNetwork.sendToServer(new thaumcraft.network.GearMovementPayload(movingForward));
        }
        boolean inGame = minecraft.screen == null;
        boolean focusClicked = false;
        while (CHANGE_FOCUS.consumeClick()) {
            focusClicked = true;
        }
        ItemStack held = player.getMainHandItem();
        boolean canChangeFocus = Wands.isWand(held) && !Wands.isSceptre(held);
        boolean holdingBell = held.getItem() instanceof GolemBellItem;
        if (inGame && (canChangeFocus || holdingBell) && (CHANGE_FOCUS.isDown() || focusClicked)
            && minecraft.options.keySwapOffhand.isActiveAndMatches(CHANGE_FOCUS.getKey())) {
            while (minecraft.options.keySwapOffhand.consumeClick()) {
            }
        }
        if (inGame && holdingBell && focusClicked) {
            ClientNetwork.sendToServer(new BellResetPayload());
        }
        if (CHANGE_FOCUS.isDown()) {
            if (inGame || radialActive) {
                if (!pressedF) {
                    radialLock = false;
                }
                if (!radialLock && canChangeFocus) {
                    if (player.isShiftKeyDown()) {
                        ClientNetwork.sendToServer(new FocusChangePayload(WandManager.REMOVE_FOCUS));
                    } else {
                        radialActive = true;
                    }
                }
                pressedF = true;
            }
        } else {
            radialActive = false;
            pressedF = false;
        }
        if (HOVER_HARNESS.isDown()) {
            if (inGame && !pressedH && !Hover.getHarness(player).isEmpty()) {
                ClientNetwork.sendToServer(new HoverTogglePayload());
            }
            pressedH = inGame || pressedH;
        } else {
            pressedH = false;
        }
        if (MISC_TOGGLE.isDown()) {
            if (inGame && !pressedG) {
                ClientNetwork.sendToServer(new MiscTogglePayload(player.isShiftKeyDown()));
            }
            pressedG = inGame || pressedG;
        } else {
            pressedG = false;
        }
    }
}
