package thaumcraft.client.platform.baubles;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.ClientNetwork;
import thaumcraft.platform.baubles.BaubleMenus;
import thaumcraft.platform.baubles.OpenBaublesPayload;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class BaublesClientEvents {
    public static final KeyMapping OPEN_KEY = new KeyMapping("key.thaumcraft.baubles", InputConstants.KEY_B, KeyMapping.Category.INVENTORY);

    private BaublesClientEvents() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BaubleMenus.EXPANDED.get(), BaublesScreen::new);
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientNetwork.sendToServer(new OpenBaublesPayload());
            }
        }
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(new BaublesButton(screen, Component.translatable("button.thaumcraft.baubles"), () -> ClientNetwork.sendToServer(new OpenBaublesPayload())));
        } else if (event.getScreen() instanceof BaublesScreen screen) {
            event.addListener(new BaublesButton(screen, Component.translatable("button.thaumcraft.normal"), () -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.player.connection.send(new ServerboundContainerClosePacket(screen.getMenu().containerId));
                minecraft.player.containerMenu = minecraft.player.inventoryMenu;
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            }));
        }
    }
}
