package thaumcraft.client.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.entity.golem.Marker;
import thaumcraft.item.golem.GolemBellItem;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class GolemMarkerOverlay {
    private static final Identifier MARKER = Thaumcraft.id("textures/misc/mark.png");
    private static final Identifier HOME = Thaumcraft.id("textures/misc/home.png");
    private static final int[] COLORS = {
        0xFFF0F0F0, 0xFFEB8844, 0xFFC354CD, 0xFF6689D3, 0xFFDECF2A, 0xFF41CD34, 0xFFD88198, 0xFF434343,
        0xFFA0A0A0, 0xFF287697, 0xFF7B2FBE, 0xFF253192, 0xFF51301A, 0xFF3B511A, 0xFFB3312C, 0xFF1E1B1B
    };

    private GolemMarkerOverlay() {
    }

    @SubscribeEvent
    static void onSubmit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GolemBellItem)) {
            return;
        }
        GolemKeys.BellSelection selection = GolemKeys.BELL.get(stack);
        if (selection == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        List<Overlay> overlays = new ArrayList<>();
        if (selection.home().dimension().equals(minecraft.level.dimension()) && camera.distanceToSqr(Vec3.atCenterOf(selection.home().pos())) < 4096.0) {
            Marker home = selection.homeMarker();
            overlays.add(new Overlay(home.pos(), home.side(), home.color(), true));
        }
        for (Marker marker : selection.markers()) {
            if (marker.inLevel(minecraft.level) && camera.distanceToSqr(Vec3.atCenterOf(marker.pos())) < 4096.0) {
                overlays.add(new Overlay(marker.pos(), marker.side(), marker.color(), false));
            }
        }
        if (overlays.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        long ticks = System.nanoTime() / 30_000_000L;
        if (overlays.stream().anyMatch(overlay -> !overlay.home())) {
            event.getSubmitNodeCollector().submitCustomGeometry(poseStack, TcRenderTypes.additive(MARKER), (pose, buffer) -> {
                QuadSink sink = new VertexQuadSink(pose, buffer);
                for (Overlay overlay : overlays) {
                    if (!overlay.home()) {
                        draw(sink, overlay, camera, ticks, 0.2F);
                    }
                }
            });
        }
        if (overlays.stream().anyMatch(Overlay::home)) {
            event.getSubmitNodeCollector().submitCustomGeometry(poseStack, TcRenderTypes.additive(HOME), (pose, buffer) -> {
                QuadSink sink = new VertexQuadSink(pose, buffer);
                for (Overlay overlay : overlays) {
                    if (overlay.home()) {
                        draw(sink, overlay, camera, ticks, 0.325F);
                    }
                }
            });
        }
    }

    private static void draw(QuadSink sink, Overlay overlay, Vec3 camera, long ticks, float size) {
        var pos = overlay.pos();
        Direction side = overlay.side();
        float pulse = 0.8F + Mth.sin(ticks / 12.0F + side.ordinal()) * 0.2F;
        int tint = overlay.color() >= 0 && overlay.color() < COLORS.length ? COLORS[overlay.color()] : 0xFF000000 | (int) (pulse * 255.0F) << 16 | (int) (pulse * 255.0F) << 8 | (int) (pulse * 255.0F);
        float nx = side.getStepX();
        float ny = side.getStepY();
        float nz = side.getStepZ();
        float centerX = (float) (pos.getX() + 0.5 + nx * 0.51 - camera.x);
        float centerY = (float) (pos.getY() + 0.5 + ny * 0.51 - camera.y);
        float centerZ = (float) (pos.getZ() + 0.5 + nz * 0.51 - camera.z);
        float half = size / 2.0F;
        float ax = side.getAxis() == Direction.Axis.Z || side.getAxis() == Direction.Axis.Y ? half : 0.0F;
        float ay = side.getAxis() == Direction.Axis.X ? half : 0.0F;
        float az = 0.0F;
        float bx = 0.0F;
        float by = side.getAxis() == Direction.Axis.Z ? half : 0.0F;
        float bz = side.getAxis() == Direction.Axis.X || side.getAxis() == Direction.Axis.Y ? half : 0.0F;
        float x0 = centerX - ax - bx;
        float y0 = centerY - ay - by;
        float z0 = centerZ - az - bz;
        float x1 = centerX + ax - bx;
        float y1 = centerY + ay - by;
        float z1 = centerZ + az - bz;
        float x2 = centerX + ax + bx;
        float y2 = centerY + ay + by;
        float z2 = centerZ + az + bz;
        float x3 = centerX - ax + bx;
        float y3 = centerY - ay + by;
        float z3 = centerZ - az + bz;
        sink.vertex(x0, y0, z0, 0.0F, 1.0F, tint, 0xF000F0);
        sink.vertex(x1, y1, z1, 1.0F, 1.0F, tint, 0xF000F0);
        sink.vertex(x2, y2, z2, 1.0F, 0.0F, tint, 0xF000F0);
        sink.vertex(x3, y3, z3, 0.0F, 0.0F, tint, 0xF000F0);
    }

    private record Overlay(net.minecraft.core.BlockPos pos, Direction side, int color, boolean home) {
    }
}
