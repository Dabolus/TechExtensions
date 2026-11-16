package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class TEModelProvider extends FabricModelProvider {
    public TEModelProvider(FabricDataOutput output) {
        super(output);
    }

    static <T> void add(T target, Consumer<T> consumer) {
        consumer.accept(target);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {}

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        Consumer<ItemLike> toGenerated =
                (info) -> itemModelGenerators.generateFlatItem(info.asItem(), ModelTemplates.FLAT_ITEM);
        Consumer<Item> toHandheld =
                (item) -> itemModelGenerators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);

        add(TEContent.ELECTRIC_DUCTED_FAN, toGenerated);
        add(TEContent.ELECTRIC_JETPACK, toGenerated);
        add(TEContent.META_TOOL, toHandheld);
        // The screen layer (tint index 1) is colored by ResonanceScannerScreenTintSource
        add(
                TEContent.RESONANCE_SCANNER,
                (info) -> itemModelGenerators.generateLayeredItem(
                        ModelLocationUtils.getModelLocation(info.asItem()),
                        TextureMapping.getItemTexture(info.asItem()),
                        TextureMapping.getItemTexture(info.asItem(), "_screen")));
    }
}
