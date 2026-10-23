package thaumcraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.ClientFx;
import thaumcraft.client.platform.IrisCompat;
import thaumcraft.client.platform.ModRenderPipelines;
import thaumcraft.client.platform.NodeRenderTypes;
import thaumcraft.client.research.ResearchBrowserScreen;
import thaumcraft.client.research.ThaumometerClient;
import thaumcraft.client.fx.MachineClientFx;
import thaumcraft.client.screen.AlchemicalFurnaceScreen;
import thaumcraft.client.screen.ArcaneBoreScreen;
import thaumcraft.client.screen.ArcaneSpaScreen;
import thaumcraft.client.screen.ArcaneWorkbenchScreen;
import thaumcraft.client.screen.DeconstructionTableScreen;
import thaumcraft.client.screen.FocalManipulatorScreen;
import thaumcraft.client.screen.FocusPouchScreen;
import thaumcraft.client.screen.HoverHarnessScreen;
import thaumcraft.client.screen.ResearchTableScreen;
import thaumcraft.client.screen.ThaumatoriumScreen;
import thaumcraft.client.screen.PechTradeScreen;
import thaumcraft.device.MachineFx;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModMenus;
import thaumcraft.research.ResearchClientHooks;

@Mod(value = Thaumcraft.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumcraftClient {
    public ThaumcraftClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        ClientPayloadHandlers.register();
        Fx.set(new ClientFx());
        MachineFx.set(new MachineClientFx());
        ResearchClientHooks.setBookOpener(ResearchBrowserScreen::open);
        ThaumometerClient.register();
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
        event.register(ModMenus.ARCANE_WORKBENCH.get(), ArcaneWorkbenchScreen::new);
        event.register(ModMenus.DECONSTRUCTION_TABLE.get(), DeconstructionTableScreen::new);
        event.register(ModMenus.ALCHEMICAL_FURNACE.get(), AlchemicalFurnaceScreen::new);
        event.register(ModMenus.THAUMATORIUM.get(), ThaumatoriumScreen::new);
        event.register(ModMenus.FOCAL_MANIPULATOR.get(), FocalManipulatorScreen::new);
        event.register(ModMenus.FOCUS_POUCH.get(), FocusPouchScreen::new);
        event.register(ModMenus.HOVER_HARNESS.get(), HoverHarnessScreen::new);
        event.register(ModMenus.ARCANE_BORE.get(), ArcaneBoreScreen::new);
        event.register(ModMenus.ARCANE_SPA.get(), ArcaneSpaScreen::new);
        event.register(ModMenus.PECH_TRADE.get(), PechTradeScreen::new);
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("iris")) {
            event.enqueueWork(IrisCompat::register);
        }
    }

    @SubscribeEvent
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ModRenderPipelines.ELDRITCH_PORTAL);
        event.registerPipeline(ModRenderPipelines.ADDITIVE_PARTICLE);
        event.registerPipeline(ModRenderPipelines.TRANSLUCENT_PARTICLE);
        event.registerPipeline(ModRenderPipelines.GUI_ADDITIVE);
        event.registerPipeline(ModRenderPipelines.GUI_TEXTURED_ADDITIVE);
        NodeRenderTypes.forEachPipeline(event::registerPipeline);
    }
}
