package dev.vethcraft.vethenchant.util;

import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemEnchantments {

    private ItemEnchantments() {
    }

    public static Map<Enchantment, Integer> enchantments(ItemStack item) {
        Map<Enchantment, Integer> result = new LinkedHashMap<>();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return result;
        }

        ItemMeta meta = item.getItemMeta();
        result.putAll(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            result.putAll(storageMeta.getStoredEnchants());
        }
        return result;
    }

    public static int highestRegisteredLevel(ItemStack item, EffectDispatcher dispatcher) {
        int level = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchantments(item).entrySet()) {
            if (dispatcher.effect(Registry.ENCHANTMENT.getKey(entry.getKey())).isPresent()) {
                level = Math.max(level, entry.getValue());
            }
        }
        return level;
    }

    public static int level(ItemStack item, NamespacedKey key) {
        if (key == null) {
            return 0;
        }
        int level = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchantments(item).entrySet()) {
            NamespacedKey enchantmentKey = Registry.ENCHANTMENT.getKey(entry.getKey());
            if (key.equals(enchantmentKey)) {
                level = Math.max(level, entry.getValue());
            }
        }
        return level;
    }
}
