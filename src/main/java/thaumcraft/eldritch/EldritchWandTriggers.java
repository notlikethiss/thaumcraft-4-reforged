package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeType;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.ResearchGate;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandTriggers;
import thaumcraft.wand.Wands;
import thaumcraft.world.dim.MazeHandler;
import thaumcraft.world.dim.OuterLands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class EldritchWandTriggers {
    private EldritchWandTriggers() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> WandTriggers.register(state -> state.is(EldritchContent.ELDRITCH_ALTAR.get()), EldritchWandTriggers::activateAltar));
    }

    private static boolean activateAltar(Level level, ItemStack stack, Player player, BlockPos pos, Direction side) {
        if (!(level instanceof ServerLevel serverLevel)
            || !(player instanceof ServerPlayer serverPlayer)
            || !Wands.isWand(stack)
            || !ResearchGate.isComplete(player, "OCULUS")
            || !(level.getBlockEntity(pos) instanceof EldritchBlockEntity altar)
            || altar.eyeCount() != 4
            || altar.isOpen()
            || !(level.getBlockEntity(pos.above()) instanceof NodeBlockEntity node)
            || node.getNodeType() != NodeType.DARK) {
            return false;
        }
        ServerLevel outer = serverLevel.getServer().getLevel(OuterLands.OUTER_LANDS);
        if (outer == null || !MazeHandler.ensureMaze(outer, pos)) {
            return false;
        }
        AspectList cost = new AspectList()
            .add(Aspect.AIR, 100)
            .add(Aspect.FIRE, 100)
            .add(Aspect.EARTH, 100)
            .add(Aspect.WATER, 100)
            .add(Aspect.ORDER, 100)
            .add(Aspect.ENTROPY, 100);
        if (!(stack.getItem() instanceof WandItem wand) || !wand.consumeAllVisCrafting(stack, serverPlayer, cost, true)) {
            return false;
        }
        altar.setOpen(true);
        level.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), EldritchContent.ELDRITCH_PORTAL.get().defaultBlockState(), 3);
        level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ModNetwork.sendToNear(serverLevel, null, pos.getX(), pos.getY(), pos.getZ(), 32.0, new thaumcraft.network.BlockSparkleFxPayload(pos, -9999));
        altar.sync();
        return true;
    }
}
