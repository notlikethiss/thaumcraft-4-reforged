package thaumcraft.client.render;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import thaumcraft.Thaumcraft;
import thaumcraft.client.hud.WandHud;
import thaumcraft.client.hud.GearHud;
import thaumcraft.client.wand.FocusRadial;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ItemRenderers {
    private static final List<String> CUSTOM_RENDERED = List.of(
        "wand",
        "thaumometer",
        "aura_node",
        "node_jar",
        "node_stabilizer",
        "advanced_node_stabilizer",
        "node_transducer",
        "vis_relay",
        "vis_charge_relay",
        "runic_matrix",
        "alchemical_centrifuge",
        "essentia_crystallizer",
        "essentia_reservoir",
        "filled_jar",
        "filled_void_jar",
        "alembic",
        "flux_scrubber",
        "arcane_bellows",
        "essentia_valve",
        "air_crystal_cluster",
        "fire_crystal_cluster",
        "water_crystal_cluster",
        "earth_crystal_cluster",
        "order_crystal_cluster",
        "entropy_crystal_cluster",
        "mixed_crystal_cluster",
        "strange_crystals",
        "hungry_chest",
        "brain_jar",
        "banner",
        "arcane_bore_base",
        "arcane_bore"
    );
    private static TcItemRenderer renderer;

    private ItemRenderers() {
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new TcItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        };
        for (String path : CUSTOM_RENDERED) {
            Item item = BuiltInRegistries.ITEM.get(Thaumcraft.id(path));
            if (item != Items.AIR) {
                event.registerItem(extensions, item);
            }
        }
    }

    @SubscribeEvent
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TcItemRenderer.VALVE_BASE);
        event.register(TcItemRenderer.BRAIN_JAR_BASE);
    }

    @SubscribeEvent
    static void wrapWandModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) -> id.id().equals(Thaumcraft.id("wand")) ? new WandItemModel(model) : model);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Thaumcraft.id("wand_hud"), WandHud::render);
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Thaumcraft.id("gear_hud"), GearHud::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Thaumcraft.id("focus_radial"), FocusRadial::render);
    }
}
