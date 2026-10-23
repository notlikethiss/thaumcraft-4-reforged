package thaumcraft.platform;

import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public final class Registration {
    private Registration() {
    }

    private static ToolMaterial convert(TcToolMaterial material) {
        return new ToolMaterial(
            material.incorrectForDrops(),
            material.durability(),
            material.miningSpeed(),
            material.attackDamageBonus(),
            material.enchantability(),
            material.repairItems()
        );
    }

    private static ArmorType convert(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> ArmorType.HELMET;
            case CHESTPLATE -> ArmorType.CHESTPLATE;
            case LEGGINGS -> ArmorType.LEGGINGS;
            case BOOTS -> ArmorType.BOOTS;
        };
    }

    private static ArmorMaterial convert(TcArmorMaterial material) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, material.assetId());
        Map<ArmorType, Integer> defense = Map.of(
            ArmorType.HELMET, material.helmetDefense(),
            ArmorType.CHESTPLATE, material.chestplateDefense(),
            ArmorType.LEGGINGS, material.leggingsDefense(),
            ArmorType.BOOTS, material.bootsDefense(),
            ArmorType.BODY, material.chestplateDefense()
        );
        return new ArmorMaterial(
            material.durabilityMultiplier(),
            defense,
            material.enchantability(),
            material.equipSound(),
            material.toughness(),
            material.knockbackResistance(),
            material.repairItems(),
            asset
        );
    }

    public static Item.Properties sword(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.sword(convert(material), attackDamage, attackSpeed);
    }

    public static Item.Properties pickaxe(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.pickaxe(convert(material), attackDamage, attackSpeed);
    }

    public static Item.Properties pickaxeAndShovel(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        Tool tool = new Tool(
            List.of(
                Tool.Rule.deniesDrops(blocks.getOrThrow(material.incorrectForDrops())),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), material.miningSpeed()),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), material.miningSpeed())
            ),
            1.0F,
            1,
            true
        );
        return properties.pickaxe(convert(material), attackDamage, attackSpeed).component(DataComponents.TOOL, tool);
    }

    public static Item.Properties axe(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.axe(convert(material), attackDamage, attackSpeed);
    }

    public static Item.Properties shovel(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.shovel(convert(material), attackDamage, attackSpeed);
    }

    public static Item.Properties hoe(Item.Properties properties, TcToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.hoe(convert(material), attackDamage, attackSpeed);
    }

    public static Item.Properties armor(Item.Properties properties, TcArmorMaterial material, ArmorSlot slot) {
        return properties.humanoidArmor(convert(material), convert(slot));
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
        Consumable.Builder consumable = Consumables.defaultFood().consumeSeconds(seconds);
        if (effect != null) {
            consumable.onConsume(new ApplyStatusEffectsConsumeEffect(effect, chance));
        }
        return properties.food(food.build(), consumable.build());
    }
}
