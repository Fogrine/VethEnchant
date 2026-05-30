package dev.vethcraft.vethenchant.config;

import org.bukkit.NamespacedKey;

import java.util.List;

public record EnchantDefinition(
    NamespacedKey key,
    String type,
    boolean enabled,
    String name,
    String rarity,
    List<String> description,
    List<String> icons,
    List<String> groups,
    int priority
) {
}
