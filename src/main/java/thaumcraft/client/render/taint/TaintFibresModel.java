package thaumcraft.client.render.taint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.ClientConfig;
import thaumcraft.taint.TaintBlock;
import thaumcraft.taint.TaintFibresBlock;
import thaumcraft.client.platform.TcRenderTypes;

public class TaintFibresModel implements DynamicBlockStateModel {
    private static final int OVERLAY_LIGHT = 200;
    private static final int BLANK_OVERLAY = 0;
    private static final int OVERLAY_VARIANTS = 4;

    private final List<BlockStateModel> faces;
    private final List<List<BlockStateModel>> overlays;
    private final BlockStateModel grass;
    private final BlockStateModel plant;
    private final BlockStateModel stalk;
    private final BlockStateModel matureStalk;
    private final Material.Baked particle;
    private final int flags;

    public TaintFibresModel(
        List<BlockStateModel> faces,
        List<List<BlockStateModel>> overlays,
        BlockStateModel grass,
        BlockStateModel plant,
        BlockStateModel stalk,
        BlockStateModel matureStalk
    ) {
        this.faces = faces;
        this.overlays = overlays;
        this.grass = grass;
        this.plant = plant;
        this.stalk = stalk;
        this.matureStalk = matureStalk;
        this.particle = faces.get(0).particleMaterial();
        int materialFlags = BakedQuad.FLAG_TRANSLUCENT;
        for (BlockStateModel model : faces) {
            materialFlags |= model.materialFlags();
        }
        for (List<BlockStateModel> models : overlays) {
            for (BlockStateModel model : models) {
                materialFlags |= model.materialFlags();
            }
        }
        materialFlags |= grass.materialFlags() | plant.materialFlags() | stalk.materialFlags() | matureStalk.materialFlags();
        this.flags = materialFlags;
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return null;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        TaintFibresBlock.Kind kind = ((TaintFibresBlock) state.getBlock()).getKind();
        if (kind == TaintFibresBlock.Kind.FIBRES) {
            this.collectFibres(level, pos, parts);
            return;
        }
        if ((kind == TaintFibresBlock.Kind.GRASS || kind == TaintFibresBlock.Kind.PLANT)
            && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return;
        }
        BlockStateModel stage = switch (kind) {
            case GRASS -> this.grass;
            case PLANT -> this.plant;
            case STALK -> this.stalk;
            case MATURE_STALK -> this.matureStalk;
            default -> throw new IllegalStateException("Unexpected taint fibre kind " + kind);
        };
        List<BlockStateModelPart> stageParts = new ArrayList<>();
        stage.collectParts(level, pos, state, random, stageParts);
        float[] offset = kind == TaintFibresBlock.Kind.GRASS || kind == TaintFibresBlock.Kind.PLANT ? crossedOffset(pos) : new float[] { 0.0F, 0.0F };
        for (BlockStateModelPart part : stageParts) {
            parts.add(new OffsetPart(part, offset[0], offset[1]));
        }
    }

    private void collectFibres(BlockAndTintGetter level, BlockPos pos, List<BlockStateModelPart> parts) {
        @SuppressWarnings("unchecked")
        List<BakedQuad>[] quads = new List[Direction.values().length];
        for (Direction direction : Direction.values()) {
            int face = direction.get3DDataValue();
            BlockPos supportPos = pos.relative(direction);
            BlockState support = level.getBlockState(supportPos);
            if (!support.isFaceSturdy(level, supportPos, direction.getOpposite()) || support.getBlock() instanceof TaintBlock) {
                quads[face] = List.of();
                continue;
            }
            List<BakedQuad> sideQuads = new ArrayList<>();
            List<BakedQuad> base = new ArrayList<>();
            collect(this.faces.get(face), level, pos, level.getBlockState(pos), direction.getOpposite(), base);
            for (BakedQuad quad : base) {
                sideQuads.add(attachedFace(quad, direction, 0.995F));
            }
            int overlay = overlayVariant(pos, face);
            if (overlay != BLANK_OVERLAY && ClientConfig.glowyTaint()) {
                List<BakedQuad> selected = new ArrayList<>();
                collect(this.overlays.get(face).get(overlay), level, pos, level.getBlockState(pos), direction.getOpposite(), selected);
                for (BakedQuad quad : selected) {
                    sideQuads.add(emissiveOverlay(attachedFace(quad, direction, 0.98F)));
                }
            }
            quads[face] = sideQuads;
        }
        parts.add(new Part(quads, this.particle, this.flags));
    }

    private static void collect(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction, List<BakedQuad> result) {
        List<BlockStateModelPart> modelParts = new ArrayList<>();
        model.collectParts(level, pos, state, RandomSource.create(pos.asLong()), modelParts);
        for (BlockStateModelPart part : modelParts) {
            result.addAll(part.getQuads(direction));
        }
    }

    private static int overlayVariant(BlockPos pos, int side) {
        long seed = side + pos.getY() + (long) pos.getX() * pos.getZ();
        Random random = new Random(seed);
        return random.nextInt(100) < 95 ? BLANK_OVERLAY : random.nextInt(OVERLAY_VARIANTS - 1) + 1;
    }

