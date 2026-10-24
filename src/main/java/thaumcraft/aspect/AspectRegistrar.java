package thaumcraft.aspect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import thaumcraft.Thaumcraft;

public abstract class AspectRegistrar {
    private final AspectRegistry registry;

    protected AspectRegistrar(AspectRegistry registry) {
        this.registry = registry;
    }

    protected static AspectList tags() {
        return new AspectList();
    }

    protected AspectList copy(String target) {
        for (Item item : resolve(target)) {
            AspectList existing = registry.getObjectTags(item);
            return existing == null ? new AspectList() : existing.copy();
        }
        return new AspectList();
    }

    protected void register(String target, AspectList aspects) {
        for (Item item : resolve(target)) {
            registry.register(item, aspects.copy());
        }
    }

    protected void fill(String target, AspectList aspects) {
        for (Item item : resolve(target)) {
            if (!registry.hasAspects(item)) {
                registry.register(item, aspects.copy());
            }
        }
    }

    protected void complex(String target, AspectList aspects) {
        for (Item item : resolve(target)) {
            if (!registry.exists(item)) {
                AspectList combined = aspects.copy();
                AspectList generated = registry.generate(item, new ArrayList<>());
                if (generated != null) {
                    combined.add(generated);
                }
                registry.register(item, combined);
            }
        }
    }

    protected void entity(String target, AspectList aspects, Object... nbt) {
        BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(target)).ifPresentOrElse(
            type -> registry.registerEntity(type, aspects.copy(), nbt),
            () -> Thaumcraft.LOGGER.debug("Aspect entity target {} is not registered", target)
        );
    }

    protected void entityCopy(String target, AspectList aspects, Object... nbt) {
        BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(target)).ifPresentOrElse(
            type -> registry.mergeEntity(type, aspects.copy(), nbt),
            () -> Thaumcraft.LOGGER.debug("Aspect entity target {} is not registered", target)
        );
    }

    private static List<Item> resolve(String target) {
        List<Item> items = new ArrayList<>();
        if (target.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(target.substring(1)));
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                items.add(holder.value());
            }
            return items;
        }
        BuiltInRegistries.ITEM.getOptional(Identifier.parse(target)).ifPresentOrElse(
            items::add,
            () -> Thaumcraft.LOGGER.debug("Aspect target {} is not registered", target)
        );
        return items;
    }
}
