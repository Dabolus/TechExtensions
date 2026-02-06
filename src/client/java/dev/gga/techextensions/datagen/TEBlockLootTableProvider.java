package dev.gga.techextensions.datagen;

import dev.gga.techextensions.blocks.machine.ElectricDuctedFanBlock;
import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class TEBlockLootTableProvider extends FabricBlockLootTableProvider {
    public TEBlockLootTableProvider(
            FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // Electric Ducted Fan drops the number of fans stacked
        add(
                TEContent.ELECTRIC_DUCTED_FAN,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0f))
                                .when(ExplosionCondition.survivesExplosion())
                                .add(LootItem.lootTableItem(TEContent.ELECTRIC_DUCTED_FAN)
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f))
                                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(
                                                                TEContent.ELECTRIC_DUCTED_FAN)
                                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(ElectricDuctedFanBlock.FANS, 1))))
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0f))
                                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(
                                                                TEContent.ELECTRIC_DUCTED_FAN)
                                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(ElectricDuctedFanBlock.FANS, 2))))
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0f))
                                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(
                                                                TEContent.ELECTRIC_DUCTED_FAN)
                                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(ElectricDuctedFanBlock.FANS, 3))))
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(4.0f))
                                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(
                                                                TEContent.ELECTRIC_DUCTED_FAN)
                                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(ElectricDuctedFanBlock.FANS, 4)))))));
        ;
    }
}
