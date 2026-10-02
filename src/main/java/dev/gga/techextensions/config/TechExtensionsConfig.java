package dev.gga.techextensions.config;

import dev.gga.techextensions.TechExtensions;
import net.minecraft.resources.Identifier;
import reborncore.common.config.Config;
import reborncore.common.config.ConfigGroup;
import reborncore.common.config.ConfigValue;
import reborncore.common.config.RebornCoreConfigApi;

/**
 * Tech Extensions configuration, built on top of the RebornCore config API.
 *
 * <p>Values are read with {@link ConfigValue#get()} so that the values loaded from the
 * {@code config/techextensions/*.cfg} files are respected. {@link #init()} must be called before
 * any content that captures a config value is constructed.
 */
public final class TechExtensionsConfig {
    private static final Config ITEMS =
            RebornCoreConfigApi.config(Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "items"));
    private static final Config MACHINES =
            RebornCoreConfigApi.config(Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "machines"));

    private static final ConfigGroup ITEMS_POWER = ITEMS.group("power");
    private static final ConfigGroup ITEMS_PHYSICS = ITEMS.group("physics");
    private static final ConfigGroup MACHINES_POWER = MACHINES.group("power");

    // items / power
    public static final ConfigValue<Integer> cyberShieldCharge =
            ITEMS_POWER.intValue("cyberShieldCharge", 2_000_000).comment("Energy Capacity for Cyber Shield (EU)");
    public static final ConfigValue<Long> cyberShieldStandardCostPerTick = ITEMS_POWER
            .longValue("cyberShieldStandardCostPerTick", 100)
            .comment("Energy Cost per tick while blocking with Cyber Shield in Standard Mode (EU/t)");
    public static final ConfigValue<Long> cyberShieldPermaParryCostPerTick = ITEMS_POWER
            .longValue("cyberShieldPermaParryCostPerTick", 500)
            .comment("Energy Cost per tick while blocking with Cyber Shield in Perma-Parry Mode (EU/t)");
    public static final ConfigValue<Integer> cyberShieldPerfectParryWindow = ITEMS_POWER
            .intValue("cyberShieldPerfectParryWindow", 5)
            .comment("Perfect parry window in ticks after the shield becomes active (default: 5, i.e. 0.25s)");
    public static final ConfigValue<Float> cyberShieldPerfectParryDamage = ITEMS_POWER
            .floatValue("cyberShieldPerfectParryDamage", 2.0F)
            .comment("Damage dealt to the attacker on a perfect parry (hearts)");
    public static final ConfigValue<Float> cyberShieldPerfectParryKnockback = ITEMS_POWER
            .floatValue("cyberShieldPerfectParryKnockback", 1.2F)
            .comment("Knockback strength applied to the attacker on a perfect parry");
    public static final ConfigValue<Long> cyberShieldPerfectParryCost = ITEMS_POWER
            .longValue("cyberShieldPerfectParryCost", 5000)
            .comment("Energy Cost for a successful perfect parry (EU)");
    public static final ConfigValue<Integer> metaToolCharge =
            ITEMS_POWER.intValue("metaToolCharge", 40_000_000).comment("Energy Capacity for Meta-Tool");
    public static final ConfigValue<Integer> metaToolCost =
            ITEMS_POWER.intValue("metaToolCost", 200).comment("Energy Cost for Meta-Tool");
    public static final ConfigValue<Integer> metaToolHitCost =
            ITEMS_POWER.intValue("metaToolHitCost", 250).comment("Hit Energy Cost for Meta-Tool");
    public static final ConfigValue<Integer> electricJetpackCharge =
            ITEMS_POWER.intValue("electricJetpackCharge", 600_000).comment("Energy Capacity for Electric Jetpack");
    public static final ConfigValue<Long> electricJetpackFlyingCost = ITEMS_POWER
            .longValue("electricJetpackFlyingCost", 150)
            .comment("Electric Jetpack Flying Cost per tick while thrusting");
    public static final ConfigValue<Long> electricJetpackHoverCost = ITEMS_POWER
            .longValue("electricJetpackHoverCost", 50)
            .comment("Electric Jetpack Energy Cost per tick while hovering");
    public static final ConfigValue<Long> electricJetpackSprintCost = ITEMS_POWER
            .longValue("electricJetpackSprintCost", 100)
            .comment("Electric Jetpack additional Energy Cost per tick while sprinting");
    public static final ConfigValue<Integer> resonanceScannerCharge =
            ITEMS_POWER.intValue("resonanceScannerCharge", 200_000).comment("Energy Capacity for Resonance Scanner");
    public static final ConfigValue<Long> resonanceScannerBaseCost =
            ITEMS_POWER.longValue("resonanceScannerBaseCost", 100).comment("Base Energy Cost for Resonance Scanner");
    public static final ConfigValue<Long> resonanceScannerPerItemCost = ITEMS_POWER
            .longValue("resonanceScannerPerItemCost", 10)
            .comment("Energy Cost increase per item stored in Resonance Scanner");
    public static final ConfigValue<Double> resonanceScannerBaseRange = ITEMS_POWER
            .doubleValue("resonanceScannerBaseRange", 6)
            .comment("Base Range for Resonance Scanner (blocks)");
    public static final ConfigValue<Double> resonanceScannerRangeMultiplier = ITEMS_POWER
            .doubleValue("resonanceScannerRangeMultiplier", 10)
            .comment(
                    "Range Multiplier per each item in the Resonance Scanner. The formula is round(baseRange + (rangeMultiplier * ln(itemCount)))");
    public static final ConfigValue<Integer> resonanceScannerScanCooldown = ITEMS_POWER
            .intValue("resonanceScannerScanCooldown", 80)
            .comment("Cooldown between scans for Resonance Scanner (ticks)");
    public static final ConfigValue<Integer> shrinkRayCharge =
            ITEMS_POWER.intValue("shrinkRayCharge", 1_000_000).comment("Energy Capacity for Shrink Ray");
    public static final ConfigValue<Integer> shrinkRayCost =
            ITEMS_POWER.intValue("shrinkRayCost", 200).comment("Energy Cost for Shrink Ray");
    public static final ConfigValue<Integer> bubbleGunCharge =
            ITEMS_POWER.intValue("bubbleGunCharge", 400_000).comment("Energy Capacity for Bubble Gun (EU)");
    public static final ConfigValue<Long> bubbleGunEnergyCostPerTick = ITEMS_POWER
            .longValue("bubbleGunEnergyCostPerTick", 10)
            .comment("Energy Cost per tick while shooting with Bubble Gun (EU/t)");
    public static final ConfigValue<Long> bubbleGunWaterCapacity = ITEMS_POWER
            .longValue("bubbleGunWaterCapacity", 16_000)
            .comment("Internal water tank capacity for Bubble Gun (mB)");
    public static final ConfigValue<Long> bubbleGunWaterCostPerTick = ITEMS_POWER
            .longValue("bubbleGunWaterCostPerTick", 5)
            .comment("Water consumed per tick while shooting with Bubble Gun (mB/t)");
    public static final ConfigValue<Double> bubbleGunRange =
            ITEMS_POWER.doubleValue("bubbleGunRange", 12).comment("Range of the Bubble Gun (blocks)");
    public static final ConfigValue<Integer> bubbleGunCleanDuration = ITEMS_POWER
            .intValue("bubbleGunCleanDuration", 20)
            .comment("Ticks of continuous targeting required to clean a block with Bubble Gun");
    public static final ConfigValue<Integer> bubbleGunTrapDuration = ITEMS_POWER
            .intValue("bubbleGunTrapDuration", 40)
            .comment("Ticks of continuous targeting required to trap a living entity in a bubble");
    public static final ConfigValue<Integer> bubbleGunBubbleFloatDuration = ITEMS_POWER
            .intValue("bubbleGunBubbleFloatDuration", 60)
            .comment("Ticks the trapped entity floats upward inside the bubble before it pops");
    public static final ConfigValue<Integer> vacuumGunCharge =
            ITEMS_POWER.intValue("vacuumGunCharge", 200_000).comment("Energy Capacity for Vacuum Gun (EU)");
    public static final ConfigValue<Long> vacuumGunCostPerAction = ITEMS_POWER
            .longValue("vacuumGunCostPerAction", 50)
            .comment("Energy Cost per vacuum/blow action for Vacuum Gun (EU)");
    public static final ConfigValue<Double> vacuumGunRange =
            ITEMS_POWER.doubleValue("vacuumGunRange", 12).comment("Range of the Vacuum Gun (blocks)");
    public static final ConfigValue<Integer> vacuumGunCooldown =
            ITEMS_POWER.intValue("vacuumGunCooldown", 8).comment("Cooldown between vacuum/blow actions (ticks)");

    // items / physics
    public static final ConfigValue<Double> electricJetpackVerticalThrust = ITEMS_PHYSICS
            .doubleValue("electricJetpackVerticalThrust", 0.12)
            .comment("Electric Jetpack Vertical Thrust (blocks/tick^2)");
    public static final ConfigValue<Double> electricJetpackHorizontalThrust = ITEMS_PHYSICS
            .doubleValue("electricJetpackHorizontalThrust", 0.04)
            .comment("Electric Jetpack Horizontal Thrust when moving (blocks/tick^2)");
    public static final ConfigValue<Double> electricJetpackMaxVerticalSpeed = ITEMS_PHYSICS
            .doubleValue("electricJetpackMaxVerticalSpeed", 0.5)
            .comment("Electric Jetpack Maximum Vertical Speed (blocks/tick)");
    public static final ConfigValue<Double> electricJetpackMaxHorizontalSpeed = ITEMS_PHYSICS
            .doubleValue("electricJetpackMaxHorizontalSpeed", 0.3)
            .comment("Electric Jetpack Maximum Horizontal Speed (blocks/tick)");
    public static final ConfigValue<Double> electricJetpackHorizontalDrag = ITEMS_PHYSICS
            .doubleValue("electricJetpackHorizontalDrag", 0.85)
            .comment("Electric Jetpack Horizontal Drag/Friction coefficient (0-1, lower = more friction)");
    public static final ConfigValue<Double> electricJetpackVerticalDrag = ITEMS_PHYSICS
            .doubleValue("electricJetpackVerticalDrag", 0.92)
            .comment("Electric Jetpack Vertical Drag when ascending (0-1, lower = more friction)");
    public static final ConfigValue<Double> electricJetpackHoverStrength = ITEMS_PHYSICS
            .doubleValue("electricJetpackHoverStrength", 0.08)
            .comment("Electric Jetpack Hover strength - counteracts gravity when sneaking (0-1)");
    public static final ConfigValue<Double> electricJetpackSprintThrust = ITEMS_PHYSICS
            .doubleValue("electricJetpackSprintThrust", 0.08)
            .comment("Electric Jetpack Sprint forward thrust multiplier");
    public static final ConfigValue<Double> bubbleGunBubbleFloatSpeed = ITEMS_PHYSICS
            .doubleValue("bubbleGunBubbleFloatSpeed", 0.15)
            .comment("Upward speed of the bubble while floating (blocks/tick)");

    // machines / power
    public static final ConfigValue<Integer> electricDuctedFanMaxEnergy = MACHINES_POWER
            .intValue("electricDuctedFanMaxEnergy", 32_768)
            .comment("Maximum energy storage for Electric Ducted Fan (EU)");
    public static final ConfigValue<Integer> electricDuctedFanMaxInput = MACHINES_POWER
            .intValue("electricDuctedFanMaxInput", 16_384)
            .comment("Maximum energy input rate for Electric Ducted Fan (EU/t)");
    public static final ConfigValue<Integer> electricDuctedFanMinEnergyCost = MACHINES_POWER
            .intValue("electricDuctedFanMinEnergyCost", 4)
            .comment("Minimum energy cost per operation for Electric Ducted Fan (EU)");
    public static final ConfigValue<Integer> electricDuctedFanMaxEnergyCost = MACHINES_POWER
            .intValue("electricDuctedFanMaxEnergyCost", 16_384)
            .comment("Maximum energy cost per operation for Electric Ducted Fan (EU)");

    private TechExtensionsConfig() {}

    /** Registers (loads and saves) all config files. Must run before content registration. */
    public static void init() {
        RebornCoreConfigApi.register(ITEMS);
        RebornCoreConfigApi.register(MACHINES);
    }
}
