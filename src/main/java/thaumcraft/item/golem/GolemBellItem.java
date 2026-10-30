package thaumcraft.item.golem;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.entity.golem.Marker;
import thaumcraft.platform.Transfer;

public class GolemBellItem extends Item {
    public GolemBellItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof GolemBase golem)) return InteractionResult.PASS;
        if (!player.level().isClientSide()) {
            if (player.isShiftKeyDown()) GolemKeys.BELL.remove(stack);
            else GolemKeys.BELL.set(stack, selection(golem));
            ring(player.level(), golem.blockPosition());
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.ANY);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && !level.getFluidState(hit.getBlockPos()).isEmpty()) {
            return this.useOn(new UseOnContext(player, hand, hit));
        }
        if (level instanceof ServerLevel server && !(player.isShiftKeyDown() && resetMarkers(server, player.getItemInHand(hand), player))) ring(level, player.blockPosition());
        return InteractionResult.SUCCESS;
    }
    public static boolean resetMarkers(ServerLevel level, ItemStack stack, Player player) {
        var selection = GolemKeys.BELL.get(stack);
        if (selection == null || !(level.getEntity(selection.entity()) instanceof GolemBase golem)) return false;
        golem.setMarkers(java.util.List.of());
        GolemKeys.BELL.set(stack, selection(golem));
        ring(level, player.blockPosition());
        return true;
    }
    private static GolemKeys.BellSelection selection(GolemBase golem) {
        return new GolemKeys.BellSelection(golem.getUUID(), GlobalPos.of(golem.level().dimension(), golem.getHomePosition()), golem.getHomeFacing(), golem.getMarkers());
    }
    private static void ring(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.0F + level.getRandom().nextFloat() * 0.1F);
    }
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var selection = GolemKeys.BELL.get(context.getItemInHand());
        if (context.getPlayer() == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (selection == null) {
            ring(level, context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        if (!(level.getEntity(selection.entity()) instanceof GolemBase golem)) { GolemKeys.BELL.remove(context.getItemInHand()); return InteractionResult.SUCCESS; }
        GlobalPos pos = GlobalPos.of(level.dimension(), context.getClickedPos());
        if (context.getPlayer().isShiftKeyDown() && Transfer.hasItems(level, context.getClickedPos(), context.getClickedFace())) {
            golem.setHome(context.getClickedPos().relative(context.getClickedFace()), context.getClickedFace());
            golem.getNavigation().stop();
            golem.rebuildGoals();
            GolemKeys.BELL.set(context.getItemInHand(), selection(golem));
            ring(level, context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        var markers = new ArrayList<>(golem.getMarkers());
        int existing = -1;
        for (int index = 0; index < markers.size(); index++) if (markers.get(index).location().equals(pos) && markers.get(index).side() == context.getClickedFace()) { existing = index; break; }
        if (existing >= 0) {
            Marker marker = markers.remove(existing);
            if (golem.getUpgradeAmount(4) > 0 && !context.getPlayer().isShiftKeyDown() && marker.color() < 15) markers.add(new Marker(pos, context.getClickedFace(), marker.color() + 1));
        } else markers.add(new Marker(pos, context.getClickedFace(), -1));
        golem.setMarkers(markers);
        GolemKeys.BELL.set(context.getItemInHand(), selection(golem));
        ring(level, context.getClickedPos());
        return InteractionResult.SUCCESS;
    }
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof thaumcraft.entity.golem.TravelingTrunk trunk) {
            if (trunk.getUpgrade() == 3 && !player.getUUID().equals(trunk.getOwnerUUID())) return true;
            if (entity.level() instanceof ServerLevel level && entity.isAlive()) {
                int upgrade = trunk.getUpgrade();
                if (player.isShiftKeyDown() && upgrade >= 0 && level.getRandom().nextBoolean()) trunk.spawnAtLocation(level, GolemItems.upgrade(upgrade));
                trunk.spawnAtLocation(level, trunk.toItem(player.isShiftKeyDown()));
                trunk.discard();
            }
            return true;
        }
        if (!(entity instanceof GolemBase golem)) return false;
        if (entity.level() instanceof ServerLevel level && entity.isAlive()) {
            boolean dismantle = player.isShiftKeyDown();
            if (dismantle) {
                if (golem.getCore() >= 0) golem.spawnAtLocation(level, GolemItems.core(golem.getCore()));
                for (int index = 0; index < golem.snapshot().upgrades().size(); index++) if (golem.getUpgrade(index) >= 0 && level.getRandom().nextBoolean()) golem.spawnAtLocation(level, GolemItems.upgrade(golem.getUpgrade(index)));
            }
            golem.spawnAtLocation(level, golem.toItem(dismantle));
            golem.dropCarried();
            golem.discard();
        }
        return true;
    }
}
