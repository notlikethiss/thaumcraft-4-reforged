package thaumcraft.client.render.armor;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import javax.annotation.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.BoltRenderer;
import thaumcraft.client.geom.LightningBolt;
import thaumcraft.item.armor.Hover;
import thaumcraft.item.armor.HoverHarnessItem;
import thaumcraft.registry.ModItems;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class HoverHarnessEffects {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(Thaumcraft.id("armor_hover_harness"), "main");
    private static final Map<Integer, Long> SHOCK_TIMES = new HashMap<>();
    private static @Nullable HumanoidModel<LivingEntity> model;

    private HoverHarnessEffects() {
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.6F)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, HoverHarnessEffects::createLayer);
    }

    @SubscribeEvent
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        model = new HumanoidModel<>(event.getEntityModels().bakeLayer(LAYER));
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HoverHarnessLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (model == null) {
                    model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER));
                }
                return model;
            }
        }, ModItems.HOVER_HARNESS.get());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            SHOCK_TIMES.clear();
            return;
        }
        long time = System.currentTimeMillis();
        RandomSource random = level.getRandom();
        for (Player player : level.players()) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof HoverHarnessItem) || !Hover.isHovering(chest)) {
                continue;
            }
            Long next = SHOCK_TIMES.get(player.getId());
            if (next != null && next >= time) {
                continue;
            }
            SHOCK_TIMES.put(player.getId(), time + 50L + random.nextInt(50));
            float mod = player.isCrouching() ? 0.075F : 0.0F;
            double y = player.getY() + 1.62 - 0.45F - mod;
            float yaw = player.yBodyRot - 90.0F - random.nextInt(180);
            float pitch = -80 + random.nextInt(160);
            float cosYaw = Mth.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
            float sinYaw = Mth.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
            float cosPitch = -Mth.cos(-pitch * (float) (Math.PI / 180.0));
            float sinPitch = Mth.sin(-pitch * (float) (Math.PI / 180.0));
            Vec3 start = new Vec3(player.getX(), y, player.getZ());
            Vec3 end = start.add(sinYaw * cosPitch * 6.0, sinPitch * 6.0, cosYaw * cosPitch * 6.0);
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            float back = (player.yBodyRot + 90.0F) / 180.0F * (float) Math.PI;
            BoltRenderer.add(new LightningBolt(
                player.getX() - Mth.cos(back) * 0.5F,
                y,
                player.getZ() - Mth.sin(back) * 0.5F,
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                random.nextLong(),
                1,
                2.0F,
                3
            ).defaultFractal().setType(6).setWidth(0.015F));
        }
    }
}
