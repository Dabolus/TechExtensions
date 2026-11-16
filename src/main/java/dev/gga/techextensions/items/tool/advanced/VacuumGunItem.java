package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.items.TEEnergyItem;
import dev.gga.techextensions.menu.VacuumGunMenu;
import dev.gga.techextensions.utils.TECacheUtils;
import dev.gga.techextensions.utils.TEContainerUtils;
import dev.gga.techextensions.utils.TENbtUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import reborncore.api.blockentity.IUpgrade;
import reborncore.api.blockentity.IUpgradeable;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRContent;
import techreborn.items.UpgradeItem;

public class VacuumGunItem extends Item implements TEEnergyItem, IUpgradeable {
    public enum VacuumGunMode {
        VACUUM,
        BLOW,
        INSPECT
    }

    public static final int INVENTORY_SIZE = 5;
    public static final int ALLOWED_UPGRADES = 2;
    public static final int TOTAL_SLOTS = INVENTORY_SIZE + ALLOWED_UPGRADES;

    public final RcEnergyTier tier = RcEnergyTier.HIGH;

    public VacuumGunItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    // Mode handling

    public static VacuumGunMode getCurrentMode(ItemStack stack) {
        Integer mode = TEDataComponentTypes.getToolMode(stack);
        if (mode == null) {
            return VacuumGunMode.VACUUM;
        }
        VacuumGunMode[] modes = VacuumGunMode.values();
        return mode >= 0 && mode < modes.length ? modes[mode] : VacuumGunMode.VACUUM;
    }

    private void switchMode(ItemStack stack, Player entity) {
        VacuumGunMode[] modes = VacuumGunMode.values();
        int nextMode = (getCurrentMode(stack).ordinal() + 1) % modes.length;
        TEDataComponentTypes.setToolMode(stack, nextMode);
        if (entity instanceof ServerPlayer serverPlayer) {
            String modeTranslationKey = switch (modes[nextMode]) {
                case VACUUM -> "techextensions.message.vacuum_gun.mode_vacuum";
                case BLOW -> "techextensions.message.vacuum_gun.mode_blow";
                case INSPECT -> "techextensions.message.vacuum_gun.mode_inspect";
            };
            serverPlayer.displayClientMessage(
                    Component.translatable("techextensions.message.set_to")
                            .withStyle(ChatFormatting.GRAY)
                            .append(" ")
                            .append(Component.translatable(modeTranslationKey).withStyle(ChatFormatting.GOLD)),
                    true);
        }
    }

    // Item overrides

    @Override
    public boolean allowNbtUpdateAnimation(
            Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public long getStoredEnergy(ItemStack stack) {
        return Math.min(TEEnergyItem.super.getStoredEnergy(stack), getEnergyCapacity(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemUtils.getPowerForDurabilityBar(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemUtils.getColorForDurabilityBar(stack);
    }

    // RcEnergyItem

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return getEnergyCapacityFromCache(stack);
    }

    @Override
    public RcEnergyTier getTier() {
        return tier;
    }

    // Interaction

    @Override
    public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResultHolder.consume(stack);
        }

        if (world.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }

        ServerLevel serverLevel = (ServerLevel) world;

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        long cost = TechExtensionsConfig.vacuumGunCostPerAction;
        if (getStoredEnergy(stack) < cost) {
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResultHolder.fail(stack);
        }

        VacuumGunMode mode = getCurrentMode(stack);
        boolean didAction = switch (mode) {
            case VACUUM -> VacuumGunActions.performVacuum(serverLevel, player, stack);
            case BLOW -> VacuumGunActions.performBlow(serverLevel, player, stack);
            case INSPECT -> {
                player.openMenu(new SimpleMenuProvider(
                        (syncId, inventory, _p) -> new VacuumGunMenu(syncId, inventory), stack.getHoverName()));
                yield true;
            }
        };

        if (didAction) {
            tryUseEnergy(stack, cost);
            player.getCooldowns().addCooldown(this, getCooldown(stack));
        }

        return InteractionResultHolder.consume(stack);
    }

    // IUpgradeable

    @Override
    public boolean canBeUpgraded() {
        return true;
    }

    @Override
    public Container getUpgradeInventory() {
        return null; // Cannot be implemented for Item
    }

    @Override
    public int getUpgradeSlotCount() {
        return ALLOWED_UPGRADES;
    }

    @Override
    public boolean isUpgradeValid(IUpgrade upgrade, ItemStack stack) {
        return stack.is(TRContent.Upgrades.OVERCLOCKER.item) || stack.is(TRContent.Upgrades.ENERGY_STORAGE.item);
    }

    // Inventory management

    /**
     * Returns a 7-slot container (5 inventory + 2 upgrades) backed by the
     * stack's NBT.
     */
    public static Container getInventory(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                TEContainerUtils.save(stack, this.items);
                updateCache(stack, this);
            }
        };

