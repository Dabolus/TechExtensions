package dev.gga.techextensions.items.armor;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEArmorMaterials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;
import reborncore.api.items.ArmorBlockEntityTicker;
import reborncore.common.powerSystem.RcEnergyTier;

public class ElectricJetpackItem extends TEEnergyArmourItem implements ArmorBlockEntityTicker {

    public ElectricJetpackItem(String name) {
        super(
                TEArmorMaterials.ELECTRIC_JETPACK,
                ArmorType.CHESTPLATE,
                TechExtensionsConfig.electricJetpackCharge,
                RcEnergyTier.HIGH,
                name);
    }

    // TREnergyArmourItem
    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    // ArmorBlockEntityTicker
    @Override
    public void tickArmor(ItemStack stack, boolean hasFullSuit, Player playerEntity) {
        if (playerEntity instanceof ServerPlayer serverPlayer && !playerEntity.isCreative()) {
            long storedEnergy = getStoredEnergy(stack);

            if (storedEnergy > TechExtensionsConfig.electricJetpackFlyingCost) {
                Vec3 currentVelocity = playerEntity.getDeltaMovement();
                double vx = currentVelocity.x;
                double vy = currentVelocity.y;
                double vz = currentVelocity.z;

                boolean isThrusting = false;
                boolean isHovering = false;
                boolean isSprinting = false;

                // Check if player is pressing jump (space) to thrust upward
                // Allow takeoff from ground OR continued thrust in air
                if (serverPlayer.getLastClientInput().jump()) {
                    // Apply vertical thrust
                    vy += TechExtensionsConfig.electricJetpackVerticalThrust;

                    // Cap vertical speed
                    if (vy > TechExtensionsConfig.electricJetpackMaxVerticalSpeed) {
                        vy = TechExtensionsConfig.electricJetpackMaxVerticalSpeed;
                    }

                    // Apply horizontal thrust based on movement input
                    float forwardInput = serverPlayer.getLastClientInput().forward()
                            ? 1.0f
                            : (serverPlayer.getLastClientInput().backward() ? -1.0f : 0.0f);
                    float strafeInput = serverPlayer.getLastClientInput().left()
                            ? 1.0f
                            : (serverPlayer.getLastClientInput().right() ? -1.0f : 0.0f);

                    if (forwardInput != 0 || strafeInput != 0) {
                        // Calculate movement direction based on player's yaw
                        double yawRad = Math.toRadians(playerEntity.getYRot());
                        double sin = Math.sin(yawRad);
                        double cos = Math.cos(yawRad);

                        // Forward/backward movement
                        vx += (-sin * forwardInput + cos * strafeInput)
                                * TechExtensionsConfig.electricJetpackHorizontalThrust;
                        vz += (cos * forwardInput + sin * strafeInput)
                                * TechExtensionsConfig.electricJetpackHorizontalThrust;
                    }

                    // Sprint boost - applies forward thrust when sprinting while flying
                    if (serverPlayer.isSprinting()
                            && storedEnergy
                                    > TechExtensionsConfig.electricJetpackFlyingCost
                                            + TechExtensionsConfig.electricJetpackSprintCost) {
                        double yawRad = Math.toRadians(playerEntity.getYRot());
                        double sin = Math.sin(yawRad);
                        double cos = Math.cos(yawRad);

                        // Apply sprint thrust in the direction the player is looking
                        vx += -sin * TechExtensionsConfig.electricJetpackSprintThrust;
                        vz += cos * TechExtensionsConfig.electricJetpackSprintThrust;

                        isSprinting = true;
                    }

                    isThrusting = true;
                }
                // Hover mode when sneaking in the air
                else if (serverPlayer.getLastClientInput().shift() && !playerEntity.onGround()) {
                    // Counteract gravity to hover
                    vy += TechExtensionsConfig.electricJetpackHoverStrength;

                    // Slow descent while hovering
                    if (vy < -0.1) {
                        vy = -0.1;
                    }

                    isHovering = true;
                }

                // Apply physics when jetpack is active
                if (isThrusting || isHovering) {
                    // Horizontal drag
                    vx *= TechExtensionsConfig.electricJetpackHorizontalDrag;
                    vz *= TechExtensionsConfig.electricJetpackHorizontalDrag;

                    // Cap horizontal speed (allow higher speed when sprinting)
                    double maxHorizontalSpeed = isSprinting
                            ? TechExtensionsConfig.electricJetpackMaxHorizontalSpeed * 1.5
                            : TechExtensionsConfig.electricJetpackMaxHorizontalSpeed;
                    double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);
                    if (horizontalSpeed > maxHorizontalSpeed) {
                        double scale = maxHorizontalSpeed / horizontalSpeed;
                        vx *= scale;
                        vz *= scale;
                    }

                    // Apply vertical drag when ascending
                    if (vy > 0) {
                        vy *= TechExtensionsConfig.electricJetpackVerticalDrag;
                    }

                    // Set the new velocity
                    playerEntity.setDeltaMovement(vx, vy, vz);
                    // IMPORTANT: Mark velocity as changed to sync with client
                    playerEntity.hurtMarked = true;

                    // Reduce fall damage by resetting fall distance when jetpack is active
                    if (!playerEntity.onGround()) {
                        playerEntity.fallDistance = 0.0f;
                    }

                    // Consume energy
                    long energyCost = 0;
                    if (isThrusting) {
                        energyCost = TechExtensionsConfig.electricJetpackFlyingCost;
                        if (isSprinting) {
                            energyCost += TechExtensionsConfig.electricJetpackSprintCost;
                        }
                    } else if (isHovering) {
                        energyCost = TechExtensionsConfig.electricJetpackHoverCost;
                    }
                    tryUseEnergy(stack, energyCost);
                }
            }
        }
    }
}
