package dev.gga.techextensions.config;

import reborncore.common.config.Config;

public class TechExtensionsConfig {
    @Config(config = "items", category = "power", key = "metaToolCharge", comment = "Energy Capacity for Meta-Tool")
    public static int metaToolCharge = 40_000_000;

    @Config(config = "items", category = "power", key = "metaToolCost", comment = "Energy Cost for Meta-Tool")
    public static int metaToolCost = 200;

    @Config(config = "items", category = "power", key = "metaToolHitCost", comment = "Hit Energy Cost for Meta-Tool")
    public static int metaToolHitCost = 250;

    @Config(config = "items", category = "power", key = "electricJetpackCharge", comment = "Energy Capacity for Electric Jetpack")
    public static int electricJetpackCharge = 600_000;

    @Config(config = "items", category = "power", key = "electricJetpackFlyingCost", comment = "Electric Jetpack Flying Cost")
    public static long electricJetpackFlyingCost = 150;

    @Config(config = "items", category = "power", key = "resonanceScannerCharge", comment = "Energy Capacity for Resonance Scanner")
    public static int resonanceScannerCharge = 200_000;

    @Config(config = "items", category = "power", key = "resonanceScannerBaseCost", comment = "Base Energy Cost for Resonance Scanner")
    public static long resonanceScannerBaseCost = 100;

    @Config(config = "items", category = "power", key = "resonanceScannerPerItemCost", comment = "Energy Cost increase per item stored in Resonance Scanner")
    public static long resonanceScannerPerItemCost = 10;

    @Config(config = "items", category = "power", key = "resonanceScannerBaseRange", comment = "Base Range for Resonance Scanner (blocks)")
    public static double resonanceScannerBaseRange = 6;

    @Config(config = "items", category = "power", key = "resonanceScannerRangeMultiplier", comment = "Range Multiplier per each item in the Resonance Scanner. The formula is round(baseRange + (rangeMultiplier * ln(itemCount)))")
    public static double resonanceScannerRangeMultiplier = 10;

    @Config(config = "items", category = "power", key = "resonanceScannerScanCooldown", comment = "Cooldown between scans for Resonance Scanner (ticks)")
    public static int resonanceScannerScanCooldown = 80;
}
