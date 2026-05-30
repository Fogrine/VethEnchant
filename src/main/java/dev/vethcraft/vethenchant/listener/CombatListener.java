package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.api.context.AttackContext;
import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class CombatListener implements Listener {

    private final EffectDispatcher dispatcher;

    public CombatListener(EffectDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = attacker(event);
        if (attacker == null) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        ItemStack item = weapon(event, attacker);
        this.dispatcher.attack(new AttackContext(event, attacker, target, item, ItemEnchantments.highestRegisteredLevel(item, this.dispatcher)));
    }

    private static Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static ItemStack weapon(EntityDamageByEntityEvent event, Player attacker) {
        if (event.getDamager() instanceof AbstractArrow arrow && arrow.getWeapon() != null) {
            return arrow.getWeapon();
        }
        return attacker.getInventory().getItemInMainHand();
    }
}
