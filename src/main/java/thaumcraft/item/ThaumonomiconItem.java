package thaumcraft.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchClientHooks;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;

public class ThaumonomiconItem extends Item {
    private final boolean cheat;

    public ThaumonomiconItem(Properties properties, boolean cheat) {
        super(properties);
        this.cheat = cheat;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(1).rarity(Rarity.UNCOMMON);
    }

    public static Properties cheatProperties(Properties properties) {
        return properties.stacksTo(1).rarity(Rarity.EPIC);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = handleUse(level, player, hand);
        return InteractionResults.holder(result, player.getItemInHand(hand), level);
    }

    private InteractionResult handleUse(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (cheat) {
                for (ResearchItem item : ResearchCategories.allResearch()) {
                    ResearchManager.complete(serverPlayer, item.key(), false);
                }
                for (Aspect aspect : Aspect.values()) {
                    if (!Knowledge.get(serverPlayer).hasDiscoveredAspect(aspect)) {
                        Knowledge.get(serverPlayer).setAspectPool(aspect, 50);
                    }
                }
            } else {
                for (ResearchItem item : ResearchCategories.allResearch()) {
                    if (ResearchManager.isComplete(serverPlayer, item.key())) {
                        for (String sibling : item.siblings()) {
                            ResearchManager.complete(serverPlayer, sibling, false);
                        }
                    }
                }
            }
            Knowledge.sync(serverPlayer);
        } else if (level.isClientSide()) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.PAGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
            ResearchClientHooks.openBook();
        }
        return InteractionResult.SUCCESS;
    }
}
