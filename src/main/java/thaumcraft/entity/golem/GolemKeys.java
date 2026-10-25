package thaumcraft.entity.golem;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import thaumcraft.infusion.InfusionKeys;
import thaumcraft.platform.StackKey;

public final class GolemKeys {
    public static final StackKey<Optional<net.neoforged.neoforge.fluids.FluidStackTemplate>> FLUID = StackKey.register("golem_fluid", net.neoforged.neoforge.fluids.FluidStackTemplate.CODEC.optionalFieldOf("fluid").codec(), ByteBufCodecs.optional(net.neoforged.neoforge.fluids.FluidStackTemplate.STREAM_CODEC));
    public static final StackKey<String> ESSENTIA = StackKey.register("golem_essentia", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final StackKey<Integer> ESSENTIA_AMOUNT = StackKey.register("golem_essentia_amount", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final StackKey<Integer> TRUNK_UPGRADE = StackKey.register("trunk_upgrade", Codec.intRange(-1, 5), ByteBufCodecs.VAR_INT);
    public static final StackKey<net.minecraft.world.item.component.ItemContainerContents> TRUNK_CONTENTS = StackKey.register("trunk_contents", net.minecraft.world.item.component.ItemContainerContents.CODEC, net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC);
    public static final StackKey<GolemSnapshot> STATE = StackKey.register("golem_state", GolemSnapshot.CODEC, ByteBufCodecs.fromCodecWithRegistries(GolemSnapshot.CODEC));
    public static final Codec<BellSelection> BELL_CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
        net.minecraft.core.UUIDUtil.CODEC.fieldOf("entity").forGetter(BellSelection::entity),
        GlobalPos.CODEC.fieldOf("home").forGetter(BellSelection::home),
        Direction.CODEC.optionalFieldOf("home_facing", Direction.UP).forGetter(BellSelection::homeFacing),
        Marker.CODEC.listOf().fieldOf("markers").forGetter(BellSelection::markers)
    ).apply(instance, BellSelection::new));
    public static final StackKey<BellSelection> BELL = StackKey.register("golem_bell_selection", BELL_CODEC, ByteBufCodecs.fromCodecWithRegistries(BELL_CODEC));

    private GolemKeys() {
    }

    public record BellSelection(UUID entity, GlobalPos home, Direction homeFacing, java.util.List<Marker> markers) {
        public BellSelection {
            markers = java.util.List.copyOf(markers);
        }

        public Marker homeMarker() {
            return new Marker(GlobalPos.of(home.dimension(), home.pos().relative(homeFacing.getOpposite())), homeFacing, -1);
        }
    }

    public static GolemSnapshot snapshot(ItemStack stack) {
        GolemSnapshot state = STATE.getOrDefault(stack, GolemSnapshot.EMPTY);
        return InfusionKeys.TAGS.getOrDefault(stack, java.util.Map.of()).containsKey("advanced") ? state.withAdvanced() : state;
    }

    public static void init() {
    }
}
