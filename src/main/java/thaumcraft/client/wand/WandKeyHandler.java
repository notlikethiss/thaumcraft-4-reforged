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
import thaumcraft.network.FocusChangePayload;
import thaumcraft.network.HoverTogglePayload;
import thaumcraft.network.MiscTogglePayload;
import thaumcraft.wand.WandManager;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WandKeyHandler {
    public static final KeyMapping CHANGE_FOCUS = new KeyMapping("key.thaumcraft.change_focus", InputConstants.KEY_F, KeyMapping.Category.MISC);
    public static final KeyMapping HOVER_HARNESS = new KeyMapping("key.thaumcraft.hover_harness", InputConstants.KEY_H, KeyMapping.Category.MISC);
    public static final KeyMapping MISC_TOGGLE = new KeyMapping("key.thaumcraft.misc_toggle", InputConstants.KEY_G, KeyMapping.Category.MISC);

    static boolean radialActive;
    static boolean radialLock;
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
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean inGame = minecraft.screen == null;
        boolean focusClicked = false;
        while (CHANGE_FOCUS.consumeClick()) {
            focusClicked = true;
        }
        ItemStack held = player.getMainHandItem();
        boolean canChangeFocus = Wands.isWand(held) && !Wands.isSceptre(held);
        if (inGame && canChangeFocus && (CHANGE_FOCUS.isDown() || focusClicked)
            && minecraft.options.keySwapOffhand.isActiveAndMatches(CHANGE_FOCUS.getKey())) {
            while (minecraft.options.keySwapOffhand.consumeClick()) {
            }
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
