package thaumcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.item.golem.GolemBellItem;

public record BellResetPayload() implements CustomPacketPayload {
    public static final Type<BellResetPayload> TYPE = new Type<>(Thaumcraft.id("bell_reset"));
    public static final StreamCodec<ByteBuf, BellResetPayload> STREAM_CODEC = StreamCodec.unit(new BellResetPayload());

    public static void handle(BellResetPayload payload, Player player) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof GolemBellItem && player.level() instanceof ServerLevel level) {
            GolemBellItem.resetMarkers(level, held, player);
        }
    }

    @Override
    public Type<BellResetPayload> type() {
        return TYPE;
    }
}
