package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.limit.EnchantLimitService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantLimitListener implements Listener {

    private static final long WARNING_COOLDOWN_MS = 1500L;

    private final VethEnchantPlugin plugin;
    private final EnchantLimitService limitService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Long> lastWarningAt = new ConcurrentHashMap<>();

    public EnchantLimitListener(VethEnchantPlugin plugin, EnchantLimitService limitService) {
        this.plugin = plugin;
        this.limitService = limitService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!this.limitService.canLimit(event.getEnchanter())) {
            return;
        }
        this.limitService.trimEnchantingTable(event.getEnchantsToAdd());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLoot(LootGenerateEvent event) {
        for (ItemStack item : event.getLoot()) {
            this.limitService.trimLootItem(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) {
            return;
        }
        ItemStack stack = item.getItemStack();
        if (this.limitService.trimFishingItem(stack)) {
            item.setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || !(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        result = normalizeAnvilRepairCost(event, result);
        if (!this.limitService.wouldExceedAnvil(player, result)) {
            return;
        }
        EnchantLimitService.SlotSummary summary = this.limitService.summary(result);
        event.setResult(null);
        long now = System.currentTimeMillis();
        long lastWarning = this.lastWarningAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastWarning < WARNING_COOLDOWN_MS) {
            return;
        }
        this.lastWarningAt.put(player.getUniqueId(), now);
        player.sendMessage(this.miniMessage.deserialize(
            this.plugin.messages().limitDeny(),
            this.plugin.paletteResolver(),
            Placeholder.parsed("used", Integer.toString(summary.used())),
            Placeholder.parsed("max", Integer.toString(summary.max()))
        ));
    }

    private ItemStack normalizeAnvilRepairCost(PrepareAnvilEvent event, ItemStack result) {
        var anvil = this.plugin.vethConfig().limits().sourceLimits().anvil();
        if (!anvil.removePriorWorkPenalty()) {
            return result;
        }
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }
        ItemStack clone = result.clone();
        ItemMeta cloneMeta = clone.getItemMeta();
        if (cloneMeta == null) {
            return result;
        }
        if (!(cloneMeta instanceof Repairable repairable)) {
            return result;
        }
        repairable.setRepairCost(anvil.maxRepairCost());
        clone.setItemMeta(cloneMeta);
        event.setResult(clone);
        return clone;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.lastWarningAt.remove(event.getPlayer().getUniqueId());
    }
}
