package dev.gga.techextensions.menu;

import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.items.tool.advanced.BubbleGunItem;
import dev.gga.techextensions.items.tool.advanced.SoapItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import reborncore.api.blockentity.IUpgrade;
import reborncore.common.fluid.container.ItemFluidInfo;

public class BubbleGunMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ItemStack gunStack;

    public BubbleGunMenu(int syncId, Inventory inventory) {
        super(TEContent.BUBBLE_GUN_MENU, syncId);

        // Find the bubble gun stack
        Player player = inventory.player;
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.getItem() instanceof BubbleGunItem) {
            this.gunStack = mainHand;
        } else if (offHand.getItem() instanceof BubbleGunItem) {
            this.gunStack = offHand;
        } else {
            this.gunStack = ItemStack.EMPTY;
        }

        this.inventory = BubbleGunItem.getInventory(this.gunStack);

        // Soap slot (index 0) — centered top area
        this.addSlot(new Slot(this.inventory, BubbleGunItem.SOAP_SLOT, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SoapItem;
            }
        });

        // Cell input slot (index 1) — left of center bottom area
        this.addSlot(new Slot(this.inventory, BubbleGunItem.CELL_INPUT_SLOT, 62, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Accept water cells or empty cells
                if (stack.getItem() instanceof ItemFluidInfo info) {
                    return info.getFluid(stack) == Fluids.WATER || info.getFluid(stack) == Fluids.EMPTY;
                }
                return false;
            }
        });

        // Cell output slot (index 2) — right of center bottom area
        this.addSlot(new Slot(this.inventory, BubbleGunItem.CELL_OUTPUT_SLOT, 98, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        // 2 Upgrade Slots (on the left side, matching Vacuum Gun layout)
        for (int j = 0; j < BubbleGunItem.ALLOWED_UPGRADES; j++) {
            this.addSlot(new Slot(this.inventory, BubbleGunItem.INVENTORY_SIZE + j, -18, 12 + j * 18) {
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
        int gunSlots = BubbleGunItem.TOTAL_SLOTS; // 3 functional + 2 upgrades = 5
        int totalSlots = gunSlots + 36; // + 36 player inventory/hotbar

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < gunSlots) {
                // Gun → Player
                if (!this.moveItemStackTo(slotStack, gunSlots, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player → Gun
                // Try upgrades first
                if (slotStack.getItem() instanceof IUpgrade) {
                    this.moveItemStackTo(
                            slotStack,
                            BubbleGunItem.INVENTORY_SIZE,
                            BubbleGunItem.INVENTORY_SIZE + BubbleGunItem.ALLOWED_UPGRADES,
                            false);
                }

                // Try soap slot
                if (!slotStack.isEmpty() && slotStack.getItem() instanceof SoapItem) {
                    this.moveItemStackTo(slotStack, BubbleGunItem.SOAP_SLOT, BubbleGunItem.SOAP_SLOT + 1, false);
                }

                // Try cell input slot
                if (!slotStack.isEmpty() && slotStack.getItem() instanceof ItemFluidInfo info) {
                    if (info.getFluid(slotStack) == Fluids.WATER || info.getFluid(slotStack) == Fluids.EMPTY) {
                        this.moveItemStackTo(
                                slotStack, BubbleGunItem.CELL_INPUT_SLOT, BubbleGunItem.CELL_INPUT_SLOT + 1, false);
                    }
                }

                // Main <-> Hotbar fallback
                if (!slotStack.isEmpty()) {
                    int playerInvStart = gunSlots;
                    int playerInvEnd = gunSlots + 27;
                    int hotbarStart = gunSlots + 27;
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
        return player.getMainHandItem() == gunStack || player.getOffhandItem() == gunStack;
    }
}
