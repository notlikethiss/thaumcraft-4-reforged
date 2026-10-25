package thaumcraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.Thaumcraft;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WorldFxRenderer {
    private static final List<WorldFx> EFFECTS = new ArrayList<>();

    private WorldFxRenderer() {
    }

    public static void add(WorldFx fx) {
        EFFECTS.add(fx);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            EFFECTS.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        EFFECTS.removeIf(fx -> {
            fx.tick();
            return fx.isDead();
        });
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack.Pose pose = event.getPoseStack().last();
        Camera cameraState = event.getCamera();
        Vec3 camera = cameraState.getPosition();
        float yaw = cameraState.getYRot();
        float pitch = cameraState.getXRot();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Set<RenderType> used = new LinkedHashSet<>();
        for (WorldFx fx : List.copyOf(EFFECTS)) {
            RenderType renderType = TcRenderTypes.byBlend(fx.texture(), fx.blendMode(), fx.depthTest());
            if (!fx.depthTest()) {
                SeeThroughGeometry.submit(event.getPoseStack(), renderType, (seeThroughPose, buffer) -> fx.render(new VertexQuadSink(seeThroughPose, buffer), partialTick, camera, yaw, pitch));
                continue;
            }
            used.add(renderType);
            fx.render(new VertexQuadSink(pose, bufferSource.getBuffer(renderType)), partialTick, camera, yaw, pitch);
        }
        for (RenderType renderType : used) {
            bufferSource.endBatch(renderType);
        }
    }
}
