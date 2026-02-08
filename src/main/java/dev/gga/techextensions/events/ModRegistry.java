package dev.gga.techextensions.events;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.init.TEBlockEntities;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.init.TEInitUtils;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.items.armor.ElectricJetpackItem;
import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import dev.gga.techextensions.items.tool.advanced.ShrinkRayItem;
import dev.gga.techextensions.items.tool.industrial.MetaToolItem;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import java.util.HashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.Validate;

public class ModRegistry {
    private static final HashMap<Object, ResourceLocation> objIdentMap = new HashMap<>();

    public static void register() {
        registerBlocks();
        registerItems();
        registerBlockEntities();
        registerMenus();
    }

    public static void registerMenu(MenuType<?> menu, ResourceLocation name) {
        Registry.register(BuiltInRegistries.MENU, name, menu);
    }

    public static void registerBlock(Block block, ResourceLocation name) {
        Registry.register(BuiltInRegistries.BLOCK, name, block);
    }

    public static void registerBlock(Block block) {
        Validate.isTrue(objIdentMap.containsKey(block));
        registerBlock(block, objIdentMap.get(block));
    }

    public static void registerItem(Item item, ResourceLocation name) {
        Registry.register(BuiltInRegistries.ITEM, name, item);
    }

    public static void registerItem(Item item) {
        Validate.isTrue(objIdentMap.containsKey(item));
        registerItem(item, objIdentMap.get(item));
    }

    private static void registerBlocks() {
        registerBlock(TEInitUtils.setup(TEContent.ELECTRIC_DUCTED_FAN, "electric_ducted_fan"));

        TechExtensions.LOGGER.debug("TechExtension's Blocks Loaded");
    }

    private static void registerItems() {
        // Block items
        registerItem(TEInitUtils.setup(
                new BlockItem(TEContent.ELECTRIC_DUCTED_FAN, TEItemSettings.item("electric_ducted_fan")),
                "electric_ducted_fan"));

        // Regular items
        registerItem(
                TEContent.ELECTRIC_JETPACK =
                        TEInitUtils.setup(new ElectricJetpackItem("electric_jetpack"), "electric_jetpack"));
        registerItem(TEContent.META_TOOL = TEInitUtils.setup(new MetaToolItem("meta_tool"), "meta_tool"));
        registerItem(TEContent.SHRINK_RAY = TEInitUtils.setup(new ShrinkRayItem("shrink_ray"), "shrink_ray"));
        registerItem(
                TEContent.RESONANCE_SCANNER =
                        TEInitUtils.setup(new ResonanceScannerItem("resonance_scanner"), "resonance_scanner"));

        TechExtensions.LOGGER.debug("TechExtension's Items Loaded");
    }

    private static void registerBlockEntities() {
        TEBlockEntities.register();
    }

    private static void registerMenus() {
        registerMenu(
                TEContent.RESONANCE_SCANNER_MENU = new MenuType<>(ResonanceScannerMenu::new, FeatureFlags.VANILLA_SET),
                ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "resonance_scanner"));
    }

    public static void registerIdent(Object object, ResourceLocation identifier) {
        objIdentMap.put(object, identifier);
    }
}
