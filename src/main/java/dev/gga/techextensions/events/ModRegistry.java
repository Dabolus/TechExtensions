package dev.gga.techextensions.events;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.entity.BubbleTrapEntity;
import dev.gga.techextensions.init.TEBlockEntities;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.init.TEInitUtils;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.items.armor.ElectricJetpackItem;
import dev.gga.techextensions.items.tool.advanced.BubbleGunItem;
import dev.gga.techextensions.items.tool.advanced.CyberShieldItem;
import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import dev.gga.techextensions.items.tool.advanced.ShrinkRayItem;
import dev.gga.techextensions.items.tool.advanced.SoapItem;
import dev.gga.techextensions.items.tool.advanced.VacuumGunItem;
import dev.gga.techextensions.items.tool.industrial.MetaToolItem;
import dev.gga.techextensions.menu.BubbleGunMenu;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import dev.gga.techextensions.menu.VacuumGunMenu;
import java.util.HashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.Validate;

public class ModRegistry {
    private static final HashMap<Object, Identifier> objIdentMap = new HashMap<>();

    public static void register() {
        registerBlocks();
        registerItems();
        registerBlockEntities();
        registerMenus();
        registerEntityTypes();
    }

    public static void registerMenu(MenuType<?> menu, Identifier name) {
        Registry.register(BuiltInRegistries.MENU, name, menu);
    }

    public static void registerBlock(Block block, Identifier name) {
        Registry.register(BuiltInRegistries.BLOCK, name, block);
    }

    public static void registerBlock(Block block) {
        Validate.isTrue(objIdentMap.containsKey(block));
        registerBlock(block, objIdentMap.get(block));
    }

    public static void registerItem(Item item, Identifier name) {
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
        registerItem(TEContent.BUBBLE_GUN = TEInitUtils.setup(new BubbleGunItem("bubble_gun"), "bubble_gun"));
        registerItem(TEContent.CYBER_SHIELD = TEInitUtils.setup(new CyberShieldItem("cyber_shield"), "cyber_shield"));
        registerItem(
                TEContent.ELECTRIC_JETPACK =
                        TEInitUtils.setup(new ElectricJetpackItem("electric_jetpack"), "electric_jetpack"));
        registerItem(TEContent.META_TOOL = TEInitUtils.setup(new MetaToolItem("meta_tool"), "meta_tool"));
        registerItem(TEContent.SHRINK_RAY = TEInitUtils.setup(new ShrinkRayItem("shrink_ray"), "shrink_ray"));
        registerItem(TEContent.SOAP = TEInitUtils.setup(new SoapItem("soap"), "soap"));
        registerItem(
                TEContent.RESONANCE_SCANNER =
                        TEInitUtils.setup(new ResonanceScannerItem("resonance_scanner"), "resonance_scanner"));
        registerItem(TEContent.VACUUM_GUN = TEInitUtils.setup(new VacuumGunItem("vacuum_gun"), "vacuum_gun"));

        TechExtensions.LOGGER.debug("TechExtension's Items Loaded");
    }

    private static void registerBlockEntities() {
        TEBlockEntities.register();
    }

    private static void registerMenus() {
        registerMenu(
                TEContent.BUBBLE_GUN_MENU = new MenuType<>(BubbleGunMenu::new, FeatureFlags.VANILLA_SET),
                Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "bubble_gun"));
        registerMenu(
                TEContent.RESONANCE_SCANNER_MENU = new MenuType<>(ResonanceScannerMenu::new, FeatureFlags.VANILLA_SET),
                Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "resonance_scanner"));
        registerMenu(
                TEContent.VACUUM_GUN_MENU = new MenuType<>(VacuumGunMenu::new, FeatureFlags.VANILLA_SET),
                Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "vacuum_gun"));
    }

    private static void registerEntityTypes() {
        Identifier id = Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "bubble_trap");
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        TEContent.BUBBLE_TRAP_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id,
                EntityType.Builder.<BubbleTrapEntity>of(BubbleTrapEntity::new, MobCategory.MISC)
                        .sized(1.0F, 1.0F)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .build(key));
    }

    public static void registerIdent(Object object, Identifier identifier) {
        objIdentMap.put(object, identifier);
    }
}
