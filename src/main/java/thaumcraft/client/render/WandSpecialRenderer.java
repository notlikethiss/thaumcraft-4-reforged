package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.WandGeometry;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.WandParts;
import thaumcraft.wand.WandRod;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.FocusItem;

public class WandSpecialRenderer implements SpecialModelRenderer<WandSpecialRenderer.State>, OwnerSpecialModel.OwnerArgument<WandSpecialRenderer.State> {
    private static final Identifier FOCUS = Thaumcraft.id("textures/model/wand.png");

    public record Focus(@Nullable Identifier ornament, @Nullable Identifier depth, int color) {
    }

    public record State(WandParts parts, @Nullable Focus focus, float useTicks, boolean wave, boolean charge) {
    }

    private final LegacyItemView view;

    public WandSpecialRenderer(LegacyItemView view) {
        this.view = view;
    }

    private static void transform(PoseStack poseStack, LegacyItemView view, boolean staff) {
        view.apply(poseStack);
        if (staff) {
            poseStack.translate(0.0F, 0.5F, 0.0F);
        }
        switch (view) {
            case INVENTORY -> {
                if (staff) {
                    poseStack.scale(0.8F, 0.8F, 0.8F);
                }
                poseStack.mulPose(Axis.ZP.rotationDegrees(66.0F));
                poseStack.translate(0.0F, 0.6F, 0.0F);
                if (staff) {
                    poseStack.translate(-0.7F, 0.6F, 0.0F);
                }
            }
            case ENTITY -> {
                if (staff) {
                    poseStack.translate(0.0F, 1.5F, 0.0F);
                    poseStack.scale(0.9F, 0.9F, 0.9F);
                } else {
                    poseStack.translate(0.0F, 1.0F, 0.0F);
                }
            }
            case EQUIPPED, FIRST_PERSON -> {
                poseStack.translate(0.5F, 1.5F, 0.5F);
                if (view == LegacyItemView.FIRST_PERSON) {
                    poseStack.scale(1.0F, 1.1F, 1.0F);
                }
            }
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    private static final Identifier SCRIPT = Thaumcraft.id("textures/misc/script.png");
    private static final int BLEND_ADDITIVE = 1;

    private void animate(PoseStack poseStack, State state) {
        if ((view != LegacyItemView.FIRST_PERSON && view != LegacyItemView.EQUIPPED) || state.useTicks() < 0.0F) {
            return;
        }
        float t = Math.min(state.useTicks(), 3.0F);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        if (view != LegacyItemView.FIRST_PERSON) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(33.0F));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
        }
        poseStack.mulPose(Axis.XN.rotationDegrees(60.0F * (t / 3.0F)));
        if (state.wave()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(state.useTicks() / 10.0F) * 10.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(state.useTicks() / 15.0F) * 10.0F));
        } else if (state.charge()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(state.useTicks() / 0.8F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(state.useTicks() / 0.7F)));
        }
        poseStack.translate(0.0F, -1.0F, 0.0F);
    }

    private static @Nullable TextureAtlasSprite sprite(@Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        return atlas.getSprite(id);
    }

    private static void renderFocus(PoseStack poseStack, SubmitNodeCollector collector, Focus focus, boolean staff, float ticks, int lightCoords) {
        TextureAtlasSprite ornament = sprite(focus.ornament());
        if (ornament != null) {
            ItemGeometry.solid(collector.order(0), poseStack, ornament.atlasLocation(), true, sink -> WandGeometry.focusOrnament(
                sink, staff, ornament.getU0(), ornament.getV0(), ornament.getU1(), ornament.getV1(), lightCoords
            ));
        }
        float alpha = 0.95F;
        TextureAtlasSprite depth = sprite(focus.depth());
        if (depth != null) {
            ItemGeometry.solid(collector.order(1), poseStack, depth.atlasLocation(), true, sink -> WandGeometry.focusDepth(
                sink, staff, depth.getU0(), depth.getV0(), depth.getU1(), depth.getV1(), lightCoords
            ));
            alpha = 0.6F;
        }
        int glow = (int) (195.0F + Mth.sin(ticks / 3.0F) * 10.0F + 10.0F);
        int light = Math.max(lightCoords & 0xFFFF, glow) | (lightCoords & 0xFFFF0000);
        int argb = Math.clamp((int) (alpha * 255.0F), 0, 255) << 24 | focus.color() & 0xFFFFFF;
        ItemGeometry.solid(collector.order(2), poseStack, FOCUS, true, sink -> WandGeometry.focus(sink, staff, argb, light));
    }

    @Override
    public void submit(
        @Nullable State state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int lightCoords,
        int overlayCoords,
        boolean hasFoil,
        int outlineColor
    ) {
        poseStack.pushPose();
        WandParts resolved = state == null ? WandParts.DEFAULT : state.parts();
        WandRod rod = resolved.getRod();
        boolean staff = rod.staff();
        boolean sceptre = resolved.sceptre();
        transform(poseStack, view, staff);
        if (state != null) {
            animate(poseStack, state);
        }
        float ticks = Util.getMillis() / 50.0F;
        int rodLight = lightCoords;
        if (rod.glowing()) {
            int glow = (int) (200.0F + (float) Math.sin(ticks) * 5.0F + 5.0F);
            rodLight = Math.max(lightCoords & 0xFFFF, glow) | (lightCoords & 0xFFFF0000);
        }
        int light = rodLight;
        ItemGeometry.solid(collector.order(0), poseStack, rod.texture(), false, sink -> WandGeometry.rod(sink, staff, light, 0xFFFFFFFF));
        ItemGeometry.solid(collector.order(0), poseStack, resolved.getCap().texture(), false, sink -> WandGeometry.caps(sink, staff, sceptre, lightCoords));
        if (state != null && state.focus() != null) {
            renderFocus(poseStack, collector, state.focus(), staff, ticks, lightCoords);
        }
        if (sceptre || rod.runes()) {
            int runeLight = Math.max(lightCoords & 0xFFFF, 200) | (lightCoords & 0xFFFF0000);
            ItemGeometry.blended(
                collector.order(3),
                poseStack,
                SCRIPT,
                BLEND_ADDITIVE,
                sink -> WandGeometry.runes(sink, staff, sceptre, rod.runes(), ticks, runeLight)
            );
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (boolean staff : new boolean[] {false, true}) {
            PoseStack poseStack = new PoseStack();
            transform(poseStack, view, staff);
            float top = staff ? 2.4F : 1.4F;
            for (float x : new float[] {-0.15F, 0.15F}) {
                for (float y : new float[] {-0.1F, top}) {
                    for (float z : new float[] {-0.15F, 0.15F}) {
                        output.accept(poseStack.last().pose().transformPosition(x, y, z, new Vector3f()));
                    }
                }
            }
        }
    }

    @Override
    public @Nullable State extractArgument(ItemStack stack) {
        return extractArgument(stack, null);
    }

    @Override
    public @Nullable State extractArgument(ItemStack stack, @Nullable ItemOwner owner) {
        FocusItem focusItem = Wands.getFocus(stack);
        ItemStack focusStack = Wands.getFocusItem(stack);
        Focus focus = focusItem == null ? null : new Focus(
            focusItem.getOrnamentTexture(focusStack),
            focusItem.getDepthLayerTexture(focusStack),
            focusItem.getFocusColor(focusStack)
        );
        float useTicks = -1.0F;
        LivingEntity living = owner == null ? null : owner.asLivingEntity();
        if (living instanceof Player player && player.isUsingItem() && player.getUseItem() == stack) {
            useTicks = player.getTicksUsingItem(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        }
        FocusItem.WandFocusAnimation animation = focusItem == null ? null : focusItem.getAnimation(focusStack);
        boolean wave = WandKeys.TARGET.has(stack) || animation == FocusItem.WandFocusAnimation.WAVE;
        boolean charge = !wave && animation == FocusItem.WandFocusAnimation.CHARGE;
        return new State(Wands.getParts(stack), focus, useTicks, wave, charge);
    }

    @Override
    public boolean isAnimated(@Nullable State state) {
        if (state == null) {
            return false;
        }
        WandRod rod = state.parts().getRod();
        return state.useTicks() >= 0.0F || state.focus() != null || rod.glowing() || rod.runes() || state.parts().sceptre();
    }

    public record Unbaked(LegacyItemView view) implements SpecialModelRenderer.Unbaked<State> {
        public static final MapCodec<Unbaked> MAP_CODEC = LegacyItemView.FIELD.xmap(Unbaked::new, Unbaked::view);

        @Override
        public SpecialModelRenderer<State> bake(SpecialModelRenderer.BakingContext context) {
            return new WandSpecialRenderer(view);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
