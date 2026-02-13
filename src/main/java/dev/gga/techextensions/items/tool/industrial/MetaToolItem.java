package dev.gga.techextensions.items.tool.industrial;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.init.TEToolMaterials;
import dev.gga.techextensions.utils.TECuttingUtils;
import dev.gga.techextensions.utils.TEItemsUtils;
import dev.gga.techextensions.utils.TEMiningUtils;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import reborncore.api.IToolHandler;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;
import techreborn.utils.TRItemUtils;
import techreborn.utils.ToolsUtil;

public class MetaToolItem extends Item implements RcEnergyItem, IToolHandler {
    public enum MetaToolMode {
        INACTIVE,
        AOE_3x3,
        SMART, // Vein-mining for ores, tree-capitating for logs/leaves, 3x3 otherwise
    }

    private BlockState lastCheckedBlockState;

    public MetaToolItem(String name) {
        super(TEItemSettings.unbreakable(name)
                .tool(TEToolMaterials.META_TOOL, TEContent.BlockTags.META_TOOL_MINEABLE, 3f, 1f, 0.0F));
    }

    private MetaToolMode getCurrentMode(ItemStack stack) {
        if (!TRItemUtils.isActive(stack) || stack.get(TEDataComponentTypes.TOOL_MODE) == null) {
            return MetaToolMode.INACTIVE;
        }
        int currentModeOrdinal = stack.get(TEDataComponentTypes.TOOL_MODE);
        return MetaToolMode.values()[currentModeOrdinal];
    }

    private void switchMode(ItemStack stack, int cost, Player entity) {
        TRItemUtils.checkActive(stack, cost, entity);
        if (!TRItemUtils.isActive(stack)) {
            TRItemUtils.switchActive(stack, cost, entity);
            stack.set(TEDataComponentTypes.TOOL_MODE, MetaToolMode.AOE_3x3.ordinal());
            if (entity instanceof ServerPlayer serverPlayerEntity) {
                serverPlayerEntity.displayClientMessage(
                        Component.translatable("techextensions.message.setTo")
                                .withStyle(ChatFormatting.GRAY)
                                .append(" ")
                                .append(Component.literal("3*3").withStyle(ChatFormatting.GOLD)),
                        true);
            }
        } else {
            // Cycle through modes
            MetaToolMode[] metaToolModes = MetaToolMode.values();
            int currentModeOrdinal = getCurrentMode(stack).ordinal();
            int nextMode = (currentModeOrdinal + 1) % metaToolModes.length;
            stack.set(TEDataComponentTypes.TOOL_MODE, nextMode);
            // If we cycled back to INACTIVE, turn off the tool
            if (nextMode == MetaToolMode.INACTIVE.ordinal()) {
                TRItemUtils.switchActive(stack, cost, entity);
            }
            if (entity instanceof ServerPlayer serverPlayerEntity) {
                String modeText =
                        switch (metaToolModes[nextMode]) {
                            case INACTIVE -> "Inactive";
                            case AOE_3x3 -> "3*3";
                            case SMART -> "Smart";
                        };
                serverPlayerEntity.displayClientMessage(
                        Component.translatable("techextensions.message.setTo")
                                .withStyle(ChatFormatting.GRAY)
                                .append(" ")
                                .append(Component.literal(modeText).withStyle(ChatFormatting.GOLD)),
                        true);
            }
        }
    }

