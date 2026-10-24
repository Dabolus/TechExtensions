package dev.gga.techextensions.menu;

import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import reborncore.api.blockentity.IUpgrade;

public class ResonanceScannerMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOTS = 36;

    private final Container inventory;
    private final ItemStack scannerStack;

    public ResonanceScannerMenu(int syncId, Inventory inventory) {
        super(TEContent.RESONANCE_SCANNER_MENU, syncId);

        // Find the scanner stack
        Player player = inventory.player;
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.getItem() instanceof ResonanceScannerItem) {
            this.scannerStack = mainHand;
        } else if (offHand.getItem() instanceof ResonanceScannerItem) {
            this.scannerStack = offHand;
        } else {
            this.scannerStack = ItemStack.EMPTY;
        }

        this.inventory = ResonanceScannerItem.getInventory(this.scannerStack);

        // Target slot
        this.addSlot(new Slot(this.inventory, ResonanceScannerItem.TARGET_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof ResonanceScannerItem);
            }
        });

        // Upgrade Slots
        for (int j = 0; j < ResonanceScannerItem.ALLOWED_UPGRADES; j++) {
            this.addSlot(new Slot(this.inventory, ResonanceScannerItem.INVENTORY_SIZE + j, -18, 12 + j * 18) {
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
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int itemSlots = ResonanceScannerItem.TOTAL_SLOTS;
        int totalSlots = itemSlots + PLAYER_SLOTS;

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < itemSlots) {
                // Scanner → Player
                if (!this.moveItemStackTo(slotStack, itemSlots, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player → Scanner: upgrades first
                if (slotStack.getItem() instanceof IUpgrade) {
                    this.moveItemStackTo(
                            slotStack, ResonanceScannerItem.INVENTORY_SIZE, ResonanceScannerItem.TOTAL_SLOTS, false);
                }

                // Then the target slot, unless part of the stack already went into the upgrades
                if (!slotStack.isEmpty()
                        && slotStack.getCount() == result.getCount()
                        && this.slots.get(ResonanceScannerItem.TARGET_SLOT).mayPlace(slotStack)) {
                    this.moveItemStackTo(
                            slotStack, ResonanceScannerItem.TARGET_SLOT, ResonanceScannerItem.TARGET_SLOT + 1, false);
                }

                // Main <-> Hotbar fallback
                if (!slotStack.isEmpty()) {
                    int playerInvStart = itemSlots;
                    int playerInvEnd = itemSlots + 27;
                    int hotbarStart = playerInvEnd;
                    int hotbarEnd = totalSlots;

                    if (index < playerInvEnd) {
                        if (!this.moveItemStackTo(slotStack, hotbarStart, hotbarEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!this.moveItemStackTo(slotStack, playerInvStart, playerInvEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == scannerStack || player.getOffhandItem() == scannerStack;
    }
}
