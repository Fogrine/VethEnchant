package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.DamageContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class FreeRunnerEffect implements VethEnchantEffect {

    private static final NamespacedKey KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":freerunner");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onDamaged(DamageContext context) {
        if (context.event().getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        double chance = 0.20D + 0.20D * context.level();
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            context.event().setCancelled(true);
        }
    }
}
