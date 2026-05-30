package dev.vethcraft.vethenchant.config;

import org.bukkit.NamespacedKey;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnchantDefinitionRegistry {

    private final Map<NamespacedKey, EnchantDefinition> definitions;

    public EnchantDefinitionRegistry(Map<NamespacedKey, EnchantDefinition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }

    public static EnchantDefinitionRegistry empty() {
        return new EnchantDefinitionRegistry(new LinkedHashMap<>());
    }

    public Optional<EnchantDefinition> find(NamespacedKey key) {
        return Optional.ofNullable(this.definitions.get(key));
    }

    public Collection<EnchantDefinition> all() {
        return this.definitions.values();
    }

    public boolean isEnabled(NamespacedKey key) {
        return find(key).map(EnchantDefinition::enabled).orElse(true);
    }

    public String rarity(NamespacedKey key) {
        return find(key).map(EnchantDefinition::rarity).orElse("common");
    }

    public List<String> groups(NamespacedKey key) {
        return find(key).map(EnchantDefinition::groups).orElse(List.of());
    }

    public int priority(NamespacedKey key) {
        return find(key).map(EnchantDefinition::priority).orElse(0);
    }
}