    private static BakedQuad attachedFace(BakedQuad quad, Direction support, float distance) {
        float x = support.getStepX() * distance;
        float y = support.getStepY() * distance;
        float z = support.getStepZ() * distance;
        return new BakedQuad(
            new Vector3f(quad.position0()).add(x, y, z),
            new Vector3f(quad.position1()).add(x, y, z),
            new Vector3f(quad.position2()).add(x, y, z),
            new Vector3f(quad.position3()).add(x, y, z),
            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
            quad.direction(), quad.materialInfo(), quad.bakedNormals(), quad.bakedColors()
        );
    }

    private static float[] crossedOffset(BlockPos pos) {
        long seed = (long) pos.getX() * 3129871L ^ (long) pos.getZ() * 116129781L ^ pos.getY();
        seed = seed * seed * 42317861L + seed * 11L;
        float x = ((seed >> 16 & 15L) / 15.0F - 0.5F) * 0.5F;
        float z = ((seed >> 24 & 15L) / 15.0F - 0.5F) * 0.5F;
        return new float[] { x, z };
    }

    private static BakedQuad emissiveOverlay(BakedQuad quad) {
        BakedQuad.MaterialInfo material = quad.materialInfo();
        BakedQuad.MaterialInfo overlayMaterial = new BakedQuad.MaterialInfo(
            material.sprite(),
            ChunkSectionLayer.TRANSLUCENT,
            TcRenderTypes.translucentMovingBlock(),
            material.tintIndex(),
            material.shade(),
            OVERLAY_LIGHT,
            false
        );
        return new BakedQuad(
            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
            quad.direction(), overlayMaterial, quad.bakedNormals(), quad.bakedColors()
        );
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.particle;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return this.particle;
    }

    @Override
    public int materialFlags() {
        return this.flags;
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return this.flags;
    }

    private record Part(List<BakedQuad>[] quads, Material.Baked particle, @BakedQuad.MaterialFlags int flags) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction != null) {
                return List.of();
            }
            List<BakedQuad> attached = new ArrayList<>();
            for (List<BakedQuad> face : quads) {
                attached.addAll(face);
            }
            return attached;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return flags;
        }
    }

    private record OffsetPart(BlockStateModelPart delegate, float x, float z) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            List<BakedQuad> quads = this.delegate.getQuads(direction);
            if (this.x == 0.0F && this.z == 0.0F || quads.isEmpty()) {
                return quads;
            }
            List<BakedQuad> shifted = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                shifted.add(new BakedQuad(
                    new Vector3f(quad.position0()).add(this.x, 0.0F, this.z),
                    new Vector3f(quad.position1()).add(this.x, 0.0F, this.z),
                    new Vector3f(quad.position2()).add(this.x, 0.0F, this.z),
                    new Vector3f(quad.position3()).add(this.x, 0.0F, this.z),
                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo(), quad.bakedNormals(), quad.bakedColors()
                ));
            }
            return shifted;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.delegate.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.delegate.particleMaterial();
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return this.delegate.materialFlags();
        }
    }

    public record Unbaked(
        List<BlockStateModel.Unbaked> faces,
        List<List<BlockStateModel.Unbaked>> overlays,
        BlockStateModel.Unbaked grass,
        BlockStateModel.Unbaked plant,
        BlockStateModel.Unbaked stalk,
        BlockStateModel.Unbaked matureStalk
    ) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStateModel.Unbaked.CODEC.listOf(6, 6).fieldOf("faces").forGetter(Unbaked::faces),
            BlockStateModel.Unbaked.CODEC.listOf(4, 4).listOf(6, 6).fieldOf("overlays").forGetter(Unbaked::overlays),
            BlockStateModel.Unbaked.CODEC.fieldOf("grass").forGetter(Unbaked::grass),
            BlockStateModel.Unbaked.CODEC.fieldOf("plant").forGetter(Unbaked::plant),
            BlockStateModel.Unbaked.CODEC.fieldOf("stalk").forGetter(Unbaked::stalk),
            BlockStateModel.Unbaked.CODEC.fieldOf("mature_stalk").forGetter(Unbaked::matureStalk)
        ).apply(instance, Unbaked::new));

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return new TaintFibresModel(
                this.faces.stream().map(model -> model.bake(baker)).toList(),
                this.overlays.stream().map(side -> side.stream().map(model -> model.bake(baker)).toList()).toList(),
                this.grass.bake(baker), this.plant.bake(baker), this.stalk.bake(baker), this.matureStalk.bake(baker)
            );
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            this.faces.forEach(model -> model.resolveDependencies(resolver));
            this.overlays.forEach(side -> side.forEach(model -> model.resolveDependencies(resolver)));
            this.grass.resolveDependencies(resolver);
            this.plant.resolveDependencies(resolver);
            this.stalk.resolveDependencies(resolver);
            this.matureStalk.resolveDependencies(resolver);
        }

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return MAP_CODEC;
        }
    }
}
