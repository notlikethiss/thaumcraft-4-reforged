package thaumcraft.client.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;

public final class NodeRenderTypes {
    private static final Map<Key, RenderType> TYPES = new HashMap<>();

    private enum Layer {
        ADDITIVE(false, false),
        TRANSLUCENT(true, false),
        ADDITIVE_NO_DEPTH(false, true),
        TRANSLUCENT_NO_DEPTH(true, true);

        final RenderPipeline pipeline;
        final OitPipelineSet oit;

        Layer(boolean translucent, boolean depthIgnore) {
            Identifier id = Thaumcraft.id("node_" + name().toLowerCase(java.util.Locale.ROOT));
            pipeline = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(id.withPrefix("pipeline/"))
                .withFragmentShader(Thaumcraft.id("core/node"))
                .withCull(false)
                .withColorTargetState(new ColorTargetState(translucent ? BlendFunction.TRANSLUCENT : BlendFunction.LIGHTNING))
                .withDepthStencilState(new DepthStencilState(depthIgnore ? CompareOp.ALWAYS_PASS : CompareOp.GREATER_THAN_OR_EQUAL, false))
                .build();
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.OIT_PARTICLE_SNIPPET)
                .withFragmentShader(Thaumcraft.id("core/node"))
                .withCull(false);
            if (!translucent) {
                builder.withShaderDefine("OIT_ADDITIVE");
            }
            OitPipelineSet.Builder oitBuilder = OitPipelineSet.builder(id, builder);
            oit = (depthIgnore ? oitBuilder.withoutDepthTest() : oitBuilder).build();
        }
    }

    private record Key(Identifier texture, Layer layer) {
    }

    private NodeRenderTypes() {
    }

    public static void forEachPipeline(Consumer<RenderPipeline> consumer) {
        for (Layer layer : Layer.values()) {
            consumer.accept(layer.pipeline);
        }
    }

    public static void forEachOitPipelineSet(Consumer<OitPipelineSet> consumer) {
        for (Layer layer : Layer.values()) {
            consumer.accept(layer.oit);
        }
    }

    public static RenderType of(Identifier texture, boolean translucent, boolean depthIgnore) {
        Layer layer = translucent
            ? (depthIgnore ? Layer.TRANSLUCENT_NO_DEPTH : Layer.TRANSLUCENT)
            : (depthIgnore ? Layer.ADDITIVE_NO_DEPTH : Layer.ADDITIVE);
        return TYPES.computeIfAbsent(new Key(texture, layer), key -> RenderType.create(
            "thaumcraft_node_" + layer.name().toLowerCase(java.util.Locale.ROOT),
            RenderSetup.builder(layer.pipeline)
                .setOitPipelines(layer.oit)
                .withTexture("Sampler0", key.texture(), () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .createRenderSetup()
        ));
    }
}
