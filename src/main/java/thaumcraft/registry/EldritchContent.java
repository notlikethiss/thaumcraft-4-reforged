package thaumcraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.eldritch.EldritchBlock;
import thaumcraft.eldritch.EldritchBlockEntity;
import thaumcraft.eldritch.EldritchNothingBlock;
import thaumcraft.eldritch.EldritchPortalBlock;
import thaumcraft.eldritch.EldritchPortalBlockEntity;
import thaumcraft.eldritch.EldritchObjectItem;
import thaumcraft.eldritch.EldritchNothingBlockEntity;

public final class EldritchContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Thaumcraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Thaumcraft.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Thaumcraft.MODID);

    public static final DeferredBlock<EldritchBlock> ELDRITCH_ALTAR = block("eldritch_altar", 0, 50.0F);
    public static final DeferredBlock<EldritchBlock> ELDRITCH_OBELISK = block("eldritch_obelisk", 1, 50.0F);
    public static final DeferredBlock<EldritchBlock> ELDRITCH_CAPSTONE = block("eldritch_capstone", 3, 50.0F);
    public static final DeferredBlock<EldritchBlock> GLOWING_CRUSTED_STONE = block("glowing_crusted_stone", 4, 2.0F);
    public static final DeferredBlock<EldritchBlock> GLYPHED_STONE = block("glyphed_stone", 5, 2.0F);
    public static final DeferredBlock<EldritchBlock> ANCIENT_DOORWAY = block("ancient_doorway", 7, -1.0F);
    public static final DeferredBlock<EldritchBlock> ANCIENT_LOCKING_MECHANISM = block("ancient_locking_mechanism", 8, -1.0F);
    public static final DeferredBlock<EldritchBlock> CRUSTED_OPENING = block("crusted_opening", 9, 15.0F);
    public static final DeferredBlock<EldritchBlock> RUNED_STONE = block("runed_stone", 10, 15.0F);
    public static final DeferredBlock<EldritchPortalBlock> ELDRITCH_PORTAL = RegistryHelper.registerBlock(BLOCKS,
        "eldritch_portal",
        EldritchPortalBlock::new,
        properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 200000.0F).lightLevel(state -> 15).noOcclusion().noCollission().noLootTable()
    );
    public static final DeferredBlock<EldritchNothingBlock> ELDRITCH_NOTHING = RegistryHelper.registerBlock(BLOCKS,
        "eldritch_nothing",
        EldritchNothingBlock::new,
        properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 6000000.0F).sound(SoundType.WOOL).lightLevel(state -> 3).isValidSpawn((state, level, pos, type) -> false).noLootTable()
    );
    public static final DeferredItem<BlockItem> GLOWING_CRUSTED_STONE_ITEM = RegistryHelper.registerSimpleBlockItem(ITEMS, GLOWING_CRUSTED_STONE);

    public static final DeferredItem<EldritchObjectItem> ELDRITCH_EYE = object("eldritch_eye", 0, Rarity.UNCOMMON);
    public static final DeferredItem<EldritchObjectItem> CRIMSON_RITES = object("crimson_rites", 1, Rarity.UNCOMMON);
    public static final DeferredItem<EldritchObjectItem> RUNED_TABLET = object("runed_tablet", 2, Rarity.RARE);
    public static final DeferredItem<EldritchObjectItem> PRIMORDIAL_PEARL = object("primordial_pearl", 3, Rarity.EPIC);
    public static final DeferredItem<EldritchObjectItem> OBELISK_PLACER = object("eldritch_obelisk_placer", 4, Rarity.UNCOMMON);

    public static final Supplier<BlockEntityType<EldritchBlockEntity>> ELDRITCH_BLOCK_ENTITY = BLOCK_ENTITIES.register(
        "eldritch_block",
        () -> BlockEntityType.Builder.of(EldritchBlockEntity::new, ELDRITCH_ALTAR.get(), ELDRITCH_OBELISK.get(), ELDRITCH_CAPSTONE.get(), ANCIENT_LOCKING_MECHANISM.get(), CRUSTED_OPENING.get(), RUNED_STONE.get()).build(null)
    );
    public static final Supplier<BlockEntityType<EldritchPortalBlockEntity>> ELDRITCH_PORTAL_ENTITY = BLOCK_ENTITIES.register(
        "eldritch_portal",
        () -> BlockEntityType.Builder.of(EldritchPortalBlockEntity::new, ELDRITCH_PORTAL.get()).build(null)
    );
    public static final Supplier<BlockEntityType<EldritchNothingBlockEntity>> ELDRITCH_NOTHING_ENTITY = BLOCK_ENTITIES.register(
        "eldritch_nothing",
        () -> BlockEntityType.Builder.of(EldritchNothingBlockEntity::new, ELDRITCH_NOTHING.get()).build(null)
    );

    private EldritchContent() {
    }

    private static DeferredItem<EldritchObjectItem> object(String id, int kind, Rarity rarity) {
        return RegistryHelper.registerItem(ITEMS, id, properties -> new EldritchObjectItem(kind, properties), properties -> properties.stacksTo(1).rarity(rarity));
    }

    private static DeferredBlock<EldritchBlock> block(String id, int kind, float strength) {
        float resistance = kind == 7 || kind == 8 ? Float.MAX_VALUE : (kind == 4 || kind == 5 || kind == 9 || kind == 10 ? 30.0F : Math.max(30.0F, strength * 400.0F));
        int light = kind == 4 || kind == 5 || kind == 7 ? 12 : (kind == 6 || kind == 8 ? 5 : (kind == 9 ? 4 : (kind == 10 ? 0 : 8)));
        return RegistryHelper.registerBlock(BLOCKS, id, properties -> kind == 4 || kind == 5 ? new thaumcraft.eldritch.EldritchGlowingBlock(kind, properties) : new EldritchBlock(kind, properties), properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(strength, resistance).sound(SoundType.STONE).noOcclusion().isValidSpawn((state, level, pos, type) -> false).lightLevel(state -> light));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
