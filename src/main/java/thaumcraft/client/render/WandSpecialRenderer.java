package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.focus.FocusItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.client.geom.WandGeometry;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.wand.WandParts;
import thaumcraft.wand.WandRod;
import thaumcraft.wand.Wands;

public final class WandSpecialRenderer {
    private static final ResourceLocation FOCUS = Thaumcraft.id("textures/model/wand.png");
    private static @Nullable LivingEntity owner;

    public record Focus(@Nullable ResourceLocation ornament, @Nullable ResourceLocation depth, int color) {
    }

    public record State(WandParts parts, @Nullable Focus focus, float useTicks, boolean wave, boolean charge) {
    }

    public static void setOwner(@Nullable LivingEntity entity) {
        owner = entity;
    }

    private static State state(ItemStack stack) {
        FocusItem focus = Wands.getFocus(stack);
        ItemStack focusStack = Wands.getFocusItem(stack);
        Focus visual = focus == null ? null : new Focus(focus.getOrnamentTexture(focusStack), focus.getDepthLayerTexture(focusStack), focus.getFocusColor(focusStack));
        float useTicks = -1.0F;
        if (owner instanceof Player player && player.isUsingItem() && player.getUseItem() == stack) {
            useTicks = player.getTicksUsingItem() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        }
        FocusItem.WandFocusAnimation animation = focus == null ? null : focus.getAnimation(focusStack);
        boolean wave = WandKeys.TARGET.has(stack) || animation == FocusItem.WandFocusAnimation.WAVE;
        return new State(Wands.getParts(stack), visual, useTicks, wave, !wave && animation == FocusItem.WandFocusAnimation.CHARGE);
    }

    private WandSpecialRenderer() {
    }

    private static void transform(PoseStack poseStack, LegacyItemView view, boolean staff, float useTicks) {
        view.apply(poseStack, useTicks);
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

    private static final ResourceLocation SCRIPT = Thaumcraft.id("textures/misc/script.png");
    private static final int BLEND_ADDITIVE = 1;

    private static void animate(PoseStack poseStack, State state, LegacyItemView view) {
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

    private static @Nullable TextureAtlasSprite sprite(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
    }

    private static void renderFocus(PoseStack poseStack, MultiBufferSource buffers, Focus focus, boolean staff, float ticks, int lightCoords) {
        TextureAtlasSprite ornament = sprite(focus.ornament());
        if (ornament != null) {
            ItemGeometry.solid(buffers, poseStack, ornament.atlasLocation(), true, sink -> WandGeometry.focusOrnament(
                sink, staff, ornament.getU0(), ornament.getV0(), ornament.getU1(), ornament.getV1(), lightCoords
            ));
        }
        float alpha = 0.95F;
        TextureAtlasSprite depth = sprite(focus.depth());
        if (depth != null) {
            ItemGeometry.solid(buffers, poseStack, depth.atlasLocation(), true, sink -> WandGeometry.focusDepth(
                sink, staff, depth.getU0(), depth.getV0(), depth.getU1(), depth.getV1(), lightCoords
            ));
            alpha = 0.6F;
        }
        int glow = (int) (195.0F + Mth.sin(ticks / 3.0F) * 10.0F + 10.0F);
        int light = Math.max(lightCoords & 0xFFFF, glow) | (lightCoords & 0xFFFF0000);
        int argb = Math.clamp((int) (alpha * 255.0F), 0, 255) << 24 | focus.color() & 0xFFFFFF;
        ItemGeometry.solid(buffers, poseStack, FOCUS, true, sink -> WandGeometry.focus(sink, staff, argb, light));
    }

    public static void render(ItemStack stack, LegacyItemView view, PoseStack poseStack, MultiBufferSource buffers, int lightCoords) {
        poseStack.pushPose();
        State state = state(stack);
        WandParts parts = state.parts();
        WandParts resolved = parts == null ? WandParts.DEFAULT : parts;
        WandRod rod = resolved.getRod();
        boolean staff = rod.staff();
        boolean sceptre = resolved.sceptre();
        transform(poseStack, view, staff, state.useTicks());
        animate(poseStack, state, view);
        float ticks = Util.getMillis() / 50.0F;
        int rodLight = lightCoords;
        if (rod.glowing()) {
            int glow = (int) (200.0F + (float) Math.sin(ticks) * 5.0F + 5.0F);
            rodLight = Math.max(lightCoords & 0xFFFF, glow) | (lightCoords & 0xFFFF0000);
        }
        int light = rodLight;
        ItemGeometry.solid(buffers, poseStack, rod.texture(), false, sink -> WandGeometry.rod(sink, staff, light, 0xFFFFFFFF));
        ItemGeometry.solid(buffers, poseStack, resolved.getCap().texture(), false, sink -> WandGeometry.caps(sink, staff, sceptre, lightCoords));
        if (state.focus() != null) {
            renderFocus(poseStack, buffers, state.focus(), staff, ticks, lightCoords);
        }
        if (sceptre || rod.runes()) {
            int runeLight = Math.max(lightCoords & 0xFFFF, 200) | (lightCoords & 0xFFFF0000);
            ItemGeometry.blended(
                buffers,
                poseStack,
                SCRIPT,
                BLEND_ADDITIVE,
                sink -> WandGeometry.runes(sink, staff, sceptre, rod.runes(), ticks, runeLight)
            );
        }
        poseStack.popPose();
    }
}
