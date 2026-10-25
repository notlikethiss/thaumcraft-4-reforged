package thaumcraft.client.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import thaumcraft.Thaumcraft;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class SeeThroughGeometry {
    private static final List<Consumer<MultiBufferSource.BufferSource>> PENDING = new ArrayList<>();

    private SeeThroughGeometry() {
    }

    public static void submit(PoseStack poseStack, RenderType renderType, BiConsumer<PoseStack.Pose, VertexConsumer> renderer) {
        PoseStack.Pose pose = poseStack.last().copy();
        PENDING.add(buffers -> {
            renderer.accept(pose, buffers.getBuffer(renderType));
            buffers.endBatch(renderType);
        });
    }

    public static void submitText(PoseStack poseStack, float x, float y, Component text, boolean dropShadow, int light, int color) {
        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        PENDING.add(buffers -> {
            Minecraft.getInstance().font.drawInBatch(text, x, y, color, dropShadow, pose, buffers, Font.DisplayMode.SEE_THROUGH, 0, light);
            buffers.endBatch();
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void afterLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || PENDING.isEmpty()) {
            return;
        }
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        for (Consumer<MultiBufferSource.BufferSource> entry : PENDING) {
            entry.accept(buffers);
        }
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        PENDING.clear();
    }
}
