package thaumcraft.entity.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.component.ItemContainerContents;

public record GolemSnapshot(int core, boolean advanced, List<Integer> upgrades, String decoration, List<Marker> markers, ItemContainerContents filters, List<Integer> colors, int toggles) {
    public static final GolemSnapshot EMPTY = new GolemSnapshot(-1, false, List.of(), "", List.of(), ItemContainerContents.EMPTY, List.of(), 0);
    public static final Codec<GolemSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(-1, 11).optionalFieldOf("core", -1).forGetter(GolemSnapshot::core),
        Codec.BOOL.optionalFieldOf("advanced", false).forGetter(GolemSnapshot::advanced),
        Codec.intRange(-1, 5).listOf().optionalFieldOf("upgrades", List.of()).forGetter(GolemSnapshot::upgrades),
        Codec.STRING.optionalFieldOf("decoration", "").forGetter(GolemSnapshot::decoration),
        Marker.CODEC.listOf().optionalFieldOf("markers", List.of()).forGetter(GolemSnapshot::markers),
        ItemContainerContents.CODEC.optionalFieldOf("filters", ItemContainerContents.EMPTY).forGetter(GolemSnapshot::filters),
        Codec.intRange(-1, 15).listOf().optionalFieldOf("colors", List.of()).forGetter(GolemSnapshot::colors),
        Codec.intRange(0, 255).optionalFieldOf("toggles", 0).forGetter(GolemSnapshot::toggles)
    ).apply(instance, GolemSnapshot::new));

    public GolemSnapshot {
        upgrades = List.copyOf(upgrades);
        markers = List.copyOf(markers);
        colors = List.copyOf(colors);
    }

    public GolemSnapshot withAdvanced() {
        return new GolemSnapshot(core, true, upgrades, decoration, markers, filters, colors, toggles);
    }
}
