package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.blocks.machine.ElectricDuctedFanBlock;
import dev.gga.techextensions.events.ModRegistry;
import dev.gga.techextensions.menu.BubbleGunMenu;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import dev.gga.techextensions.menu.VacuumGunMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class TEContent {
    public static final Marker DATAGEN = MarkerFactory.getMarker("datagen");

    // Blocks
    public static Block ELECTRIC_DUCTED_FAN = new ElectricDuctedFanBlock(12, 4, 12, "electric_ducted_fan");

    // Items
    public static Item BUBBLE_GUN;
    public static Item CYBER_SHIELD;
    public static Item ELECTRIC_JETPACK;
    public static Item META_TOOL;
    public static Item SHRINK_RAY;
    public static Item SOAP;
    public static Item RESONANCE_SCANNER;
    public static Item VACUUM_GUN;

    public static MenuType<BubbleGunMenu> BUBBLE_GUN_MENU;
    public static MenuType<ResonanceScannerMenu> RESONANCE_SCANNER_MENU;
    public static MenuType<VacuumGunMenu> VACUUM_GUN_MENU;

    public static final class BlockTags {
        public static final TagKey<Block> META_TOOL_MINEABLE = TagKey.create(
                Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "mineable/meta_tool"));

        private BlockTags() {}
    }

    public static void register() {
        ModRegistry.register();
        TEItemGroup.register();
    }
}
