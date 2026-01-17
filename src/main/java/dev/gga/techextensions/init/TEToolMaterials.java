package dev.gga.techextensions.init;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;

public class TEToolMaterials {
    // Same as Netherite, but as fast as an Industrial tool, with the damage of a Nanosaber, and the enchantability of a
    // Golden tool.
    public static final ToolMaterial META_TOOL = new ToolMaterial(
            ToolMaterial.NETHERITE.incorrectBlocksForDrops(),
            ToolMaterial.NETHERITE.durability(),
            20.0F, // Speed
            20.0F, // Attack Damage Bonus
            ToolMaterial.GOLD.enchantmentValue(),
            ItemTags.NETHERITE_TOOL_MATERIALS);
}
