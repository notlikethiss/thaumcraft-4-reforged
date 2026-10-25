package thaumcraft.client.platform;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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
    static void hideCoveredLayers(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();
        if (ArmorModels.hasModel(player.getItemBySlot(EquipmentSlot.HEAD))) {
            model.hat.visible = false;
        }
        if (ArmorModels.hasModel(player.getItemBySlot(EquipmentSlot.CHEST))) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (ArmorModels.hasModel(player.getItemBySlot(EquipmentSlot.LEGS))) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }
}
