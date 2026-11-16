package dev.gga.techextensions.datagen;

import dev.gga.techextensions.blocks.machine.ElectricDuctedFanBlock;
import dev.gga.techextensions.init.TEContent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class TEBlockLootTableProvider extends FabricBlockLootSubProvider {
    public TEBlockLootTableProvider(
            FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
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
                                        .apply(
                                                List.of(1, 2, 3, 4),
                                                fans -> SetItemCountFunction.setCount(ConstantValue.exactly(fans))
                                                        .when(
                                                                LootItemBlockStatePropertyCondition
                                                                        .hasBlockStateProperties(
                                                                                TEContent.ELECTRIC_DUCTED_FAN)
                                                                        .setProperties(
                                                                                StatePropertiesPredicate.Builder
                                                                                        .properties()
                                                                                        .hasProperty(
                                                                                                ElectricDuctedFanBlock
                                                                                                        .FANS,
                                                                                                fans)))))));
    }
}
