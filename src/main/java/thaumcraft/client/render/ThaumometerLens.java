package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import thaumcraft.client.geom.ProjectedWindow;
import thaumcraft.registry.ModItems;

/** The scanner's glass projected into the same clip space as the world. */
final class ThaumometerLens {
    // Inside the opaque frame of scanscreen.png, in its local [-0.5, 0.5] coordinates.
    private static final float[][] GLASS = {
        {-0.22F, 0.38F}, {0.22F, 0.38F}, {0.44F, 0.0F},
        {0.22F, -0.38F}, {-0.22F, -0.38F}, {-0.44F, 0.0F}
    };

    private ThaumometerLens() {
    }

    static List<Vector4f> clipPlanes(CameraRenderState camera, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        var game = minecraft.gameRenderer.gameRenderState();
        var player = game.levelRenderState.playerRenderState;
        var hands = player.firstPersonHandsAndItems;
        var avatar = player.avatarRenderState;
        if (!camera.isFirstPerson || camera.isPanoramicMode || game.guiRenderState.isHudHidden
            || camera.entityRenderState.isSleeping || !player.hasPlayer || avatar == null
            || minecraft.player == null || minecraft.player.isSpectator()
            || !hands.mainHandItem.is(ModItems.THAUMOMETER.get())
            || hands.handRenderSelection == null || !hands.handRenderSelection.renderMainHand) {
            return ProjectedWindow.hidden();
        }

        PoseStack bob = new PoseStack();
        applyCameraEffects(bob, camera, game.optionsRenderState);
        // Hand rendering starts at inverse(view), so modelView * pose is bob * hand.
        PoseStack lens = new PoseStack();
        lens.mulPose(bob.last().pose());
        float pitchBob = hands.viewXRot - hands.xBob;
        float yawBob = hands.viewYRot - hands.yBob;
        lens.mulPose(Axis.XP.rotationDegrees(pitchBob * 0.1F));
        lens.mulPose(Axis.YP.rotationDegrees(yawBob * 0.1F));
        float swing = hands.attackHand == InteractionHand.MAIN_HAND ? avatar.swingAnimation : 0.0F;
        float hidden = hands.mainHandSwapScale * (1.0F - Mth.lerp(camera.cameraEntityPartialTicks, hands.oldMainHandHeight, hands.mainHandHeight));
        ThaumometerHandRenderer.applyEquipTransform(lens, swing, hidden, avatar.isUsingItem);
        ThaumometerHandRenderer.applyGripTransform(lens, pitchBob, yawBob);
        ThaumometerHandRenderer.applyModelTransform(lens);
        ThaumometerItemRenderer.applyScreenTransform(lens);

        float aspect = (float) game.windowRenderState.width / game.windowRenderState.height;
        Matrix4f handProjection = new Matrix4f().setPerspective((float) Math.toRadians(camera.hudFov), aspect, 0.05F, camera.depthFar);
        Matrix4f glassToClip = handProjection.mul(lens.last().pose());
        Matrix4f worldToClip = new Matrix4f(camera.projectionMatrix).mul(bob.last().pose());
        applyPortalEffects(worldToClip, player.portalEffectIntensity, player.nauseaEffectIntensity,
            player.spinningEffectAngle, game.optionsRenderState.screenEffectScale);
        worldToClip.mul(camera.viewRotationMatrix).translate(
            (float) (pos.getX() - camera.pos.x), (float) (pos.getY() - camera.pos.y), (float) (pos.getZ() - camera.pos.z));
        return ProjectedWindow.planes(GLASS, glassToClip, worldToClip);
    }

    private static void applyCameraEffects(PoseStack pose, CameraRenderState camera, OptionsRenderState options) {
        var entity = camera.entityRenderState;
        if (entity.isLiving) {
            if (entity.isDeadOrDying) {
                float duration = Math.min(entity.deathTime, 20.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (duration + 200.0F)));
            }
            if (entity.hurtTime >= 0.0F && entity.hurtDuration > 0 && !entity.preventFlinch) {
                float hurt = entity.hurtTime / entity.hurtDuration;
                hurt = Mth.sin(hurt * hurt * hurt * hurt * Mth.PI);
                pose.mulPose(Axis.YP.rotationDegrees(-entity.hurtDir));
                pose.mulPose(Axis.ZP.rotationDegrees((float) (-hurt * 14.0 * options.damageTiltStrength)));
                pose.mulPose(Axis.YP.rotationDegrees(entity.hurtDir));
            }
        }
        if (options.bobView && entity.isPlayer) {
            float walk = entity.backwardsInterpolatedWalkDistance * Mth.PI;
            pose.translate(Mth.sin(walk) * entity.bob * 0.5F, -Math.abs(Mth.cos(walk) * entity.bob), 0);
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(walk) * entity.bob * 3.0F));
            pose.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(walk - 0.2F) * entity.bob) * 5.0F));
        }
    }

    private static void applyPortalEffects(Matrix4f projection, float portal, float nausea, float angleDegrees, float effectScale) {
        float intensity = Math.max(portal, nausea) * effectScale * effectScale;
        if (intensity > 0) {
            float skew = 5.0F / (intensity * intensity + 5.0F) - intensity * 0.04F;
            skew *= skew;
            float angle = (float) Math.toRadians(angleDegrees);
            float axis = Mth.SQRT_OF_TWO / 2.0F;
            projection.rotate(angle, 0, axis, axis).scale(1.0F / skew, 1, 1).rotate(-angle, 0, axis, axis);
        }
    }
}
