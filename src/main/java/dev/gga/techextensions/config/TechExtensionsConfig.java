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
}
