package thaumcraft.client.render.armor;

import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.item.armor.FortressArmorItem;

public class TcArmorModel extends HumanoidModel<HumanoidRenderState> {
    private static final String[] SET_2 = {
        "book", "flap_l", "flap_r", "shoulderplate_ltop", "shoulderplate_l1", "shoulderplate_rtop", "shoulderplate_r1", "sidepanel_r2", "sidepanel_l2"
    };
    private static final String[] SET_3 = {
        "scroll", "ornament_l", "ornament_l2", "ornament_r", "ornament_r2", "gemornament", "gem", "shoulderplate_l2", "shoulderplate_l3",
        "shoulderplate_r2", "shoulderplate_r3", "sidepanel_r3", "sidepanel_l3"
    };

    private final ArmorKind kind;
    private final EquipmentSlot slot;
    private final Map<String, ModelPart> parts;

    public TcArmorModel(ModelPart root, ArmorKind kind, EquipmentSlot slot) {
        super(root);
        this.kind = kind;
        this.slot = slot;
        this.parts = kind.parts(root);
    }

    private void setXRot(String name, float value) {
        ModelPart part = parts.get(name);
        if (part != null) {
            part.xRot = value;
        }
    }

    private void setHidden(String name, boolean hidden) {
        ModelPart part = parts.get(name);
        if (part != null) {
            part.visible = !hidden;
        }
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        head.visible = slot == EquipmentSlot.HEAD;
        hat.visible = slot == EquipmentSlot.HEAD;
        body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
        rightArm.visible = slot == EquipmentSlot.CHEST;
        leftArm.visible = slot == EquipmentSlot.CHEST;
        rightLeg.visible = slot == EquipmentSlot.LEGS;
        leftLeg.visible = slot == EquipmentSlot.LEGS;
        if (state instanceof ZombieRenderState) {
            zombieArms(state);
        }
        if (!state.isBaby) {
            head.xScale = 1.01F;
            head.yScale = 1.01F;
            head.zScale = 1.01F;
        }
        float swing = state.walkAnimationPos;
        float amount = state.walkAnimationSpeed;
        float a = Mth.cos(swing * 0.6662F) * 1.4F * amount;
        float b = Mth.cos(swing * 0.6662F + (float) Math.PI) * 1.4F * amount;
        float c = Math.min(a, b);
        switch (kind) {
            case ROBE -> {
                setXRot("frontcloth_r1", c - 0.1047198F);
                setXRot("frontcloth_l1", c - 0.1047198F);
                setXRot("frontcloth_r2", c - 0.3316126F);
                setXRot("frontcloth_l2", c - 0.3316126F);
                setXRot("cloth_back_r1", -c + 0.1047198F);
                setXRot("cloth_back_l1", -c + 0.1047198F);
                for (String name : new String[] {"cloth_back_r2", "cloth_back_l2", "cloth_back_r3", "cloth_back_l3"}) {
                    setXRot(name, -c + 0.2268928F);
                }
            }
            case KNIGHT -> {
                setXRot("frontcloth1", c - 0.1047198F);
                setXRot("frontcloth2", c - 0.3316126F);
                setXRot("cloak1", -c / 2.0F + 0.1396263F);
                setXRot("cloak2", -c / 2.0F + 0.3069452F);
                setXRot("cloak3", -c / 2.0F + 0.4465716F);
            }
            case LEADER -> {
                setXRot("leg_cloth_r", a - 0.1047198F);
                setXRot("leg_cloth_l", b - 0.1047198F);
                setXRot("cloak1", -c / 2.0F + 0.1396263F);
                setXRot("cloak2", -c / 2.0F + 0.3069452F);
                setXRot("cloak3", -c / 2.0F + 0.4465716F);
            }
            case FORTRESS -> fortress(state);
        }
    }

    private void fortress(HumanoidRenderState state) {
        int set = 0;
        ItemStack helmet = state.headEquipment;
        for (ItemStack piece : new ItemStack[] {state.legsEquipment, state.chestEquipment, helmet}) {
            if (piece.getItem() instanceof FortressArmorItem) {
                set++;
            }
        }
        boolean fortressHelmet = helmet.getItem() instanceof FortressArmorItem;
        int mask = fortressHelmet ? FortressArmorItem.getMask(helmet) : -1;
        setHidden("goggles", !fortressHelmet || !FortressArmorItem.hasGoggles(helmet));
        for (int index = 0; index < 3; index++) {
            setHidden("mask" + index, mask != index);
        }
        for (String name : SET_2) {
            setHidden(name, set < 2);
        }
        for (String name : SET_3) {
            setHidden(name, set < 3);
        }
    }

    private void zombieArms(HumanoidRenderState state) {
        float f6 = Mth.sin(state.attackTime * (float) Math.PI);
        float f7 = Mth.sin((1.0F - (1.0F - state.attackTime) * (1.0F - state.attackTime)) * (float) Math.PI);
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.yRot = -(0.1F - f6 * 0.6F);
        leftArm.yRot = 0.1F - f6 * 0.6F;
        rightArm.xRot = (float) (-Math.PI / 2);
        leftArm.xRot = (float) (-Math.PI / 2);
        rightArm.xRot -= f6 * 1.2F - f7 * 0.4F;
        leftArm.xRot -= f6 * 1.2F - f7 * 0.4F;
        rightArm.zRot += Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        leftArm.zRot -= Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        rightArm.xRot += Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        leftArm.xRot -= Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
    }
}
