package thaumcraft.item.gear;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import thaumcraft.Thaumcraft;
import thaumcraft.platform.TcArmorMaterial;
import thaumcraft.platform.TcToolMaterial;

public final class TcMaterials {
    public static final TagKey<Item> REPAIRS_THAUMIUM = tag("repairs_thaumium");
    public static final TagKey<Item> REPAIRS_VOID_METAL = tag("repairs_void_metal");
    public static final TagKey<Item> REPAIRS_PRIMAL_CHARM = tag("repairs_primal_charm");
    public static final TagKey<Item> REPAIRS_ENCHANTED_FABRIC = tag("repairs_enchanted_fabric");
    public static final TagKey<Item> REPAIRS_NONE = tag("repairs_none");

    public static final TcToolMaterial THAUMIUM_TOOL = new TcToolMaterial(400, 7.0F, 2.0F, 22, REPAIRS_THAUMIUM, BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
    public static final TcToolMaterial VOID_TOOL = new TcToolMaterial(150, 8.0F, 3.0F, 10, REPAIRS_PRIMAL_CHARM, BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
    public static final TcToolMaterial ELEMENTAL_TOOL = new TcToolMaterial(1500, 10.0F, 3.0F, 18, REPAIRS_THAUMIUM, BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
    public static final TcToolMaterial CRIMSON_VOID_TOOL = new TcToolMaterial(200, 8.0F, 3.5F, 20, REPAIRS_PRIMAL_CHARM, BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
    public static final TcToolMaterial PRIMAL_VOID_TOOL = new TcToolMaterial(500, 8.0F, 4.0F, 20, REPAIRS_PRIMAL_CHARM, BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    public static final TcArmorMaterial THAUMIUM_ARMOR = armor(25, 2, 6, 5, 2, 25, REPAIRS_THAUMIUM, "thaumium");
    public static final TcArmorMaterial ROBE_ARMOR = armor(25, 1, 3, 2, 1, 25, REPAIRS_ENCHANTED_FABRIC, "robe");
    public static final TcArmorMaterial TRAVELLER_ARMOR = armor(25, 1, 3, 2, 1, 25, REPAIRS_NONE, "boots_traveller");
    public static final TcArmorMaterial HARNESS_ARMOR = armor(25, 1, 3, 2, 1, 25, net.neoforged.neoforge.common.Tags.Items.INGOTS_GOLD, "hover_harness");
    public static final TcArmorMaterial FORTRESS_ARMOR = armor(40, 3, 7, 6, 3, 25, REPAIRS_THAUMIUM, "fortress");
    public static final TcArmorMaterial VOID_ARMOR = armor(10, 3, 7, 6, 3, 10, REPAIRS_VOID_METAL, "void");
    public static final TcArmorMaterial VOID_ROBE_ARMOR = armor(10, 3, 7, 6, 3, 10, REPAIRS_VOID_METAL, "void_robe");
    public static final TcArmorMaterial CULTIST_ROBE_ARMOR = armor(15, 2, 6, 5, 2, 9, net.neoforged.neoforge.common.Tags.Items.INGOTS_IRON, "cultist_robe");
    public static final TcArmorMaterial CULTIST_PLATE_ARMOR = armor(15, 2, 6, 5, 2, 9, net.neoforged.neoforge.common.Tags.Items.INGOTS_IRON, "cultist_plate");
    public static final TcArmorMaterial CULTIST_BOOTS_ARMOR = armor(15, 2, 6, 5, 2, 9, net.neoforged.neoforge.common.Tags.Items.INGOTS_IRON, "cultist_boots");
    public static final TcArmorMaterial CULTIST_LEADER_ARMOR = armor(40, 3, 7, 6, 3, 25, net.neoforged.neoforge.common.Tags.Items.INGOTS_IRON, "cultist_leader");

    private TcMaterials() {
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, Thaumcraft.id(name));
    }

    private static TcArmorMaterial armor(int durability, int helmet, int chest, int legs, int boots, int enchantability, TagKey<Item> repair, String asset) {
        return new TcArmorMaterial(durability, helmet, chest, legs, boots, enchantability, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, repair, asset);
    }
}
