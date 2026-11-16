package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.items.TEEnergyItem;
import dev.gga.techextensions.particle.TEParticleTypes;
import dev.gga.techextensions.utils.TEAimingUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.Vec3;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;
import virtuoel.pehkui.util.ScaleUtils;

public class ShrinkRayItem extends Item implements TEEnergyItem {
    public enum ShrinkRayMode {
        SHRINK,
        ENLARGE,
        RESTORE
    }

    public final RcEnergyTier tier = RcEnergyTier.INSANE;

    private static final int SHRINK_DURATION_TICKS = 10;
    private static final int BEAM_DURATION_TICKS = 10;
    private static final double MIN_SCALE = 0.0625D;
    private static final double MAX_SCALE = 16.0D;
    private static final WeakHashMap<LivingEntity, ShrinkTask> SHRINK_TASKS = new WeakHashMap<>();
    private static final List<BeamTask> BEAM_TASKS = new ArrayList<>();

    /**
     * An attribute adjusted together with the size.
     *
     * @param multiplyDelta how much of the size change (as a ratio to the natural size) applies to the attribute
     */
    private record AttributeModifierInfo(
            Attribute attribute, String name, double multiplyDelta, AttributeModifier.Operation operation) {

        AttributeModifierInfo {
            name = TechExtensions.MOD_ID + ":shrink_ray_" + name + "_adjustment";
        }

        AttributeModifierInfo(Attribute attribute, String modifierName, double multiplyDelta) {
            this(attribute, modifierName, multiplyDelta, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        /** Modifiers are identified by UUID in 1.20.1: derive a stable one from the name. */
        UUID modifierId() {
            return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        }

        /** Modifier for a size ratio (new scale / natural scale); a ratio of 1 means "unchanged". */
        AttributeModifier forRatio(double ratio) {
            return new AttributeModifier(modifierId(), name, (ratio - 1.0D) * multiplyDelta, operation);
        }
    }

    /**
     * A Pehkui scale standing in for an attribute that 1.20.1 doesn't have, changed like a multiplied attribute.
     *
     * @param multiplyDelta how much of the size change (as a ratio to the natural size) applies to the scale
     */
    private record ScaleInfo(ScaleType scaleType, double multiplyDelta) {
        float forRatio(double ratio) {
            return (float) Math.max(MIN_PEHKUI_SCALE, 1.0D + (ratio - 1.0D) * multiplyDelta);
        }
    }

    /** Pehkui scales must stay positive, unlike the attributes they stand in for (a fall damage multiplier of 0). */
    private static final double MIN_PEHKUI_SCALE = 1.0E-4D;

    /**
     * The size, as a ratio to the natural size: the Pehkui height scale holds it, as there is no scale attribute in
     * 1.20.1. The width and third person camera distance follow it, like with the scale attribute of later versions.
     */
    private static final List<ScaleType> SIZE_SCALE_TYPES =
            List.of(ScaleTypes.HEIGHT, ScaleTypes.WIDTH, ScaleTypes.THIRD_PERSON);

    private static final List<ScaleInfo> SCALE_INFO = List.of(
            // Block break speed
            new ScaleInfo(ScaleTypes.MINING_SPEED, 0.8D),
            // Block and entity interaction ranges
            new ScaleInfo(ScaleTypes.BLOCK_REACH, 0.6D),
            new ScaleInfo(ScaleTypes.ENTITY_REACH, 0.6D),
            // Fall damage multiplier: bigger = less fall damage (since you jump higher)
            new ScaleInfo(ScaleTypes.FALLING, -0.8D),
            // Jump strength
            new ScaleInfo(ScaleTypes.JUMP_HEIGHT, 0.75D),
            // Step height
            new ScaleInfo(ScaleTypes.STEP_HEIGHT, 0.8D));

    /** How much of the size change applies to the gravity (see `ShrinkRayGravityMixin`). */
    public static final double GRAVITY_MULTIPLY_DELTA = 0.9D;

    private static final List<AttributeModifierInfo> ATTRIBUTE_MODIFIER_INFO = List.of(
            new AttributeModifierInfo(Attributes.ATTACK_DAMAGE, "attack_damage", 0.8D),
            new AttributeModifierInfo(
                    Attributes.ATTACK_KNOCKBACK, "attack_knockback", 0.34D, AttributeModifier.Operation.ADDITION),
            // bigger = slower attack speed
            new AttributeModifierInfo(Attributes.ATTACK_SPEED, "attack_speed", -0.8D),
            new AttributeModifierInfo(Attributes.FLYING_SPEED, "flying_speed", 0.8D),
            new AttributeModifierInfo(
                    Attributes.KNOCKBACK_RESISTANCE,
                    "knockback_resistance",
                    0.068D,
                    AttributeModifier.Operation.ADDITION),
            // bigger = less luck (since it's much easier to destroy blocks)
            new AttributeModifierInfo(Attributes.LUCK, "luck", -3.0D, AttributeModifier.Operation.ADDITION),
            new AttributeModifierInfo(Attributes.MAX_HEALTH, "max_health", 0.4D),
            new AttributeModifierInfo(Attributes.MOVEMENT_SPEED, "movement_speed", 0.8D));

    /**
     * Animates the scale from start to target. Both are relative to the entity's natural scale, which is 1 as the
     * Pehkui scales only hold this ray's change (babies and other scale changes are applied on top of them).
     */
    private static final class ShrinkTask {
        final double startScale;
        final double targetScale;
        final double naturalScale;
        int currentTick = 0;

        ShrinkTask(double startScale, double targetScale, double naturalScale) {
            this.startScale = startScale;
            this.targetScale = targetScale;
            this.naturalScale = naturalScale;
        }
    }

    private static final class BeamTask {
        final ServerLevel level;
        final Vec3 origin;
        final Vec3 target;
        final SoundSource soundSource;
        int currentTick = 0;

        BeamTask(ServerLevel level, Vec3 origin, Vec3 target, SoundSource soundSource) {
            this.level = level;
            this.origin = origin;
            this.target = target;
            this.soundSource = soundSource;
        }
    }

    public ShrinkRayItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    /** Registers the server tick handler that drives beams and scale animations. */
    public static void initTicking() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tickTasks());
    }

