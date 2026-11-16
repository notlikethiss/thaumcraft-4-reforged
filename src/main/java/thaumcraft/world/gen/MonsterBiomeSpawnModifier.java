package thaumcraft.world.gen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModEntities;

public record MonsterBiomeSpawnModifier() implements BiomeModifier {
    public static final MapCodec<MonsterBiomeSpawnModifier> CODEC = MapCodec.unit(MonsterBiomeSpawnModifier::new);

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;
        var monsters = builder.getMobSpawnSettings().getSpawner(MobCategory.MONSTER);
        if (monsters == null || monsters.isEmpty()) return;
        boolean eerie = biome.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Thaumcraft.id("eerie")));
        if (biome.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD) && !eerie) {
            builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.BRAINY_ZOMBIE.get(), 10, 1, 1));
        }
        if (biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_MAGICAL)) {
            boolean forest = biome.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Thaumcraft.id("magical_forest")));
            builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.PECH.get(), 10, 1, forest ? 2 : 1));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
