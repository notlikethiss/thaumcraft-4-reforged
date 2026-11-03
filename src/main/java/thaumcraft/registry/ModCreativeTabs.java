package thaumcraft.registry;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.item.AspectTooltips;
import thaumcraft.wand.WandCap;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.WandRod;
import thaumcraft.wand.Wands;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);

    private static final Set<String> HIDDEN = Set.of("ancient_stone_no_spawn", "strange_crystals", "node_jar");

    private static final Map<String, String> REQUIRES_METAL = Map.of(
        "tin_nugget", "tin",
        "native_tin_cluster", "tin",
        "silver_nugget", "silver",
        "native_silver_cluster", "silver",
        "lead_nugget", "lead",
        "native_lead_cluster", "lead",
        "copper_wand_cap", "copper",
        "silver_wand_cap", "silver",
        "inert_silver_wand_cap", "silver"
    );

    public static final Supplier<CreativeModeTab> THAUMCRAFT = TABS.register("thaumcraft", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.thaumcraft"))
        .icon(() -> new ItemStack(ModItems.THAUMIUM_INGOT.get()))
        .displayItems((parameters, output) -> {
            for (DeferredHolder<Item, ? extends Item> holder : ModItems.ITEMS.getEntries()) {
                Item item = holder.get();
                String path = holder.getId().getPath();
                if (HIDDEN.contains(path) || !isMetalAvailable(path, parameters)) {
                    continue;
                }
                if (item == ModItems.ESSENTIA_PHIAL.get()) {
                    addPerAspect(output, item, 8);
                } else if (item == ModItems.WISP_ESSENCE.get()) {
                    addPerAspect(output, item, 2);
                } else if (item == ModItems.CRYSTALLIZED_ESSENCE.get()) {
                    addPerAspect(output, item, 1);
                } else if (item == ModItems.WAND.get()) {
                    addWands(output);
                } else {
                    output.accept(item);
                }
            }
            for (DeferredHolder<Item, ? extends Item> holder : EldritchContent.ITEMS.getEntries()) {
                output.accept(holder.get());
            }
        })
        .build());

    private ModCreativeTabs() {
    }

    private static void addWands(CreativeModeTab.Output output) {
        output.accept(filled(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false));
        output.accept(filled(WandRegistry.CAP_GOLD, WandRegistry.ROD_GREATWOOD, false));
        output.accept(filled(WandRegistry.CAP_THAUMIUM, WandRegistry.ROD_SILVERWOOD, false));
        output.accept(filled(WandRegistry.CAP_THAUMIUM, WandRegistry.ROD_SILVERWOOD, true));
    }

    private static ItemStack filled(WandCap cap, WandRod rod, boolean sceptre) {
        ItemStack stack = Wands.create(cap, rod, sceptre);
        Wands.fill(stack);
        return stack;
    }

    private static void addPerAspect(CreativeModeTab.Output output, Item item, int amount) {
        for (Aspect aspect : Aspect.values()) {
            ItemStack stack = new ItemStack(item);
            AspectTooltips.setSingle(stack, aspect, amount);
            output.accept(stack);
        }
    }

    private static boolean isMetalAvailable(String path, CreativeModeTab.ItemDisplayParameters parameters) {
        String metal = REQUIRES_METAL.get(path);
        if (metal == null) {
            return true;
        }
        TagKey<Item> ingots = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/" + metal));
        return parameters.holders()
            .lookup(Registries.ITEM)
            .flatMap(lookup -> lookup.get(ingots))
            .map(HolderSet::size)
            .orElse(0) > 0;
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
