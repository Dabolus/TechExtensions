package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.particle.TEParticleTypes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;

public class ShrinkRayItem extends Item implements RcEnergyItem {
    public enum ShrinkRayMode {
        SHRINK,
        ENLARGE,
        RESTORE
    }

    public final RcEnergyTier tier = RcEnergyTier.INSANE;

    private static final int SHRINK_DURATION_TICKS = 10;
    private static final int BEAM_DURATION_TICKS = 10;
    private static final WeakHashMap<LivingEntity, ShrinkTask> SHRINK_TASKS = new WeakHashMap<>();
    private static final List<BeamTask> BEAM_TASKS = new ArrayList<>();

    private static class AttributeModifierInfo {
        Holder<Attribute> attribute;
        ResourceLocation modifierId;
        double multiplyDelta;
        AttributeModifier.Operation operation;

        AttributeModifierInfo(
                Holder<Attribute> attribute,
                String modifierName,
                double multiplyDelta,
                AttributeModifier.Operation operation) {
            this.attribute = attribute;
            this.modifierId = ResourceLocation.fromNamespaceAndPath(
                    TechExtensions.MOD_ID, "shrink_ray_" + modifierName + "_adjustment");
            this.multiplyDelta = multiplyDelta;
            this.operation = operation;
        }

        AttributeModifierInfo(Holder<Attribute> attribute, String modifierName, double multiplyDelta) {
            this(attribute, modifierName, multiplyDelta, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        AttributeModifier getAttributeModifier(double modifierValue) {
            return new AttributeModifier(modifierId, modifierValue - 1.0D, operation);
        }
    }

    private static final AttributeModifierInfo SCALE_MODIFIER_INFO =
            new AttributeModifierInfo(Attributes.SCALE, "scale", 1.0D);

    private static final List<AttributeModifierInfo> ATTRIBUTE_MODIFIER_INFO = List.of(
            new AttributeModifierInfo(Attributes.ATTACK_DAMAGE, "attack_damage", 0.8D),
            new AttributeModifierInfo(
                    Attributes.ATTACK_KNOCKBACK, "attack_knockback", 0.34D, AttributeModifier.Operation.ADD_VALUE),
            // bigger = slower attack speed
            new AttributeModifierInfo(Attributes.ATTACK_SPEED, "attack_speed", -0.8D),
            new AttributeModifierInfo(Attributes.BLOCK_BREAK_SPEED, "block_break_speed", 0.8D),
            new AttributeModifierInfo(Attributes.BLOCK_INTERACTION_RANGE, "block_interaction_range", 0.6D),
            new AttributeModifierInfo(Attributes.ENTITY_INTERACTION_RANGE, "entity_interaction_range", 0.6D),
            // bigger = less fall damage (since you jump higher)
            new AttributeModifierInfo(Attributes.FALL_DAMAGE_MULTIPLIER, "fall_damage_multiplier", -0.8D),
            new AttributeModifierInfo(Attributes.FLYING_SPEED, "flying_speed", 0.8D),
            new AttributeModifierInfo(Attributes.GRAVITY, "gravity", 0.9D),
            new AttributeModifierInfo(Attributes.JUMP_STRENGTH, "jump_strength", 0.75D),
            new AttributeModifierInfo(
                    Attributes.KNOCKBACK_RESISTANCE,
                    "knockback_resistance",
                    0.068D,
                    AttributeModifier.Operation.ADD_VALUE),
            // bigger = less luck (since it's much easier to destroy blocks)
            new AttributeModifierInfo(Attributes.LUCK, "luck", -3.0D, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifierInfo(Attributes.MAX_HEALTH, "max_health", 0.4D),
            new AttributeModifierInfo(Attributes.MOVEMENT_SPEED, "movement_speed", 0.8D),
            new AttributeModifierInfo(Attributes.STEP_HEIGHT, "step_height", 0.8D));

    private static class ShrinkTask {
        final double startScale;
        final double targetScale;
        int currentTick = 0;

        ShrinkTask(double startScale, double targetScale) {
            this.startScale = startScale;
            this.targetScale = targetScale;
        }
    }

    private static class BeamTask {
        final Vec3 origin;
        final Vec3 target;
        int currentTick = 0;

        BeamTask(Vec3 origin, Vec3 target) {
            this.origin = origin;
            this.target = target;
        }
    }

    public ShrinkRayItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    private ShrinkRayMode getCurrentMode(ItemStack stack) {
        if (stack.get(TEDataComponentTypes.TOOL_MODE) == null) {
            return ShrinkRayMode.SHRINK;
        }
        int currentModeOrdinal = stack.get(TEDataComponentTypes.TOOL_MODE);
        return ShrinkRayMode.values()[currentModeOrdinal];
    }

    private void switchMode(ItemStack stack, Player entity) {
        // Cycle through modes
        ShrinkRayMode[] shrinkRayModes = ShrinkRayMode.values();
        int currentModeOrdinal = getCurrentMode(stack).ordinal();
        int nextMode = (currentModeOrdinal + 1) % shrinkRayModes.length;
        stack.set(TEDataComponentTypes.TOOL_MODE, nextMode);
        if (entity instanceof ServerPlayer serverPlayerEntity) {
            String modeText;
            switch (shrinkRayModes[nextMode]) {
                case SHRINK -> modeText = "Shrink";
                case ENLARGE -> modeText = "Enlarge";
                case RESTORE -> modeText = "Restore";
                default -> modeText = "Unknown";
            }
            serverPlayerEntity.displayClientMessage(
                    Component.translatable("techextensions.message.setTo")
                            .withStyle(ChatFormatting.GRAY)
                            .append(" ")
                            .append(Component.literal(modeText).withStyle(ChatFormatting.GOLD)),
                    true);
        }
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
        return TechExtensionsConfig.shrinkRayCharge;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public RcEnergyTier getTier() {
        return tier;
    }

    @Override
    public InteractionResult use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResult.SUCCESS;
        }
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        // Not enough energy, no-op
        if (!tryUseEnergy(stack, TechExtensionsConfig.shrinkRayCost)) {
            return InteractionResult.SUCCESS;
        }
        // Raycast for entities along the player's look vector
        final double range = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) * 8.0D;
        final Vec3 look = player.getViewVector(1.0F);
        final Vec3 start = getBeamStartPosition(player, look, hand);
        final Vec3 end = start.add(look.scale(range));
        final AABB searchBox =
                player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);

