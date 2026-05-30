package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class CraftEngineReloadListener implements Listener {

    private final VethEnchantPlugin plugin;

    public CraftEngineReloadListener(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> this.plugin.customCropService().reload());
    }
}
