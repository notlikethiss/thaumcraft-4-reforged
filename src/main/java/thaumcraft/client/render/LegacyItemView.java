package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

public enum LegacyItemView implements StringRepresentable {
    INVENTORY("inventory"),
    EQUIPPED("equipped"),
    FIRST_PERSON("first_person"),
    ENTITY("entity");

    public static final Codec<LegacyItemView> CODEC = StringRepresentable.fromEnum(LegacyItemView::values);
    public static final MapCodec<LegacyItemView> FIELD = CODEC.optionalFieldOf("view", ENTITY);

    private final String name;

    LegacyItemView(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public void apply(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (this) {
            case INVENTORY -> {
                poseStack.scale(1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(-10.0F, -5.0F, 0.0F);
                poseStack.scale(10.0F, 10.0F, 10.0F);
                poseStack.translate(1.0F, 0.5F, 1.0F);
                poseStack.scale(1.0F, 1.0F, -1.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(210.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            case EQUIPPED -> {
                poseStack.translate(-1.0F / 16.0F, -2.0F / 16.0F, 10.0F / 16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.translate(-0.0625F, 0.4375F, 0.0625F);
                poseStack.translate(0.0F, 0.1875F, 0.0F);
                poseStack.scale(0.625F, -0.625F, 0.625F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            case FIRST_PERSON -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            case ENTITY -> poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
