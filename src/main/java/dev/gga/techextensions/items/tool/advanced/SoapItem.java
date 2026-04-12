package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.utils.TECleaningUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A simple durability-based item that cleans oxidized copper, waxed copper,
 * and mossy blocks using a brush-like hold mechanic.
 */
public class SoapItem extends Item {
    /** Ticks of continuous use required before the block is cleaned. */
    private static final int CLEAN_THRESHOLD_TICKS = 20;

    public SoapItem(String name) {
        super(TEItemSettings.item(name).durability(64));
    }

    // --- Interaction ---

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!TECleaningUtils.isCleanable(level, pos)) {
            return InteractionResult.PASS;
        }

        // Store target block pos in item NBT so onUseTick knows where to act
        ItemStack stack = context.getItemInHand();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong("soap_target_pos", pos.asLong());
        });

        if (context.getPlayer() != null) {
            context.getPlayer().startUsingItem(context.getHand());

            // Play a wet/soapy sound when starting to clean
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.PLAYERS, 0.7F, 1.4F);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32; // ~1.6 seconds total hold time
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide()) {
            return;
        }

        int usedTicks = getUseDuration(stack, entity) - remainingUseTicks;

        // Play brushing sound periodically
        if (usedTicks > 0 && usedTicks % 6 == 0) {
            level.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.BRUSH_GENERIC,
                    SoundSource.PLAYERS,
                    0.6F,
                    1.0F + level.getRandom().nextFloat() * 0.2F);
        }

        // Once threshold reached, clean the block
        if (usedTicks >= CLEAN_THRESHOLD_TICKS) {
            BlockPos targetPos = getTargetPos(stack);
            if (targetPos != null) {
                if (TECleaningUtils.cleanBlock(level, targetPos)) {
                    level.playSound(null, targetPos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // Damage the soap
                    stack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
                }
            }
            // Stop using
            clearTargetPos(stack);
            entity.stopUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        clearTargetPos(stack);
        return super.releaseUsing(stack, level, entity, timeLeft);
    }

    // --- NBT helpers ---

    private static BlockPos getTargetPos(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("soap_target_pos")) {
            return BlockPos.of(tag.getLong("soap_target_pos").orElse(0L));
        }
        return null;
    }

    private static void clearTargetPos(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("soap_target_pos");
        });
    }
}
