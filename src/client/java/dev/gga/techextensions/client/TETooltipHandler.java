package dev.gga.techextensions.client;

import dev.gga.techextensions.init.TEContent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

public class TETooltipHandler implements ItemTooltipCallback {

    public static void setup() {
        ItemTooltipCallback.EVENT.register(new TETooltipHandler());
    }

    @Override
    public void getTooltip(ItemStack stack, Item.TooltipContext tooltipContext, TooltipFlag tooltipType, List<Component> lines) {
        Item item = stack.getItem();

        // Can currently be executed by a ForkJoinPool.commonPool-worker when REI is in async search mode
        // We skip this method until a thread-safe solution is in place
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread())
            return;

        if (item == TEContent.META_TOOL) {
            String motto = I18n.get("techextensions.tooltip.meta_tool_motto");
            List<MutableComponent> mottoLines = Arrays.stream(motto.split("\\r?\\n"))
                    .map(mottoLine -> Component.literal(ChatFormatting.YELLOW + mottoLine)).toList();
            lines.addAll(mottoLines);
        }
    }
}
