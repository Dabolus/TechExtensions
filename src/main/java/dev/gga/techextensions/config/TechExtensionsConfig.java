package dev.gga.techextensions.config;

import reborncore.common.config.Config;

public class TechExtensionsConfig {
    @Config(config = "items", category = "power", key = "metaToolCharge", comment = "Energy Capacity for Meta-Tool")
    public static int metaToolCharge = 40_000_000;

    @Config(config = "items", category = "power", key = "metaToolCost", comment = "Energy Cost for Meta-Tool")
    public static int metaToolCost = 200;

    @Config(config = "items", category = "power", key = "metaToolHitCost", comment = "Hit Energy Cost for Meta-Tool")
    public static int metaToolHitCost = 250;

    @Config(
            config = "items",
            category = "power",
            key = "electricJetpackCharge",
            comment = "Energy Capacity for Electric Jetpack")
    public static int electricJetpackCharge = 600_000;

    @Config(
            config = "items",
            category = "power",
            key = "electricJetpackFlyingCost",
            comment = "Electric Jetpack Flying Cost per tick while thrusting")
    public static long electricJetpackFlyingCost = 150;

    @Config(
            config = "items",
            category = "power",
            key = "electricJetpackHoverCost",
            comment = "Electric Jetpack Energy Cost per tick while hovering")
    public static long electricJetpackHoverCost = 50;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackVerticalThrust",
            comment = "Electric Jetpack Vertical Thrust (blocks/tick^2)")
    public static double electricJetpackVerticalThrust = 0.12;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackHorizontalThrust",
            comment = "Electric Jetpack Horizontal Thrust when moving (blocks/tick^2)")
    public static double electricJetpackHorizontalThrust = 0.04;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackMaxVerticalSpeed",
            comment = "Electric Jetpack Maximum Vertical Speed (blocks/tick)")
    public static double electricJetpackMaxVerticalSpeed = 0.5;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackMaxHorizontalSpeed",
            comment = "Electric Jetpack Maximum Horizontal Speed (blocks/tick)")
    public static double electricJetpackMaxHorizontalSpeed = 0.3;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackHorizontalDrag",
            comment = "Electric Jetpack Horizontal Drag/Friction coefficient (0-1, lower = more friction)")
    public static double electricJetpackHorizontalDrag = 0.85;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackVerticalDrag",
            comment = "Electric Jetpack Vertical Drag when ascending (0-1, lower = more friction)")
    public static double electricJetpackVerticalDrag = 0.92;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackHoverStrength",
            comment = "Electric Jetpack Hover strength - counteracts gravity when sneaking (0-1)")
    public static double electricJetpackHoverStrength = 0.08;

    @Config(
            config = "items",
            category = "physics",
            key = "electricJetpackSprintThrust",
            comment = "Electric Jetpack Sprint forward thrust multiplier")
    public static double electricJetpackSprintThrust = 0.08;

    @Config(
            config = "items",
            category = "power",
            key = "electricJetpackSprintCost",
            comment = "Electric Jetpack additional Energy Cost per tick while sprinting")
    public static long electricJetpackSprintCost = 100;

    @Config(
            config = "items",
            category = "power",
            key = "resonanceScannerCharge",
            comment = "Energy Capacity for Resonance Scanner")
    public static int resonanceScannerCharge = 200_000;

    @Config(
            config = "items",
            category = "power",
            key = "resonanceScannerBaseCost",
            comment = "Base Energy Cost for Resonance Scanner")
    public static long resonanceScannerBaseCost = 100;

    @Config(
            config = "items",
            category = "power",
            key = "resonanceScannerPerItemCost",
            comment = "Energy Cost increase per item stored in Resonance Scanner")
    public static long resonanceScannerPerItemCost = 10;

    @Config(
            config = "items",
            category = "power",
            key = "resonanceScannerBaseRange",
            comment = "Base Range for Resonance Scanner (blocks)")
    public static double resonanceScannerBaseRange = 6;

    @Config(
            config = "items",
            category = "power",
            key = "resonanceScannerRangeMultiplier",
            comment =
                    "Range Multiplier per each item in the Resonance Scanner. The formula is round(baseRange + (rangeMultiplier * ln(itemCount)))")
    public static double resonanceScannerRangeMultiplier = 10;

    @Config(
            config = "items",
            category = "power",
            key = "resonanceScannerScanCooldown",
            comment = "Cooldown between scans for Resonance Scanner (ticks)")
    public static int resonanceScannerScanCooldown = 80;
}
