package dev.vethcraft.vethenchant.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ConfigHealthReport(
    int totalEnchantments,
    int enabledEnchantments,
    int disabledEnchantments,
    int missingRegistryEntries,
    Map<String, Integer> byType,
    Map<String, Integer> byRarity,
    List<String> warnings
) {

    public static ConfigHealthReport empty() {
        return new ConfigHealthReport(0, 0, 0, 0, Map.of(), Map.of(), List.of());
    }

    public static ConfigHealthReport build(
        EnchantDefinitionRegistry registry,
        Map<String, ?> rarities,
        List<String> loadWarnings
    ) {
        int enabled = 0;
        int disabled = 0;
        int missing = 0;
        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<String, Integer> byRarity = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>(loadWarnings);

        for (EnchantDefinition definition : registry.all()) {
            String type = normalize(definition.type(), "unknown");
            String rarity = VethEnchantConfig.normalizeRarity(definition.rarity());
            byType.merge(type, 1, Integer::sum);
            byRarity.merge(rarity, 1, Integer::sum);

            if (definition.enabled()) {
                enabled++;
                Enchantment enchantment = Registry.ENCHANTMENT.get(definition.key());
                if (enchantment == null) {
                    missing++;
                    warnings.add("附魔 " + definition.key().asString() + " 当前未在注册表中找到，新增/硬修改后通常需要重启服务器。");
                }
            } else {
                disabled++;
            }

            if (!rarities.containsKey(rarity)) {
                warnings.add("附魔 " + definition.key().asString() + " 使用了未知稀有度 " + definition.rarity() + "，展示时会回退到 common。");
            }
        }

        return new ConfigHealthReport(
            enabled + disabled,
            enabled,
            disabled,
            missing,
            Collections.unmodifiableMap(byType),
            Collections.unmodifiableMap(byRarity),
            List.copyOf(warnings)
        );
    }

    public boolean healthy() {
        return this.warnings.isEmpty() && this.missingRegistryEntries == 0;
    }

    public int warningCount() {
        return this.warnings.size();
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
