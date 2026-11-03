package thaumcraft.client.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.platform.ModNetwork;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ClientNetwork {
    private static final List<Consumer<RegisterClientPayloadHandlersEvent>> HANDLERS = new ArrayList<>();

    private ClientNetwork() {
    }

    public static <T extends CustomPacketPayload> void handle(CustomPacketPayload.Type<T> type, ModNetwork.Handler<T> handler) {
        HANDLERS.add(event -> event.register(type, (payload, context) -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                handler.handle(payload, player);
            }
        }));
    }

    @SubscribeEvent
    static void register(RegisterClientPayloadHandlersEvent event) {
        for (Consumer<RegisterClientPayloadHandlersEvent> handler : HANDLERS) {
            handler.accept(event);
        }
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
