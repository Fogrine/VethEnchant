package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import net.momirealms.customcrops.api.event.CustomCropsReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class CustomCropsReloadListener implements Listener {

    private final VethEnchantPlugin plugin;

    public CustomCropsReloadListener(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCustomCropsReload(CustomCropsReloadEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> this.plugin.customCropService().reload());
    }
}
