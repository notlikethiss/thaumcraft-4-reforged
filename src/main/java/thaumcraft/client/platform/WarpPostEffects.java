package thaumcraft.client.platform;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WarpPostEffects {
    private static final CrossFrameResourcePool RESOURCE_POOL = new CrossFrameResourcePool(3);
    private static List<Identifier> active = List.of();

    private WarpPostEffects() {
    }

    public static void sync(LocalPlayer player, Collection<Identifier> ownedEffects, Collection<Identifier> activeEffects) {
        active = List.copyOf(activeEffects);
    }

    public static void clear(@Nullable LocalPlayer player, Collection<Identifier> ownedEffects) {
        active = List.of();
    }

    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        if (active.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        for (Identifier effect : active) {
            PostChain chain = minecraft.getShaderManager().getPostChain(effect, LevelTargetBundle.MAIN_TARGETS);
            if (chain != null) {
                chain.process(minecraft.getMainRenderTarget(), RESOURCE_POOL);
            }
        }
        RESOURCE_POOL.endFrame();
    }
}
