package dev.vethcraft.vethenchant.tooltip;

import java.util.List;

public record TooltipDefinition(
    String name,
    String rarity,
    List<String> description,
    List<String> icons
) {
}
