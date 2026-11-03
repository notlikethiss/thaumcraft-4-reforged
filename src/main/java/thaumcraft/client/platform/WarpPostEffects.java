package thaumcraft.client.platform;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;
import thaumcraft.Thaumcraft;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WarpPostEffects implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final WarpPostEffects INSTANCE = new WarpPostEffects();

    private final Map<ResourceLocation, PostChain> chains = new LinkedHashMap<>();
    private List<ResourceLocation> activeEffects = List.of();
    private int width = -1;
    private int height = -1;

    private WarpPostEffects() {
    }

    public static ResourceLocation effect(String path) {
        return Thaumcraft.id(path);
    }

    public void setActiveEffects(Collection<ResourceLocation> effects) {
        this.activeEffects = List.copyOf(effects);
    }

    public void clearActiveEffects() {
        this.activeEffects = List.of();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        closeChains();
        Minecraft minecraft = Minecraft.getInstance();
        this.width = minecraft.getWindow().getWidth();
        this.height = minecraft.getWindow().getHeight();
        for (String name : List.of("desaturatetc", "blurtc", "hunger", "sunscorned")) {
            ResourceLocation effect = effect(name);
            try {
                PostChain chain = new PostChain(
                    minecraft.getTextureManager(), resourceManager, minecraft.getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(effect.getNamespace(), "shaders/post/" + effect.getPath() + ".json")
                );
                chain.resize(this.width, this.height);
                this.chains.put(effect, chain);
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Failed to load Thaumcraft warp post effect {}", effect, exception);
            }
        }
    }

    public void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || this.activeEffects.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int currentWidth = minecraft.getWindow().getWidth();
        int currentHeight = minecraft.getWindow().getHeight();
        if (this.width != currentWidth || this.height != currentHeight) {
            this.width = currentWidth;
            this.height = currentHeight;
            this.chains.values().forEach(chain -> chain.resize(currentWidth, currentHeight));
        }

        boolean processed = false;
        for (ResourceLocation effect : this.activeEffects) {
            PostChain chain = this.chains.get(effect);
            if (chain != null) {
                if (!processed) {
                    RenderSystem.disableBlend();
                    RenderSystem.disableDepthTest();
                    RenderSystem.resetTextureMatrix();
                }
                chain.process(event.getPartialTick().getGameTimeDeltaPartialTick(false));
                processed = true;
            }
        }
        if (processed) {
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    @SubscribeEvent
    static void renderStage(RenderLevelStageEvent event) {
        INSTANCE.render(event);
    }

    public void close() {
        this.activeEffects = List.of();
        closeChains();
    }

    private void closeChains() {
        for (PostChain chain : this.chains.values()) {
            chain.close();
        }
        this.chains.clear();
    }
}
