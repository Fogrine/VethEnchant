package dev.vethcraft.vethenchant.api.context;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public record DamageContext(
    EntityDamageEvent event,
    Player player,
    ItemStack item,
    int level
) {
}
