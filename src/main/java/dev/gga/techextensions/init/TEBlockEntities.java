package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.blockentity.machine.ElectricDuctedFanBlockEntity;
import java.util.function.BiFunction;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.Validate;

public class TEBlockEntities {
    public static final BlockEntityType<ElectricDuctedFanBlockEntity> ELECTRIC_DUCTED_FAN =
            register(ElectricDuctedFanBlockEntity::new, "electric_ducted_fan", TEContent.ELECTRIC_DUCTED_FAN);

    public static <T extends BlockEntity> BlockEntityType<T> register(
            BiFunction<BlockPos, BlockState, T> supplier, String name, Block... blocks) {
        Validate.isTrue(blocks.length > 0, "no blocks for blockEntity entity type!");
        return register(
                Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, name),
                FabricBlockEntityTypeBuilder.create(supplier::apply, blocks));
    }

    public static <T extends BlockEntity> BlockEntityType<T> register(
            Identifier id, FabricBlockEntityTypeBuilder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, builder.build());
    }

    public static void register() {
        // Forces static initialization
        TechExtensions.LOGGER.debug("TechExtension's Block Entities Loaded");
    }
}
