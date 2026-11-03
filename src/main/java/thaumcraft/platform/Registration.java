package thaumcraft.platform;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.Tool;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredRegister;
import javax.annotation.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModItems;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class Registration {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Thaumcraft.MODID);
    private static final Map<String, Holder<ArmorMaterial>> ARMOR_CACHE = new java.util.HashMap<>();

    private Registration() {
    }

    static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
        bus.addListener(Registration::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (var item : List.of(ModItems.ROBE_CHESTPLATE, ModItems.ROBE_LEGGINGS, ModItems.ROBE_BOOTS,
                ModItems.VOID_ROBE_HELMET, ModItems.VOID_ROBE_CHESTPLATE, ModItems.VOID_ROBE_LEGGINGS)) {
                CauldronInteraction.WATER.map().put(item.get(), CauldronInteraction.DYED_ITEM);
            }
        });
    }

    public static Tier tier(TcToolMaterial material) {
        return new SimpleTier(
            material.incorrectForDrops(),
            material.durability(),
            material.miningSpeed(),
            material.attackDamageBonus(),
            material.enchantability(),
            () -> Ingredient.of(material.repairItems())
        );
    }

    public static ArmorItem.Type armorType(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> ArmorItem.Type.HELMET;
            case CHESTPLATE -> ArmorItem.Type.CHESTPLATE;
            case LEGGINGS -> ArmorItem.Type.LEGGINGS;
            case BOOTS -> ArmorItem.Type.BOOTS;
        };
    }

    public static synchronized Holder<ArmorMaterial> armorMaterial(TcArmorMaterial material) {
        return ARMOR_CACHE.computeIfAbsent(material.assetName(), name -> {
            Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.HELMET, material.helmetDefense());
            defense.put(ArmorItem.Type.CHESTPLATE, material.chestplateDefense());
            defense.put(ArmorItem.Type.LEGGINGS, material.leggingsDefense());
            defense.put(ArmorItem.Type.BOOTS, material.bootsDefense());
            defense.put(ArmorItem.Type.BODY, material.chestplateDefense());
            return ARMOR_MATERIALS.register(name.replace(':', '/'), () -> new ArmorMaterial(
                defense,
                material.enchantability(),
                material.equipSound(),
                () -> Ingredient.of(material.repairItems()),
                armorLayers(material.assetId()),
                material.toughness(),
                material.knockbackResistance()
            ));
        });
    }

    private static List<ArmorMaterial.Layer> armorLayers(net.minecraft.resources.ResourceLocation id) {
        String name = id.getNamespace().equals(Thaumcraft.MODID) ? id.getPath() : id.toString();
        if (name.equals("robe") || name.equals("void_robe")) {
            return List.of(
                new ArmorMaterial.Layer(Thaumcraft.id(name), "", true),
                new ArmorMaterial.Layer(Thaumcraft.id(name), "_overlay", false)
            );
        }
        return List.of(new ArmorMaterial.Layer(id));
    }

    public static Item.Properties sword(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        Tier tier = tier(material);
        return properties.durability(tier.getUses())
            .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed))
            .component(DataComponents.TOOL, SwordItem.createToolProperties());
    }

    public static Item.Properties pickaxe(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return digger(properties, material, attackDamage, attackSpeed, BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public static Item.Properties pickaxeAndShovel(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        var blocks = BuiltInRegistries.BLOCK;
        Tool tool = new Tool(List.of(
            Tool.Rule.deniesDrops(material.incorrectForDrops()),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, material.miningSpeed()),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, material.miningSpeed())
        ), 1.0F, 1);
        return pickaxe(properties, material, attackDamage, attackSpeed).component(DataComponents.TOOL, tool);
    }

    public static Item.Properties axe(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return digger(properties, material, attackDamage, attackSpeed, BlockTags.MINEABLE_WITH_AXE);
    }

    public static Item.Properties shovel(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return digger(properties, material, attackDamage, attackSpeed, BlockTags.MINEABLE_WITH_SHOVEL);
    }

    public static Item.Properties hoe(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return digger(properties, material, attackDamage, attackSpeed, BlockTags.MINEABLE_WITH_HOE);
    }

    private static Item.Properties digger(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> mineable) {
        Tier tier = tier(material);
        return properties.durability(tier.getUses())
            .attributes(DiggerItem.createAttributes(tier, attackDamage, attackSpeed))
            .component(DataComponents.TOOL, tier.createToolProperties(mineable));
    }

    public static Item.Properties armor(Item.Properties properties, TcArmorMaterial material, ArmorSlot slot) {
        return properties.durability(armorType(slot).getDurability(material.durabilityMultiplier()));
    }

    public static Item.Properties fuel(Item.Properties properties, int ticks) {
        return properties;
    }

    public static Item.Properties food(Item.Properties properties, int nutrition, float saturation) {
        return food(properties, nutrition, saturation, false, 1.6F, null, 0.0F);
    }

    public static Item.Properties food(Item.Properties properties, int nutrition, float saturation, boolean alwaysEdible, float seconds) {
        return food(properties, nutrition, saturation, alwaysEdible, seconds, null, 0.0F);
    }

    public static Item.Properties food(
        Item.Properties properties,
        int nutrition,
        float saturation,
        boolean alwaysEdible,
        float seconds,
        @Nullable MobEffectInstance effect,
        float chance
    ) {
        FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
        if (alwaysEdible) {
            food.alwaysEdible();
        }
        if (seconds < 1.6F) {
            food.fast();
        }
        if (effect != null) {
            food.effect(() -> new MobEffectInstance(effect), chance);
        }
        return properties.food(food.build());
    }
}
