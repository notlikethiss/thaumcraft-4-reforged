package thaumcraft.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import thaumcraft.Thaumcraft;
import thaumcraft.client.hud.GearHud;
import thaumcraft.client.hud.SanityHud;
import thaumcraft.client.hud.WandHud;
import thaumcraft.client.wand.FocusRadial;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ItemRenderers {
    private ItemRenderers() {
    }

    @SubscribeEvent
    static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Thaumcraft.id("wand"), WandSpecialRenderer.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("aura_node"), NodeItemRenderer.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("node_jar"), NodeJarItemRenderer.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("thaumometer"), ThaumometerItemRenderer.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("filled_jar"), EssentiaSpecialRenderers.FilledJar.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("centrifuge"), EssentiaSpecialRenderers.Centrifuge.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("essentia_crystallizer"), EssentiaSpecialRenderers.Crystallizer.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("essentia_reservoir"), EssentiaSpecialRenderers.Reservoir.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("tube_valve"), EssentiaSpecialRenderers.TubeValve.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("alembic"), AlchemySpecialRenderers.Alembic.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("flux_scrubber"), AlchemySpecialRenderers.FluxScrubber.Unbaked.MAP_CODEC);
        event.register(Thaumcraft.id("arcane_bellows"), AlchemySpecialRenderers.Bellows.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(Thaumcraft.id("special_owner"), OwnerSpecialModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Thaumcraft.id("wand_hud"), WandHud::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Thaumcraft.id("sanity_hud"), SanityHud::render);
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Thaumcraft.id("gear_hud"), GearHud::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Thaumcraft.id("focus_radial"), FocusRadial::render);
    }
}
