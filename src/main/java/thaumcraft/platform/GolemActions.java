package thaumcraft.platform;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.entity.golem.GolemBase;

public final class GolemActions {
    private static final GameProfile PROFILE = new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(StandardCharsets.UTF_8)), "FakeThaumcraftGolem");
    private GolemActions() { }

    private static ServerPlayer prepare(GolemBase golem, ItemStack stack) {
        ServerPlayer player = FakePlayerFactory.get((ServerLevel) golem.level(), PROFILE);
        player.getInventory().clearContent();
        player.setPos(golem.position());
        player.setYRot(golem.getYRot());
        player.setXRot(golem.getXRot());
        player.setShiftKeyDown(golem.getToggles()[2]);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        return player;
    }

    private static ItemStack finish(GolemBase golem, ServerPlayer player) {
        ItemStack remaining = player.getMainHandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack extra = player.getInventory().getItem(slot);
            if (!extra.isEmpty()) golem.spawnAtLocation(extra.copy());
        }
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        return remaining;
    }

    public static boolean useItem(GolemBase golem, BlockPos pos, Direction side) {
        if (!(golem.level() instanceof ServerLevel level) || !level.hasChunkAt(pos)) return false;
        ServerPlayer player = prepare(golem, golem.getCarried());
        try {
            if (golem.getToggles()[1]) {
                var state = level.getBlockState(pos);
                state.attack(level, pos, player);
                if (!state.isAir() && state.getDestroyProgress(player, level, pos) >= 1) player.gameMode.destroyBlock(pos);
                return !state.isAir();
            }
            return player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, side)).consumesAction();
        } finally {
            golem.setCarried(finish(golem, player));
        }
    }

    public static boolean activate(GolemBase golem, BlockPos pos, Direction side) {
        if (!(golem.level() instanceof ServerLevel level) || !level.hasChunkAt(pos)) return false;
        ServerPlayer player = prepare(golem, ItemStack.EMPTY);
        try {
            return player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit(pos, side)).consumesAction();
        } finally {
            ItemStack remainder = finish(golem, player);
            if (!remainder.isEmpty()) golem.spawnAtLocation(remainder);
        }
    }

    public static ItemStack plant(GolemBase golem, BlockPos pos, Direction side, ItemStack stack) {
        if (!(golem.level() instanceof ServerLevel level) || !level.hasChunkAt(pos)) return stack;
        ServerPlayer player = prepare(golem, stack);
        try {
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, side));
        } finally {
            stack = finish(golem, player);
        }
        return stack;
    }

    public static boolean harvest(GolemBase golem, BlockPos pos) {
        if (!(golem.level() instanceof ServerLevel level) || !level.hasChunkAt(pos)) return false;
        ServerPlayer player = prepare(golem, ItemStack.EMPTY);
        try {
            return player.gameMode.destroyBlock(pos);
        } finally {
            ItemStack remainder = finish(golem, player);
            if (!remainder.isEmpty()) golem.spawnAtLocation(remainder);
        }
    }

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.5)), side, pos, false);
    }
}
