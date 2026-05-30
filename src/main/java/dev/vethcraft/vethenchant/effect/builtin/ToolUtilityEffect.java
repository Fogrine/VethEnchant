package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.BlockDropContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ToolUtilityEffect implements VethEnchantEffect {

    private static final NamespacedKey TELEKINESIS_KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":telekinesis");
    private static final Map<Material, Material> SMELT_RESULTS = new EnumMap<>(Material.class);
    private static final Map<Material, Material> COMPACT_RESULTS = new EnumMap<>(Material.class);

    static {
        SMELT_RESULTS.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELT_RESULTS.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELT_RESULTS.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        SMELT_RESULTS.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_RESULTS.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_RESULTS.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_RESULTS.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_RESULTS.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_RESULTS.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);

        COMPACT_RESULTS.put(Material.COAL, Material.COAL_BLOCK);
        COMPACT_RESULTS.put(Material.RAW_IRON, Material.RAW_IRON_BLOCK);
        COMPACT_RESULTS.put(Material.RAW_GOLD, Material.RAW_GOLD_BLOCK);
        COMPACT_RESULTS.put(Material.RAW_COPPER, Material.RAW_COPPER_BLOCK);
        COMPACT_RESULTS.put(Material.IRON_INGOT, Material.IRON_BLOCK);
        COMPACT_RESULTS.put(Material.GOLD_INGOT, Material.GOLD_BLOCK);
        COMPACT_RESULTS.put(Material.COPPER_INGOT, Material.COPPER_BLOCK);
        COMPACT_RESULTS.put(Material.REDSTONE, Material.REDSTONE_BLOCK);
        COMPACT_RESULTS.put(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK);
        COMPACT_RESULTS.put(Material.DIAMOND, Material.DIAMOND_BLOCK);
        COMPACT_RESULTS.put(Material.EMERALD, Material.EMERALD_BLOCK);
        COMPACT_RESULTS.put(Material.QUARTZ, Material.QUARTZ_BLOCK);
    }

    private final VethEnchantPlugin plugin;
    private final NamespacedKey key;

    public ToolUtilityEffect(VethEnchantPlugin plugin, String id) {
        this.plugin = plugin;
        this.key = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":" + id);
    }

    @Override
    public NamespacedKey key() {
        return this.key;
    }

    @Override
    public void onBlockDrop(BlockDropContext context) {
        switch (this.key.getKey()) {
            case "smelter" -> smeltDrops(context);
            case "compact" -> compactDrops(context);
            case "magnetism" -> moveDropsToInventory(context);
            case "gemfinder" -> bonusGem(context);
            default -> {
            }
        }
    }

    private static void smeltDrops(BlockDropContext context) {
        for (Item item : context.event().getItems()) {
            ItemStack stack = item.getItemStack();
            Material result = SMELT_RESULTS.get(stack.getType());
            if (result == null) {
                continue;
            }
            item.setItemStack(new ItemStack(result, stack.getAmount()));
        }
    }

    private void compactDrops(BlockDropContext context) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (Item item : new ArrayList<>(context.event().getItems())) {
            ItemStack stack = item.getItemStack();
            Material result = COMPACT_RESULTS.get(stack.getType());
            if (result == null || stack.getAmount() < 9) {
                continue;
            }
            int blocks = stack.getAmount() / 9;
            int remainder = stack.getAmount() % 9;
            item.setItemStack(new ItemStack(result, blocks));
            if (remainder > 0) {
                leftovers.add(new ItemStack(stack.getType(), remainder));
            }
        }
        appendToExistingDrops(context, leftovers);
    }

    private void moveDropsToInventory(BlockDropContext context) {
        if (ItemEnchantments.level(context.item(), TELEKINESIS_KEY) > 0) {
            return;
        }
        int moved = 0;
        int limit = 8 + context.level() * 8;
        for (Item item : new ArrayList<>(context.event().getItems())) {
            if (moved >= limit) {
                return;
            }
            ItemStack stack = item.getItemStack();
            Map<Integer, ItemStack> overflow = context.player().getInventory().addItem(stack);
            if (overflow.isEmpty()) {
                item.remove();
                context.event().getItems().remove(item);
                moved += stack.getAmount();
            } else {
                item.setItemStack(overflow.values().iterator().next());
            }
        }
    }

    private void bonusGem(BlockDropContext context) {
        Material blockType = context.event().getBlock().getType();
        if (!isOre(blockType) || ThreadLocalRandom.current().nextDouble() > 0.025D * context.level()) {
            return;
        }
        Material bonus = switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0 -> Material.AMETHYST_SHARD;
            case 1 -> Material.LAPIS_LAZULI;
            case 2 -> Material.REDSTONE;
            case 3 -> Material.QUARTZ;
            default -> Material.EMERALD;
        };
        appendToExistingDrops(context, List.of(new ItemStack(bonus, 1)));
    }

    private static void appendToExistingDrops(BlockDropContext context, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        List<Item> items = context.event().getItems();
        if (items.isEmpty()) {
            for (ItemStack stack : stacks) {
                context.event().getBlock().getWorld().dropItemNaturally(context.event().getBlock().getLocation(), stack);
            }
            return;
        }
        Item anchor = items.getLast();
        for (ItemStack stack : stacks) {
            items.add(context.event().getBlock().getWorld().dropItemNaturally(anchor.getLocation(), stack));
        }
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }
}
