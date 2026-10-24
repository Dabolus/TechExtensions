package dev.gga.techextensions.utils;

import java.util.Locale;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import reborncore.common.util.ItemUsageContextCustomStack;

public final class TEItemsUtils {
    private TEItemsUtils() {}

    /**
     * Places a torch from the player's inventory at the clicked position, trying the offhand first and then
     * the main inventory.
     */
    public static InteractionResult placeTorch(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        Inventory inventory = player.getInventory();
        if (tryPlaceTorch(context, player, inventory.getItem(Inventory.SLOT_OFFHAND), Inventory.SLOT_OFFHAND)) {
            return InteractionResult.SUCCESS;
        }
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (tryPlaceTorch(context, player, inventory.getItem(slot), slot)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    private static boolean isTorch(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem
                && stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT).contains("torch");
    }

    /**
     * @param torchStack    the candidate stack
     * @param inventorySlot the player inventory slot the stack lives in, cleared if the stack is used up
     */
    private static boolean tryPlaceTorch(UseOnContext context, Player player, ItemStack torchStack, int inventorySlot) {
        if (!isTorch(torchStack)) {
            return false;
        }
        int oldSize = torchStack.getCount();
        UseOnContext torchContext = new ItemUsageContextCustomStack(
                context.getLevel(),
                player,
                context.getHand(),
                torchStack,
                new BlockHitResult(
                        context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), true));
        InteractionResult result = torchStack.useOn(torchContext);
        if (player.isCreative()) {
            torchStack.setCount(oldSize);
        } else if (torchStack.isEmpty()) {
            // Clear the slot the torch came from, never the hand holding the tool
            player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
        }
        return result.consumesAction();
    }
}
