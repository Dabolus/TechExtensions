package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import reborncore.common.powerSystem.RcEnergyItem;

public class TEItemGroup {
    private static final ResourceKey<CreativeModeTab> ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "item_group"));

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.techextensions.item_group"))
                .icon(() -> new ItemStack(TEContent.META_TOOL))
                .build());

        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(TEItemGroup::entries);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(TEItemGroup::addTools);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(TEItemGroup::addIngredients);
    }

    private static void entries(FabricItemGroupEntries entries) {
        entries.addAfter(Items.NETHER_BRICK, TEContent.ELECTRIC_DUCTED_FAN);
        addPoweredItem(TEContent.META_TOOL, entries, null, true);
        addPoweredItem(TEContent.ELECTRIC_JETPACK, entries, null, true);
        addPoweredItem(TEContent.RESONANCE_SCANNER, entries, null, true);
    }

    private static void addTools(FabricItemGroupEntries entries) {
        addPoweredItem(TEContent.META_TOOL, entries, Items.BUCKET, false);
        addPoweredItem(TEContent.ELECTRIC_JETPACK, entries, Items.OAK_BOAT, false);
        addPoweredItem(TEContent.RESONANCE_SCANNER, entries, Items.IRON_SHOVEL, false);
    }

    private static void addIngredients(FabricItemGroupEntries entries) {
        entries.addAfter(Items.NETHER_BRICK, TEContent.ELECTRIC_DUCTED_FAN);
    }

    private static void addPoweredItem(Item item, FabricItemGroupEntries entries, ItemLike before, boolean includeUncharged) {
        ItemStack uncharged = new ItemStack(item);
        ItemStack charged = new ItemStack(item);
        RcEnergyItem energyItem = (RcEnergyItem) item;

        energyItem.setStoredEnergy(charged, energyItem.getEnergyCapacity(null));

        if (before == null) {
            if (includeUncharged) {
                entries.accept(uncharged);
            }
            entries.accept(charged);
        }
        else {
            if (includeUncharged) {
                entries.addBefore(before, uncharged, charged);
            }
            else {
                entries.addBefore(before, charged);
            }
        }
    }
}
