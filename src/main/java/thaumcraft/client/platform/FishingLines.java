package thaumcraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import thaumcraft.client.geom.FishingLineGeometry;

public final class FishingLines {
    private FishingLines() { }

    public static void submit(PoseStack poses, SubmitNodeCollector collector, float dx, float dy, float dz) {
        float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        collector.submitCustomGeometry(poses, RenderTypes.lines(), (pose, buffer) ->
            FishingLineGeometry.render(dx, dy, dz, (x, y, z, nx, ny, nz) ->
                buffer.addVertex(pose, x, y, z).setColor(0xFF000000).setNormal(pose, nx, ny, nz).setLineWidth(width)));
    }
}
