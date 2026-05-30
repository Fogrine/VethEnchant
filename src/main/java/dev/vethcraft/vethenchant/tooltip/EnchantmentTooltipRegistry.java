package dev.vethcraft.vethenchant.tooltip;

import dev.vethcraft.vethenchant.config.EnchantDefinition;
import dev.vethcraft.vethenchant.config.VethEnchantConfig;
import org.bukkit.NamespacedKey;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnchantmentTooltipRegistry {

    private final Map<NamespacedKey, TooltipDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, RarityDefinition> rarities = new LinkedHashMap<>();

    public EnchantmentTooltipRegistry(VethEnchantConfig config) {
        this.rarities.putAll(config.rarities());
        for (EnchantDefinition definition : config.enchants().all()) {
            if (!definition.enabled()) {
                continue;
            }
            this.definitions.put(definition.key(), new TooltipDefinition(
                definition.name(),
                definition.rarity(),
                definition.description(),
                definition.icons()
            ));
        }
    }

    public Optional<TooltipDefinition> find(NamespacedKey key) {
        return Optional.ofNullable(this.definitions.get(key));
    }

    public RarityDefinition rarity(String id) {
        String normalized = normalizeRarity(id);
        return this.rarities.getOrDefault(normalized, this.rarities.get("common"));
    }

    public boolean isManagedTooltipStyle(NamespacedKey key) {
        if (key == null) {
            return false;
        }
        for (RarityDefinition rarity : this.rarities.values()) {
            if (key.equals(rarity.tooltipStyle())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRarity(String id) {
        return VethEnchantConfig.normalizeRarity(id);
    }
}
