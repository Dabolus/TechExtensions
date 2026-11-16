package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class TEArmorMaterials {
    /** Durability multiplier, applied per armor type through {@link ArmorItem.Type#getDurability(int)}. */
    public static final int ELECTRIC_JETPACK_DURABILITY = 33;

    public static final Holder<ArmorMaterial> ELECTRIC_JETPACK = register(
            "electric_jetpack",
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 0);
                map.put(ArmorItem.Type.LEGGINGS, 0);
                map.put(ArmorItem.Type.CHESTPLATE, 5);
                map.put(ArmorItem.Type.HELMET, 0);
                map.put(ArmorItem.Type.BODY, 0);
            }),
            10,
            SoundEvents.ARMOR_EQUIP_TURTLE,
            0.0f,
            0.0f,
            () -> Ingredient.EMPTY);

    private static Holder<ArmorMaterial> register(
            String id,
            EnumMap<ArmorItem.Type, Integer> defense,
            int enchantability,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, id);
        // The layer id resolves to textures/models/armor/<id>_layer_1.png
        return Registry.registerForHolder(
                BuiltInRegistries.ARMOR_MATERIAL,
                location,
                new ArmorMaterial(
                        defense,
                        enchantability,
                        equipSound,
                        repairIngredient,
                        List.of(new ArmorMaterial.Layer(location)),
                        toughness,
                        knockbackResistance));
    }
}
