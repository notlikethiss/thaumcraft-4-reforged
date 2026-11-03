package thaumcraft.client.render.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;

public class FireBatModel extends net.minecraft.client.model.HierarchicalModel<thaumcraft.entity.monster.FireBat> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Thaumcraft.id("fire_bat"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart outerRightWing;
    private final ModelPart outerLeftWing;

    public FireBatModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        rightWing = body.getChild("right_wing");
        leftWing = body.getChild("left_wing");
        outerRightWing = rightWing.getChild("outer_right_wing");
        outerLeftWing = leftWing.getChild("outer_left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-4.0F, -6.0F, -2.0F, 3.0F, 4.0F, 1.0F), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(1.0F, -6.0F, -2.0F, 3.0F, 4.0F, 1.0F), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 12.0F, 6.0F).texOffs(0, 34).addBox(-5.0F, 16.0F, 0.0F, 10.0F, 6.0F, 1.0F),
            PartPose.ZERO
        );
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(42, 0).addBox(-12.0F, 1.0F, 1.5F, 10.0F, 16.0F, 1.0F), PartPose.ZERO);
        rightWing.addOrReplaceChild("outer_right_wing", CubeListBuilder.create().texOffs(24, 16).addBox(-8.0F, 1.0F, 0.0F, 8.0F, 12.0F, 1.0F), PartPose.offset(-12.0F, 1.0F, 1.5F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(42, 0).mirror().addBox(2.0F, 1.0F, 1.5F, 10.0F, 16.0F, 1.0F), PartPose.ZERO);
        leftWing.addOrReplaceChild("outer_left_wing", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(0.0F, 1.0F, 0.0F, 8.0F, 12.0F, 1.0F), PartPose.offset(12.0F, 1.0F, 1.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(thaumcraft.entity.monster.FireBat entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitchDegrees) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float headPitch = headPitchDegrees * Mth.DEG_TO_RAD;
        float headYaw = netHeadYaw * Mth.DEG_TO_RAD;
        if (entity.isHanging()) {
            head.xRot = headPitch;
            head.yRot = (float) Math.PI - headYaw;
            head.zRot = (float) Math.PI;
            head.setPos(0.0F, -2.0F, 0.0F);
            rightWing.setPos(-3.0F, 0.0F, 3.0F);
            leftWing.setPos(3.0F, 0.0F, 3.0F);
            body.xRot = (float) Math.PI;
            rightWing.xRot = (float) (-Math.PI / 20);
            rightWing.yRot = (float) (-Math.PI * 2.0 / 5.0);
            outerRightWing.yRot = -1.7278761F;
            leftWing.xRot = rightWing.xRot;
            leftWing.yRot = -rightWing.yRot;
            outerLeftWing.yRot = -outerRightWing.yRot;
        } else {
            head.xRot = headPitch;
            head.yRot = headYaw;
            head.zRot = 0.0F;
            head.setPos(0.0F, 0.0F, 0.0F);
            rightWing.setPos(0.0F, 0.0F, 0.0F);
            leftWing.setPos(0.0F, 0.0F, 0.0F);
            body.xRot = (float) (Math.PI / 4) + Mth.cos(ageInTicks * 0.1F) * 0.15F;
            body.yRot = 0.0F;
            rightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float) Math.PI * 0.25F;
            leftWing.yRot = -rightWing.yRot;
            outerRightWing.yRot = rightWing.yRot * 0.5F;
            outerLeftWing.yRot = -rightWing.yRot * 0.5F;
        }
    }
}
