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
                TechExtensionsConfig.electricJetpackCharge.get(),
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
        if (!(playerEntity instanceof ServerPlayer serverPlayer) || playerEntity.isCreative()) {
            return;
        }
        long storedEnergy = getStoredEnergy(stack);
        long flyingCost = TechExtensionsConfig.electricJetpackFlyingCost.get();
        if (storedEnergy <= flyingCost) {
            return;
        }

        // Config values are read once per tick: every ConfigValue.get() takes a lock
        long sprintCost = TechExtensionsConfig.electricJetpackSprintCost.get();
        long hoverCost = TechExtensionsConfig.electricJetpackHoverCost.get();
        double verticalThrust = TechExtensionsConfig.electricJetpackVerticalThrust.get();
        double horizontalThrust = TechExtensionsConfig.electricJetpackHorizontalThrust.get();
        double maxVerticalSpeed = TechExtensionsConfig.electricJetpackMaxVerticalSpeed.get();
        double maxHorizontalSpeed = TechExtensionsConfig.electricJetpackMaxHorizontalSpeed.get();
        double horizontalDrag = TechExtensionsConfig.electricJetpackHorizontalDrag.get();
        double verticalDrag = TechExtensionsConfig.electricJetpackVerticalDrag.get();
        double hoverStrength = TechExtensionsConfig.electricJetpackHoverStrength.get();
        double sprintThrust = TechExtensionsConfig.electricJetpackSprintThrust.get();

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
            // Apply vertical thrust, capped
            vy = Math.min(vy + verticalThrust, maxVerticalSpeed);

            // Apply horizontal thrust based on movement input
            float forwardInput = serverPlayer.getLastClientInput().forward()
                    ? 1.0f
                    : (serverPlayer.getLastClientInput().backward() ? -1.0f : 0.0f);
            float strafeInput = serverPlayer.getLastClientInput().left()
                    ? 1.0f
                    : (serverPlayer.getLastClientInput().right() ? -1.0f : 0.0f);

            // Movement direction based on player's yaw
            double yawRad = Math.toRadians(playerEntity.getYRot());
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);

            if (forwardInput != 0 || strafeInput != 0) {
                // Forward/backward and strafe movement
                vx += (-sin * forwardInput + cos * strafeInput) * horizontalThrust;
                vz += (cos * forwardInput + sin * strafeInput) * horizontalThrust;
            }

            // Sprint boost - applies forward thrust when sprinting while flying
            if (serverPlayer.isSprinting() && storedEnergy > flyingCost + sprintCost) {
                // Apply sprint thrust in the direction the player is looking
                vx += -sin * sprintThrust;
                vz += cos * sprintThrust;
                isSprinting = true;
            }

            isThrusting = true;
        }
        // Hover mode when sneaking in the air
        else if (serverPlayer.getLastClientInput().shift() && !playerEntity.onGround()) {
            // Counteract gravity to hover, with a slow descent
            vy = Math.max(vy + hoverStrength, -0.1);
            isHovering = true;
        }

        if (!isThrusting && !isHovering) {
            return;
        }

        // Horizontal drag
        vx *= horizontalDrag;
        vz *= horizontalDrag;

        // Cap horizontal speed (allow higher speed when sprinting)
        double maxSpeed = isSprinting ? maxHorizontalSpeed * 1.5 : maxHorizontalSpeed;
        double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);
        if (horizontalSpeed > maxSpeed) {
            double scale = maxSpeed / horizontalSpeed;
            vx *= scale;
            vz *= scale;
        }

        // Apply vertical drag when ascending
        if (vy > 0) {
            vy *= verticalDrag;
        }

        // Set the new velocity and mark it as changed to sync with the client
        playerEntity.setDeltaMovement(vx, vy, vz);
        playerEntity.syncVelocity = true;

        // Reduce fall damage by resetting fall distance when jetpack is active
        if (!playerEntity.onGround()) {
            playerEntity.fallDistance = 0.0f;
        }

        // Consume energy
        long energyCost = isThrusting ? flyingCost + (isSprinting ? sprintCost : 0) : hoverCost;
        tryUseEnergy(stack, energyCost);
    }
}
