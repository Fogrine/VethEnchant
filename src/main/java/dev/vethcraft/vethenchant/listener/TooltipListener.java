package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.tooltip.EnchantTooltipService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public final class TooltipListener implements Listener {

    private final VethEnchantPlugin plugin;
    private final EnchantTooltipService tooltipService;

    public TooltipListener(VethEnchantPlugin plugin, EnchantTooltipService tooltipService) {
        this.plugin = plugin;
        this.tooltipService = tooltipService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        normalizeSoon(event.getPlayer());
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        this.tooltipService.normalizeInventory(event.getInventory());
        if (event.getPlayer() instanceof Player player) {
            this.tooltipService.normalizeInventory(player.getInventory());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null) {
            this.tooltipService.normalize(event.getCurrentItem());
        }
        if (event.getCursor() != null) {
            this.tooltipService.normalize(event.getCursor());
        }
        if (event.getWhoClicked() instanceof Player player) {
            normalizeSoon(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            normalizeSoon(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        normalizeSoon(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        this.tooltipService.normalize(event.getMainHandItem());
        this.tooltipService.normalize(event.getOffHandItem());
        normalizeSoon(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        normalizeSoon(event.getEnchanter());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (this.tooltipService.normalize(stack)) {
            event.getItem().setItemStack(stack);
        }
        normalizeSoon(player);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }
        ItemStack clone = result.clone();
        if (this.tooltipService.normalize(clone)) {
            event.setResult(clone);
        }
    }

    private void normalizeSoon(Player player) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.tooltipService.normalizeInventory(player.getInventory()));
    }
}
