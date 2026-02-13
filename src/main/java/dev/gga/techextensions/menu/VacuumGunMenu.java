package dev.gga.techextensions.menu;

import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.items.tool.advanced.VacuumGunItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import reborncore.api.blockentity.IUpgrade;

public class VacuumGunMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ItemStack gunStack;

    public VacuumGunMenu(int i, Inventory inventory) {
        super(TEContent.VACUUM_GUN_MENU, i);

        // Find the vacuum gun stack
        Player playerEntity = inventory.player;
        ItemStack mainHand = playerEntity.getMainHandItem();
        ItemStack offHand = playerEntity.getOffhandItem();
        if (mainHand.getItem() instanceof VacuumGunItem) {
            this.gunStack = mainHand;
        } else if (offHand.getItem() instanceof VacuumGunItem) {
            this.gunStack = offHand;
        } else {
            this.gunStack = ItemStack.EMPTY;
        }

        this.inventory = VacuumGunItem.getInventory(this.gunStack);

        // 5 Hopper-like inventory slots (row of 5, centered)
        for (int j = 0; j < VacuumGunItem.INVENTORY_SIZE; j++) {
            this.addSlot(new Slot(this.inventory, j, 44 + j * 18, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !(stack.getItem() instanceof VacuumGunItem);
                }
            });
        }

        // 2 Upgrade Slots (on the left side)
        for (int j = 0; j < VacuumGunItem.ALLOWED_UPGRADES; j++) {
            this.addSlot(new Slot(this.inventory, VacuumGunItem.INVENTORY_SIZE + j, -18, 12 + j * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof IUpgrade;
                }
            });
        }

        // Player Inventory
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(inventory, k + j * 9 + 9, 8 + k * 18, 84 + j * 18));
            }
        }

        // Hotbar
        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inventory, j, 8 + j * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        int gunSlots = VacuumGunItem.TOTAL_SLOTS; // 5 inventory + 2 upgrades = 7
        int totalSlots = gunSlots + 36; // + 36 player inventory/hotbar

        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            if (i < gunSlots) { // Gun Inventory -> Player Inventory
                if (!this.moveItemStackTo(itemStack2, gunSlots, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory -> Gun Inventory
                // Try Upgrades first
                if (itemStack2.getItem() instanceof IUpgrade) {
                    this.moveItemStackTo(
                            itemStack2,
                            VacuumGunItem.INVENTORY_SIZE,
                            VacuumGunItem.INVENTORY_SIZE + VacuumGunItem.ALLOWED_UPGRADES,
                            false);
                }

                // Try inventory slots
                if (!itemStack2.isEmpty()) {
                    if (!this.moveItemStackTo(itemStack2, 0, VacuumGunItem.INVENTORY_SIZE, false)) {
                        // Main <-> Hotbar
                        int playerInvStart = gunSlots;
                        int playerInvEnd = gunSlots + 27;
                        int hotbarStart = gunSlots + 27;
                        int hotbarEnd = totalSlots;

                        if (i < playerInvEnd) { // Main Inv -> Hotbar
                            if (!this.moveItemStackTo(itemStack2, hotbarStart, hotbarEnd, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else { // Hotbar -> Main Inv
                            if (!this.moveItemStackTo(itemStack2, playerInvStart, playerInvEnd, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == gunStack || player.getOffhandItem() == gunStack;
    }
}
