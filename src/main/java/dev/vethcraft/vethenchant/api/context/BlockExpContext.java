package dev.vethcraft.vethenchant.api.context;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public record BlockExpContext(
    BlockBreakEvent event,
    Player player,
    ItemStack item,
    int level
) {
}
