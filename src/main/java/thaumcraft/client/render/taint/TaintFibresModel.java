package thaumcraft.client.render.taint;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import thaumcraft.taint.TaintBlock;
import thaumcraft.taint.TaintFibresBlock;

public final class TaintFibresModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<WorldPos> WORLD_POS = new ModelProperty<>();
    private final List<BakedModel> faces;
    private final List<List<BakedModel>> overlays;
    private final BakedModel grass;
    private final BakedModel plant;
    private final BakedModel stalk;
    private final BakedModel matureStalk;
    private final TextureAtlasSprite particle;

    private TaintFibresModel(BakedModel fallback, List<BakedModel> faces, List<List<BakedModel>> overlays,
                             BakedModel grass, BakedModel plant, BakedModel stalk, BakedModel matureStalk) {
        super(fallback);
        this.faces = faces;
        this.overlays = overlays;
        this.grass = grass;
        this.plant = plant;
        this.stalk = stalk;
        this.matureStalk = matureStalk;
        this.particle = faces.get(0).getParticleIcon();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return modelData.derive().with(WORLD_POS, new WorldPos(level, pos)).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        if (state == null || !(state.getBlock() instanceof TaintFibresBlock fibres)) return List.of();
        WorldPos worldPos = data.get(WORLD_POS);
        if (worldPos == null) return List.of();
        TaintFibresBlock.Kind kind = fibres.getKind();
        if (kind != TaintFibresBlock.Kind.FIBRES) {
            if ((kind == TaintFibresBlock.Kind.GRASS || kind == TaintFibresBlock.Kind.PLANT)
                && !worldPos.level.getBlockState(worldPos.pos.below()).isFaceSturdy(worldPos.level, worldPos.pos.below(), Direction.UP)) return List.of();
            BakedModel stage = switch (kind) {
                case GRASS -> grass;
                case PLANT -> plant;
                case STALK -> stalk;
                case MATURE_STALK -> matureStalk;
                default -> throw new IllegalStateException("Unexpected fibre kind " + kind);
            };
            if (renderType != RenderType.cutout()) return List.of();
            List<BakedQuad> result = stage.getQuads(state, side, random, data, renderType);
            return kind == TaintFibresBlock.Kind.GRASS || kind == TaintFibresBlock.Kind.PLANT
                ? offset(result, worldPos.pos) : result;
        }
        if (side != null) return List.of();
        List<BakedQuad> result = new ArrayList<>();
        boolean overlayPass = renderType == RenderType.translucent();
        if (!overlayPass && renderType != RenderType.cutout()) return result;
        for (Direction supportDirection : Direction.values()) {
            BlockPos supportPos = worldPos.pos.relative(supportDirection);
            BlockState support = worldPos.level.getBlockState(supportPos);
            if (!support.isFaceSturdy(worldPos.level, supportPos, supportDirection.getOpposite()) || support.getBlock() instanceof TaintBlock) continue;
            int face = supportDirection.get3DDataValue();
            Direction inwardFace = supportDirection.getOpposite();
            if (overlayPass) {
                int overlay = overlayVariant(worldPos.pos, face);
                if (overlay == 0) continue;
                result.addAll(translate(emissive(overlays.get(face).get(overlay).getQuads(state, inwardFace, random, data, renderType)), supportDirection, 0.98F));
            } else {
                result.addAll(translate(faces.get(face).getQuads(state, inwardFace, random, data, renderType), supportDirection, 0.995F));
            }
        }
        return result;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return List.of();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() { return particle; }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        if (state != null && state.getBlock() instanceof TaintFibresBlock fibres && fibres.getKind() == TaintFibresBlock.Kind.FIBRES) {
            return ChunkRenderTypeSet.of(RenderType.cutout(), RenderType.translucent());
        }
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

    private static int overlayVariant(BlockPos pos, int side) {
        Random random = new Random(side + pos.getY() + (long) pos.getX() * pos.getZ());
        return random.nextInt(100) < 95 ? 0 : random.nextInt(3) + 1;
    }

    private static List<BakedQuad> emissive(List<BakedQuad> quads) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices().clone();
            int stride = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
            for (int vertex = 0; vertex < 4; vertex++) vertices[vertex * stride + 6] = 200;
            result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), false, false));
        }
        return result;
    }

    private static List<BakedQuad> offset(List<BakedQuad> quads, BlockPos pos) {
        long seed = (long) pos.getX() * 3129871L ^ (long) pos.getZ() * 116129781L ^ pos.getY();
        seed = seed * seed * 42317861L + seed * 11L;
        float x = ((seed >> 16 & 15L) / 15.0F - 0.5F) * 0.5F;
        float z = ((seed >> 24 & 15L) / 15.0F - 0.5F) * 0.5F;
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices().clone();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * (DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES);
                vertices[base] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base]) + x);
                vertices[base + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 2]) + z);
            }
            result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return result;
    }

    private static List<BakedQuad> translate(List<BakedQuad> quads, Direction direction, float distance) {
        if (quads.isEmpty()) return quads;
        int stride = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
        int dx = direction.getStepX();
        int dy = direction.getStepY();
        int dz = direction.getStepZ();
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices().clone();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * stride;
                vertices[base] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base]) + dx * distance);
                vertices[base + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 1]) + dy * distance);
                vertices[base + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 2]) + dz * distance);
            }
            result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return result;
    }

    private record WorldPos(BlockAndTintGetter level, BlockPos pos) {}

    public static final class Loader implements IGeometryLoader<Unbaked> {
        @Override
        public Unbaked read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            List<ResourceLocation> faces = locations(json.getAsJsonArray("faces"));
            JsonArray rows = json.getAsJsonArray("overlays");
            if (faces.size() != 6 || rows.size() != 6) throw new JsonParseException("taint_fibres requires six faces and six overlay rows");
            List<List<ResourceLocation>> overlays = new ArrayList<>(6);
            for (int side = 0; side < 6; side++) {
                List<ResourceLocation> row = locations(rows.get(side).getAsJsonArray());
                if (row.size() != 4) throw new JsonParseException("taint_fibres overlay rows must contain four models");
                overlays.add(row);
            }
            return new Unbaked(faces, overlays, location(json, "grass"), location(json, "plant"), location(json, "stalk"), location(json, "mature_stalk"));
        }
    }

    public record Unbaked(List<ResourceLocation> faces, List<List<ResourceLocation>> overlays,
                          ResourceLocation grass, ResourceLocation plant, ResourceLocation stalk,
                          ResourceLocation matureStalk) implements IUnbakedGeometry<Unbaked> {
        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> getter, IGeometryBakingContext context) {
            for (ResourceLocation model : faces) getter.apply(model).resolveParents(getter);
            for (List<ResourceLocation> row : overlays) for (ResourceLocation model : row) getter.apply(model).resolveParents(getter);
            getter.apply(grass).resolveParents(getter);
            getter.apply(plant).resolveParents(getter);
            getter.apply(stalk).resolveParents(getter);
            getter.apply(matureStalk).resolveParents(getter);
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> sprites,
                               ModelState state, ItemOverrides overrides) {
            List<BakedModel> bakedFaces = faces.stream().map(id -> baker.bake(id, state)).toList();
            List<List<BakedModel>> bakedOverlays = overlays.stream().map(row -> row.stream().map(id -> baker.bake(id, state)).toList()).toList();
            BakedModel fallback = baker.bake(ResourceLocation.withDefaultNamespace("block/stone"), state);
            return new TaintFibresModel(fallback, bakedFaces, bakedOverlays, baker.bake(grass, state), baker.bake(plant, state), baker.bake(stalk, state), baker.bake(matureStalk, state));
        }
    }

    private static List<ResourceLocation> locations(JsonArray array) {
        List<ResourceLocation> result = new ArrayList<>(array.size());
        for (var element : array) result.add(ResourceLocation.parse(element.getAsString()));
        return result;
    }

    private static ResourceLocation location(JsonObject json, String key) {
        return ResourceLocation.parse(json.get(key).getAsString());
    }
}
