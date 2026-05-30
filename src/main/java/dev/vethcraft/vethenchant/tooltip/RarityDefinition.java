package dev.vethcraft.vethenchant.tooltip;

import org.bukkit.NamespacedKey;

public record RarityDefinition(
    String id,
    String displayName,
    int weight,
    NamespacedKey tooltipStyle,
    String enchantFormat,
    String descriptionFormat
) {
}
