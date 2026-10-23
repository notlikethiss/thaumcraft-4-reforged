package thaumcraft.world.gen;

import java.util.Calendar;
import java.util.function.Supplier;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModEntities;

public record HalloweenFirebatSpawnModifier(HolderSet<Biome> biomes) implements BiomeModifier {
    private static final MapCodec<HalloweenFirebatSpawnModifier> SERIALIZER = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RegistryCodecs.holderSet(Registries.BIOME).fieldOf("biomes").forGetter(HalloweenFirebatSpawnModifier::biomes)
    ).apply(instance, HalloweenFirebatSpawnModifier::new));
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> CODECS = DeferredRegister.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
        Thaumcraft.MODID
    );
    public static final Supplier<MapCodec<? extends BiomeModifier>> CODEC = CODECS.register(
        "halloween_firebat_spawns",
        () -> SERIALIZER
    );

    public static void register(IEventBus bus) {
        CODECS.register("monster_biome_spawns", () -> MonsterBiomeSpawnModifier.CODEC);
        CODECS.register(bus);
    }

    public static boolean isHalloween(int month, int day) {
        return month == 10 && day == 31;
    }

    private static boolean isHalloween() {
        Calendar date = Calendar.getInstance();
        return isHalloween(date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && isHalloween() && this.biomes.contains(biome)) {
            builder.getMobSpawnSettings().addSpawn(ModEntities.FIREBAT.get(), 5, 1, 2);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }
}
