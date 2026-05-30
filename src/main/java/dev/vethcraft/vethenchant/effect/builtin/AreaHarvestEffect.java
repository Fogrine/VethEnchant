package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.BlockBreakContext;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import dev.vethcraft.vethenchant.effect.EffectExecutionGuards;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import dev.vethcraft.vethenchant.util.CropUtil;
import dev.vethcraft.vethenchant.util.CustomCropData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class AreaHarvestEffect implements VethEnchantEffect {

    private final NamespacedKey key;
    private final VethEnchantPlugin plugin;

    public AreaHarvestEffect(VethEnchantPlugin plugin, String id) {
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
        if ("vein_miner".equals(id)) {
            chainSameType(context, Math.min(8 + context.level() * 6, 28), true);
        } else if ("timber".equals(id) && isLog(context.event().getBlock().getType())) {
            chainSameType(context, Math.min(8 + context.level() * 8, 32), false);
        } else if ("quarry".equals(id)) {
            breakFacingCube(context, 3, 3);
        } else if ("tunneling".equals(id) || "excavator".equals(id)) {
            breakCube(context, 1, 8 + context.level() * 4);
        }
    }

    @Override
    public void onFarm(FarmContext context) {
        if (!"harvester".equals(this.key.getKey())) {
            return;
        }
        if (context.customCrop() != null) {
            harvestCustomCrops(context);
            return;
        }
        harvestVanillaCrops(context);
    }

    private void harvestCustomCrops(FarmContext context) {
        int radius = Math.min(1 + context.level(), 4);
        int limit = 8 + context.level() * 8;
        int changed = 0;
        Block origin = context.crop();
        for (int x = -radius; x <= radius && changed < limit; x++) {
            for (int z = -radius; z <= radius && changed < limit; z++) {
                Block block = origin.getRelative(x, 0, z);
                if (block.equals(origin)) {
                    continue;
                }
                CustomCropData crop = this.plugin.customCropService().find(block).orElse(null);
                if (crop == null || !this.plugin.customCropService().isMature(block)) {
                    continue;
                }
                if (!this.plugin.protectionService().canModifyBlock(context.player(), block)) {
                    continue;
                }
                if (!this.plugin.customCropService().breakWithoutFarmDispatch(context.player(), block)) {
                    continue;
                }
                scheduleCustomReplant(context, block, crop);
                changed++;
            }
        }
    }

    private void scheduleCustomReplant(FarmContext context, Block block, CustomCropData crop) {
        if (!this.plugin.customCropService().consumeSeed(context.player(), crop)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () ->
            this.plugin.customCropService().replant(block.getLocation(), crop), 1L);
    }

    private void harvestVanillaCrops(FarmContext context) {
        int radius = Math.min(1 + context.level(), 4);
        int limit = 8 + context.level() * 8;
        int changed = 0;
        Block origin = context.crop();
        for (int x = -radius; x <= radius && changed < limit; x++) {
            for (int z = -radius; z <= radius && changed < limit; z++) {
                Block block = origin.getRelative(x, 0, z);
                if (block.equals(origin) || block.getType() != context.cropType() || !CropUtil.isMatureCrop(block)) {
                    continue;
                }
                if (!this.plugin.protectionService().canModifyBlock(context.player(), block)) {
                    continue;
                }
                    EffectExecutionGuards.runAreaBreak(() -> block.breakNaturally(context.item(), true, true));
                replantVanilla(context, block);
                changed++;
            }
        }
    }

    private static void replantVanilla(FarmContext context, Block block) {
        ItemStack seed = CropUtil.seedFor(context.cropType());
        if (seed == null || !context.player().getInventory().containsAtLeast(seed, 1)) {
            return;
        }
        context.player().getInventory().removeItem(seed);
        CropUtil.replant(block, context.cropType());
    }

    private void chainSameType(BlockBreakContext context, int limit, boolean oresOnly) {
        Block origin = context.event().getBlock();
        Material type = origin.getType();
        if (oresOnly && !isOre(type)) {
            return;
        }
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        int broken = 0;
        while (!queue.isEmpty() && broken < limit) {
            Block current = queue.removeFirst();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        Block next = current.getRelative(x, y, z);
                        if (!seen.add(next) || next.equals(origin) || next.getType() != type) {
                            continue;
                        }
                        if (!canAreaBreak(context, next)) {
                            continue;
                        }
                        queue.add(next);
                        breakAreaBlock(context, next);
                        broken++;
                        if (broken >= limit) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private void breakFacingCube(BlockBreakContext context, int width, int depth) {
        Block origin = context.event().getBlock();
        Material originType = origin.getType();
        Axis axis = dominantAxis(context.player().getLocation().getDirection());
        int sign = axisSign(context.player().getLocation().getDirection(), axis);
        int radius = Math.max(0, width / 2);
        int broken = 0;
        int limit = Math.max(0, width * width * depth - 1);
        for (int forward = 0; forward < depth && broken < limit; forward++) {
            for (int horizontal = -radius; horizontal <= radius && broken < limit; horizontal++) {
                for (int vertical = -radius; vertical <= radius && broken < limit; vertical++) {
                    Block block = relativeByFacing(origin, axis, sign, forward, horizontal, vertical);
                    if (block.equals(origin) || block.getType().isAir() || block.isLiquid()) {
                        continue;
                    }
                    if (!sameMiningFamily(originType, block.getType())) {
                        continue;
                    }
                    if (!canAreaBreak(context, block)) {
                        continue;
                    }
                    breakAreaBlock(context, block);
                    broken++;
                }
            }
        }
    }

    private void breakCube(BlockBreakContext context, int radius, int limit) {
        Block origin = context.event().getBlock();
        Material originType = origin.getType();
        int broken = 0;
        for (int x = -radius; x <= radius && broken < limit; x++) {
            for (int y = -radius; y <= radius && broken < limit; y++) {
                for (int z = -radius; z <= radius && broken < limit; z++) {
                    Block block = origin.getRelative(x, y, z);
                    if (block.equals(origin) || block.getType().isAir() || block.isLiquid()) {
                        continue;
                    }
                    if (!sameMiningFamily(originType, block.getType())) {
                        continue;
                    }
                    if (!canAreaBreak(context, block)) {
                        continue;
                    }
                    breakAreaBlock(context, block);
                    broken++;
                }
            }
        }
    }

    private boolean canAreaBreak(BlockBreakContext context, Block block) {
        return !this.plugin.placedBlockTracker().shouldSkipAreaBreak(context.player(), block)
            && this.plugin.protectionService().canBreakBlock(context.player(), block);
    }

    private void breakAreaBlock(BlockBreakContext context, Block block) {
        Material type = block.getType();
        EffectExecutionGuards.runAreaBreak(() -> block.breakNaturally(context.item(), true, true));
        applyAreaBreakBonuses(context, block, type);
        this.plugin.placedBlockTracker().unmark(block);
    }

    private void applyAreaBreakBonuses(BlockBreakContext context, Block block, Material brokenType) {
        for (Map.Entry<Enchantment, Integer> entry : context.item().getEnchantments().entrySet()) {
            NamespacedKey enchantKey = Registry.ENCHANTMENT.getKey(entry.getKey());
            if (enchantKey == null || !EnchantCatalog.NAMESPACE.equals(enchantKey.getNamespace())) {
                continue;
            }
            if (!this.plugin.isEnchantEnabled(enchantKey)) {
                continue;
            }
            int level = entry.getValue();
            switch (enchantKey.getKey()) {
                case "wisdom" -> dropAreaExperience(context, block, areaBaseExperience(brokenType), 0.15D * level);
                case "prospector" -> {
                    if (isOre(brokenType)) {
                        dropAreaExperience(context, block, areaBaseExperience(brokenType), 0.07D * level);
                    }
                }
                case "deep_delver" -> {
                    if (block.getY() <= 32) {
                        dropAreaExperience(context, block, level, 0.0D);
                    }
                }
                case "mineral_luck" -> bonusOreDrop(block, brokenType, level, 0.035D);
                case "flinting" -> {
                    if (brokenType == Material.GRAVEL && ThreadLocalRandom.current().nextDouble() <= 0.10D * level) {
                        dropBonus(block, new ItemStack(Material.FLINT));
                    }
                }
                case "clayfinder" -> {
                    if ((brokenType == Material.CLAY || brokenType == Material.SAND || brokenType == Material.RED_SAND)
                        && ThreadLocalRandom.current().nextDouble() <= 0.045D * level) {
                        dropBonus(block, new ItemStack(Material.CLAY_BALL, 1 + ThreadLocalRandom.current().nextInt(2)));
                    }
                }
                case "lumber_bounty" -> {
                    if (isLog(brokenType) && ThreadLocalRandom.current().nextDouble() <= 0.06D * level) {
                        dropBonus(block, new ItemStack(brokenType.name().contains("MANGROVE") ? Material.STICK : Material.APPLE));
                    }
                }
                default -> {
                }
            }
        }
    }

    private void dropAreaExperience(BlockBreakContext context, Block block, int baseExp, double multiplier) {
        int exp = multiplier <= 0.0D ? baseExp : (int) Math.ceil(baseExp * multiplier);
        if (exp <= 0) {
            return;
        }
        if (context.item().getEnchantments().keySet().stream().anyMatch(enchantment -> {
            NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
            return key != null && EnchantCatalog.NAMESPACE.equals(key.getNamespace()) && "telekinesis".equals(key.getKey());
        }) && this.plugin.getConfig().getBoolean("telekinesis.collect-exp", true)) {
            context.player().giveExp(exp);
            return;
        }
        block.getWorld().spawn(block.getLocation().add(0.5D, 0.5D, 0.5D), org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(exp));
    }

    private static int areaBaseExperience(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE, NETHER_GOLD_ORE, NETHER_QUARTZ_ORE -> 1;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> 2;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 3;
            case ANCIENT_DEBRIS -> 2;
            default -> 0;
        };
    }

    private static void bonusOreDrop(Block block, Material brokenType, int level, double chancePerLevel) {
        if (!isOre(brokenType) || ThreadLocalRandom.current().nextDouble() > chancePerLevel * level) {
            return;
        }
        Material bonus = switch (brokenType) {
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
            dropBonus(block, new ItemStack(bonus));
        }
    }

    private static void dropBonus(Block block, ItemStack item) {
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), item);
    }

    private static Block relativeByFacing(Block origin, Axis axis, int sign, int forward, int horizontal, int vertical) {
        return switch (axis) {
            case X -> origin.getRelative(sign * forward, vertical, horizontal);
            case Y -> origin.getRelative(horizontal, sign * forward, vertical);
            case Z -> origin.getRelative(horizontal, vertical, sign * forward);
        };
    }

    private static Axis dominantAxis(Vector direction) {
        double x = Math.abs(direction.getX());
        double y = Math.abs(direction.getY());
        double z = Math.abs(direction.getZ());
        if (y >= x && y >= z) {
            return Axis.Y;
        }
        return x >= z ? Axis.X : Axis.Z;
    }

    private static int axisSign(Vector direction, Axis axis) {
        double value = switch (axis) {
            case X -> direction.getX();
            case Y -> direction.getY();
            case Z -> direction.getZ();
        };
        return value < 0 ? -1 : 1;
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    private static boolean sameMiningFamily(Material origin, Material current) {
        return isStoneLike(origin) && isStoneLike(current)
            || isDirtLike(origin) && isDirtLike(current)
            || isLog(origin) && isLog(current);
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.endsWith("_ORES") || name.equals("ANCIENT_DEBRIS");
    }

    private static boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_WOOD") || name.endsWith("_HYPHAE");
    }

    private static boolean isStoneLike(Material material) {
        String name = material.name();
        return name.contains("STONE") || name.contains("DEEPSLATE") || name.contains("TUFF")
            || name.contains("ANDESITE") || name.contains("DIORITE") || name.contains("GRANITE")
            || name.endsWith("_ORE") || name.equals("NETHERRACK") || name.equals("END_STONE");
    }

    private static boolean isDirtLike(Material material) {
        String name = material.name();
        return name.contains("DIRT") || name.contains("GRASS_BLOCK") || name.contains("SAND")
            || name.contains("GRAVEL") || name.contains("CLAY") || name.contains("MUD");
    }
}
