package thaumcraft.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.network.AspectDiscoveryPayload;
import thaumcraft.network.AspectPoolPayload;
import thaumcraft.network.PlayerCompletePayload;
import thaumcraft.network.ResearchCompletePayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModSounds;

public final class ResearchManager {
    public static final int ACTION_PURCHASE = 0;
    public static final int ACTION_CREATE_NOTE = 1;

    private ResearchManager() {
    }

    public static boolean isComplete(Player player, String key) {
        return Knowledge.get(player).isResearchComplete(key);
    }

    public static boolean hasRequisites(Player player, String key) {
        ResearchItem item = ResearchCategories.getResearch(key);
        if (item == null) {
            return false;
        }
        for (String parent : item.parents()) {
            if (!isComplete(player, parent)) {
                return false;
            }
        }
        for (String parent : item.parentsHidden()) {
            if (!isComplete(player, parent)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBuyable(ResearchItem item) {
        return ResearchConfig.RESEARCH_DIFFICULTY == -1 || ResearchConfig.RESEARCH_DIFFICULTY == 0 && item.isSecondary();
    }

    public static boolean complete(ServerPlayer player, String key) {
        return complete(player, key, true);
    }

    public static boolean complete(ServerPlayer player, String key, boolean notify) {
        PlayerKnowledge knowledge = Knowledge.get(player);
        if (!knowledge.addResearch(key)) {
            return false;
        }
        ResearchItem item = ResearchCategories.getResearch(key);
        int warp = item == null ? 0 : item.warp();
        if (warp > 0 && !ResearchConfig.WUSS_MODE) {
            if (warp > 1) {
                int sticky = warp / 2;
                if (warp - sticky > 0) {
                    Warp.add(player, warp - sticky, WarpType.PERMANENT);
                }
                if (sticky > 0) {
                    Warp.add(player, sticky, WarpType.STICKY);
                }
            } else {
                Warp.add(player, warp, WarpType.PERMANENT);
            }
        }
        Knowledge.sync(player);
        if (notify) {
            ModNetwork.sendToPlayer(player, new ResearchCompletePayload(key));
        }
        return true;
    }

    public static void completeWithSiblings(ServerPlayer player, String key) {
        complete(player, key);
        ResearchItem item = ResearchCategories.getResearch(key);
        if (item == null) {
            return;
        }
        for (String sibling : item.siblings()) {
            if (!isComplete(player, sibling) && hasRequisites(player, sibling)) {
                complete(player, sibling);
            }
        }
    }

    public static void completeRecursive(ServerPlayer player, String key) {
        if (isComplete(player, key)) {
            return;
        }
        complete(player, key, false);
        ResearchItem item = ResearchCategories.getResearch(key);
        if (item == null) {
            return;
        }
        for (String parent : item.parents()) {
            completeRecursive(player, parent);
        }
        for (String parent : item.parentsHidden()) {
            completeRecursive(player, parent);
        }
        for (String sibling : item.siblings()) {
            completeRecursive(player, sibling);
        }
    }

    public static void grantAutoUnlock(ServerPlayer player) {
        for (ResearchItem item : ResearchCategories.allResearch()) {
            if (item.isAutoUnlock()) {
                complete(player, item.key(), false);
            }
        }
    }

    public static void grantStartingPool(ServerPlayer player) {
        PlayerKnowledge knowledge = Knowledge.get(player);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            knowledge.setAspectPool(aspect, 15 + player.getRandom().nextInt(5));
        }
        knowledge.setStarted(true);
        Knowledge.sync(player);
    }

    public static boolean addAspectPool(ServerPlayer player, Aspect aspect, int amount) {
        PlayerKnowledge knowledge = Knowledge.get(player);
        if (amount == 0) {
            return false;
        }
        if (amount > 0) {
            knowledge.discoverAspect(aspect);
            knowledge.addAspectPool(aspect, amount);
        } else if (knowledge.getAspectPool(aspect) > 0) {
            knowledge.setAspectPool(aspect, Math.max(0, knowledge.getAspectPool(aspect) + amount));
        } else {
            return false;
        }
        Knowledge.sync(player);
        ModNetwork.sendToPlayer(player, new AspectPoolPayload(aspect, amount, knowledge.getAspectPool(aspect)));
        return true;
    }

    public static boolean spendAspectPool(ServerPlayer player, Aspect aspect, int amount) {
        if (amount <= 0 || Knowledge.get(player).getAspectPool(aspect) < amount) {
            return false;
        }
        return addAspectPool(player, aspect, -amount);
    }

    public static boolean checkAndSyncAspectKnowledge(ServerPlayer player, Aspect aspect, int amount) {
        return awardAspect(player, aspect, amount) > 0;
    }

    public static int awardAspect(ServerPlayer player, Aspect aspect, int amount) {
        PlayerKnowledge knowledge = Knowledge.get(player);
        int saved = 0;
        if (!knowledge.hasDiscoveredAspect(aspect)) {
            ModNetwork.sendToPlayer(player, new AspectDiscoveryPayload(aspect));
            amount += 2;
            saved = amount;
        }
        int pool = knowledge.getAspectPool(aspect);
        if (pool >= ResearchConfig.ASPECT_TOTAL_CAP) {
            amount = (int) Math.sqrt(amount);
        }
        if (amount > 1 && pool >= ResearchConfig.ASPECT_TOTAL_CAP * 1.25F) {
            amount = 1;
        }
        if (addAspectPool(player, aspect, amount)) {
            saved = amount;
        }
        return saved;
    }

    public static boolean createClue(ServerPlayer player, ItemStack stack, @Nullable AspectList aspects) {
        return createClue(player, BuiltInRegistries.ITEM.getKey(stack.getItem()), true, aspects);
    }

    public static boolean createClue(ServerPlayer player, ResourceLocation entityType, @Nullable AspectList aspects) {
        return createClue(player, entityType, false, aspects);
    }

    private static boolean createClue(ServerPlayer player, ResourceLocation clue, boolean item, @Nullable AspectList aspects) {
        List<String> keys = new ArrayList<>();
        for (ResearchItem research : ResearchCategories.allResearch()) {
            boolean valid = research.tags() != null
                && research.tags().size() > 0
                && (research.isLost() || research.isHidden())
                && !isComplete(player, research.key())
                && !isComplete(player, "@" + research.key());
            if (!valid) {
                continue;
            }
            if (item && !research.itemTriggers().isEmpty()) {
                if (research.itemTriggers().contains(clue)) {
                    keys.add(research.key());
                    continue;
                }
            } else if (!item && !research.entityTriggers().isEmpty()) {
                if (research.entityTriggers().contains(clue)) {
                    keys.add(research.key());
                    continue;
                }
            }
            if (aspects != null && aspects.size() > 0) {
                for (Aspect aspect : research.aspectTriggers()) {
                    if (aspects.getAmount(aspect) > 0) {
                        keys.add(research.key());
                        break;
                    }
                }
            }
        }
        if (keys.isEmpty()) {
            return false;
        }
        complete(player, "@" + keys.get(player.getRandom().nextInt(keys.size())));
        return true;
    }

    public static boolean createClue(ServerPlayer player, String researchKey) {
        ResearchItem research = ResearchCategories.getResearch(researchKey);
        if (research == null || isComplete(player, researchKey) || isComplete(player, "@" + researchKey)) {
            return false;
        }
        return complete(player, "@" + researchKey);
    }

    public static @Nullable String findHiddenResearch(ServerPlayer player) {
        List<String> keys = new ArrayList<>();
        for (ResearchItem research : ResearchCategories.allResearch()) {
            boolean triggered = !research.itemTriggers().isEmpty() || !research.entityTriggers().isEmpty() || !research.aspectTriggers().isEmpty();
            if (research.isHidden()
                && research.tags() != null
                && research.tags().size() > 0
                && !isComplete(player, research.key())
                && hasRequisites(player, research.key())
                && triggered) {
                keys.add(research.key());
            }
        }
        if (keys.isEmpty()) {
            return null;
        }
        Random random = new Random(player.level().getGameTime() / 10L / 5L);
        return keys.get(random.nextInt(keys.size()));
    }

    public static void handlePlayerComplete(PlayerCompletePayload payload, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String key = payload.key();
        ResearchItem item = ResearchCategories.getResearch(key);
        if (item == null || isComplete(serverPlayer, key)) {
            return;
        }
        if (!hasRequisites(serverPlayer, key)) {
            serverPlayer.sendSystemMessage(Component.translatable("tc.researcherror"));
            return;
        }
        if (payload.action() == ACTION_PURCHASE) {
            if (!isBuyable(item) || item.tags().size() == 0 || !canAfford(serverPlayer, item.tags())) {
                return;
            }
            for (Aspect aspect : item.tags().getAspects()) {
                spendAspectPool(serverPlayer, aspect, item.tags().getAmount(aspect));
            }
            completeWithSiblings(serverPlayer, key);
            playLearn(serverPlayer);
        } else if (payload.action() == ACTION_CREATE_NOTE) {
            ResearchNotes.createNoteForPlayer(serverPlayer, key);
            playLearn(serverPlayer);
        }
    }

    public static boolean canAfford(Player player, AspectList cost) {
        PlayerKnowledge knowledge = Knowledge.get(player);
        for (Aspect aspect : cost.getAspects()) {
            if (knowledge.getAspectPool(aspect) < cost.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    private static void playLearn(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
    }
}
