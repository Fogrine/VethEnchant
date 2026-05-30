package dev.vethcraft.vethenchant.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

public final class CropUtil {

    private CropUtil() {
    }

    public static boolean isMatureCrop(Block block) {
        return block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }

    public static ItemStack seedFor(Material crop) {
        return switch (crop) {
            case WHEAT -> new ItemStack(Material.WHEAT_SEEDS);
            case CARROTS -> new ItemStack(Material.CARROT);
            case POTATOES -> new ItemStack(Material.POTATO);
            case BEETROOTS -> new ItemStack(Material.BEETROOT_SEEDS);
            case NETHER_WART -> new ItemStack(Material.NETHER_WART);
            default -> null;
        };
    }

    public static void replant(Block block, Material cropType) {
        if (!block.getType().isAir()) {
            return;
        }
        block.setType(cropType, false);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        }
    }
}
