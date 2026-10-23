package thaumcraft.platform;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class FollowingItemMotion {
    private FollowingItemMotion() {
    }

    public static void sync(ItemEntity item) {
        item.needsSync = true;
    }

    public static void tick(ItemEntity item, Vec3 movement) {
        if (item.getItem().onEntityItemUpdate(item)) {
            return;
        }
        item.baseTick();
        item.xo = item.getX();
        item.yo = item.getY();
        item.zo = item.getZ();
        if (item.pickupDelay > 0 && item.pickupDelay != 32767) {
            item.pickupDelay--;
        }
        if (item.age != -32768) {
            item.age++;
        }
        item.noPhysics = true;
        item.setDeltaMovement(movement);
        item.move(MoverType.SELF, movement);
        if (!item.level().isClientSide()) {
            sync(item);
        }
        if (!item.level().isClientSide() && item.age >= item.lifespan) {
            item.lifespan = Math.clamp((long) item.lifespan + EventHooks.onItemExpire(item), 0, Short.MAX_VALUE - 1);
            if (item.age >= item.lifespan) {
                item.discard();
            }
        }
        if (item.getItem().isEmpty()) {
            item.discard();
        }
    }
}
