package thaumcraft.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TieredItem;
import thaumcraft.platform.Registration;
import thaumcraft.item.gear.TcMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.SelfRepair;
import thaumcraft.item.gear.WarpingGear;
import thaumcraft.wand.focus.FocusUtils;

public class PrimalCrusherItem extends TieredItem implements Repairable, WarpingGear {
    private static final int WARP = 2;
    private static final int FOLLOW_COLOR = 2;
    private static final Block[] EFFECTIVE = {
        Blocks.COBBLESTONE, Blocks.SMOOTH_STONE_SLAB, Blocks.STONE_SLAB, Blocks.STONE, Blocks.SANDSTONE, Blocks.MOSSY_COBBLESTONE,
        Blocks.IRON_ORE, Blocks.IRON_BLOCK, Blocks.COAL_ORE, Blocks.GOLD_BLOCK, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE, Blocks.DIAMOND_BLOCK,
        Blocks.ICE, Blocks.NETHERRACK, Blocks.LAPIS_ORE, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_ORE, Blocks.RAIL, Blocks.DETECTOR_RAIL,
        Blocks.POWERED_RAIL, Blocks.ACTIVATOR_RAIL, Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.SNOW,
        Blocks.SNOW_BLOCK, Blocks.CLAY, Blocks.FARMLAND, Blocks.SOUL_SAND, Blocks.MYCELIUM, Blocks.OBSIDIAN
    };

    public PrimalCrusherItem(Properties properties) {
        super(Registration.tier(TcMaterials.PRIMAL_VOID_TOOL), properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        SelfRepair.tick(stack, owner);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return WARP;
    }

    private static boolean isEffective(ItemStack stack, BlockState state) {
        if (stack.isCorrectToolForDrops(state) || state.is(FocusUtils.TAINT)) {
            return true;
        }
        for (Block block : EFFECTIVE) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (ToolHarvest.isHarvesting()) {
            return true;
        }
        if (owner.isShiftKeyDown()) {
            return super.mineBlock(stack, level, state, pos, owner);
        }
        if (level instanceof ServerLevel serverLevel && owner instanceof ServerPlayer player && isEffective(stack, state)) {
            ToolHarvest.breakArea(stack, serverLevel, player, pos, target -> isEffective(stack, target), FOLLOW_COLOR);
        }
        return true;
    }
}
