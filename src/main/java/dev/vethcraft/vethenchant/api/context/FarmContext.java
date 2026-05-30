package dev.vethcraft.vethenchant.api.context;

import dev.vethcraft.vethenchant.util.CustomCropData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public record FarmContext(
    Event event,
    Player player,
    ItemStack item,
    Block crop,
    Material cropType,
    CustomCropData customCrop,
    int level
) {

    public static FarmContext vanilla(Event event, Player player, ItemStack item, Block crop, int level) {
        return new FarmContext(event, player, item, crop, crop.getType(), null, level);
    }

    public static FarmContext custom(Event event, Player player, ItemStack item, Block crop, CustomCropData customCrop, int level) {
        return new FarmContext(event, player, item, crop, crop.getType(), customCrop, level);
    }
}