        Entity closestEntity = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity entity : world.getEntities(player, searchBox, e -> e.isAlive() && !e.isSpectator())) {
            final AABB entityBB = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.5D);
            final Optional<Vec3> intersection = entityBB.clip(start, end);
            if (intersection.isPresent()) {
                final double distSq = start.distanceToSqr(intersection.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestEntity = entity;
                }
            }
        }

        // Also check if the player is shooting at their own feet.
        // We use the lower part of the body so the box is below the eye position,
        // since AABB.clip returns empty when the ray starts inside the box.
        final AABB feetBB = getFeetAABB(player);
        final Optional<Vec3> selfHit = feetBB.clip(start, end);
        if (selfHit.isPresent()) {
            final double selfDistSq = start.distanceToSqr(selfHit.get());
            if (closestEntity == null || selfDistSq < closestDistSq) {
                closestEntity = player;
            }
        }

        // Beam particle trail
        final Vec3 beamEnd;
        if (closestEntity != null) {
            beamEnd = closestEntity.position().add(0, closestEntity.getBbHeight() * 0.5D, 0);
        } else {
            beamEnd = end;
        }
        BEAM_TASKS.add(new BeamTask(start, beamEnd));

        if (closestEntity instanceof LivingEntity living) {
            final double baseScale = living.getAttributeBaseValue(SCALE_MODIFIER_INFO.attribute);
            final double currentScale = living.getAttributeValue(SCALE_MODIFIER_INFO.attribute);
            final ShrinkRayMode mode = getCurrentMode(stack);

            // Bell-curve scaling: change is largest at the entity's base scale
            // and diminishes toward the min/max extremes using a Gaussian in log-space
            final double maxChange = 0.1D;
            final double logRatio = Math.log(currentScale / baseScale) / Math.log(2.0D);
            final double sigma = 1.8D;
            final double bellFactor = Math.exp(-(logRatio * logRatio) / (2.0D * sigma * sigma));
            final double change = maxChange * bellFactor;

            final double targetScale =
                    switch (mode) {
                        case SHRINK -> Math.max(0.0625D, currentScale * (1.0D - change));
                        case ENLARGE -> Math.min(16.0D, currentScale * (1.0D + change));
                        case RESTORE -> baseScale;
                    };
            SHRINK_TASKS.put(living, new ShrinkTask(currentScale, targetScale));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private Vec3 getBeamStartPosition(Player player, Vec3 viewVector, InteractionHand hand) {
        double playerScale = player.getAttributeValue(SCALE_MODIFIER_INFO.attribute);
        int playerHandSide = (player.getMainArm() == HumanoidArm.RIGHT) ? 1 : -1;
        if (hand == InteractionHand.OFF_HAND) {
            playerHandSide *= -1;
        }
        Vec3 rightVector = viewVector.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 downVector = viewVector.cross(rightVector).normalize();
        double forwardOffset = playerScale * -1.5D;
        double sideOffset = playerScale * 0.3D;
        double verticalOffset = playerScale * 0.1D;

        return player.getEyePosition(1.0F)
                .add(viewVector.scale(forwardOffset)) // Move forward from the eyes
                .add(rightVector.scale(sideOffset * playerHandSide)) // Move to the right or left depending on the hand
                .add(downVector.scale(verticalOffset)); // Move slightly down
    }

    private static AABB getFeetAABB(Player player) {
        final double playerHalfBBWidth = player.getBbWidth() * 0.5D;
        final double playerFeetHalfBBHeight = player.getBbHeight() * 0.25D;
        return new AABB(
                player.getX() - playerHalfBBWidth,
                player.getY() - playerFeetHalfBBHeight,
                player.getZ() - playerHalfBBWidth,
                player.getX() + playerHalfBBWidth,
                player.getY() + playerFeetHalfBBHeight,
                player.getZ() + playerHalfBBWidth);
    }

    // Item
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel worldIn, Entity entityIn, @Nullable EquipmentSlot slot) {
        if (worldIn.isClientSide() || (SHRINK_TASKS.isEmpty() && BEAM_TASKS.isEmpty())) {
            return;
        }

        // Process beam particles
        final Iterator<BeamTask> beamIt = BEAM_TASKS.iterator();
        while (beamIt.hasNext()) {
            final BeamTask beam = beamIt.next();
            beam.currentTick++;
            final double progress = Math.min(1.0D, (double) beam.currentTick / BEAM_DURATION_TICKS);
            final Vec3 particlePos =
                    beam.origin.add(beam.target.subtract(beam.origin).scale(progress));
            worldIn.playSound(
                    null,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE,
                    entityIn.getSoundSource(),
                    0.5F,
                    1.8F + (float) progress * 0.2F);
            worldIn.sendParticles(
                    TEParticleTypes.SHRINK_RAY,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.01D);
            if (beam.currentTick >= BEAM_DURATION_TICKS) {
                beamIt.remove();
            }
        }

        // Process scale animations
        final Iterator<Map.Entry<LivingEntity, ShrinkTask>> it =
                SHRINK_TASKS.entrySet().iterator();

        while (it.hasNext()) {
            final Map.Entry<LivingEntity, ShrinkTask> entry = it.next();
            final LivingEntity target = entry.getKey();
            final ShrinkTask task = entry.getValue();

            if (target.isRemoved()) {
                it.remove();
                continue;
            }

            task.currentTick++;
            final double progress = Math.min(1.0D, (double) task.currentTick / SHRINK_DURATION_TICKS);

            // Ease-out quadratic for smooth deceleration
            final double easedProgress = 1.0D - (1.0D - progress) * (1.0D - progress);

            // Interpolate scale with easing
            final double currentScale = task.startScale + (task.targetScale - task.startScale) * easedProgress;
            final AttributeInstance scaleAttr = target.getAttribute(SCALE_MODIFIER_INFO.attribute);
            if (scaleAttr != null) {
                scaleAttr.addOrReplacePermanentModifier(SCALE_MODIFIER_INFO.getAttributeModifier(currentScale));
                target.refreshDimensions();
            }
            if (task.currentTick >= SHRINK_DURATION_TICKS) {
                // Adjust attributes proportionally to the new scale
                double baseScale = scaleAttr == null ? 1.0D : scaleAttr.getBaseValue();
                for (AttributeModifierInfo attrInfo : ATTRIBUTE_MODIFIER_INFO) {
                    final AttributeInstance attrInstance = target.getAttribute(attrInfo.attribute);
                    if (attrInstance != null) {
                        // Apply the delta to the percent change in scale, so
                        // attributes adjust proportionally to the new size
                        double modifierValue = ((task.targetScale - baseScale) * attrInfo.multiplyDelta) + baseScale;
                        attrInstance.addOrReplacePermanentModifier(attrInfo.getAttributeModifier(modifierValue));
                    }
                }
                target.refreshDimensions();
                // Give saturation if target is shrunk, hunger if enlarged, remove both if restored
                Holder<MobEffect> newEffect = null;
                if (task.targetScale < baseScale) {
                    newEffect = MobEffects.SATURATION;
                    target.removeEffect(MobEffects.HUNGER);
                } else if (task.targetScale > baseScale) {
                    newEffect = MobEffects.HUNGER;
                    target.removeEffect(MobEffects.SATURATION);
                } else {
                    target.removeEffect(MobEffects.SATURATION);
                    target.removeEffect(MobEffects.HUNGER);
                }
                // Apply the new effect, if any
                if (newEffect != null) {
                    double absMultiplier =
                            task.targetScale > baseScale ? task.targetScale / baseScale : baseScale / task.targetScale;
                    int effectDuration = (int) Math.round(absMultiplier * 750.0D);
                    int effectLevel = (int) Math.round(absMultiplier * 0.22D); //
                    target.addEffect(new MobEffectInstance(newEffect, effectDuration, effectLevel, false, false, true));
                }
                it.remove();
            }
        }
    }
}
