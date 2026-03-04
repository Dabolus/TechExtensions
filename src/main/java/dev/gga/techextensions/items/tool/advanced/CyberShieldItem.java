package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;

public class CyberShieldItem extends Item implements RcEnergyItem {
    public enum CyberShieldMode {
        STANDARD,
        PERMA_PARRY,
    }

    private static final float STANDARD_BLOCKING_ANGLE = 120.0F;
    private static final float PERMA_PARRY_BLOCKING_ANGLE = 40.0F;

    private static final float CHARGED_DAMAGE_REDUCTION = 0.9F;
    private static final float DISCHARGED_DAMAGE_REDUCTION = 0.1F;

    /**
     * Tracks how many ticks each player has been blocking, so we can drain
     * energy in bulk when they stop.
     */
    private static final Map<LivingEntity, Integer> blockingTicks = new HashMap<>();

    /**
     * Creates a `BlocksAttacks` component with the given parameters.
     *
     * @param blockingAngle horizontal blocking angle in degrees
     * @param damageReductionFactor 0.0 (no protection) to 1.0 (full protection)
     */
    private static BlocksAttacks createBlocksAttacks(float blockingAngle, float damageReductionFactor) {
        return new BlocksAttacks(
                0.25F,
                1.0F,
                List.of(new BlocksAttacks.DamageReduction(
                        blockingAngle, Optional.empty(), 0.0F, damageReductionFactor)),
                new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK));
    }

    /** Returns the current `CyberShieldMode` from the stack's data components. */
    public static CyberShieldMode getMode(ItemStack stack) {
        Integer ordinal = stack.get(TEDataComponentTypes.TOOL_MODE);
        if (ordinal == null || ordinal < 0 || ordinal >= CyberShieldMode.values().length) {
            return CyberShieldMode.STANDARD;
        }
        return CyberShieldMode.values()[ordinal];
    }

    private static float blockingAngleForMode(CyberShieldMode mode) {
        return mode == CyberShieldMode.PERMA_PARRY ? PERMA_PARRY_BLOCKING_ANGLE : STANDARD_BLOCKING_ANGLE;
    }

    private static long blockCostPerTickForMode(CyberShieldMode mode) {
        return mode == CyberShieldMode.PERMA_PARRY
                ? TechExtensionsConfig.cyberShieldPermaParryCostPerTick
                : TechExtensionsConfig.cyberShieldStandardCostPerTick;
    }

    public CyberShieldItem(String name) {
        super(TEItemSettings.unbreakable(name)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .component(
                        DataComponents.BLOCKS_ATTACKS,
                        createBlocksAttacks(STANDARD_BLOCKING_ANGLE, CHARGED_DAMAGE_REDUCTION)));
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
        return TechExtensionsConfig.cyberShieldCharge;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public RcEnergyTier getTier() {
        return RcEnergyTier.HIGH;
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    /**
     * Tracks blocking ticks and force-stops blocking when the shield
     * cannot afford another tick of energy cost.
     *
     * We do not call `tryUseEnergy` here to avoid modifying the stack's
     * components, which would cause flickering.
     * Energy is drained in bulk in `inventoryTick` once blocking ends.
     */
    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (world.isClientSide() || !entity.isUsingItem()) return;

        CyberShieldMode mode = getMode(stack);
        long costPerTick = blockCostPerTickForMode(mode);

        int ticks = blockingTicks.getOrDefault(entity, 0) + 1;
        blockingTicks.put(entity, ticks);

        // Force-stop blocking when remaining energy can't cover the next tick
        long totalCost = (long) ticks * costPerTick;
        if (getStoredEnergy(stack) < totalCost + costPerTick) {
            entity.stopUsingItem();
        }
    }

    /**
     * Drains accumulated blocking energy in bulk once the player stops blocking
     * to prevent animation flickering from per-tick component updates.
     * Updates the `BlocksAttacks` component to reflect charged vs discharged
     * state and the current mode's blocking angle.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        // Bulk-drain energy when blocking ends
        if (entity instanceof LivingEntity livingEntity) {
            boolean isBlocking =
                    livingEntity.isUsingItem() && livingEntity.getUseItem().getItem() instanceof CyberShieldItem;

            if (!isBlocking) {
                Integer ticks = blockingTicks.remove(livingEntity);
                if (ticks != null && ticks > 0) {
                    CyberShieldMode mode = getMode(stack);
                    long totalCost = (long) ticks * blockCostPerTickForMode(mode);
                    tryUseEnergy(stack, totalCost);
                }
            }
        }

        CyberShieldMode mode = getMode(stack);

        // Update the protection component if the charged state or mode changed
        boolean charged = getStoredEnergy(stack) > 0;
        float targetFactor = charged ? CHARGED_DAMAGE_REDUCTION : DISCHARGED_DAMAGE_REDUCTION;
        float targetAngle = blockingAngleForMode(mode);

        BlocksAttacks current = stack.get(DataComponents.BLOCKS_ATTACKS);
        if (current != null
                && !current.damageReductions().isEmpty()
                && current.damageReductions().getFirst().factor() == targetFactor
                && current.damageReductions().getFirst().horizontalBlockingAngle() == targetAngle) {
            return;
        }

        stack.set(DataComponents.BLOCKS_ATTACKS, createBlocksAttacks(targetAngle, targetFactor));
    }

    /**
     * Called from `CyberShieldParryMixin` when the Cyber Shield successfully blocks damage.
     * If the player raised the shield within the configurable parry window, triggers a "perfect parry".
     */
    public static void handleParry(
            ServerLevel level,
            LivingEntity defender,
            ItemStack shieldStack,
            CyberShieldItem shield,
            DamageSource source) {

        // Check energy
        if (shield.getStoredEnergy(shieldStack) < TechExtensionsConfig.cyberShieldPerfectParryCost) return;

        // In Perma-Parry mode, every blocked hit is a parry.
        // In Standard mode, check whether the block happened within the parry window.
        CyberShieldMode mode = getMode(shieldStack);
        if (mode != CyberShieldMode.PERMA_PARRY) {
            BlocksAttacks blocksAttacks = shieldStack.get(DataComponents.BLOCKS_ATTACKS);
            int blockDelay = blocksAttacks != null ? blocksAttacks.blockDelayTicks() : 0;
            int ticksSinceBlocking = defender.getTicksUsingItem() - blockDelay;

            if (ticksSinceBlocking < 0 || ticksSinceBlocking > TechExtensionsConfig.cyberShieldPerfectParryWindow)
                return;
        }

        // Drain parry energy cost
        shield.tryUseEnergy(shieldStack, TechExtensionsConfig.cyberShieldPerfectParryCost);

        // Play parry sound
        level.playSound(
                null,
                defender.getX(),
                defender.getY(),
                defender.getZ(),
                SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS,
                0.6F,
                1.8F);
        // Spawn particles
        double px = defender.getX() + defender.getLookAngle().x * 0.5;
        double py = defender.getEyeY() - 0.2;
        double pz = defender.getZ() + defender.getLookAngle().z * 0.5;
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 15, 0.4, 0.4, 0.4, 0.2);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 10, 0.3, 0.3, 0.3, 0.1);

        Entity directEntity = source.getDirectEntity();

        // Projectile reflection: spawn a new arrow aimed back at the attacker
        if (directEntity instanceof AbstractArrow originalArrow) {
            Entity attacker = source.getEntity();
            if (attacker != null) {
                AbstractArrow reflected = new Arrow(
                        level,
                        defender.getX(),
                        defender.getEyeY(),
                        defender.getZ(),
                        originalArrow.getWeaponItem(),
                        null);
                reflected.setOwner(defender);
                double dx = attacker.getX() - defender.getX();
                double dy = attacker.getEyeY() - defender.getEyeY();
                double dz = attacker.getZ() - defender.getZ();
                reflected.shoot(dx, dy, dz, 2.0F, 0.0F);
                level.addFreshEntity(reflected);
                originalArrow.discard();
            }
            return;
        }

        // Other projectile reflection (fireballs, etc.)
        if (directEntity instanceof Projectile projectile) {
            Entity attacker = source.getEntity();
            if (attacker != null) {
                double dx = attacker.getX() - defender.getX();
                double dy = attacker.getEyeY() - defender.getEyeY();
                double dz = attacker.getZ() - defender.getZ();
                projectile.deflect(ProjectileDeflection.REVERSE, defender, EntityReference.of(defender), true);
                projectile.setDeltaMovement(
                        projectile.getDeltaMovement().normalize().scale(1.5));
            }
            return;
        }

        // Melee parry: damage + knockback the attacker
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            livingAttacker.hurtServer(
                    level, level.damageSources().thorns(defender), TechExtensionsConfig.cyberShieldPerfectParryDamage);

            double dx = defender.getX() - livingAttacker.getX();
            double dz = defender.getZ() - livingAttacker.getZ();
            livingAttacker.knockback(TechExtensionsConfig.cyberShieldPerfectParryKnockback, dx, dz);
        }
    }

    private void switchMode(ItemStack stack, Player player) {
        CyberShieldMode[] modes = CyberShieldMode.values();
        CyberShieldMode current = getMode(stack);
        CyberShieldMode next = modes[(current.ordinal() + 1) % modes.length];
        stack.set(TEDataComponentTypes.TOOL_MODE, next.ordinal());

        if (player instanceof ServerPlayer serverPlayer) {
            String modeTranslationKey =
                    switch (next) {
                        case STANDARD -> "techextensions.message.cyber_shield.mode_standard";
                        case PERMA_PARRY -> "techextensions.message.cyber_shield.mode_perma_parry";
                    };
            serverPlayer.displayClientMessage(
                    Component.translatable("techextensions.message.set_to")
                            .withStyle(ChatFormatting.GRAY)
                            .append(" ")
                            .append(Component.translatable(modeTranslationKey).withStyle(ChatFormatting.GOLD)),
                    true);
        }
    }

    @Override
    public InteractionResult use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
}
