package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;

public class CyberShieldItem extends Item implements RcEnergyItem, Equipable {
    public enum CyberShieldMode {
        STANDARD,
        PERMA_PARRY,
    }

    private static final float STANDARD_BLOCKING_ANGLE = 120.0F;
    private static final float PERMA_PARRY_BLOCKING_ANGLE = 40.0F;

    private static final float CHARGED_DAMAGE_REDUCTION = 0.9F;
    private static final float DISCHARGED_DAMAGE_REDUCTION = 0.1F;

    /** Ticks after raising the shield before it starts blocking (hardcoded by vanilla in 1.21.1). */
    private static final int BLOCK_DELAY_TICKS = 5;

    /**
     * Tracks how many ticks each player has been blocking, so we can drain
     * energy in bulk when they stop. Weak keys so entities that die or
     * disconnect while blocking are not retained.
     */
    private static final Map<LivingEntity, Integer> blockingTicks = new WeakHashMap<>();

    /**
     * Whether the Cyber Shield being used by `defender` blocks `source`: the attack must come from within the
     * mode's horizontal blocking angle (measured from the look direction). Called from `CyberShieldParryMixin`,
     * as 1.21.1 has no `BlocksAttacks` component and vanilla blocks everything in front of the player.
     */
    public static boolean blocks(LivingEntity defender, ItemStack stack, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)
                || (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0)) {
            return false;
        }
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null) {
            return false;
        }
        Vec3 view = defender.calculateViewVector(0.0F, defender.getYHeadRot());
        Vec3 toSource = sourcePosition.subtract(defender.position());
        toSource = new Vec3(toSource.x, 0.0, toSource.z).normalize();
        return Math.acos(Mth.clamp(toSource.dot(view), -1.0, 1.0))
                <= Math.toRadians(blockingAngleForMode(getMode(stack)));
    }

    /** Fraction of a blocked hit absorbed by the shield: 0.0 (no protection) to 1.0 (full protection). */
    public float damageReductionFactor(ItemStack stack) {
        return getStoredEnergy(stack) > 0 ? CHARGED_DAMAGE_REDUCTION : DISCHARGED_DAMAGE_REDUCTION;
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
        super(TEItemSettings.unbreakable(name));
    }

    // Equipable: mobs and dispensers put it in the off hand, like a shield
    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.OFFHAND;
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
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
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
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Called on both sides in 1.21.1, the logic below is server-only
        if (level.isClientSide()) {
            return;
        }
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
            int ticksSinceBlocking = defender.getTicksUsingItem() - BLOCK_DELAY_TICKS;

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
                        originalArrow.getPickupItemStackOrigin(),
                        originalArrow.getWeaponItem());
                reflected.setOwner(defender);
                reflected.pickup = originalArrow.pickup;
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
                projectile.deflect(ProjectileDeflection.REVERSE, defender, defender, true);
                projectile.setDeltaMovement(
                        projectile.getDeltaMovement().normalize().scale(1.5));
            }
            return;
        }

        // Melee parry: damage + knockback the attacker
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            DamageSource parrySource = level.damageSources().thorns(defender);
            float parryDamage = TechExtensionsConfig.cyberShieldPerfectParryDamage;
            livingAttacker.hurt(parrySource, parryDamage);

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
            String modeTranslationKey = switch (next) {
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
    public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
