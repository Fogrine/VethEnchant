package dev.vethcraft.vethenchant.api.context;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public record BlockDropContext(
    BlockDropItemEvent event,
    Player player,
    ItemStack item,
    int level
) {
}
