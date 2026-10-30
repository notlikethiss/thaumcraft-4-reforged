package thaumcraft.item.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.registry.ModEntities;

public class GolemPlacerItem extends Item {
    private final GolemType type;
    public GolemPlacerItem(Properties properties, GolemType type) { super(properties.stacksTo(1)); this.type = type; }
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            GolemBase golem = new GolemBase(ModEntities.GOLEM.get(), context.getLevel());
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            golem.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            golem.setYRot(context.getLevel().getRandom().nextFloat() * 360);
            golem.configure(type, GolemKeys.snapshot(context.getItemInHand()));
            GolemKeys.FLUID.getOrDefault(context.getItemInHand(), java.util.Optional.empty()).ifPresent(fluid -> golem.setFluidCarried(fluid.create()));
            golem.setEssentia(thaumcraft.aspect.Aspect.getAspect(GolemKeys.ESSENTIA.getOrDefault(context.getItemInHand(), "")), GolemKeys.ESSENTIA_AMOUNT.getOrDefault(context.getItemInHand(), 0));
            golem.setHealth(golem.getMaxHealth());
            golem.setHome(pos, context.getClickedFace());
            golem.setOwnerUUID(context.getPlayer().getUUID());
            golem.setCustomName(context.getItemInHand().get(DataComponents.CUSTOM_NAME));
            if (!context.getLevel().noCollision(golem) || !context.getLevel().addFreshEntity(golem)) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