        if (stack == null) {
            return container;
        }

        if (TEContainerUtils.hasItems(stack)) {
            TEContainerUtils.load(stack, container.items);
        }

        return container;
    }

    /**
     * Tries to insert a stack into the first `INVENTORY_SIZE` slots.
     *
     * @return the leftover items that did not fit
     */
    public static ItemStack insertIntoInventory(Container inventory, ItemStack stack) {
        ItemStack remaining = stack.copy();
        // First pass: merge into existing matching stacks, saving the container once at the end
        boolean merged = false;
        for (int i = 0; i < INVENTORY_SIZE && !remaining.isEmpty(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, remaining)) {
                int canFit = slot.getMaxStackSize() - slot.getCount();
                int toAdd = Math.min(remaining.getCount(), canFit);
                if (toAdd > 0) {
                    slot.grow(toAdd);
                    remaining.shrink(toAdd);
                    merged = true;
                }
            }
        }
        if (merged) {
            inventory.setChanged();
        }
        // Second pass: place into empty slots
        for (int i = 0; i < INVENTORY_SIZE && !remaining.isEmpty(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, remaining.copy());
                remaining.setCount(0);
            }
        }
        return remaining;
    }

    // Cached upgrade values

    private static void updateCache(ItemStack stack, Container inventory) {
        int overclockers = 0;
        int energyStorage = 0;

        for (int i = INVENTORY_SIZE; i < TOTAL_SLOTS; i++) {
            ItemStack s = inventory.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof UpgradeItem) {
                if (s.is(TRContent.Upgrades.OVERCLOCKER.item)) overclockers += s.getCount();
                if (s.is(TRContent.Upgrades.ENERGY_STORAGE.item)) energyStorage += s.getCount();
            }
        }

        final int cooldown = computeCooldown(overclockers);
        final long energyCapacity =
                TechExtensionsConfig.vacuumGunCharge + (long) (energyStorage * TechRebornConfig.energyStoragePower);

        TENbtUtils.update(stack, tag -> {
            tag.putInt("cache:cooldown", cooldown);
            tag.putLong("cache:energy_capacity", energyCapacity);
        });
    }

    public static int getCooldown(ItemStack stack) {
        return TECacheUtils.getCachedValue(
                stack,
                "cache:cooldown",
                TechExtensionsConfig.vacuumGunCooldown,
                tag -> tag.getInt("cache:cooldown"),
                VacuumGunItem::getInventory,
                VacuumGunItem::updateCache);
    }

    private static long getEnergyCapacityFromCache(ItemStack stack) {
        return TECacheUtils.getCachedValue(
                stack,
                "cache:energy_capacity",
                (long) TechExtensionsConfig.vacuumGunCharge,
                tag -> tag.getLong("cache:energy_capacity"),
                VacuumGunItem::getInventory,
                VacuumGunItem::updateCache);
    }

    public static int computeCooldown(int overclockerUpgrades) {
        double speedMultiplier = TechRebornConfig.overclockerSpeed * overclockerUpgrades;
        double cooldown = TechExtensionsConfig.vacuumGunCooldown * (1.0 - speedMultiplier);
        return (int) Math.round(cooldown);
    }
}
