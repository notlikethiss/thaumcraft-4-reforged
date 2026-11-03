package thaumcraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.node.Revealer;
import thaumcraft.registry.ModItems;

public final class NodeVisibility {
    private NodeVisibility() {
    }

    public static boolean wearsRevealer(LivingEntity viewer) {
        ItemStack helmet = viewer.getItemBySlot(EquipmentSlot.HEAD);
        return !helmet.isEmpty() && helmet.getItem() instanceof Revealer revealer && revealer.showNodes(helmet, viewer);
    }

    public static boolean holdsThaumometer(Player viewer) {
        return viewer.getMainHandItem().is(ModItems.THAUMOMETER.get());
    }

    public static boolean isVisibleTo(float fov, Entity entity, double x, double y, double z) {
        double dist = entity.position().distanceTo(new Vec3(x, y, z));
        if (dist < 2.0) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        double viewTolerance = fov + minecraft.options.fov().get() / 2.0F;
        double renderDistance = 400.0;
        float f1 = Mth.cos(-entity.getYRot() * 0.01745329F - 3.141593F);
        float f3 = Mth.sin(-entity.getYRot() * 0.01745329F - 3.141593F);
        float f5 = -Mth.cos(-entity.getXRot() * 0.01745329F);
        float f7 = Mth.sin(-entity.getXRot() * 0.01745329F);
        double lookX = f3 * f5;
        double lookY = f7;
        double lookZ = f1 * f5;
        Vec3 eye = entity.getEyePosition();
        double dx = x + 0.5 - eye.x;
        double dy = y + 0.5 - eye.y;
        double dz = z + 0.5 - eye.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double dot = dx / length * lookX + dy / length * lookY + dz / length * lookZ;
        double angle = Math.acos(Math.clamp(dot, -1.0, 1.0));
        return angle < viewTolerance && firstPerson && dist < renderDistance || !firstPerson && dist < renderDistance;
    }

    public static double revealDistance(Entity viewer, int blockX, int blockY, int blockZ) {
        if (viewer instanceof Player player) {
            if (wearsRevealer(player)) {
                return 64.0;
            }
            if (holdsThaumometer(player) && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                return 48.0;
            }
        }
        return 0.0;
    }
}
