package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.AttackContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public final class AbrasionEffect implements VethEnchantEffect {

    private static final NamespacedKey KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":abrasion");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onAttack(AttackContext context) {
        if (!(context.target() instanceof Player target)) {
            return;
        }
        double chance = 0.03D * context.level();
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        for (ItemStack armor : target.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir()) {
                continue;
            }
            armor.damage(context.level(), target);
        }
    }
}
