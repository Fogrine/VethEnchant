package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.enchanting.EnchantingPreviewService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class EnchantingPreviewListener implements Listener {

    private final EnchantingPreviewService previewService;

    public EnchantingPreviewListener(EnchantingPreviewService previewService) {
        this.previewService = previewService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        this.previewService.update(event.getEnchanter(), event.getOffers());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            this.previewService.clear(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.previewService.clear(event.getPlayer());
    }
}
