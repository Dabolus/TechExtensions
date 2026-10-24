package dev.gga.techextensions.client;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.items.tool.advanced.BubbleGunItem;
import dev.gga.techextensions.items.tool.advanced.SoapItem;
import dev.gga.techextensions.utils.TEFormatUtils;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class TETooltipHandler implements ItemTooltipCallback {
    // The motto is split into lines once per language change instead of on every frame
    private String cachedMotto;
    private List<Component> cachedMottoLines = List.of();

    public static void setup() {
        ItemTooltipCallback.EVENT.register(new TETooltipHandler());
    }

    @Override
    public void getTooltip(
            ItemStack stack, Item.TooltipContext tooltipContext, TooltipFlag tooltipType, List<Component> lines) {
        Item item = stack.getItem();

        // Can currently be executed by a ForkJoinPool.commonPool-worker when REI is in async search mode
        // We skip this method until a thread-safe solution is in place
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return;

        if (item == TEContent.META_TOOL) {
            lines.addAll(mottoLines());
        }

        if (item instanceof SoapItem) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            lines.add(Component.translatable("techextensions.tooltip.soap.durability", remaining, stack.getMaxDamage())
                    .withStyle(ChatFormatting.GRAY));
        }

        if (item instanceof BubbleGunItem) {
            // Show contained soap info (reads only the soap slot of the container component)
            ItemStack soapStack = BubbleGunItem.getSoap(stack);
            if (!soapStack.isEmpty() && soapStack.getItem() instanceof SoapItem) {
                int soapRemaining = soapStack.getMaxDamage() - soapStack.getDamageValue();
                lines.add(Component.translatable(
                                "techextensions.tooltip.bubble_gun.soap", soapRemaining, soapStack.getMaxDamage())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("techextensions.tooltip.bubble_gun.no_soap")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            // Show water level
            long waterAmount = BubbleGunItem.getWaterAmount(stack);
            long maxWater = TechExtensionsConfig.bubbleGunWaterCapacity.get();
            lines.add(Component.translatable(
                            "techextensions.tooltip.bubble_gun.water_level",
                            TEFormatUtils.formatNumber(waterAmount),
                            TEFormatUtils.formatNumber(maxWater))
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private List<Component> mottoLines() {
        String motto = I18n.get("techextensions.tooltip.meta_tool_motto");
        if (!motto.equals(cachedMotto)) {
            cachedMotto = motto;
            cachedMottoLines = Arrays.stream(motto.split("\\r?\\n"))
                    .<Component>map(mottoLine -> Component.literal(mottoLine).withStyle(ChatFormatting.YELLOW))
                    .toList();
        }
        return cachedMottoLines;
    }
}
