package thaumcraft.world.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import thaumcraft.block.LootBlock;
import thaumcraft.registry.ModBlocks;

public final class MoundGenerator {
    private record Entry(int x, int y, int z, BlockState state) {
    }

    private static List<Entry> template;

    private MoundGenerator() {
    }

    private static BlockState parse(String text) {
        String name = text;
        String properties = null;
        int bracket = text.indexOf('[');
        if (bracket >= 0) {
            name = text.substring(0, bracket);
            properties = text.substring(bracket + 1, text.length() - 1);
        }
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
        BlockState state = block.defaultBlockState();
        if (properties != null) {
            for (String pair : properties.split(",")) {
                String[] parts = pair.split("=");
                state = withProperty(state, parts[0], parts[1]);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withProperty(BlockState state, String name, String value) {
        @SuppressWarnings("unchecked")
        Property<T> property = (Property<T>) state.getBlock().getStateDefinition().getProperty(name);
        return state.setValue(property, property.getValue(value).orElseThrow());
    }

    private static synchronized List<Entry> template() {
        if (template != null) {
            return template;
        }
        List<Entry> entries = new ArrayList<>();
        try (InputStream stream = MoundGenerator.class.getResourceAsStream("/thaumcraft/structures/mound.txt")) {
            if (stream == null) {
                throw new IllegalStateException("Missing mound template");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(" ");
                entries.add(new Entry(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parse(parts[3])));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        template = entries;
        return entries;
    }

    public static boolean generate(LevelAccessor level, Random random, int i, int j, int k) {
        if (!LegacyGen.locationIsValidSpawn(level, i + 9, j + 9, k + 9)
            || !LegacyGen.locationIsValidSpawn(level, i, j + 9, k)
            || !LegacyGen.locationIsValidSpawn(level, i + 18, j + 9, k)
            || !LegacyGen.locationIsValidSpawn(level, i + 18, j + 9, k + 18)
            || !LegacyGen.locationIsValidSpawn(level, i, j + 9, k + 18)) {
            return false;
        }
        for (Entry entry : template()) {
            LegacyGen.set(level, i + entry.x(), j + entry.y(), k + entry.z(), entry.state());
        }
        LegacyGen.set(level, i + 9, j + 1, k + 7, lootBlock(random));
        LegacyGen.set(level, i + 9, j + 1, k + 11, lootBlock(random));
        if (random.nextInt(3) == 0) {
            LegacyGen.set(level, i + 10, j + 1, k + 9, Blocks.TRAPPED_CHEST.defaultBlockState());
            LegacyGen.set(level, i + 10, j - 1, k + 9, Blocks.TNT.defaultBlockState());
        } else {
            LegacyGen.set(level, i + 10, j + 1, k + 9, Blocks.CHEST.defaultBlockState());
        }
        loot(level, new BlockPos(i + 10, j + 1, k + 9), BuiltInLootTables.SIMPLE_DUNGEON, random);
        LegacyGen.set(level, i + 4, j + 5, k + 4, Blocks.SPAWNER.defaultBlockState());
        LegacyGen.set(level, i + 4, j + 5, k + 14, Blocks.SPAWNER.defaultBlockState());
        spawner(level, new BlockPos(i + 4, j + 5, k + 4), EntityType.SKELETON);
        spawner(level, new BlockPos(i + 4, j + 5, k + 14), EntityType.ZOMBIE);
        return true;
    }

    private static BlockState lootBlock(Random random) {
        float roll = random.nextFloat();
        LootBlock.Tier tier = roll < 0.1F ? LootBlock.Tier.RARE : (roll < 0.33F ? LootBlock.Tier.UNCOMMON : LootBlock.Tier.COMMON);
        Block block = random.nextFloat() < 0.3F ? ModBlocks.LOOT_CRATE.get() : ModBlocks.LOOT_URN.get();
        return block.defaultBlockState().setValue(LootBlock.TIER, tier);
    }

    static void loot(LevelAccessor level, BlockPos pos, ResourceKey<LootTable> table, Random random) {
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(table, random.nextLong());
        }
    }

    static void spawner(LevelAccessor level, BlockPos pos, EntityType<?> type) {
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, level.getRandom());
        }
    }
}
