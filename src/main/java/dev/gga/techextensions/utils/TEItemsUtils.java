package dev.gga.techextensions.utils;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import reborncore.common.util.ItemUsageContextCustomStack;

import java.util.Locale;

public final class TEItemsUtils {
    public static InteractionResult placeTorch(UseOnContext itemUsageContext) {
        Player player = itemUsageContext.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        // TODO: optimize next blocks to avoid code duplication
        // Try with offhand first
        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.isEmpty() && offhandStack.getItem().getDescriptionId().toLowerCase(Locale.ROOT).contains("torch")) {
            if (offhandStack.getItem() instanceof BlockItem) {
                int oldSize = offhandStack.getCount();
                UseOnContext context = new ItemUsageContextCustomStack(itemUsageContext.getLevel(), player, itemUsageContext.getHand(), offhandStack, new BlockHitResult(itemUsageContext.getClickLocation(), itemUsageContext.getClickedFace(), itemUsageContext.getClickedPos(), true));
                InteractionResult result = offhandStack.useOn(context);
                if (player.isCreative()) {
                    offhandStack.setCount(oldSize);
                } else if (offhandStack.getCount() <= 0) {
                    player.setItemInHand(itemUsageContext.getHand(), ItemStack.EMPTY);
                }
                if (result == InteractionResult.SUCCESS) {
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // Then try main inventory
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack torchStack = player.getInventory().getItem(i);
            if (torchStack.isEmpty() || !torchStack.getItem().getDescriptionId().toLowerCase(Locale.ROOT).contains("torch")) {
                continue;
            }
            if (!(torchStack.getItem() instanceof BlockItem)) {
                continue;
            }

            int oldSize = torchStack.getCount();
            UseOnContext context = new ItemUsageContextCustomStack(itemUsageContext.getLevel(), player, itemUsageContext.getHand(), torchStack, new BlockHitResult(itemUsageContext.getClickLocation(), itemUsageContext.getClickedFace(), itemUsageContext.getClickedPos(), true));
            InteractionResult result = torchStack.useOn(context);
            if (player.isCreative()) {
                torchStack.setCount(oldSize);
            } else if (torchStack.getCount() <= 0) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
            if (result == InteractionResult.SUCCESS) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }
}
