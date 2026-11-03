package thaumcraft.client.render.entity.eldritch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import thaumcraft.entity.monster.eldritch.EldritchGuardian;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;

public class EldritchGuardianModel<T extends net.minecraft.world.entity.LivingEntity> extends EntityModel<T> {
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER = new net.minecraft.client.model.geom.ModelLayerLocation(Thaumcraft.id("eldritch_guardian"), "main");
    private final ModelPart root;
    private final ModelPart hood1;
    private final ModelPart armL1;
    private final ModelPart armR1;
    private final ModelPart legpanelC1;
    private final ModelPart legpanelC2;
    private final ModelPart legpanelC3;
    private final ModelPart cloak1;
    private final ModelPart cloak2;
    private final ModelPart cloak3;
    private final ModelPart sidepanelL2;
    private final ModelPart sidepanelL3;
    private final ModelPart sidepanelL4;
    private final ModelPart sidepanelR2;
    private final ModelPart sidepanelR3;
    private final ModelPart sidepanelR4;

    public EldritchGuardianModel(ModelPart root) {
        super();
        this.root = root;
        this.hood1 = root.getChild("hood1");
        this.armL1 = root.getChild("arm_l1");
        this.armR1 = root.getChild("arm_r1");
        this.legpanelC1 = root.getChild("legpanel_c1");
        this.legpanelC2 = this.legpanelC1.getChild("legpanel_c2");
        this.legpanelC3 = this.legpanelC2.getChild("legpanel_c3");
        this.cloak1 = root.getChild("cloak1");
        this.cloak2 = this.cloak1.getChild("cloak2");
        this.cloak3 = this.cloak2.getChild("cloak3");
        this.sidepanelL2 = root.getChild("sidepanel_l2");
        this.sidepanelL3 = this.sidepanelL2.getChild("sidepanel_l3");
        this.sidepanelL4 = this.sidepanelL3.getChild("sidepanel_l4");
        this.sidepanelR2 = root.getChild("sidepanel_r2");
        this.sidepanelR3 = this.sidepanelR2.getChild("sidepanel_r3");
        this.sidepanelR4 = this.sidepanelR3.getChild("sidepanel_r4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition beltR = root.addOrReplaceChild("belt_r", CubeListBuilder.create().texOffs(76, 44).mirror().addBox(-5F, 4F, -3F, 1F, 3F, 6F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition mbelt = root.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).mirror().addBox(-4F, 8F, -3F, 8F, 4F, 1F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition mbeltL = root.addOrReplaceChild("mbelt_l", CubeListBuilder.create().texOffs(76, 44).mirror().addBox(4F, 8F, -3F, 1F, 3F, 6F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition mbeltR = root.addOrReplaceChild("mbelt_r", CubeListBuilder.create().texOffs(76, 44).mirror().addBox(-5F, 8F, -3F, 1F, 3F, 6F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition beltL = root.addOrReplaceChild("belt_l", CubeListBuilder.create().texOffs(76, 44).mirror().addBox(4F, 4F, -3F, 1F, 3F, 6F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition chestplate = root.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).mirror().addBox(-4F, 1F, -4F, 8F, 7F, 2F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition hood1 = root.addOrReplaceChild("hood1", CubeListBuilder.create().texOffs(40, 12).mirror().addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition hood2 = hood1.addOrReplaceChild("hood2", CubeListBuilder.create().texOffs(36, 28).mirror().addBox(-3.5F, -8.7F, 2F, 7F, 7F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, -0.2268928F, 0F, 0F));
        PartDefinition hood3 = hood1.addOrReplaceChild("hood3", CubeListBuilder.create().texOffs(22, 19).mirror().addBox(-3F, -9F, 2.5F, 6F, 6F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, -0.3490659F, 0F, 0F));
        PartDefinition hood4 = hood1.addOrReplaceChild("hood4", CubeListBuilder.create().texOffs(40, 4).mirror().addBox(-2.5F, -9.7F, 3.5F, 5F, 5F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, -0.5759587F, 0F, 0F));
        PartDefinition backplate = root.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).mirror().addBox(-4F, 1F, 2F, 8F, 11F, 2F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition shoulderplateTopR = root.addOrReplaceChild("shoulderplate_top_r", CubeListBuilder.create().texOffs(110, 37).mirror().addBox(-5.5F, -2.5F, -3.5F, 2F, 1F, 7F), PartPose.offsetAndRotation(-5F, -4F, 0F, -0.3665191F, 0.3141593F, 0.4363323F));
        PartDefinition shoulderplateR1 = root.addOrReplaceChild("shoulderplate_r1", CubeListBuilder.create().texOffs(110, 45).mirror().addBox(3.5F, -1.5F, -3.5F, 1F, 4F, 7F), PartPose.offsetAndRotation(5F, -4F, 0F, -0.3665191F, -0.3141593F, -0.4363323F));
        PartDefinition shoulderplateR2 = root.addOrReplaceChild("shoulderplate_r2", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(-3.5F, 1.5F, -3.5F, 1F, 3F, 7F), PartPose.offsetAndRotation(-5F, -4F, 0F, -0.3665191F, 0.3141593F, 0.4363323F));
        PartDefinition shoulderplateR3 = root.addOrReplaceChild("shoulderplate_r3", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(-2.5F, 3.5F, -3.5F, 1F, 3F, 7F), PartPose.offsetAndRotation(-5F, -4F, 0F, -0.3665191F, 0.3141593F, 0.4363323F));
        PartDefinition shoulderR = root.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-3.5F, -2.5F, -2.5F, 5F, 5F, 5F), PartPose.offsetAndRotation(-5F, -4F, 0F, -0.3665191F, 0.122173F, 0.0349066F));
        PartDefinition armL1 = root.addOrReplaceChild("arm_l1", CubeListBuilder.create().texOffs(72, 8).mirror().addBox(-1F, 2.5F, -1.5F, 4F, 10F, 5F), PartPose.offsetAndRotation(5F, -4F, 0F, -0.9599311F, -0.1047198F, -0.1919862F));
        PartDefinition armL2 = armL1.addOrReplaceChild("arm_l2", CubeListBuilder.create().texOffs(76, 28).mirror().addBox(-1F, 9.5F, 3.5F, 4F, 3F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition armL3 = armL1.addOrReplaceChild("arm_l3", CubeListBuilder.create().texOffs(76, 23).mirror().addBox(-1F, 6.5F, 3.5F, 4F, 3F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition armR1 = root.addOrReplaceChild("arm_r1", CubeListBuilder.create().texOffs(72, 8).mirror().addBox(-3F, 2.5F, -1.5F, 4F, 10F, 5F), PartPose.offsetAndRotation(-5F, -4F, 0F, -0.9599311F, 0.1047198F, 0.1919862F));
        PartDefinition armR2 = armR1.addOrReplaceChild("arm_r2", CubeListBuilder.create().texOffs(76, 28).mirror().addBox(-3F, 9.5F, 3.5F, 4F, 3F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition armR3 = armR1.addOrReplaceChild("arm_r3", CubeListBuilder.create().texOffs(76, 23).mirror().addBox(-3F, 6.5F, 3.5F, 4F, 3F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition shoulderL = root.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5F, -2.5F, -2.5F, 5F, 5F, 5F), PartPose.offsetAndRotation(5F, -4F, 0F, -0.3665191F, -0.122173F, -0.0349066F));
        PartDefinition shoulderplateLtop = root.addOrReplaceChild("shoulderplate_ltop", CubeListBuilder.create().texOffs(110, 37).addBox(3.5F, -2.5F, -3.5F, 2F, 1F, 7F), PartPose.offsetAndRotation(5F, -4F, 0F, -0.3665191F, -0.3141593F, -0.4363323F));
        PartDefinition shoulderplateL1 = root.addOrReplaceChild("shoulderplate_l1", CubeListBuilder.create().texOffs(110, 45).mirror().addBox(-4.5F, -1.5F, -3.5F, 1F, 4F, 7F), PartPose.offsetAndRotation(-5F, -4F, 0F, -0.3665191F, 0.3141593F, 0.4363323F));
        PartDefinition shoulderplateL2 = root.addOrReplaceChild("shoulderplate_l2", CubeListBuilder.create().texOffs(94, 45).addBox(2.5F, 1.5F, -3.5F, 1F, 3F, 7F), PartPose.offsetAndRotation(5F, -4F, 0F, -0.3665191F, -0.3141593F, -0.4363323F));
        PartDefinition shoulderplateL3 = root.addOrReplaceChild("shoulderplate_l3", CubeListBuilder.create().texOffs(94, 45).addBox(1.5F, 3.5F, -3.5F, 1F, 3F, 7F), PartPose.offsetAndRotation(5F, -4F, 0F, -0.3665191F, -0.3141593F, -0.4363323F));
        PartDefinition legpanelR4 = root.addOrReplaceChild("legpanel_r4", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(-3F, 0.5F, -3.5F, 2F, 3F, 1F), PartPose.offsetAndRotation(-2F, 6F, 0F, -0.4363323F, 0F, 0F));
        PartDefinition legpanelR5 = root.addOrReplaceChild("legpanel_r5", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(-3F, 2.5F, -2.5F, 2F, 3F, 1F), PartPose.offsetAndRotation(-2F, 6F, 0F, -0.4363323F, 0F, 0F));
        PartDefinition legpanelR6 = root.addOrReplaceChild("legpanel_r6", CubeListBuilder.create().texOffs(6, 43).mirror().addBox(-3F, 4.5F, -1.5F, 2F, 3F, 1F), PartPose.offsetAndRotation(-2F, 6F, 0F, -0.4363323F, 0F, 0F));
        PartDefinition backpanelR1 = root.addOrReplaceChild("backpanel_r1", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-3F, 0.5F, 2.5F, 5F, 3F, 1F), PartPose.offsetAndRotation(-2F, 6F, 0F, 0.4363323F, 0F, 0F));
        PartDefinition backpanelR2 = root.addOrReplaceChild("backpanel_r2", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-3F, 2.5F, 1.5F, 5F, 3F, 1F), PartPose.offsetAndRotation(-2F, 6F, 0F, 0.4363323F, 0F, 0F));
        PartDefinition backpanelR3 = root.addOrReplaceChild("backpanel_r3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-3F, 4.5F, 0.5F, 5F, 3F, 1F), PartPose.offsetAndRotation(-2F, 6F, 0F, 0.4363323F, 0F, 0F));
        PartDefinition backpanelL3 = root.addOrReplaceChild("backpanel_l3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2F, 4.5F, 0.5F, 5F, 3F, 1F), PartPose.offsetAndRotation(2F, 6F, 0F, 0.4363323F, 0F, 0F));
        PartDefinition legpanelL4 = root.addOrReplaceChild("legpanel_l4", CubeListBuilder.create().texOffs(0, 43).addBox(1F, 0.5F, -3.5F, 2F, 3F, 1F), PartPose.offsetAndRotation(2F, 6F, 0F, -0.4363323F, 0F, 0F));
        PartDefinition legpanelL5 = root.addOrReplaceChild("legpanel_l5", CubeListBuilder.create().texOffs(0, 47).addBox(1F, 2.5F, -2.5F, 2F, 3F, 1F), PartPose.offsetAndRotation(2F, 6F, 0F, -0.4363323F, 0F, 0F));
        PartDefinition legpanelL6 = root.addOrReplaceChild("legpanel_l6", CubeListBuilder.create().texOffs(6, 43).addBox(1F, 4.5F, -1.5F, 2F, 3F, 1F), PartPose.offsetAndRotation(2F, 6F, 0F, -0.4363323F, 0F, 0F));
        PartDefinition backpanelL1 = root.addOrReplaceChild("backpanel_l1", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2F, 0.5F, 2.5F, 5F, 3F, 1F), PartPose.offsetAndRotation(2F, 6F, 0F, 0.4363323F, 0F, 0F));
        PartDefinition backpanelL2 = root.addOrReplaceChild("backpanel_l2", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2F, 2.5F, 1.5F, 5F, 3F, 1F), PartPose.offsetAndRotation(2F, 6F, 0F, 0.4363323F, 0F, 0F));
        PartDefinition sidepanelL1 = root.addOrReplaceChild("sidepanel_l1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(1.5F, 0.5F, -2.5F, 1F, 4F, 5F), PartPose.offsetAndRotation(2F, 6F, 0F, 0F, 0F, -0.4363323F));
        PartDefinition sidepanelR1 = root.addOrReplaceChild("sidepanel_r1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.5F, 0.5F, -2.5F, 1F, 4F, 5F), PartPose.offsetAndRotation(-2F, 6F, 0F, 0F, 0F, 0.4363323F));
        PartDefinition sidepanelR2 = root.addOrReplaceChild("sidepanel_r2", CubeListBuilder.create().texOffs(0, 54).mirror().addBox(0F, 0F, -0.5F, 1F, 5F, 5F), PartPose.offsetAndRotation(-4.5F, 9.5F, -2F, 0F, 0F, 0.122173F));
        PartDefinition sidepanelR3 = sidepanelR2.addOrReplaceChild("sidepanel_r3", CubeListBuilder.create().texOffs(0, 35).mirror().addBox(0F, 0F, -0.5F, 1F, 3F, 5F), PartPose.offsetAndRotation(0F, 5F, 0F, 0F, 0F, 0.296706F));
        PartDefinition sidepanelR4 = sidepanelR3.addOrReplaceChild("sidepanel_r4", CubeListBuilder.create().texOffs(24, 35).mirror().addBox(0F, 0F, -0.5F, 1F, 3F, 5F), PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0.523598776F));
        PartDefinition sidepanelL2 = root.addOrReplaceChild("sidepanel_l2", CubeListBuilder.create().texOffs(0, 54).mirror().addBox(0F, 0F, -0.5F, 1F, 5F, 5F), PartPose.offsetAndRotation(4.5F, 9.5F, -2F, 0F, 0F, -0.122173F));
        PartDefinition sidepanelL3 = sidepanelL2.addOrReplaceChild("sidepanel_l3", CubeListBuilder.create().texOffs(0, 35).mirror().addBox(0F, 0F, -0.5F, 1F, 3F, 5F), PartPose.offsetAndRotation(0F, 5F, 0F, 0F, 0F, -0.296706F));
        PartDefinition sidepanelL4 = sidepanelL3.addOrReplaceChild("sidepanel_l4", CubeListBuilder.create().texOffs(24, 35).mirror().addBox(0F, 0F, -0.5F, 1F, 3F, 5F), PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, -0.523598776F));
        PartDefinition legpanelC1 = root.addOrReplaceChild("legpanel_c1", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-3F, 0F, -0.5F, 6F, 8F, 1F), PartPose.offsetAndRotation(0F, 5.5F, -3F, 0F, 0F, 0F));
        PartDefinition legpanelC2 = legpanelC1.addOrReplaceChild("legpanel_c2", CubeListBuilder.create().texOffs(16, 54).mirror().addBox(-3F, 0F, -0.5F, 6F, 4F, 1F), PartPose.offsetAndRotation(0F, 8F, 0F, 0F, 0F, 0F));
        PartDefinition legpanelC3 = legpanelC2.addOrReplaceChild("legpanel_c3", CubeListBuilder.create().texOffs(32, 59).mirror().addBox(-3F, 0F, -0.5F, 6F, 4F, 1F), PartPose.offsetAndRotation(0F, 4F, 0F, 0F, 0F, 0F));
        PartDefinition cloak1 = root.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(106, 0).mirror().addBox(0F, 0F, -0.5F, 10F, 18F, 1F), PartPose.offsetAndRotation(-5F, -6F, 4F, 0F, 0F, 0F));
        PartDefinition cloak2 = cloak1.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(106, 19).mirror().addBox(0F, 0F, -0.5F, 10F, 4F, 1F), PartPose.offsetAndRotation(0F, 18F, 0F, 0F, 0F, 0F));
        PartDefinition cloak3 = cloak2.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(106, 24).mirror().addBox(0F, 0F, -0.5F, 10F, 4F, 1F), PartPose.offsetAndRotation(0F, 4F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.hood1.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.hood1.xRot = headPitch * ((float) Math.PI / 180.0F);
        this.armL1.xRot = -1.0F - (entity instanceof EldritchGuardian guard ? guard.armLiftLeft : ((thaumcraft.entity.boss.EldritchWarden) entity).getArmLiftLeft()) + Mth.sin((ageInTicks + 20.0F) / 10.0F) * 0.08F;
        this.armR1.xRot = -1.0F - (entity instanceof EldritchGuardian guard ? guard.armLiftRight : ((thaumcraft.entity.boss.EldritchWarden) entity).getArmLiftRight()) + Mth.sin(ageInTicks / 10.0F) * 0.08F;
        this.legpanelC1.xRot = -0.15F + Mth.sin(ageInTicks / 8.0F) * 0.12F;
        this.legpanelC2.xRot = Mth.sin((ageInTicks - 5.0F) / 8.0F) * 0.13F;
        this.legpanelC3.xRot = Mth.sin((ageInTicks - 10.0F) / 8.0F) * 0.14F;
        this.cloak1.xRot = 0.2F + Mth.sin(ageInTicks / 7.0F) * 0.08F;
        this.cloak2.xRot = Mth.sin((ageInTicks - 5.0F) / 7.0F) * 0.1F;
        this.cloak3.xRot = Mth.sin((ageInTicks - 10.0F) / 7.0F) * 0.12F;
        this.sidepanelL2.zRot = -0.2F + Mth.sin((ageInTicks + 10.0F) / 8.0F) * 0.12F;
        this.sidepanelL3.zRot = Mth.sin((ageInTicks + 5.0F) / 8.0F) * 0.13F;
        this.sidepanelL4.zRot = Mth.sin(ageInTicks / 8.0F) * 0.14F;
        this.sidepanelR2.zRot = 0.2F + Mth.sin((ageInTicks - 5.0F) / 8.0F) * 0.12F;
        this.sidepanelR3.zRot = Mth.sin((ageInTicks - 10.0F) / 8.0F) * 0.13F;
        this.sidepanelR4.zRot = Mth.sin((ageInTicks - 15.0F) / 8.0F) * 0.14F;
    }
}
