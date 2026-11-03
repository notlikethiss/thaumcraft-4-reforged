package thaumcraft.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.alchemy.AdvancedAlchemicalFurnaceBlock;
import thaumcraft.alchemy.AlchemicalFurnaceBlock;
import thaumcraft.alchemy.AlembicBlock;
import thaumcraft.alchemy.BellowsBlock;
import thaumcraft.alchemy.CrucibleBlock;
import thaumcraft.alchemy.FluxBlock;
import thaumcraft.alchemy.FluxGasBlock;
import thaumcraft.alchemy.FluxGooBlock;
import thaumcraft.alchemy.FluxScrubberBlock;
import thaumcraft.arcane.FocalManipulatorBlock;
import thaumcraft.alchemy.MnemonicMatrixBlock;
import thaumcraft.alchemy.ThaumatoriumBlock;
import thaumcraft.arcane.ArcaneWorkbenchBlock;
import thaumcraft.arcane.DeconstructionTableBlock;
import thaumcraft.arcane.VisChargeRelayBlock;
import thaumcraft.block.AmberBlock;
import thaumcraft.block.CandleBlock;
import thaumcraft.block.ChargedObsidianTotemBlock;
import thaumcraft.block.CustomPlantBlock;
import thaumcraft.block.FlammableBlock;
import thaumcraft.block.GolemFetterBlock;
import thaumcraft.block.HoleBlock;
import thaumcraft.block.InfusedStoneBlock;
import thaumcraft.block.LootBlock;
import thaumcraft.block.LiquidDeathBlock;
import thaumcraft.block.MagicalLeavesBlock;
import thaumcraft.block.MagicalLogBlock;
import thaumcraft.block.MagicalSaplingBlock;
import thaumcraft.block.ManaPodBlock;
import thaumcraft.block.NodeJarBlock;
import thaumcraft.block.ObsidianTotemBlock;
import thaumcraft.block.PurifyingFluidBlock;
import thaumcraft.block.RechargeFocusBlock;
import thaumcraft.block.ResearchTableBlock;
import thaumcraft.block.ResearchTablePartBlock;
import thaumcraft.block.SilverwoodKnotBlock;
import thaumcraft.block.TableBlock;
import thaumcraft.block.TravelPavingStoneBlock;
import thaumcraft.block.WandPedestalBlock;
import thaumcraft.block.WardedBlock;
import thaumcraft.block.WardingPavingStoneBlock;
import thaumcraft.device.ArcaneDoorBlock;
import thaumcraft.device.ArcaneDoorItem;
import thaumcraft.device.ArcanePressurePlateBlock;
import thaumcraft.device.BannerBlock;
import thaumcraft.device.BannerItem;
import thaumcraft.device.BrainJarBlock;
import thaumcraft.device.HungryChestBlock;
import thaumcraft.device.WardedGlassBlock;
import thaumcraft.device.ArcaneBoreBaseBlock;
import thaumcraft.device.ArcaneBoreBlock;
import thaumcraft.device.ArcaneSpaBlock;
import thaumcraft.device.InfernalFurnaceBlock;
import thaumcraft.essentia.CentrifugeBlock;
import thaumcraft.essentia.EssentiaBufferBlock;
import thaumcraft.essentia.EssentiaCrystallizerBlock;
import thaumcraft.essentia.EssentiaReservoirBlock;
import thaumcraft.essentia.FilledJarItem;
import thaumcraft.essentia.JarBlock;
import thaumcraft.essentia.TubeBlock;
import thaumcraft.infusion.InfusionMatrixBlock;
import thaumcraft.infusion.InfusionPillarBlock;
import thaumcraft.infusion.PedestalBlock;
import thaumcraft.item.ManaBeanItem;
import thaumcraft.item.NodeJarItem;
import thaumcraft.node.AuraNodeBlock;
import thaumcraft.node.EnergizedNodeBlock;
import thaumcraft.node.NodeStabilizerBlock;
import thaumcraft.node.NodeTransducerBlock;
import thaumcraft.platform.Registration;
import thaumcraft.taint.EtherealBloomBlock;
import thaumcraft.taint.TaintBlock;
import thaumcraft.taint.TaintFibresBlock;
import thaumcraft.vis.VisRelayBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Thaumcraft.MODID);

    public static final DeferredBlock<PurifyingFluidBlock> PURIFYING_FLUID = RegistryHelper.registerBlock(BLOCKS,
        "purifying_fluid",
        properties -> new PurifyingFluidBlock(ModFluids.PURIFYING_FLUID.get(), properties),
        properties -> properties.mapColor(MapColor.WATER).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).lightLevel(state -> 10).noLootTable().liquid().sound(SoundType.EMPTY)
    );
    public static final DeferredBlock<LiquidDeathBlock> LIQUID_DEATH = RegistryHelper.registerBlock(BLOCKS,
        "liquid_death",
        properties -> new LiquidDeathBlock(ModFluids.LIQUID_DEATH.get(), properties),
        properties -> properties.mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).lightLevel(state -> 8).noLootTable().liquid().sound(SoundType.EMPTY)
    );

    public static final String[] CANDLE_COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    };
    public static final int[] CANDLE_TINTS = {
        15790320, 15435844, 12801229, 6719955, 14602026, 4312372, 14188952, 4408131,
        10526880, 2651799, 8073150, 2437522, 5320730, 3887386, 11743532, 1973019,
    };

    public static final DeferredBlock<DropExperienceBlock> CINNABAR_ORE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "cinnabar_ore",
        properties -> new DropExperienceBlock(ConstantInt.of(0), properties),
        properties -> rock(properties, 1.5F, 5.0F)
    ));
    public static final DeferredBlock<InfusedStoneBlock> AIR_INFUSED_STONE = infusedStone("air_infused_stone", 0xFFFF7E);
    public static final DeferredBlock<InfusedStoneBlock> FIRE_INFUSED_STONE = infusedStone("fire_infused_stone", 0xFF3C01);
    public static final DeferredBlock<InfusedStoneBlock> WATER_INFUSED_STONE = infusedStone("water_infused_stone", 0x0090FF);
    public static final DeferredBlock<InfusedStoneBlock> EARTH_INFUSED_STONE = infusedStone("earth_infused_stone", 0x00A000);
    public static final DeferredBlock<InfusedStoneBlock> ORDER_INFUSED_STONE = infusedStone("order_infused_stone", 0xEECCFF);
    public static final DeferredBlock<InfusedStoneBlock> ENTROPY_INFUSED_STONE = infusedStone("entropy_infused_stone", 0x555577);
    public static final DeferredBlock<DropExperienceBlock> AMBER_ORE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "amber_ore",
        properties -> new DropExperienceBlock(UniformInt.of(1, 4), properties),
        properties -> rock(properties, 1.5F, 5.0F)
    ));

    public static final DeferredBlock<MagicalLogBlock> GREATWOOD_LOG = withItem(
        RegistryHelper.registerBlock(BLOCKS, "greatwood_log", MagicalLogBlock::new, ModBlocks::log),
        properties -> Registration.fuel(properties, 400)
    );
    public static final DeferredBlock<MagicalLogBlock> SILVERWOOD_LOG = withItem(
        RegistryHelper.registerBlock(BLOCKS, "silverwood_log", MagicalLogBlock::new, ModBlocks::log),
        properties -> Registration.fuel(properties, 400)
    );
    public static final DeferredBlock<MagicalLeavesBlock> GREATWOOD_LEAVES = withItem(RegistryHelper.registerBlock(BLOCKS,
        "greatwood_leaves",
        properties -> new MagicalLeavesBlock(() -> ModBlocks.GREATWOOD_SAPLING.get(), 200, false, properties),
        ModBlocks::leaves
    ));
    public static final DeferredBlock<MagicalLeavesBlock> SILVERWOOD_LEAVES = withItem(RegistryHelper.registerBlock(BLOCKS,
        "silverwood_leaves",
        properties -> new MagicalLeavesBlock(() -> ModBlocks.SILVERWOOD_SAPLING.get(), 250, true, properties),
        properties -> leaves(properties).lightLevel(state -> 7)
    ));
    public static final DeferredBlock<MagicalSaplingBlock> GREATWOOD_SAPLING = withItem(RegistryHelper.registerBlock(BLOCKS,
        "greatwood_sapling",
        properties -> new MagicalSaplingBlock(false, properties),
        properties -> plant(properties).randomTicks()
    ));
    public static final DeferredBlock<MagicalSaplingBlock> SILVERWOOD_SAPLING = withItem(RegistryHelper.registerBlock(BLOCKS,
        "silverwood_sapling",
        properties -> new MagicalSaplingBlock(true, properties),
        properties -> plant(properties).randomTicks().lightLevel(state -> 8)
    ));
    public static final DeferredBlock<CustomPlantBlock> SHIMMERLEAF = withItem(RegistryHelper.registerBlock(BLOCKS,
        "shimmerleaf",
        properties -> new CustomPlantBlock(CustomPlantBlock.Kind.SHIMMERLEAF, properties),
        properties -> plant(properties).lightLevel(state -> 8)
    ));
    public static final DeferredBlock<CustomPlantBlock> CINDERPEARL = withItem(RegistryHelper.registerBlock(BLOCKS,
        "cinderpearl",
        properties -> new CustomPlantBlock(CustomPlantBlock.Kind.CINDERPEARL, properties),
        properties -> plant(properties).lightLevel(state -> 8)
    ));
    public static final DeferredBlock<EtherealBloomBlock> ETHEREAL_BLOOM = withItem(RegistryHelper.registerBlock(BLOCKS,
        "ethereal_bloom",
        EtherealBloomBlock::new,
        properties -> plant(properties).lightLevel(state -> 15)
    ));
    public static final DeferredBlock<CustomPlantBlock> VISHROOM = withItem(RegistryHelper.registerBlock(BLOCKS,
        "vishroom",
        properties -> new CustomPlantBlock(CustomPlantBlock.Kind.VISHROOM, properties),
        properties -> plant(properties).lightLevel(state -> 8)
    ));

    public static final DeferredBlock<TaintBlock> CRUSTED_TAINT = withItem(RegistryHelper.registerBlock(BLOCKS,
        "crusted_taint",
        properties -> new TaintBlock(TaintBlock.Kind.CRUST, properties),
        properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.GRAVEL)
    ));
    public static final DeferredBlock<TaintBlock> TAINTED_SOIL = withItem(RegistryHelper.registerBlock(BLOCKS,
        "tainted_soil",
        properties -> new TaintBlock(TaintBlock.Kind.SOIL, properties),
        properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(0.6F).sound(SoundType.GRAVEL)
    ));
    public static final DeferredBlock<TaintBlock> FLESH_BLOCK = withItem(RegistryHelper.registerBlock(BLOCKS,
        "flesh_block",
        properties -> new TaintBlock(TaintBlock.Kind.FLESH, properties),
        properties -> properties.mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.SLIME_BLOCK)
    ));
    public static final DeferredBlock<TaintFibresBlock> FIBROUS_TAINT = withItem(RegistryHelper.registerBlock(BLOCKS,
        "fibrous_taint",
        properties -> new TaintFibresBlock(TaintFibresBlock.Kind.FIBRES, properties),
        properties -> plant(properties)
    ));
    public static final DeferredBlock<TaintFibresBlock> TAINTED_GRASS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "tainted_grass",
        properties -> new TaintFibresBlock(TaintFibresBlock.Kind.GRASS, properties),
        properties -> plant(properties)
    ));
    public static final DeferredBlock<TaintFibresBlock> TAINTED_PLANT = withItem(RegistryHelper.registerBlock(BLOCKS,
        "tainted_plant",
        properties -> new TaintFibresBlock(TaintFibresBlock.Kind.PLANT, properties),
        properties -> plant(properties)
    ));
    public static final DeferredBlock<TaintFibresBlock> SPORE_STALK = withItem(RegistryHelper.registerBlock(BLOCKS,
        "spore_stalk",
        properties -> new TaintFibresBlock(TaintFibresBlock.Kind.STALK, properties),
        properties -> plant(properties)
    ));
    public static final DeferredBlock<TaintFibresBlock> MATURE_SPORE_STALK = withItem(RegistryHelper.registerBlock(BLOCKS,
        "mature_spore_stalk",
        properties -> new TaintFibresBlock(TaintFibresBlock.Kind.MATURE_STALK, properties),
        properties -> plant(properties)
    ));

    public static final DeferredBlock<ManaPodBlock> MANA_POD = RegistryHelper.registerBlock(BLOCKS,
        "mana_pod",
        ManaPodBlock::new,
        properties -> properties
            .mapColor(MapColor.PLANT)
            .strength(0.5F)
            .sound(SoundType.GRASS)
            .randomTicks()
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
            .lightLevel(state -> state.getValue(ManaPodBlock.AGE))
    );
    public static final DeferredItem<ManaBeanItem> MANA_BEAN = RegistryHelper.registerItem(ModItems.ITEMS, "mana_bean", ManaBeanItem::new, ManaBeanItem::properties);

    public static final DeferredBlock<AmberBlock> AMBER_BLOCK = withItem(RegistryHelper.registerBlock(BLOCKS, "amber_block", AmberBlock::new, ModBlocks::amber));
    public static final DeferredBlock<AmberBlock> AMBER_BRICKS = withItem(RegistryHelper.registerBlock(BLOCKS, "amber_bricks", AmberBlock::new, ModBlocks::amber));

    public static final DeferredBlock<ObsidianTotemBlock> OBSIDIAN_TOTEM = withItem(RegistryHelper.registerBlock(BLOCKS,
        "obsidian_totem",
        ObsidianTotemBlock::new,
        properties -> obsidian(properties)
    ));
    public static final DeferredBlock<Block> OBSIDIAN_TILE = withItem(RegistryHelper.registerSimpleBlock(BLOCKS, "obsidian_tile", ModBlocks::obsidian));
    public static final DeferredBlock<TravelPavingStoneBlock> TRAVEL_PAVING_STONE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "travel_paving_stone",
        TravelPavingStoneBlock::new,
        properties -> noSpawn(rock(properties, 2.0F, 10.0F)).lightLevel(state -> 9)
    ));
    public static final DeferredBlock<WardingPavingStoneBlock> WARDING_PAVING_STONE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "warding_paving_stone",
        WardingPavingStoneBlock::new,
        properties -> noSpawn(rock(properties, 2.0F, 10.0F))
    ));
    public static final DeferredBlock<Block> THAUMIUM_BLOCK = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "thaumium_block",
        properties -> rock(properties, 4.0F, 20.0F).mapColor(MapColor.COLOR_PURPLE).sound(SoundType.METAL)
    ));
    public static final DeferredBlock<Block> TALLOW_BLOCK = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "tallow_block",
        properties -> rock(properties, 2.0F, 10.0F).mapColor(MapColor.TERRACOTTA_WHITE).sound(SoundType.WOOL)
    ));
    public static final DeferredBlock<Block> ARCANE_STONE = withItem(RegistryHelper.registerSimpleBlock(BLOCKS, "arcane_stone", properties -> rock(properties, 4.0F, 20.0F)));
    public static final DeferredBlock<Block> ARCANE_STONE_BRICKS = withItem(RegistryHelper.registerSimpleBlock(BLOCKS, "arcane_stone_bricks", properties -> rock(properties, 4.0F, 20.0F)));
    public static final DeferredBlock<GolemFetterBlock> GOLEM_FETTER = withItem(RegistryHelper.registerBlock(BLOCKS,
        "golem_fetter",
        GolemFetterBlock::new,
        properties -> rock(properties, 2.0F, 10.0F)
    ));
    public static final DeferredBlock<Block> ANCIENT_STONE = withItem(RegistryHelper.registerSimpleBlock(BLOCKS, "ancient_stone", properties -> rock(properties, 2.0F, 10.0F)));
    public static final DeferredBlock<Block> ANCIENT_ROCK = withItem(RegistryHelper.registerSimpleBlock(BLOCKS, "ancient_rock", properties -> rock(properties, 2.0F, 10.0F)));
    public static final DeferredBlock<Block> ANCIENT_STONE_NO_SPAWN = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "ancient_stone_no_spawn",
        properties -> noSpawn(rock(properties, 2.0F, 10.0F))
    ));
    public static final DeferredBlock<Block> CRUSTED_STONE = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "crusted_stone",
        properties -> rock(properties, 2.0F, 10.0F).lightLevel(state -> 4)
    ));
    public static final DeferredBlock<Block> ANCIENT_STONE_PEDESTAL = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "ancient_stone_pedestal",
        properties -> rock(properties, 2.0F, 10.0F)
    ));

    public static final DeferredBlock<FlammableBlock> GREATWOOD_PLANKS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "greatwood_planks",
        properties -> new FlammableBlock(20, 5, properties),
        ModBlocks::planks
    ));
    public static final DeferredBlock<FlammableBlock> SILVERWOOD_PLANKS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "silverwood_planks",
        properties -> new FlammableBlock(20, 5, properties),
        ModBlocks::planks
    ));
    public static final DeferredBlock<SlabBlock> GREATWOOD_SLAB = withItem(RegistryHelper.registerBlock(BLOCKS,
        "greatwood_slab",
        SlabBlock::new,
        properties -> wood(properties, 2.0F, 5.0F)
    ));
    public static final DeferredBlock<SlabBlock> SILVERWOOD_SLAB = withItem(RegistryHelper.registerBlock(BLOCKS,
        "silverwood_slab",
        SlabBlock::new,
        properties -> wood(properties, 2.0F, 5.0F)
    ));
    public static final DeferredBlock<SlabBlock> ARCANE_STONE_SLAB = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_stone_slab",
        SlabBlock::new,
        properties -> rock(properties, 2.0F, 10.0F)
    ));
    public static final DeferredBlock<SlabBlock> ANCIENT_STONE_SLAB = withItem(RegistryHelper.registerBlock(BLOCKS,
        "ancient_stone_slab",
        SlabBlock::new,
        properties -> rock(properties, 2.0F, 10.0F)
    ));
    public static final DeferredBlock<StairBlock> GREATWOOD_STAIRS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "greatwood_stairs",
        properties -> new StairBlock(ModBlocks.GREATWOOD_PLANKS.get().defaultBlockState(), properties),
        ModBlocks::planks
    ));
    public static final DeferredBlock<StairBlock> SILVERWOOD_STAIRS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "silverwood_stairs",
        properties -> new StairBlock(ModBlocks.SILVERWOOD_PLANKS.get().defaultBlockState(), properties),
        ModBlocks::planks
    ));
    public static final DeferredBlock<StairBlock> ARCANE_STONE_STAIRS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_stone_stairs",
        properties -> new StairBlock(ModBlocks.ARCANE_STONE_BRICKS.get().defaultBlockState(), properties),
        properties -> rock(properties, 2.0F, 10.0F)
    ));
    public static final DeferredBlock<StairBlock> ANCIENT_STONE_STAIRS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "ancient_stone_stairs",
        properties -> new StairBlock(ModBlocks.ANCIENT_STONE.get().defaultBlockState(), properties),
        properties -> rock(properties, 2.0F, 10.0F)
    ));

    public static final DeferredBlock<LootBlock> LOOT_URN = withItem(RegistryHelper.registerBlock(BLOCKS,
        "loot_urn",
        properties -> new LootBlock(LootBlock.Kind.URN, properties),
        properties -> properties.mapColor(MapColor.CLAY).strength(0.15F, 0.0F).noOcclusion()
    ));
    public static final DeferredBlock<LootBlock> LOOT_CRATE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "loot_crate",
        properties -> new LootBlock(LootBlock.Kind.CRATE, properties),
        properties -> wood(properties, 0.15F, 0.0F).noOcclusion()
    ));

    public static final DeferredBlock<AuraNodeBlock> AURA_NODE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "aura_node",
        AuraNodeBlock::new,
        properties -> airy(properties)
    ));
    public static final DeferredBlock<EnergizedNodeBlock> ENERGIZED_AURA_NODE = RegistryHelper.registerBlock(BLOCKS,
        "energized_aura_node",
        EnergizedNodeBlock::new,
        properties -> airy(properties)
    );
    public static final DeferredBlock<ChargedObsidianTotemBlock> CHARGED_OBSIDIAN_TOTEM = withItem(RegistryHelper.registerBlock(BLOCKS,
        "charged_obsidian_totem",
        ChargedObsidianTotemBlock::new,
        properties -> obsidian(properties)
    ));
    public static final DeferredBlock<SilverwoodKnotBlock> SILVERWOOD_KNOT = RegistryHelper.registerBlock(BLOCKS,
        "silverwood_knot",
        SilverwoodKnotBlock::new,
        ModBlocks::log
    );
    public static final DeferredBlock<NodeStabilizerBlock> NODE_STABILIZER = withItem(RegistryHelper.registerBlock(BLOCKS,
        "node_stabilizer",
        properties -> new NodeStabilizerBlock(false, properties),
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<NodeStabilizerBlock> ADVANCED_NODE_STABILIZER = withItem(RegistryHelper.registerBlock(BLOCKS,
        "advanced_node_stabilizer",
        properties -> new NodeStabilizerBlock(true, properties),
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<NodeTransducerBlock> NODE_TRANSDUCER = withItem(RegistryHelper.registerBlock(BLOCKS,
        "node_transducer",
        NodeTransducerBlock::new,
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<WandPedestalBlock> WAND_RECHARGE_PEDESTAL = withItem(RegistryHelper.registerBlock(BLOCKS,
        "wand_recharge_pedestal",
        WandPedestalBlock::new,
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<RechargeFocusBlock> COMPOUND_RECHARGE_FOCUS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "compound_recharge_focus",
        RechargeFocusBlock::new,
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<VisRelayBlock> VIS_RELAY = withItem(RegistryHelper.registerBlock(BLOCKS,
        "vis_relay",
        VisRelayBlock::new,
        properties -> properties
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 17.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .lightLevel(state -> state.getValue(VisRelayBlock.CONNECTED) ? 10 : 2)
    ));
    public static final DeferredBlock<NodeJarBlock> NODE_JAR = RegistryHelper.registerBlock(BLOCKS,
        "node_jar",
        NodeJarBlock::new,
        properties -> properties
            .mapColor(MapColor.NONE)
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .lightLevel(state -> 11)
    );
    public static final DeferredItem<NodeJarItem> NODE_JAR_ITEM = RegistryHelper.registerItem(ModItems.ITEMS,
        "node_jar",
        properties -> new NodeJarItem(NODE_JAR.get(), properties),
        NodeJarItem::properties
    );

    public static final DeferredBlock<TableBlock> TABLE = withItem(RegistryHelper.registerBlock(BLOCKS, "table", TableBlock::new, ModBlocks::tableProperties));
    public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = RegistryHelper.registerBlock(BLOCKS, "research_table", ResearchTableBlock::new, ModBlocks::tableProperties);
    public static final DeferredBlock<ResearchTablePartBlock> RESEARCH_TABLE_PART = RegistryHelper.registerBlock(BLOCKS, "research_table_part", ResearchTablePartBlock::new, ModBlocks::tableProperties);

    public static final DeferredBlock<JarBlock> WARDED_JAR = withItem(RegistryHelper.registerBlock(BLOCKS, "warded_jar", properties -> new JarBlock(false, properties), ModBlocks::jar));
    public static final DeferredBlock<JarBlock> VOID_JAR = withItem(RegistryHelper.registerBlock(BLOCKS, "void_jar", properties -> new JarBlock(true, properties), ModBlocks::jar));
    public static final DeferredItem<FilledJarItem> FILLED_JAR = RegistryHelper.registerItem(ModItems.ITEMS,
        "filled_jar",
        properties -> new FilledJarItem(WARDED_JAR.get(), properties),
        properties -> properties.stacksTo(1)
    );
    public static final DeferredItem<FilledJarItem> FILLED_VOID_JAR = RegistryHelper.registerItem(ModItems.ITEMS,
        "filled_void_jar",
        properties -> new FilledJarItem(VOID_JAR.get(), properties),
        properties -> properties.stacksTo(1)
    );
    public static final DeferredBlock<TubeBlock> ESSENTIA_TUBE = tube("essentia_tube", TubeBlock.Kind.TUBE);
    public static final DeferredBlock<TubeBlock> ESSENTIA_VALVE = tube("essentia_valve", TubeBlock.Kind.VALVE);
    public static final DeferredBlock<TubeBlock> FILTERED_ESSENTIA_TUBE = tube("filtered_essentia_tube", TubeBlock.Kind.FILTER);
    public static final DeferredBlock<TubeBlock> RESTRICTED_ESSENTIA_TUBE = tube("restricted_essentia_tube", TubeBlock.Kind.RESTRICT);
    public static final DeferredBlock<TubeBlock> DIRECTIONAL_ESSENTIA_TUBE = tube("directional_essentia_tube", TubeBlock.Kind.ONEWAY);
    public static final DeferredBlock<EssentiaBufferBlock> ESSENTIA_BUFFER = withItem(RegistryHelper.registerBlock(BLOCKS, "essentia_buffer", EssentiaBufferBlock::new, ModBlocks::tubeProperties));
    public static final DeferredBlock<CentrifugeBlock> ALCHEMICAL_CENTRIFUGE = withItem(RegistryHelper.registerBlock(BLOCKS, "alchemical_centrifuge", CentrifugeBlock::new, ModBlocks::tubeProperties));
    public static final DeferredBlock<EssentiaCrystallizerBlock> ESSENTIA_CRYSTALLIZER = withItem(RegistryHelper.registerBlock(BLOCKS,
        "essentia_crystallizer",
        EssentiaCrystallizerBlock::new,
        ModBlocks::tubeProperties
    ));
    public static final DeferredBlock<EssentiaReservoirBlock> ESSENTIA_RESERVOIR = withItem(RegistryHelper.registerBlock(BLOCKS,
        "essentia_reservoir",
        EssentiaReservoirBlock::new,
        properties -> metal(properties, 2.0F, 17.0F).noOcclusion()
    ));

    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = withItem(RegistryHelper.registerBlock(BLOCKS, "crucible", CrucibleBlock::new, ModBlocks::metalDevice));
    public static final DeferredBlock<AlembicBlock> ALEMBIC = withItem(RegistryHelper.registerBlock(BLOCKS, "alembic", AlembicBlock::new, ModBlocks::metalDevice));
    public static final DeferredBlock<Block> ALCHEMICAL_CONSTRUCT = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "alchemical_construct",
        properties -> metal(properties, 3.0F, 17.0F)
    ));
    public static final DeferredBlock<Block> ADVANCED_ALCHEMICAL_CONSTRUCT = withItem(RegistryHelper.registerSimpleBlock(BLOCKS,
        "advanced_alchemical_construct",
        properties -> metal(properties, 3.0F, 17.0F).lightLevel(state -> 11)
    ));
    public static final DeferredBlock<ThaumatoriumBlock> THAUMATORIUM = RegistryHelper.registerBlock(BLOCKS,
        "thaumatorium",
        properties -> new ThaumatoriumBlock(false, properties),
        ModBlocks::metalDevice
    );
    public static final DeferredBlock<ThaumatoriumBlock> THAUMATORIUM_TOP = RegistryHelper.registerBlock(BLOCKS,
        "thaumatorium_top",
        properties -> new ThaumatoriumBlock(true, properties),
        ModBlocks::metalDevice
    );
    public static final DeferredBlock<MnemonicMatrixBlock> MNEMONIC_MATRIX = withItem(RegistryHelper.registerBlock(BLOCKS,
        "mnemonic_matrix",
        MnemonicMatrixBlock::new,
        ModBlocks::metalDevice
    ));
    public static final DeferredBlock<AlchemicalFurnaceBlock> ALCHEMICAL_FURNACE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "alchemical_furnace",
        AlchemicalFurnaceBlock::new,
        properties -> rock(properties, 3.0F, 25.0F).lightLevel(AlchemicalFurnaceBlock::lightLevel)
    ));
    public static final DeferredBlock<AdvancedAlchemicalFurnaceBlock> ADVANCED_ALCHEMICAL_FURNACE = RegistryHelper.registerBlock(BLOCKS,
        "advanced_alchemical_furnace",
        AdvancedAlchemicalFurnaceBlock::new,
        properties -> metalDevice(properties).lightLevel(AdvancedAlchemicalFurnaceBlock::lightLevel)
    );
    public static final DeferredBlock<FluxGasBlock> FLUX_GAS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "flux_gas",
        FluxGasBlock::new,
        properties -> properties
            .mapColor(MapColor.COLOR_PURPLE)
            .replaceable()
            .noCollission()
            .noOcclusion()
            .strength(100.0F)
            .noLootTable()
            .randomTicks()
            .lightLevel(state -> state.getValue(FluxBlock.LEVEL) * 7 / 8)
            .pushReaction(PushReaction.DESTROY)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)
    ));
    public static final DeferredBlock<FluxGooBlock> FLUX_GOO = withItem(RegistryHelper.registerBlock(BLOCKS,
        "flux_goo",
        FluxGooBlock::new,
        properties -> properties
            .mapColor(MapColor.COLOR_PURPLE)
            .replaceable()
            .noCollission()
            .noOcclusion()
            .strength(100.0F)
            .noLootTable()
            .randomTicks()
            .lightLevel(state -> state.getValue(FluxBlock.LEVEL) * 7 / 8)
            .sound(SoundType.SLIME_BLOCK)
            .pushReaction(PushReaction.DESTROY)
    ));
    public static final DeferredBlock<FluxScrubberBlock> FLUX_SCRUBBER = withItem(RegistryHelper.registerBlock(BLOCKS,
        "flux_scrubber",
        FluxScrubberBlock::new,
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<BellowsBlock> ARCANE_BELLOWS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_bellows",
        BellowsBlock::new,
        properties -> wood(properties, 2.5F, 10.0F).noOcclusion()
    ));

    public static final DeferredBlock<ArcaneWorkbenchBlock> ARCANE_WORKTABLE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_worktable",
        ArcaneWorkbenchBlock::new,
        ModBlocks::tableProperties
    ));
    public static final DeferredBlock<DeconstructionTableBlock> DECONSTRUCTION_TABLE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "deconstruction_table",
        DeconstructionTableBlock::new,
        ModBlocks::tableProperties
    ));
    public static final DeferredBlock<VisChargeRelayBlock> VIS_CHARGE_RELAY = withItem(RegistryHelper.registerBlock(BLOCKS,
        "vis_charge_relay",
        VisChargeRelayBlock::new,
        ModBlocks::metalDevice
    ));

    public static final DeferredBlock<InfusionMatrixBlock> RUNIC_MATRIX = withItem(RegistryHelper.registerBlock(BLOCKS,
        "runic_matrix",
        InfusionMatrixBlock::new,
        properties -> stoneDevice(properties).lightLevel(state -> 10)
    ));
    public static final DeferredBlock<PedestalBlock> ARCANE_PEDESTAL = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_pedestal",
        PedestalBlock::new,
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<InfusionPillarBlock> INFUSION_PILLAR = RegistryHelper.registerBlock(BLOCKS,
        "infusion_pillar",
        properties -> new InfusionPillarBlock(false, properties),
        properties -> stoneDevice(properties)
    );
    public static final DeferredBlock<InfusionPillarBlock> INFUSION_PILLAR_TOP = RegistryHelper.registerBlock(BLOCKS,
        "infusion_pillar_top",
        properties -> new InfusionPillarBlock(true, properties),
        properties -> stoneDevice(properties)
    );

    public static final Map<String, DeferredBlock<CandleBlock>> CANDLES = candles();

    public static final DeferredBlock<FocalManipulatorBlock> FOCAL_MANIPULATOR = withItem(RegistryHelper.registerBlock(BLOCKS,
        "focal_manipulator",
        FocalManipulatorBlock::new,
        properties -> stoneDevice(properties)
    ));
    public static final DeferredBlock<HoleBlock> HOLE = RegistryHelper.registerBlock(BLOCKS,
        "hole",
        HoleBlock::new,
        properties -> properties.mapColor(MapColor.NONE).strength(-1.0F, 6000000.0F).lightLevel(state -> 10).noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK)
    );
    public static final DeferredBlock<WardedBlock> WARDED_BLOCK = RegistryHelper.registerBlock(BLOCKS,
        "warded_block",
        WardedBlock::new,
        properties -> properties.mapColor(MapColor.STONE).sound(SoundType.STONE).strength(-1.0F, 999.0F).noLootTable().isValidSpawn(Blocks::never).pushReaction(PushReaction.BLOCK)
    );

    public static final DeferredBlock<WardedGlassBlock> WARDED_GLASS = withItem(RegistryHelper.registerBlock(BLOCKS,
        "warded_glass",
        WardedGlassBlock::new,
        properties -> properties.mapColor(MapColor.NONE).strength(-1.0F, 999.0F).sound(SoundType.STONE).noOcclusion()
    ));
    public static final DeferredBlock<ArcaneDoorBlock> ARCANE_DOOR = RegistryHelper.registerBlock(BLOCKS,
        "arcane_door",
        ArcaneDoorBlock::new,
        properties -> properties.mapColor(MapColor.METAL).strength(-1.0F, 999.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
    );
    public static final DeferredItem<ArcaneDoorItem> ARCANE_DOOR_ITEM = RegistryHelper.registerItem(ModItems.ITEMS,
        "arcane_door",
        ArcaneDoorItem::new,
        properties -> properties.stacksTo(1)
    );
    public static final DeferredBlock<ArcanePressurePlateBlock> ARCANE_PRESSURE_PLATE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_pressure_plate",
        ArcanePressurePlateBlock::new,
        properties -> properties
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(-1.0F, 999.0F)
            .sound(SoundType.WOOD)
            .noCollission()
            .pushReaction(PushReaction.DESTROY)
    ));
    public static final DeferredBlock<HungryChestBlock> HUNGRY_CHEST = withItem(RegistryHelper.registerBlock(BLOCKS,
        "hungry_chest",
        HungryChestBlock::new,
        properties -> wood(properties, 2.5F, 12.5F).noOcclusion()
    ));
    public static final DeferredBlock<BrainJarBlock> BRAIN_JAR = withItem(RegistryHelper.registerBlock(BLOCKS, "brain_jar", BrainJarBlock::new, ModBlocks::jar));
    public static final DeferredBlock<BannerBlock> BANNER = RegistryHelper.registerBlock(BLOCKS,
        "banner",
        BannerBlock::new,
        properties -> wood(properties, 2.5F, 12.5F).noCollission().noOcclusion()
    );
    public static final DeferredItem<BannerItem> BANNER_ITEM = RegistryHelper.registerItem(ModItems.ITEMS,
        "banner",
        properties -> new BannerItem(BANNER.get(), properties),
        properties -> properties
    );
    public static final DeferredBlock<InfernalFurnaceBlock> INFERNAL_FURNACE = RegistryHelper.registerBlock(BLOCKS,
        "infernal_furnace",
        InfernalFurnaceBlock::new,
        properties -> rock(properties, 10.0F, 500.0F).lightLevel(InfernalFurnaceBlock::lightLevel)
    );
    public static final DeferredBlock<ArcaneBoreBaseBlock> ARCANE_BORE_BASE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_bore_base",
        ArcaneBoreBaseBlock::new,
        properties -> wood(properties, 2.5F, 10.0F).noOcclusion()
    ));
    public static final DeferredBlock<ArcaneBoreBlock> ARCANE_BORE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_bore",
        ArcaneBoreBlock::new,
        properties -> wood(properties, 2.5F, 10.0F).noOcclusion().dynamicShape()
    ));
    public static final DeferredBlock<ArcaneSpaBlock> ARCANE_SPA = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_spa",
        ArcaneSpaBlock::new,
        properties -> stoneDevice(properties)
    ));

    public static final DeferredBlock<thaumcraft.device.NitorLightBlock> NITOR_LIGHT = RegistryHelper.registerBlock(BLOCKS,
        "nitor_light",
        thaumcraft.device.NitorLightBlock::new,
        thaumcraft.device.DeviceProperties::nitorLight
    );
    public static final DeferredBlock<thaumcraft.device.ArcaneLampLightBlock> ARCANE_LAMP_LIGHT = RegistryHelper.registerBlock(BLOCKS,
        "arcane_lamp_light",
        thaumcraft.device.ArcaneLampLightBlock::new,
        thaumcraft.device.DeviceProperties::lampLight
    );
    public static final DeferredBlock<thaumcraft.device.WardingAuraBlock> WARDING_AURA = RegistryHelper.registerBlock(BLOCKS,
        "warding_aura",
        thaumcraft.device.WardingAuraBlock::new,
        thaumcraft.device.DeviceProperties::wardingAura
    );
    public static final DeferredBlock<thaumcraft.block.EldritchFieldBlock> STATIC_FIELD = eldritchField("static_field", 10);
    public static final DeferredBlock<thaumcraft.block.EldritchFieldBlock> SAPPING_FIELD = eldritchField("sapping_field", 11);
    public static final DeferredBlock<thaumcraft.block.EldritchFieldBlock> IMPASSABLE_FIELD = eldritchField("impassable_field", 12);
    public static final DeferredBlock<thaumcraft.device.ArcaneLampBlock> ARCANE_LAMP = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_lamp",
        thaumcraft.device.ArcaneLampBlock::new,
        properties -> thaumcraft.device.DeviceProperties.metalDevice(properties).lightLevel(state -> 15)
    ));
    public static final DeferredBlock<thaumcraft.device.ChargedLampBlock> LAMP_OF_GROWTH = withItem(RegistryHelper.registerBlock(BLOCKS,
        "lamp_of_growth",
        properties -> new thaumcraft.device.ChargedLampBlock(true, properties),
        properties -> thaumcraft.device.DeviceProperties.metalDevice(properties).lightLevel(state -> state.getValue(thaumcraft.device.ChargedLampBlock.LIT) ? 15 : 8)
    ));
    public static final DeferredBlock<thaumcraft.device.ChargedLampBlock> LAMP_OF_FERTILITY = withItem(RegistryHelper.registerBlock(BLOCKS,
        "lamp_of_fertility",
        properties -> new thaumcraft.device.ChargedLampBlock(false, properties),
        properties -> thaumcraft.device.DeviceProperties.metalDevice(properties).lightLevel(state -> state.getValue(thaumcraft.device.ChargedLampBlock.LIT) ? 15 : 8)
    ));
    public static final DeferredBlock<thaumcraft.device.ItemGrateBlock> ITEM_GRATE = withItem(RegistryHelper.registerBlock(BLOCKS,
        "item_grate",
        thaumcraft.device.ItemGrateBlock::new,
        thaumcraft.device.DeviceProperties::metalDevice
    ));
    public static final DeferredBlock<thaumcraft.device.ArcaneEarBlock> ARCANE_EAR = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_ear",
        thaumcraft.device.ArcaneEarBlock::new,
        properties -> thaumcraft.device.DeviceProperties.woodDevice(properties, 10.0F).noOcclusion()
    ));
    public static final DeferredBlock<thaumcraft.device.ArcaneLevitatorBlock> ARCANE_LEVITATOR = withItem(RegistryHelper.registerBlock(BLOCKS,
        "arcane_levitator",
        thaumcraft.device.ArcaneLevitatorBlock::new,
        properties -> thaumcraft.device.DeviceProperties.woodDevice(properties, 15.0F).noOcclusion()
    ));
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> AIR_CRYSTAL_CLUSTER = crystalCluster("air_crystal_cluster", 0);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> FIRE_CRYSTAL_CLUSTER = crystalCluster("fire_crystal_cluster", 1);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> WATER_CRYSTAL_CLUSTER = crystalCluster("water_crystal_cluster", 2);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> EARTH_CRYSTAL_CLUSTER = crystalCluster("earth_crystal_cluster", 3);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> ORDER_CRYSTAL_CLUSTER = crystalCluster("order_crystal_cluster", 4);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> ENTROPY_CRYSTAL_CLUSTER = crystalCluster("entropy_crystal_cluster", 5);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> MIXED_CRYSTAL_CLUSTER = crystalCluster("mixed_crystal_cluster", 6);
    public static final DeferredBlock<thaumcraft.device.CrystalClusterBlock> STRANGE_CRYSTALS = crystalCluster("strange_crystals", 7);
    public static final DeferredBlock<thaumcraft.device.MirrorBlock> MAGIC_MIRROR = RegistryHelper.registerBlock(BLOCKS,
        "magic_mirror",
        properties -> new thaumcraft.device.MirrorBlock(false, properties),
        properties -> properties.mapColor(MapColor.NONE).noCollission().noOcclusion().strength(1.0F, 10.0F).pushReaction(PushReaction.BLOCK)
    );
    public static final DeferredBlock<thaumcraft.device.MirrorBlock> ESSENTIA_MIRROR = RegistryHelper.registerBlock(BLOCKS,
        "essentia_mirror",
        properties -> new thaumcraft.device.MirrorBlock(true, properties),
        properties -> properties.mapColor(MapColor.NONE).noCollission().noOcclusion().strength(1.0F, 10.0F).pushReaction(PushReaction.BLOCK)
    );
    public static final DeferredItem<thaumcraft.device.MirrorItem> MAGIC_MIRROR_ITEM = RegistryHelper.registerItem(ModItems.ITEMS,
        "magic_mirror",
        properties -> new thaumcraft.device.MirrorItem(MAGIC_MIRROR.get(), properties),
        properties -> properties.rarity(net.minecraft.world.item.Rarity.UNCOMMON)
    );
    public static final DeferredItem<thaumcraft.device.MirrorItem> ESSENTIA_MIRROR_ITEM = RegistryHelper.registerItem(ModItems.ITEMS,
        "essentia_mirror",
        properties -> new thaumcraft.device.MirrorItem(ESSENTIA_MIRROR.get(), properties),
        properties -> properties.rarity(net.minecraft.world.item.Rarity.UNCOMMON)
    );

    private static DeferredBlock<thaumcraft.device.CrystalClusterBlock> crystalCluster(String name, int kind) {
        return withItem(RegistryHelper.registerBlock(BLOCKS,
            name,
            properties -> new thaumcraft.device.CrystalClusterBlock(kind, properties),
            thaumcraft.device.DeviceProperties::crystal
        ));
    }


    private ModBlocks() {
    }

    private static Map<String, DeferredBlock<CandleBlock>> candles() {
        Map<String, DeferredBlock<CandleBlock>> candles = new LinkedHashMap<>();
        for (String color : CANDLE_COLORS) {
            candles.put(color, withItem(RegistryHelper.registerBlock(BLOCKS,
                color + "_tallow_candle",
                CandleBlock::new,
                properties -> properties
                    .mapColor(MapColor.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.1F)
                    .sound(SoundType.WOOL)
                    .lightLevel(state -> 14)
                    .pushReaction(PushReaction.DESTROY)
            )));
        }
        return candles;
    }

    private static DeferredBlock<TubeBlock> tube(String name, TubeBlock.Kind kind) {
        return withItem(RegistryHelper.registerBlock(BLOCKS, name, properties -> new TubeBlock(kind, properties), ModBlocks::tubeProperties));
    }

    private static BlockBehaviour.Properties metal(BlockBehaviour.Properties properties, float hardness, float resistance) {
        return properties.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(hardness, resistance).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties metalDevice(BlockBehaviour.Properties properties) {
        return metal(properties, 3.0F, 17.0F).noOcclusion();
    }

    private static BlockBehaviour.Properties tubeProperties(BlockBehaviour.Properties properties) {
        return metal(properties, 0.5F, 5.0F).noOcclusion();
    }

    private static BlockBehaviour.Properties jar(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 9)
            .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties tableProperties(BlockBehaviour.Properties properties) {
        return properties
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .ignitedByLava();
    }

    private static DeferredBlock<thaumcraft.block.EldritchFieldBlock> eldritchField(String name, int kind) {
        return RegistryHelper.registerBlock(BLOCKS, name, properties -> new thaumcraft.block.EldritchFieldBlock(kind, properties), properties -> {
            properties.noOcclusion().noLootTable().strength(kind == 12 ? -1.0F : 100.0F, kind == 12 ? Float.MAX_VALUE : 50.0F).lightLevel(state -> kind == 12 ? 0 : 8);
            if (kind != 12) {
                properties.replaceable().randomTicks();
            }
            return properties;
        });
    }

    private static <B extends Block> DeferredBlock<B> withItem(DeferredBlock<B> block) {
        RegistryHelper.registerSimpleBlockItem(ModItems.ITEMS, block);
        return block;
    }

    private static <B extends Block> DeferredBlock<B> withItem(
        DeferredBlock<B> block,
        UnaryOperator<Item.Properties> itemProperties
    ) {
        RegistryHelper.registerSimpleBlockItem(ModItems.ITEMS, block, itemProperties);
        return block;
    }

    private static DeferredBlock<InfusedStoneBlock> infusedStone(String name, int color) {
        return withItem(RegistryHelper.registerBlock(BLOCKS, name, properties -> new InfusedStoneBlock(color, properties), properties -> rock(properties, 1.5F, 5.0F)));
    }

    private static BlockBehaviour.Properties rock(BlockBehaviour.Properties properties, float hardness, float resistance) {
        return properties
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(hardness, resistance)
            .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties noSpawn(BlockBehaviour.Properties properties) {
        return properties.isValidSpawn((state, level, pos, type) -> false);
    }

    private static BlockBehaviour.Properties obsidian(BlockBehaviour.Properties properties) {
        return rock(properties, 30.0F, 999.0F).mapColor(MapColor.COLOR_BLACK);
    }

    private static BlockBehaviour.Properties stoneDevice(BlockBehaviour.Properties properties) {
        return rock(properties, 3.0F, 25.0F).noOcclusion();
    }

    private static BlockBehaviour.Properties airy(BlockBehaviour.Properties properties) {
        return properties
            .mapColor(MapColor.NONE)
            .noCollission()
            .noOcclusion()
            .strength(2.0F, 200.0F)
            .sound(SoundType.WOOL)
            .lightLevel(state -> 8)
            .randomTicks()
            .pushReaction(PushReaction.BLOCK)
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties amber(BlockBehaviour.Properties properties) {
        return rock(properties, 1.5F, 5.0F).mapColor(MapColor.COLOR_ORANGE).noOcclusion();
    }

    private static BlockBehaviour.Properties wood(BlockBehaviour.Properties properties, float hardness, float resistance) {
        return properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(hardness, resistance).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties planks(BlockBehaviour.Properties properties) {
        return wood(properties, 2.5F, 10.0F);
    }

    private static BlockBehaviour.Properties log(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties leaves(BlockBehaviour.Properties properties) {
        return properties
            .mapColor(MapColor.PLANT)
            .strength(0.2F)
            .randomTicks()
            .sound(SoundType.GRASS)
            .noOcclusion()
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties plant(BlockBehaviour.Properties properties) {
        return properties
            .mapColor(MapColor.PLANT)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.NONE)
            .pushReaction(PushReaction.DESTROY);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
