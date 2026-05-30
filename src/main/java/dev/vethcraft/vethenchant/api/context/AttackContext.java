package dev.vethcraft.vethenchant.api.context;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public record AttackContext(
    EntityDamageByEntityEvent event,
    Player attacker,
    LivingEntity target,
    ItemStack item,
    int level
) {
}
