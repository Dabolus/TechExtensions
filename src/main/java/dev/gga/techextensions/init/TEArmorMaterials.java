package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class TEArmorMaterials {
    /** Durability multiplier, applied per armor type through {@link #getDurability(ArmorItem.Type, int)}. */
    public static final int ELECTRIC_JETPACK_DURABILITY = 33;

    /** Durability of each armor type per multiplier point, as in vanilla materials. */
    private static final EnumMap<ArmorItem.Type, Integer> UNIT_DURABILITY =
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 13);
                map.put(ArmorItem.Type.LEGGINGS, 15);
                map.put(ArmorItem.Type.CHESTPLATE, 16);
                map.put(ArmorItem.Type.HELMET, 11);
            });

    public static final ArmorMaterial ELECTRIC_JETPACK = register(
            "electric_jetpack",
            ELECTRIC_JETPACK_DURABILITY,
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 0);
                map.put(ArmorItem.Type.LEGGINGS, 0);
                map.put(ArmorItem.Type.CHESTPLATE, 5);
                map.put(ArmorItem.Type.HELMET, 0);
            }),
            10,
            SoundEvents.ARMOR_EQUIP_TURTLE,
            0.0f,
            0.0f,
            () -> Ingredient.EMPTY);

    public static int getDurability(ArmorItem.Type type, int multiplier) {
        return UNIT_DURABILITY.get(type) * multiplier;
    }

    private static ArmorMaterial register(
            String id,
            int durability,
            EnumMap<ArmorItem.Type, Integer> defense,
            int enchantability,
            SoundEvent equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient) {
        // A namespaced name resolves to <namespace>:textures/models/armor/<path>_layer_1.png (Fabric API)
        String name = new ResourceLocation(TechExtensions.MOD_ID, id).toString();
        return new ArmorMaterial() {
            @Override
            public int getDurabilityForType(ArmorItem.Type type) {
                return getDurability(type, durability);
            }

            @Override
            public int getDefenseForType(ArmorItem.Type type) {
                return defense.get(type);
            }

            @Override
            public int getEnchantmentValue() {
                return enchantability;
            }

            @Override
            public SoundEvent getEquipSound() {
                return equipSound;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repairIngredient.get();
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public float getToughness() {
                return toughness;
            }

            @Override
            public float getKnockbackResistance() {
                return knockbackResistance;
            }
        };
    }
}
