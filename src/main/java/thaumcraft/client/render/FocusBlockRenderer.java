package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import thaumcraft.client.platform.ImmediateGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.blockentity.HoleBlockEntity;
import thaumcraft.blockentity.WardedBlockEntity;
import thaumcraft.client.geom.ConnectedTextureGeometry;
import thaumcraft.client.platform.FocusSurfaces;
import thaumcraft.registry.ModBlocks;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.WardingFocus;
import thaumcraft.client.geom.Colors;

public final class FocusBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public static final class State {
        BlockPos blockPos;
        boolean hole;
        int mask;
        int[] textures = new int[6];
        int color;
        boolean nearby;
        float time;
    }

    public FocusBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T entity, float partialTicks, PoseStack poses, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        State state = new State();
        state.blockPos = entity.getBlockPos();
        extract(entity, state, partialTicks, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        submit(state, poses, new ImmediateGeometry(buffers));
    }

    private void extract(T entity, State state, float partialTicks, Vec3 cameraPosition) {
        state.mask = 0;
        var level = entity.getLevel();
        if (level == null) return;
        var pos = entity.getBlockPos();
        state.hole = entity instanceof HoleBlockEntity;
        if (state.hole) {
            for (Direction side : Direction.values()) {
                var neighborPos = pos.relative(side);
                var neighbor = level.getBlockState(neighborPos);
                if (!neighbor.is(ModBlocks.HOLE.get()) && neighbor.isSolidRender(level, neighborPos)) {
                    state.mask |= 1 << side.get3DDataValue();
                }
            }
            state.nearby = cameraPosition.distanceToSqr(Vec3.atCenterOf(pos)) < 512.0;
            state.time = level.getGameTime() + partialTicks;
            return;
        }
        if (!(entity instanceof WardedBlockEntity warded)
            || !(Minecraft.getInstance().getCameraEntity() instanceof Player player)) return;
        var wand = player.getMainHandItem();
        if (!(wand.getItem() instanceof WandItem) || !(Wands.getFocus(wand) instanceof WardingFocus)) return;
        boolean owner = warded.isOwner(player.getUUID());
        float red = Mth.sin(player.tickCount / 2.0F + pos.getX()) * 0.2F + 0.8F;
        float green = Mth.sin(player.tickCount / 3.0F + pos.getY()) * 0.2F + (owner ? 0.7F : 0.28F);
        float blue = Mth.sin(player.tickCount / 4.0F + pos.getZ()) * 0.2F + 0.28F;
        state.color = Colors.withAlpha((int) (red * 255.0F) << 16 | (int) (green * 255.0F) << 8 | (int) (blue * 255.0F), owner ? 0.5F : 0.25F);
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).is(ModBlocks.WARDED_BLOCK.get())) continue;
            int index = side.get3DDataValue();
            state.mask |= 1 << index;
            state.textures[index] = ConnectedTextureGeometry.texture(index,
                (x, y, z) -> level.getBlockEntity(pos.offset(x, y, z)) instanceof WardedBlockEntity other && warded.hasSameOwner(other));
        }
    }

    private void submit(State state, PoseStack poses, ImmediateGeometry collector) {
        if (state.mask == 0) return;
        if (state.hole) {
            FocusSurfaces.submitHole(collector, poses, state.mask, state.nearby, state.time);
        } else {
            FocusSurfaces.submitWards(collector, poses, state.mask, state.textures, state.color);
        }
    }
}
