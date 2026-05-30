package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import dev.vethcraft.vethenchant.util.CustomCropData;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import net.momirealms.customcrops.api.core.block.BreakReason;
import net.momirealms.customcrops.api.event.CropBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public final class CustomCropListener implements Listener {

    private final VethEnchantPlugin plugin;
    private final EffectDispatcher dispatcher;

    public CustomCropListener(VethEnchantPlugin plugin, EffectDispatcher dispatcher) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(CropBreakEvent event) {
        if (this.plugin.customCropService().isFarmDispatchSuppressed()) {
            return;
        }
        if (event.reason() != BreakReason.BREAK) {
            return;
        }
        if (!(event.entityBreaker() instanceof Player player)) {
            return;
        }

        Block block = event.location().getBlock();
        CustomCropData crop = this.plugin.customCropService().cropById(event.cropConfig().id()).orElse(null);
        if (crop == null || !crop.isMatureStage(event.cropStageItemID())) {
            return;
        }
        if (!this.plugin.protectionService().canModifyBlock(player, block)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        int level = ItemEnchantments.highestRegisteredLevel(item, this.dispatcher);
        this.dispatcher.farm(FarmContext.custom(event, player, item, block, crop, level));
    }
}
