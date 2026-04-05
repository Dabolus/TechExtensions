package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import java.util.EnumMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class TEArmorMaterials {
    private static final TagKey<Item> EMPTY =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "empty"));

    public static final ArmorMaterial ELECTRIC_JETPACK = register(
            "electric_jetpack",
            Util.make(new EnumMap<>(ArmorType.class), map -> {
                map.put(ArmorType.BOOTS, 0);
                map.put(ArmorType.LEGGINGS, 0);
                map.put(ArmorType.CHESTPLATE, 5);
                map.put(ArmorType.HELMET, 0);
                map.put(ArmorType.BODY, 0);
            }),
            10,
            SoundEvents.ARMOR_EQUIP_TURTLE,
            0.0f,
            0.0f,
            33,
            EMPTY);

    private static ArmorMaterial register(
            String id,
            EnumMap<ArmorType, Integer> defense,
            int enchantability,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            int durability,
            TagKey<Item> repairIngredient) {
        ResourceKey<EquipmentAsset> asset =
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, id));
        return new ArmorMaterial(
                durability,
                defense,
                enchantability,
                equipSound,
                toughness,
                knockbackResistance,
                repairIngredient,
                asset);
    }
}
