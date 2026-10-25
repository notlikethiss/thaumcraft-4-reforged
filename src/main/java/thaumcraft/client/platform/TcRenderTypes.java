package thaumcraft.client.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class TcRenderTypes {
    private static final Map<Identifier, RenderType> ELDRITCH = new HashMap<>();
    private static final Map<Identifier, RenderType> ELDRITCH_PORTAL = new HashMap<>();
    private static final Map<Identifier, RenderType> ADDITIVE = new HashMap<>();
    private static final Map<Identifier, RenderType> TRANSLUCENT = new HashMap<>();
    private static final Map<Identifier, RenderType> ADDITIVE_NO_DEPTH = new HashMap<>();
    private static final Map<Identifier, RenderType> TRANSLUCENT_NO_DEPTH = new HashMap<>();
    private static final Map<Identifier, RenderType> ENTITY_TRANSLUCENT_NO_DEPTH = new HashMap<>();

    private TcRenderTypes() {
    }

    public enum Kind {
        ADDITIVE,
        TRANSLUCENT,
        ENTITY_CUTOUT,
        ENTITY_CUTOUT_NO_CULL,
        ENTITY_TRANSLUCENT
    }

    public static RenderType of(Kind kind, Identifier texture) {
        return switch (kind) {
            case ADDITIVE -> additive(texture);
            case TRANSLUCENT -> translucent(texture);
            case ENTITY_CUTOUT -> entityCutout(texture);
            case ENTITY_CUTOUT_NO_CULL -> entityCutoutNoCull(texture);
            case ENTITY_TRANSLUCENT -> entityTranslucent(texture);
        };
    }

    public static RenderType eldritchPortal(Identifier texture) {
        return ELDRITCH_PORTAL.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_eldritch_portal",
            RenderSetup.builder(ModRenderPipelines.ELDRITCH_PORTAL)
                .withTexture("Sampler0", key, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .sortOnUpload()
                .createRenderSetup()
        ));
    }

    public static RenderType eldritchSurface(Identifier texture) {
        return ELDRITCH.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_eldritch_surface",
            RenderSetup.builder(ModRenderPipelines.ELDRITCH_SURFACE)
                .withTexture("Sampler0", key, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .createRenderSetup()
        ));
    }

    public static RenderType additive(Identifier texture) {
        return ADDITIVE.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_additive",
            RenderSetup.builder(ModRenderPipelines.ADDITIVE_PARTICLE)
                .withTexture("Sampler0", key, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .createRenderSetup()
        ));
    }

    public static RenderType translucent(Identifier texture) {
        return TRANSLUCENT.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_translucent",
            RenderSetup.builder(ModRenderPipelines.TRANSLUCENT_PARTICLE)
                .withTexture("Sampler0", key, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .createRenderSetup()
        ));
    }

    public static RenderType additiveNoDepth(Identifier texture) {
        return ADDITIVE_NO_DEPTH.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_additive_no_depth",
            RenderSetup.builder(ModRenderPipelines.ADDITIVE_NO_DEPTH)
                .withTexture("Sampler0", key, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .createRenderSetup()
        ));
    }

    public static RenderType translucentNoDepth(Identifier texture) {
        return TRANSLUCENT_NO_DEPTH.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_translucent_no_depth",
            RenderSetup.builder(ModRenderPipelines.TRANSLUCENT_NO_DEPTH)
                .withTexture("Sampler0", key, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                .useLightmap()
                .createRenderSetup()
        ));
    }

    public static RenderType byBlend(Identifier texture, int blendMode, boolean depth) {
        if (depth) {
            return byBlend(texture, blendMode);
        }
        return blendMode == 771 ? translucentNoDepth(texture) : additiveNoDepth(texture);
    }

    public static RenderType entityCutout(Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    public static RenderType entityCutoutNoCull(Identifier texture) {
        return RenderTypes.entityCutout(texture, false);
    }

    public static RenderType eyes(Identifier texture) {
        return RenderTypes.eyes(texture);
    }

    public static RenderType translucentMovingBlock() {
        return RenderTypes.translucentMovingBlock();
    }

    public static RenderType dragonRays() {
        return RenderTypes.dragonRays();
    }

    public static RenderType entityTranslucent(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType entityTranslucentNoDepth(Identifier texture) {
        return ENTITY_TRANSLUCENT_NO_DEPTH.computeIfAbsent(texture, key -> RenderType.create(
            "thaumcraft_entity_translucent_no_depth",
            RenderSetup.builder(ModRenderPipelines.ENTITY_TRANSLUCENT_NO_DEPTH)
                .withTexture("Sampler0", key)
                .useLightmap()
                .useOverlay()
                .sortOnUpload()
                .createRenderSetup()
        ));
    }

    public static RenderType outline(Identifier texture) {
        return RenderTypes.outline(texture);
    }

    public static RenderType byBlend(Identifier texture, int blendMode) {
        return blendMode == 771 ? translucent(texture) : additive(texture);
    }
}
