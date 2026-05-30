package dev.vethcraft.vethenchant.limit;

import dev.vethcraft.vethenchant.config.EnchantDefinitionRegistry;
import dev.vethcraft.vethenchant.config.LimitsConfig;
import dev.vethcraft.vethenchant.config.VethEnchantConfig;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnchantLimitService {

    private final VethEnchantConfig config;

    public EnchantLimitService(VethEnchantConfig config) {
        this.config = config;
    }

    public boolean enabled() {
        return this.config.limits().enabled();
    }

    public SlotSummary summary(ItemStack item) {
        Map<Enchantment, Integer> enchantments = ItemEnchantments.enchantments(item);
        int maxSlots = maxSlots(item);
        return new SlotSummary(enchantments.size(), maxSlots);
    }

    public int maxSlots(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        if (item.getType() == Material.ENCHANTED_BOOK) {
            return this.config.limits().bookMaxSlots("default");
        }
        return this.config.limits().itemRule(itemGroup(item.getType())).maxSlots();
    }

    public boolean wouldExceedAnvil(Player player, ItemStack result) {
        if (!canLimit(player) || !this.config.limits().sourceLimits().anvil().enabled()) {
            return false;
        }
        return exceedsItemLimits(result);
    }

    public void trimEnchantingTable(Map<Enchantment, Integer> enchantsToAdd) {
        if (!this.config.limits().enabled() || !this.config.limits().sourceLimits().enchantingTable().enabled()) {
            return;
        }
        LimitsConfig.EnchantingTable limit = this.config.limits().sourceLimits().enchantingTable();
        trimMap(enchantsToAdd, limit.maxEnchantsAdded(), limit.maxCustomEnchants(), limit.maxRareOrAbove());
    }

    public boolean trimLootItem(ItemStack item) {
        if (!this.config.limits().enabled() || !this.config.limits().sourceLimits().loot().enabled()) {
            return false;
        }
        return trimGeneratedItem(item, this.config.limits().sourceLimits().loot(), "loot");
    }

    public boolean trimFishingItem(ItemStack item) {
        if (!this.config.limits().enabled() || !this.config.limits().sourceLimits().fishing().enabled()) {
            return false;
        }
        return trimGeneratedItem(item, this.config.limits().sourceLimits().fishing(), "fishing");
    }

    public boolean exceedsItemLimits(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Map<Enchantment, Integer> enchantments = ItemEnchantments.enchantments(item);
        if (enchantments.isEmpty()) {
            return false;
        }
        return enchantments.size() > maxSlots(item);
    }

    public boolean canLimit(Player player) {
        return this.config.limits().enabled()
            && (player == null || !player.hasPermission(this.config.limits().bypassPermission()));
    }

    private boolean trimGeneratedItem(ItemStack item, LimitsConfig.LootSource limits, String source) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Map<Enchantment, Integer> enchantments = ItemEnchantments.enchantments(item);
        if (enchantments.isEmpty()) {
            return false;
        }

        if (item.getType() == Material.ENCHANTED_BOOK) {
            LimitsConfig.BookLimit bookLimit = limits.bookLimit();
            Map<Enchantment, Integer> trimmed = new LinkedHashMap<>(enchantments);
            trimMap(trimmed, Math.min(bookLimit.maxSlots(), this.config.limits().bookMaxSlots(source)), bookLimit.maxCustomEnchants(), bookLimit.maxRareOrAbove());
            if (!bookLimit.allowCustomEnchants()) {
                trimmed.entrySet().removeIf(entry -> isCustom(key(entry.getKey())));
            }
            return replaceEnchantments(item, trimmed);
        }

        if (!limits.useItemSlotLimit()) {
            return false;
        }
        Map<Enchantment, Integer> trimmed = trimToItemLimits(item, enchantments);
        return replaceEnchantments(item, trimmed);
    }

    private Map<Enchantment, Integer> trimToItemLimits(ItemStack item, Map<Enchantment, Integer> enchantments) {
        Map<Enchantment, Integer> result = new LinkedHashMap<>(enchantments);
        int max = maxSlots(item);
        while (result.size() > max) {
            removeLowestPriority(result);
        }

        return result;
    }

    private void trimMap(Map<Enchantment, Integer> enchantments, int maxEnchants, int maxCustom, int maxRareOrAbove) {
        while (maxEnchants >= 0 && enchantments.size() > maxEnchants) {
            removeLowestPriority(enchantments);
        }
    }

    private boolean replaceEnchantments(ItemStack item, Map<Enchantment, Integer> enchantments) {
        Map<Enchantment, Integer> current = ItemEnchantments.enchantments(item);
        if (current.equals(enchantments)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment enchantment : new ArrayList<>(storageMeta.getStoredEnchants().keySet())) {
                storageMeta.removeStoredEnchant(enchantment);
            }
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                storageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            }
        } else {
            for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
                meta.removeEnchant(enchantment);
            }
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        item.setItemMeta(meta);
        return true;
    }

    private Map<String, Integer> groupCounts(Map<Enchantment, Integer> enchantments) {
        EnchantDefinitionRegistry registry = this.config.enchants();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Enchantment enchantment : enchantments.keySet()) {
            NamespacedKey key = key(enchantment);
            for (String group : registry.groups(key)) {
                counts.merge(group.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        return counts;
    }

    private int countCustom(Map<Enchantment, Integer> enchantments) {
        int count = 0;
        for (Enchantment enchantment : enchantments.keySet()) {
            if (isCustom(key(enchantment))) {
                count++;
            }
        }
        return count;
    }

    private int countRareOrAbove(Map<Enchantment, Integer> enchantments) {
        int count = 0;
        for (Enchantment enchantment : enchantments.keySet()) {
            if (this.config.isRareOrAbove(key(enchantment))) {
                count++;
            }
        }
        return count;
    }

    private void removeLowestPriority(Map<Enchantment, Integer> enchantments) {
        removeLowestPriority(enchantments, entry -> true);
    }

    private void removeLowestPriorityInGroup(Map<Enchantment, Integer> enchantments, String group) {
        removeLowestPriority(enchantments, entry -> this.config.enchants().groups(key(entry.getKey())).contains(group));
    }

    private void removeLowestPriority(Map<Enchantment, Integer> enchantments, java.util.function.Predicate<Map.Entry<Enchantment, Integer>> predicate) {
        enchantments.entrySet().stream()
            .filter(predicate)
            .min(Comparator
                .comparingInt((Map.Entry<Enchantment, Integer> entry) -> this.config.enchants().priority(key(entry.getKey())))
                .thenComparing(entry -> key(entry.getKey()).asString()))
            .map(Map.Entry::getKey)
            .ifPresent(enchantments::remove);
    }

    private static boolean isCustom(NamespacedKey key) {
        return key != null && !"minecraft".equals(key.getNamespace());
    }

    private static NamespacedKey key(Enchantment enchantment) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        return key == null ? enchantment.getKey() : key;
    }

    private static String itemGroup(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT);
        if (name.endsWith("_sword")) {
            return "sword";
        }
        if (name.endsWith("_axe") && !name.endsWith("_pickaxe")) {
            return "axe";
        }
        if (name.endsWith("_pickaxe")) {
            return "pickaxe";
        }
        if (name.endsWith("_shovel")) {
            return "shovel";
        }
        if (name.endsWith("_hoe")) {
            return "hoe";
        }
        if (name.endsWith("_helmet") || material == Material.TURTLE_HELMET) {
            return "helmet";
        }
        if (name.endsWith("_chestplate")) {
            return "chestplate";
        }
        if (name.endsWith("_leggings")) {
            return "leggings";
        }
        if (name.endsWith("_boots")) {
            return "boots";
        }
        if (material == Material.BOW) {
            return "bow";
        }
        if (material == Material.CROSSBOW) {
            return "crossbow";
        }
        if (material == Material.TRIDENT) {
            return "trident";
        }
        if (material == Material.MACE) {
            return "mace";
        }
        if (material == Material.ELYTRA) {
            return "elytra";
        }
        if (material == Material.FISHING_ROD) {
            return "fishing_rod";
        }
        if (material == Material.SHEARS) {
            return "shears";
        }
        return "default";
    }

    public record SlotSummary(int used, int max) {
    }
}
