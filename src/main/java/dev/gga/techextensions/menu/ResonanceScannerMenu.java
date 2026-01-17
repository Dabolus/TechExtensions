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
    private final Container inventory;
    private final ItemStack scannerStack;

    public ResonanceScannerMenu(int i, Inventory inventory) {
        super(TEContent.RESONANCE_SCANNER_MENU, i);

        // Find the scanner stack
        Player playerEntity = inventory.player;
        ItemStack mainHand = playerEntity.getMainHandItem();
        ItemStack offHand = playerEntity.getOffhandItem();
        if (mainHand.getItem() instanceof ResonanceScannerItem) {
            this.scannerStack = mainHand;
        } else if (offHand.getItem() instanceof ResonanceScannerItem) {
            this.scannerStack = offHand;
        } else {
            this.scannerStack = ItemStack.EMPTY;
        }

        this.inventory = ResonanceScannerItem.getInventory(this.scannerStack);

        // Scanner Slot
        this.addSlot(new Slot(this.inventory, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof ResonanceScannerItem);
            }
        });

        // Upgrade Slots
        for (int j = 0; j < 2; j++) {
            this.addSlot(new Slot(this.inventory, 1 + j, -18, 12 + j * 18) {
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
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            if (i < 3) { // Item Inventory (Scanner + Upgrades) -> Player Inventory
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory -> Item Inventory
                // Try Upgrades
                if (itemStack2.getItem() instanceof IUpgrade) {
                    this.moveItemStackTo(itemStack2, 1, 3, false); // Continue if failed
                }

                // Try Scanner
                if (!itemStack2.isEmpty() && this.slots.getFirst().mayPlace(itemStack2)) {
                    if (itemStack2.getCount() == itemStack.getCount()) {
                        this.moveItemStackTo(itemStack2, 0, 1, false); // Continue if failed
                    }
                }

                if (!itemStack2.isEmpty()) {
                    // Main <-> Hotbar
                    if (i < 30) { // Main Inv
                        if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (i < 39) { // Hotbar
                        if (!this.moveItemStackTo(itemStack2, 3, 30, false)) {
                            return ItemStack.EMPTY;
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
        return player.getMainHandItem() == scannerStack || player.getOffhandItem() == scannerStack;
    }
}