    private ShrinkRayMode getCurrentMode(ItemStack stack) {
        if (TEDataComponentTypes.getToolMode(stack) == null) {
            return ShrinkRayMode.SHRINK;
        }
        int currentModeOrdinal = TEDataComponentTypes.getToolMode(stack);
        return ShrinkRayMode.values()[currentModeOrdinal];
    }

    private void switchMode(ItemStack stack, Player entity) {
        // Cycle through modes
        ShrinkRayMode[] shrinkRayModes = ShrinkRayMode.values();
        int currentModeOrdinal = getCurrentMode(stack).ordinal();
        int nextMode = (currentModeOrdinal + 1) % shrinkRayModes.length;
        TEDataComponentTypes.setToolMode(stack, nextMode);
        if (entity instanceof ServerPlayer serverPlayerEntity) {
            String modeTranslationKey = switch (shrinkRayModes[nextMode]) {
                case SHRINK -> "techextensions.message.shrink_ray.mode_shrink";
                case ENLARGE -> "techextensions.message.shrink_ray.mode_enlarge";
                case RESTORE -> "techextensions.message.shrink_ray.mode_restore";
            };
            serverPlayerEntity.displayClientMessage(
                    Component.translatable("techextensions.message.set_to")
                            .withStyle(ChatFormatting.GRAY)
                            .append(" ")
                            .append(Component.translatable(modeTranslationKey).withStyle(ChatFormatting.GOLD)),
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
    public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }
        if (!(world instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }
        // Not enough energy, no-op
        if (!tryUseEnergy(stack, TechExtensionsConfig.shrinkRayCost)) {
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }
        // Raycast for entities along the player's look vector
        // 1.20.1 has no interaction range attribute: the default range, scaled by the Pehkui entity reach
        final double range = (player.isCreative() ? 5.0D : 3.0D) * ScaleUtils.getEntityReachScale(player) * 8.0D;
        final Vec3 look = player.getViewVector(1.0F);
        final Vec3 start = getBeamStartPosition(player, look, hand);
        final Vec3 end = start.add(look.scale(range));

        TEAimingUtils.EntityHit entityHit = TEAimingUtils.traceEntity(
                world,
                player,
                start,
                end,
                TEAimingUtils.ALIVE_NON_SPECTATOR,
                0.5D,
                TEAimingUtils.buildFeetAABB(player));
        Entity closestEntity = entityHit != null ? entityHit.entity() : null;

        // Beam particle trail
        final Vec3 beamEnd;
        if (closestEntity != null) {
            beamEnd = closestEntity.position().add(0, closestEntity.getBbHeight() * 0.5D, 0);
        } else {
            beamEnd = end;
        }
        BEAM_TASKS.add(new BeamTask(serverLevel, start, beamEnd, player.getSoundSource()));

        if (closestEntity instanceof LivingEntity living) {
            final double currentScale = getSizeRatio(living);
            final double naturalScale = 1.0D;
            final ShrinkRayMode mode = getCurrentMode(stack);

            // Bell-curve scaling: change is largest at the entity's natural scale
            // and diminishes toward the min/max extremes using a Gaussian in log-space
            final double maxChange = 0.1D;
            final double logRatio = Math.log(currentScale / naturalScale) / Math.log(2.0D);
            final double sigma = 1.8D;
            final double bellFactor = Math.exp(-(logRatio * logRatio) / (2.0D * sigma * sigma));
            final double change = maxChange * bellFactor;

            final double targetScale = switch (mode) {
                case SHRINK -> Math.max(MIN_SCALE, currentScale * (1.0D - change));
                case ENLARGE -> Math.min(MAX_SCALE, currentScale * (1.0D + change));
                case RESTORE -> naturalScale;
            };
            SHRINK_TASKS.put(living, new ShrinkTask(currentScale, targetScale, naturalScale));
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }

        return InteractionResultHolder.pass(stack);
    }

    /** The size this ray gave the entity, as a ratio to its natural size: 1 when it was never resized. */
    public static double getSizeRatio(Entity entity) {
        return SIZE_SCALE_TYPES.get(0).getScaleData(entity).getBaseScale();
    }

    private static void setSizeRatio(Entity entity, float ratio) {
        for (ScaleType scaleType : SIZE_SCALE_TYPES) {
            setScale(scaleType, entity, ratio);
        }
    }

    private static void setScale(ScaleType scaleType, Entity entity, float scale) {
        ScaleData scaleData = scaleType.getScaleData(entity);
        scaleData.setPersistence(true);
        scaleData.setScale(scale);
    }

    private Vec3 getBeamStartPosition(Player player, Vec3 viewVector, InteractionHand hand) {
        double playerScale = getSizeRatio(player);
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

    /** Advances beams and scale animations once per server tick. */
    private static void tickTasks() {
        if (SHRINK_TASKS.isEmpty() && BEAM_TASKS.isEmpty()) {
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
            beam.level.playSound(
                    null,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE,
                    beam.soundSource,
                    0.5F,
                    1.8F + (float) progress * 0.2F);
            beam.level.sendParticles(
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
            setSizeRatio(target, (float) (currentScale / task.naturalScale));
            target.refreshDimensions();
            if (task.currentTick >= SHRINK_DURATION_TICKS) {
                finishResize(target, task);
                it.remove();
            }
        }
    }

    /** Applies the final attribute adjustments, or removes everything when the entity is back to its natural size. */
    private static void finishResize(LivingEntity target, ShrinkTask task) {
        final double ratio = task.targetScale / task.naturalScale;
        final boolean restored = Math.abs(ratio - 1.0D) < 1.0E-6D;
        final boolean wasResized = Math.abs(task.startScale / task.naturalScale - 1.0D) > 1.0E-6D;

        for (AttributeModifierInfo attrInfo : ATTRIBUTE_MODIFIER_INFO) {
            final AttributeInstance attrInstance = target.getAttribute(attrInfo.attribute());
            if (attrInstance == null) {
                continue;
            }
            attrInstance.removeModifier(attrInfo.modifierId());
            if (!restored) {
                attrInstance.addPermanentModifier(attrInfo.forRatio(ratio));
            }
        }
        for (ScaleInfo scaleInfo : SCALE_INFO) {
            setScale(scaleInfo.scaleType(), target, restored ? 1.0F : scaleInfo.forRatio(ratio));
        }
        if (restored) {
            setSizeRatio(target, 1.0F);
            // Only drop the effects the ray applied: an entity that was never resized keeps its own
            if (wasResized) {
                target.removeEffect(MobEffects.SATURATION);
                target.removeEffect(MobEffects.HUNGER);
            }
        }
        target.refreshDimensions();
    }
}
