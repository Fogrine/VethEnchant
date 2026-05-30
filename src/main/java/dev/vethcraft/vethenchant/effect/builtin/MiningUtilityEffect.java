package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.BlockBreakContext;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

public final class MiningUtilityEffect implements VethEnchantEffect {

    private final NamespacedKey key;
    private final VethEnchantPlugin plugin;

    public MiningUtilityEffect(VethEnchantPlugin plugin, String id) {
        this.plugin = plugin;
        this.key = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":" + id);
    }

    @Override
    public NamespacedKey key() {
        return this.key;
    }

    @Override
    public void onBlockBreak(BlockBreakContext context) {
        String id = this.key.getKey();
        switch (id) {
            case "prospector" -> {
                if (isOre(context.event().getBlock().getType())) {
                    context.event().setExpToDrop((int) Math.ceil(context.event().getExpToDrop() * (1.0D + 0.07D * context.level())));
                }
            }
            case "mineral_luck" -> mineralLuck(context);
            case "stonebound", "soft_touch" -> reduceToolWear(context, "stonebound".equals(id) ? 0.10D : 0.07D);
            case "flinting" -> flinting(context);
            case "clayfinder" -> clayfinder(context);
            case "lumber_bounty" -> lumberBounty(context);
            case "deep_delver" -> {
                if (context.event().getBlock().getY() <= 32) {
                    context.event().setExpToDrop(context.event().getExpToDrop() + context.level());
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void onFarm(FarmContext context) {
        if (!"green_thumb".equals(this.key.getKey())) {
            return;
        }
        boostNearbyCrops(context);
    }

    private void mineralLuck(BlockBreakContext context) {
        Block block = context.event().getBlock();
        if (!isOre(block.getType()) || ThreadLocalRandom.current().nextDouble() > 0.035D * context.level()) {
            return;
        }
        Material bonus = switch (block.getType()) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.RAW_GOLD;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case NETHER_QUARTZ_ORE -> Material.QUARTZ;
            default -> Material.AIR;
        };
        if (!bonus.isAir()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), new org.bukkit.inventory.ItemStack(bonus));
        }
    }

    private static void flinting(BlockBreakContext context) {
        if (context.event().getBlock().getType() != Material.GRAVEL
            || ThreadLocalRandom.current().nextDouble() > 0.10D * context.level()) {
            return;
        }
        context.event().getBlock().getWorld().dropItemNaturally(
            context.event().getBlock().getLocation().add(0.5D, 0.5D, 0.5D),
            new ItemStack(Material.FLINT)
        );
    }

    private static void clayfinder(BlockBreakContext context) {
        Material type = context.event().getBlock().getType();
        if ((type != Material.CLAY && type != Material.SAND && type != Material.RED_SAND)
            || ThreadLocalRandom.current().nextDouble() > 0.045D * context.level()) {
            return;
        }
        context.event().getBlock().getWorld().dropItemNaturally(
            context.event().getBlock().getLocation().add(0.5D, 0.5D, 0.5D),
            new ItemStack(Material.CLAY_BALL, 1 + ThreadLocalRandom.current().nextInt(2))
        );
    }

    private static void lumberBounty(BlockBreakContext context) {
        if (!isLog(context.event().getBlock().getType())
            || ThreadLocalRandom.current().nextDouble() > 0.06D * context.level()) {
            return;
        }
        Material bonus = context.event().getBlock().getType().name().contains("MANGROVE")
            ? Material.STICK
            : Material.APPLE;
        context.event().getBlock().getWorld().dropItemNaturally(
            context.event().getBlock().getLocation().add(0.5D, 0.5D, 0.5D),
            new ItemStack(bonus)
        );
    }

    private static void reduceToolWear(BlockBreakContext context, double chancePerLevel) {
        if (ThreadLocalRandom.current().nextDouble() > chancePerLevel * context.level()) {
            return;
        }
        ItemStack item = context.item();
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) {
            return;
        }
        damageable.setDamage(damageable.getDamage() - 1);
        item.setItemMeta(meta);
    }

    private void boostNearbyCrops(FarmContext context) {
        int radius = Math.min(1 + context.level(), 3);
        int changed = 0;
        Block origin = context.crop();
        for (int x = -radius; x <= radius && changed < 3 + context.level(); x++) {
            for (int z = -radius; z <= radius && changed < 3 + context.level(); z++) {
                Block block = origin.getRelative(x, 0, z);
                if (block.equals(origin) || ThreadLocalRandom.current().nextDouble() > 0.16D * context.level()) {
                    continue;
                }
                if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
                    ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + 1));
                    block.setBlockData(ageable, false);
                    changed++;
                    continue;
                }
                if (this.plugin.customCropService().isCustomCropBlock(block)) {
                    if (this.plugin.customCropService().addGrowthPoint(block.getLocation(), 1)) {
                        changed++;
                    }
                }
            }
        }
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }

    private static boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_WOOD") || name.endsWith("_HYPHAE");
    }
}
