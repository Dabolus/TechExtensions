package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

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
        Consumer<Item> toHandheld = (item) ->
            itemModelGenerators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);

        add(TEContent.META_TOOL, toHandheld);
    }
}
