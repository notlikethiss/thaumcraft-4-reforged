package thaumcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import thaumcraft.aspect.AddonAspects;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.crafting.ConfigRecipes;
import thaumcraft.device.WardedKeys;
import thaumcraft.essentia.EssentiaKeys;
import thaumcraft.infusion.InfusionKeys;
import thaumcraft.item.NodeJarKeys;
import thaumcraft.item.tool.ToolKeys;
import thaumcraft.network.ModPayloads;
import thaumcraft.platform.Platform;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModCapabilities;
import thaumcraft.registry.ModCreativeTabs;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModEffects;
import thaumcraft.registry.ModFeatures;
import thaumcraft.registry.ModFluids;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModMenus;
import thaumcraft.registry.ModSounds;
import thaumcraft.registry.ModVillagers;
import thaumcraft.entity.monster.ChampionKeys;
import thaumcraft.research.ConfigResearch;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ResearchNoteKeys;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.focus.FocusKeys;
import thaumcraft.item.armor.ArmorKeys;
import thaumcraft.registry.ModRecipes;

@Mod(Thaumcraft.MODID)
public class Thaumcraft {
    public static final String MODID = "thaumcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Thaumcraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        thaumcraft.entity.golem.GolemKeys.init();
        AspectKeys.init();
        WandKeys.init();
        FocusKeys.init();
        ToolKeys.init();
        NodeJarKeys.init();
        EssentiaKeys.init();
        InfusionKeys.init();
        ArmorKeys.init();
        ResearchNoteKeys.init();
        Knowledge.init();
        WardedKeys.init();
        ChampionKeys.init();
        thaumcraft.device.MirrorKeys.init();
        Platform.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        thaumcraft.registry.EldritchContent.register(modEventBus);
        thaumcraft.registry.EldritchEntities.register(modEventBus);
        thaumcraft.registry.EldritchWorld.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        ModFeatures.register(modEventBus);
        thaumcraft.world.gen.HalloweenFirebatSpawnModifier.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCapabilities.register();
        ModPayloads.register();
        ConfigRecipes.init();
        ConfigResearch.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        AddonAspects.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
