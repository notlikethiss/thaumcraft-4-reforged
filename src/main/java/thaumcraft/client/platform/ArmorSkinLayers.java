package thaumcraft.client.platform;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.render.armor.ArmorModels;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ArmorSkinLayers {
    private ArmorSkinLayers() {
    }

    @SubscribeEvent
    static void hideCoveredLayers(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        if (ArmorModels.hasModel(state.headEquipment)) {
            state.showHat = false;
        }
        if (ArmorModels.hasModel(state.chestEquipment)) {
            state.showJacket = false;
            state.showLeftSleeve = false;
            state.showRightSleeve = false;
        }
        if (ArmorModels.hasModel(state.legsEquipment)) {
            state.showLeftPants = false;
            state.showRightPants = false;
        }
    }
}
