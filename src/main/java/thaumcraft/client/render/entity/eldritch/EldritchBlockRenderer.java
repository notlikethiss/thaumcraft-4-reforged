package thaumcraft.client.render.entity.eldritch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.platform.EldritchSurfaces;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.eldritch.EldritchBlock;
import thaumcraft.eldritch.EldritchBlockEntity;
import thaumcraft.registry.EldritchContent;
import thaumcraft.world.dim.OuterLands;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class EldritchBlockRenderer implements BlockEntityRenderer<EldritchBlockEntity, EldritchBlockRenderer.State> {
    private static final net.minecraft.resources.Identifier TUNNEL = Thaumcraft.id("textures/misc/tunnel.png");
    private static final net.minecraft.resources.Identifier FIELD = Thaumcraft.id("textures/misc/particlefield.png");
    private static final net.minecraft.resources.Identifier FIELD_FAR = Thaumcraft.id("textures/misc/particlefield32.png");
    private final ItemModelResolver itemModelResolver;
    private final net.minecraft.client.model.geom.ModelPart cube;
    private final thaumcraft.client.render.model.ObjModel obelisk;
    private final thaumcraft.client.render.model.ObjModel vent;

    static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] eyes = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
        final ItemStackRenderState key = new ItemStackRenderState();
        int kind;
        int eyeCount;
        int phase = -1;
        Direction facing = Direction.NORTH;
        float time;
        boolean nearby;
        boolean outer;
        BlockPos pos = BlockPos.ZERO;
    }

    public EldritchBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.cube = EldritchCubeModel.bake(context.bakeLayer(EldritchCubeModel.LAYER));
        this.obelisk = thaumcraft.client.render.model.ObjModel.get(Thaumcraft.id("models/obj/obelisk_cap.obj"));
        this.vent = thaumcraft.client.render.model.ObjModel.get(Thaumcraft.id("models/obj/crabvent.obj"));
    }

    @SubscribeEvent
    static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EldritchCubeModel.LAYER, EldritchCubeModel::createLayer);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EldritchBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.kind = ((EldritchBlock) entity.getBlockState().getBlock()).kind();
        state.eyeCount = entity.eyeCount();
        state.phase = entity.triggered() ? entity.timer() : -1;
        state.facing = entity.getBlockState().hasProperty(EldritchBlock.FACING) ? entity.getBlockState().getValue(EldritchBlock.FACING) : Direction.NORTH;
        state.time = (entity.getLevel() == null ? 0.0F : (float) entity.getLevel().getGameTime()) + partialTicks;
        state.nearby = cameraPosition.distanceToSqr(Vec3.atCenterOf(entity.getBlockPos())) < 512.0;
        state.outer = entity.getLevel() != null && entity.getLevel().dimension() == OuterLands.OUTER_LANDS;
        state.pos = entity.getBlockPos();
        if (entity.getLevel() != null && state.kind == 0) {
            ItemStack eye = new ItemStack(EldritchContent.ELDRITCH_EYE.get());
            for (int index = 0; index < state.eyes.length; index++) {
                state.eyes[index].clear();
                if (index < state.eyeCount) {
                    this.itemModelResolver.updateForTopItem(state.eyes[index], eye, ItemDisplayContext.FIXED, entity.getLevel(), null, (int) entity.getBlockPos().asLong() + index);
                }
            }
        }
        state.key.clear();
        if (entity.getLevel() != null && state.kind == 8 && state.phase >= 0) {
            ItemStack key = new ItemStack(EldritchContent.RUNED_TABLET.get());
            this.itemModelResolver.updateForTopItem(state.key, key, ItemDisplayContext.FIXED, entity.getLevel(), null, (int) entity.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.kind == 0) {
            this.submitEyes(state, poseStack, collector);
        } else if (state.kind == 1) {
            this.submitObelisk(state, poseStack, collector);
        } else if (state.kind == 3) {
            this.submitCap(state, poseStack, collector);
        } else if (state.kind == 9) {
            this.submitCrabVent(state, poseStack, collector);
        } else if (state.kind == 10) {
            this.submitRunedStone(state, poseStack, collector);
        } else if (state.kind == 8) {
            this.submitLock(state, poseStack, collector);
        }
    }

    private void submitObelisk(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float bob = Mth.sin(state.time / 10.0F) * 0.1F + 0.1F;
        net.minecraft.resources.Identifier side = Thaumcraft.id(state.outer ? "textures/model/obelisk_side_2.png" : "textures/model/obelisk_side.png");
        net.minecraft.resources.Identifier cap = Thaumcraft.id(state.outer ? "textures/model/obelisk_cap_2.png" : "textures/model/obelisk_cap.png");
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F + bob, 0.5F);
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
            poseStack.translate(0.0F, 0.0F, -0.5F);
            this.submitPortalLayers(poseStack, collector, state, -0.5F, 0.0F, 0.005F, 0.5F, 3.0F, 0.005F);
            EldritchSurfaces.submitRectangle(collector.order(2), poseStack, TcRenderTypes.entityTranslucent(side), -0.5F, 0.0F, 0.0F, 0.5F, 3.0F, 0.0F, 1.0F, 0.0F, 0xFFFFFFFF, 0xF000F0);
            poseStack.popPose();
        }
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F + bob, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityCutout(cap), (pose, buffer) -> this.obelisk.renderPart("Cap", pose, buffer, 0xFFFFFFFF, 0xF000F0));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5F, 4.0F + bob, 0.5F);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityCutout(cap), (pose, buffer) -> this.obelisk.renderPart("Cap", pose, buffer, 0xFFFFFFFF, 0xF000F0));
        poseStack.popPose();
    }

    private void submitCap(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityCutout(Thaumcraft.id(state.outer ? "textures/model/obelisk_cap_2.png" : "textures/model/obelisk_cap.png")), (pose, buffer) -> this.obelisk.renderPart("Cap", pose, buffer, 0xFFFFFFFF, 0xF000F0));
        poseStack.popPose();
    }

    private void submitCrabVent(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (state.facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityCutout(Thaumcraft.id("textures/model/crabvent.png")), (pose, buffer) -> this.vent.renderPart("Vent", pose, buffer, 0xFFFFFFFF, 0xF000F0));
        poseStack.popPose();
    }

    private void submitRunedStone(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        long seed = Math.abs((state.pos.getX() + "" + state.pos.getY() + "" + state.pos.getZ()).hashCode() * 100) + 1L;
        int[] textures = new int[6];
        for (int side = 0; side < textures.length; side++) {
            textures[side] = new Random(seed).nextInt(12345 + side) % 4;
        }
        Direction[] directions = Direction.values();
        for (int side = 0; side < directions.length; side++) {
            Direction direction = directions[side];
            net.minecraft.resources.Identifier texture = Thaumcraft.id("textures/block/es_" + (textures[side] + 5) + ".png");
            EldritchSurfaces.submitFace(collector, poseStack, TcRenderTypes.entityCutout(texture), direction.ordinal(), 1.0F, 0.0F, 0xFFFFFFFF, state.lightCoords);
        }
    }

    private void submitPortalLayers(PoseStack poseStack, SubmitNodeCollector collector, State state, float x0, float y0, float z0, float x1, float y1, float z1) {
        EldritchSurfaces.submitPortalRectangle(collector, poseStack, state.nearby, state.time, x0, y0, z0, x1, y1, z1);
    }

    private void submitEyes(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        collector.submitCustomGeometry(poseStack, TcRenderTypes.entityCutout(Thaumcraft.id(state.outer ? "textures/model/obelisk_cap_2.png" : "textures/model/obelisk_cap_altar.png")), (pose, buffer) -> this.obelisk.renderPart("Cap", pose, buffer, 0xFFFFFFFF, 0xF000F0));
        poseStack.popPose();
        for (int index = 0; index < state.eyeCount && index < state.eyes.length; index++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
            poseStack.translate(0.46F, 0.2F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            state.eyes[index].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private void submitLock(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 90.0F));
            for (int level = 1; level < 5 - (state.phase + side * 5) / 20; level++) {
                float wave = Mth.sin((state.time + level * 10.0F + side * 20.0F) / 20.0F) * 0.1F;
                if (level == 1 || level == 4) {
                    wave = wave / 2.0F + 0.2F;
                }
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.25F + 0.5F * level, 0.0F);
                poseStack.scale(0.5F + wave, 0.5F, 0.5F + wave);
                collector.submitModelPart(this.cube, poseStack, TcRenderTypes.entityCutout(Thaumcraft.id("textures/model/eldritch_cube.png")), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        if (!state.key.isEmpty()) {
            Direction facing = state.facing;
            poseStack.pushPose();
            poseStack.translate(0.5F + facing.getStepX() * 0.525F, 0.285F, 0.5F + facing.getStepZ() * 0.525F);
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
            state.key.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        this.submitPortalPlane(state, poseStack, collector);
    }

    private void submitPortalPlane(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        Direction facing = state.facing;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(0.0F, 0.0F, -0.505F);
        EldritchSurfaces.submitPortalRectangle(collector, poseStack, state.nearby, state.time, -2.5F, -2.0F, 0.0F, 2.5F, 3.0F, 0.0F);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(EldritchBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(3.0, 5.0, 3.0);
    }
}