    // MiningToolItem
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return Items.NETHERITE_AXE.isCorrectToolForDrops(stack, state)
                || Items.NETHERITE_SWORD.isCorrectToolForDrops(stack, state)
                || Items.NETHERITE_PICKAXE.isCorrectToolForDrops(stack, state)
                || Items.NETHERITE_SHOVEL.isCorrectToolForDrops(stack, state)
                || Items.SHEARS.isCorrectToolForDrops(stack, state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (getStoredEnergy(stack) >= TechExtensionsConfig.metaToolCost) {
            return TEToolMaterials.META_TOOL.speed();
        }
        Tool toolComponent = stack.get(DataComponents.TOOL);
        return toolComponent != null ? toolComponent.defaultMiningSpeed() : 1.0F;
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level worldIn, BlockState blockIn, BlockPos pos, LivingEntity entityLiving) {
        if (!(entityLiving instanceof Player playerIn)) {
            tryUseEnergy(stack, TechExtensionsConfig.metaToolCost);
            return true;
        }

        switch (getCurrentMode(stack)) {
            case INACTIVE -> {
                tryUseEnergy(stack, TechExtensionsConfig.metaToolCost);
            }
            case AOE_3x3 -> {
                TEMiningUtils.mineAoe3x3(stack, worldIn, playerIn, pos, entityLiving);
            }
            case SMART -> {
                // Check if we can vein-mine first
                if (lastCheckedBlockState != null && TEMiningUtils.isValidVeinMineStartBlock(lastCheckedBlockState)) {
                    List<BlockPos> ores = TEMiningUtils.findVein(worldIn, pos);
                    ores.remove(pos);
                    ores.stream()
                            .filter(p -> tryUseEnergy(stack, TechExtensionsConfig.metaToolCost))
                            .forEach(pos1 -> ToolsUtil.breakBlock(
                                    stack, worldIn, pos1, entityLiving, TechExtensionsConfig.metaToolCost));
                }
                // Then, check if we can tree-capitate
                else if (lastCheckedBlockState == null
                        || TECuttingUtils.isValidTreeCapitateStartBlock(lastCheckedBlockState)) {
                    TECuttingUtils.FindWoodResult findWoodResult = TECuttingUtils.findWood(worldIn, pos);
                    List<BlockPos> wood = findWoodResult.wood();
                    List<BlockPos> leaves = findWoodResult.leaves();
                    wood.remove(pos);
                    wood.stream()
                            .filter(p -> tryUseEnergy(stack, TechExtensionsConfig.metaToolCost))
                            .forEach(pos1 -> ToolsUtil.breakBlock(
                                    stack, worldIn, pos1, entityLiving, TechExtensionsConfig.metaToolCost));
                    leaves.remove(pos);
                    leaves.forEach(pos1 -> ToolsUtil.breakBlock(stack, worldIn, pos1, entityLiving, 0));
                }
                // Otherwise, fallback to AOE 3x3
                else {
                    TEMiningUtils.mineAoe3x3(stack, worldIn, playerIn, pos, entityLiving);
                }
            }
        }

        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (tryUseEnergy(stack, TechExtensionsConfig.metaToolHitCost)
                && target.level() instanceof ServerLevel serverWorld) {
            target.hurtServer(serverWorld, serverWorld.damageSources().playerAttack((Player) attacker), 8F);
        }
    }

    // Item
    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level world, BlockPos pos, LivingEntity miner) {
        lastCheckedBlockState = state;
        return super.canDestroyBlock(stack, state, world, pos, miner);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand interactionHand) {
        final ItemStack stack = player.getItemInHand(interactionHand);
        if (player.isShiftKeyDown()) {
            switchMode(stack, TechExtensionsConfig.metaToolCost, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        InteractionResult tryUse = Items.NETHERITE_AXE.useOn(context);
        if (tryUse != InteractionResult.PASS) {
            return tryUse;
        }

        tryUse = Items.SHEARS.useOn(context);
        if (tryUse != InteractionResult.PASS) {
            return tryUse;
        }

        tryUse = Items.NETHERITE_SHOVEL.useOn(context);
        if (tryUse != InteractionResult.PASS) {
            return tryUse;
        }

        return TEItemsUtils.placeTorch(context);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemUtils.getPowerForDurabilityBar(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemUtils.getColorForDurabilityBar(stack);
    }

    // RcEnergyItem
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return TechExtensionsConfig.metaToolCharge;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public RcEnergyTier getTier() {
        return RcEnergyTier.INSANE;
    }

    // IToolHandler
    @Override
    public boolean handleTool(
            ItemStack stack, BlockPos pos, Level world, Player player, Direction side, boolean damage) {
        if (!player.level().isClientSide() && this.getStoredEnergy(stack) >= 5.0) {
            this.tryUseEnergy(stack, 5);
            return true;
        } else {
            return false;
        }
    }
}
