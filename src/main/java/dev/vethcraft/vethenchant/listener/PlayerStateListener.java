package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.api.context.DamageContext;
import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerStateListener implements Listener {

    private final EffectDispatcher dispatcher;

    public PlayerStateListener(EffectDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            int level = ItemEnchantments.highestRegisteredLevel(armor, this.dispatcher);
            if (level <= 0) {
                continue;
            }
            this.dispatcher.damaged(new DamageContext(event, player, armor, level));
            if (event.isCancelled()) {
                return;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        int offhandLevel = ItemEnchantments.highestRegisteredLevel(offhand, this.dispatcher);
        if (offhandLevel > 0) {
            this.dispatcher.damaged(new DamageContext(event, player, offhand, offhandLevel));
        }
    }
}
