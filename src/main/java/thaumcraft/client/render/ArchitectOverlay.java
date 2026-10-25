package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.QuadSink;
import thaumcraft.client.platform.SeeThroughGeometry;
import thaumcraft.client.platform.TcRenderTypes;
import thaumcraft.client.platform.VertexQuadSink;
import thaumcraft.wand.focus.Architect;
import thaumcraft.wand.focus.FocusItem;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ArchitectOverlay {
    private static final Identifier ARROWS = Thaumcraft.id("textures/misc/architect_arrows.png");
    private static final int LIGHT = 200;
    public static final int[] CONNECTED = thaumcraft.client.geom.ConnectedTextureGeometry.CONNECTED;
    private static int lastHash;
    private static List<BlockPos> blocks = List.of();
    private static @Nullable Frame frame;

    private record Frame(BlockPos target, Set<BlockPos> blocks, boolean axisX, boolean axisY, boolean axisZ, int ticks) {
    }

    private ArchitectOverlay() {
    }

    @SubscribeEvent
    static void onExtractOutline(ExtractBlockOutlineRenderStateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof Architect architect) || stack.getItem() instanceof FocusItem) {
            return;
        }
        BlockPos pos = event.getBlockPos();
        Direction side = event.getHitResult().getDirection();
        int hash = Objects.hash(pos, side, player.tickCount / 5);
        if (hash != lastHash) {
            lastHash = hash;
            blocks = architect.getArchitectBlocks(stack, event.getLevel(), pos, side, player);
        }
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        frame = new Frame(
            pos.immutable(),
            new HashSet<>(blocks),
            architect.showAxis(stack, event.getLevel(), player, side, Direction.Axis.X),
            architect.showAxis(stack, event.getLevel(), player, side, Direction.Axis.Y),
            architect.showAxis(stack, event.getLevel(), player, side, Direction.Axis.Z),
            player.tickCount
        );
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onSubmit(SubmitCustomGeometryEvent event) {
        Frame current = frame;
        frame = null;
        if (current == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        SeeThroughGeometry.submit(
            event.getSubmitNodeCollector(),
            poseStack,
            TcRenderTypes.additiveNoDepth(atlas.location()),
            (pose, buffer) -> {
                QuadSink sink = new VertexQuadSink(pose, buffer);
                for (BlockPos pos : current.blocks()) {
                    overlayBlock(sink, atlas, current, pos, camera);
                }
            }
        );
        if (current.axisX() || current.axisY() || current.axisZ()) {
            SeeThroughGeometry.submit(
                event.getSubmitNodeCollector(),
                poseStack,
                TcRenderTypes.additiveNoDepth(ARROWS),
                (pose, buffer) -> axis(new VertexQuadSink(pose, buffer), current, camera)
            );
        }
    }

    private static void axis(QuadSink sink, Frame current, Vec3 camera) {
        BlockPos pos = current.target();
        float red = Mth.sin(current.ticks() / 4.0F + pos.getX()) * 0.2F + 0.3F;
        float green = Mth.sin(current.ticks() / 3.0F + pos.getY()) * 0.2F + 0.3F;
        float blue = Mth.sin(current.ticks() / 2.0F + pos.getZ()) * 0.2F + 0.8F;
        int argb = 0xFF000000 | (int) (red * 255.0F) << 16 | (int) (green * 255.0F) << 8 | (int) (blue * 255.0F);
        Matrix4f base = new Matrix4f().translate(
            (float) (pos.getX() + 0.5 - camera.x),
            (float) (pos.getY() + 0.5 - camera.y),
            (float) (pos.getZ() + 0.5 - camera.z)
        ).rotateX((float) Math.toRadians(90.0));
        float right = (float) (Math.PI / 2.0);
        if (current.axisX()) {
            Matrix4f matrix = new Matrix4f(base);
            arrow(sink, matrix, argb);
            arrow(sink, matrix.rotateX(right), argb);
        }
        if (current.axisZ()) {
            Matrix4f matrix = new Matrix4f(base).rotateZ(right);
            arrow(sink, matrix, argb);
            arrow(sink, matrix.rotateX(right), argb);
        }
        if (current.axisY()) {
            Matrix4f matrix = new Matrix4f(base).rotateY(right);
            arrow(sink, matrix, argb);
            arrow(sink, matrix.rotateX(right), argb);
        }
    }

    private static void arrow(QuadSink sink, Matrix4f matrix, int argb) {
        float[][] corners = {{-0.5F, 0.5F, 0.0F, 1.0F}, {0.5F, 0.5F, 1.0F, 1.0F}, {0.5F, -0.5F, 1.0F, 0.0F}, {-0.5F, -0.5F, 0.0F, 0.0F}};
        Vector3f point = new Vector3f();
        for (float[] corner : corners) {
            matrix.transformPosition(corner[0], corner[1], 0.0F, point);
            sink.vertex(point.x, point.y, point.z, corner[2], corner[3], argb, LIGHT);
        }
    }

    private static boolean connected(Frame current, int x, int y, int z) {
        return current.blocks().contains(new BlockPos(x, y, z));
    }

    private static TextureAtlasSprite icon(TextureAtlas atlas, Frame current, int x, int y, int z, int side) {
        int texture = thaumcraft.client.geom.ConnectedTextureGeometry.texture(side,
            (dx, dy, dz) -> connected(current, x + dx, y + dy, z + dz));
        return atlas.getSprite(Thaumcraft.id("block/warded_glass_" + texture));
    }

    private static void overlayBlock(QuadSink sink, TextureAtlas atlas, Frame current, BlockPos pos, Vec3 camera) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        float red = Mth.sin(current.ticks() / 2.0F + x) * 0.2F + 0.3F;
        float green = Mth.sin(current.ticks() / 3.0F + y) * 0.2F + 0.3F;
        float blue = Mth.sin(current.ticks() / 4.0F + z) * 0.2F + 0.8F;
        int argb = (int) (0.2F * 255.0F) << 24 | (int) (red * 255.0F) << 16 | (int) (green * 255.0F) << 8 | (int) (blue * 255.0F);
        float ox = (float) (x - camera.x);
        float oy = (float) (y - camera.y);
        float oz = (float) (z - camera.z);
        for (Direction direction : Direction.values()) {
            if (connected(current, x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())) {
                continue;
            }
            face(sink, icon(atlas, current, x, y, z, direction.get3DDataValue()), direction, ox, oy, oz, argb);
        }
    }

    private static void face(QuadSink sink, TextureAtlasSprite sprite, Direction direction, float ox, float oy, float oz, int argb) {
        float min = -0.001F;
        float max = 1.001F;
        float[][] corners = switch (direction) {
            case DOWN -> new float[][] {{min, min, max}, {min, min, min}, {max, min, min}, {max, min, max}};
            case UP -> new float[][] {{max, max, max}, {max, max, min}, {min, max, min}, {min, max, max}};
            case NORTH -> new float[][] {{min, max, min}, {max, max, min}, {max, min, min}, {min, min, min}};
            case SOUTH -> new float[][] {{min, max, max}, {min, min, max}, {max, min, max}, {max, max, max}};
            case WEST -> new float[][] {{min, max, max}, {min, max, min}, {min, min, min}, {min, min, max}};
            case EAST -> new float[][] {{max, min, max}, {max, min, min}, {max, max, min}, {max, max, max}};
        };
        for (float[] corner : corners) {
            float u;
            float v;
            if (direction.getAxis() == Direction.Axis.Y) {
                u = corner[0];
                v = corner[2];
            } else if (direction.getAxis() == Direction.Axis.Z) {
                u = corner[0];
                v = 1.0F - corner[1];
            } else {
                u = corner[2];
                v = 1.0F - corner[1];
            }
            sink.vertex(
                ox + corner[0],
                oy + corner[1],
                oz + corner[2],
                sprite.getU(Mth.clamp(u, 0.0F, 1.0F)),
                sprite.getV(Mth.clamp(v, 0.0F, 1.0F)),
                argb,
                LIGHT
            );
        }
    }
}
