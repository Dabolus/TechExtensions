package dev.gga.techextensions.init;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class TEToolMaterials {
    public static final Tier CYBER_SHIELD = Tiers.DIAMOND;

    // Same as Netherite, but as fast as an Industrial tool, with the damage of a Nanosaber, and the enchantability of a
    // Golden tool.
    public static final Tier META_TOOL = new Tier() {
        @Override
        public int getUses() {
            return Tiers.NETHERITE.getUses();
        }

        @Override
        public float getSpeed() {
            return 20.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 20.0F;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return Tiers.NETHERITE.getIncorrectBlocksForDrops();
        }

        @Override
        public int getEnchantmentValue() {
            return Tiers.GOLD.getEnchantmentValue();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Tiers.NETHERITE.getRepairIngredient();
        }
    };
}
