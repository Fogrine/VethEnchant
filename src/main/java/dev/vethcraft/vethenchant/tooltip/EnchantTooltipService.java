package dev.vethcraft.vethenchant.tooltip;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EnchantTooltipService {

    private final VethEnchantPlugin plugin;
    private EnchantmentTooltipRegistry registry;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey generatedLineCountKey;
    private TagResolver paletteResolver = TagResolver.empty();

    public EnchantTooltipService(VethEnchantPlugin plugin) {
        this.plugin = plugin;
        this.generatedLineCountKey = new NamespacedKey(plugin, "tooltip_lines");
        reload();
    }

    public void reload() {
        this.registry = new EnchantmentTooltipRegistry(this.plugin.vethConfig());
        this.paletteResolver = this.plugin.paletteResolver();
    }

    public void normalizeInventory(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            normalize(item);
        }
    }

    public boolean normalize(ItemStack item) {
        if (!this.plugin.getConfig().getBoolean("tooltip.enabled", true)) {
            return false;
        }
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        List<Component> existingLore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        int oldGeneratedLines = meta.getPersistentDataContainer().getOrDefault(this.generatedLineCountKey, PersistentDataType.INTEGER, 0);
        removeOldGeneratedLore(existingLore, oldGeneratedLines);

        Map<Enchantment, Integer> enchantments = ItemEnchantments.enchantments(item);
        if (enchantments.isEmpty()) {
            if (oldGeneratedLines <= 0) {
                return false;
            }
            meta.getPersistentDataContainer().remove(this.generatedLineCountKey);
            meta.lore(existingLore.isEmpty() ? null : existingLore);
            item.setItemMeta(meta);
            return true;
        }

        List<Component> generated = buildLore(item, enchantments);
        RarityDefinition highestRarity = highestRarity(enchantments);
        if (!existingLore.isEmpty() && this.plugin.getConfig().getBoolean("tooltip.blank-line-before-existing-lore", true)) {
            generated.add(Component.empty());
        }
        List<Component> newLore = new ArrayList<>(generated);
        newLore.addAll(existingLore);

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS);
        if (shouldApplyTooltipStyle(item) && highestRarity.tooltipStyle() != null) {
            meta.setTooltipStyle(highestRarity.tooltipStyle());
        } else if (meta.hasTooltipStyle() && this.registry.isManagedTooltipStyle(meta.getTooltipStyle())) {
            meta.setTooltipStyle(null);
        }
        meta.getPersistentDataContainer().set(this.generatedLineCountKey, PersistentDataType.INTEGER, generated.size());
        meta.lore(newLore);
        item.setItemMeta(meta);
        return true;
    }

    private List<Component> buildLore(ItemStack item, Map<Enchantment, Integer> enchantments) {
        List<Component> lore = new ArrayList<>();
        boolean showIcons = item.getType() == Material.ENCHANTED_BOOK;
        enchantments.entrySet().stream()
            .sorted(Comparator.comparing(entry -> key(entry.getKey()).toString()))
            .forEach(entry -> appendEnchantLore(lore, entry.getKey(), entry.getValue(), showIcons));
        appendSlotLore(lore, item);
        return lore;
    }

    private void appendEnchantLore(List<Component> lore, Enchantment enchantment, int level, boolean showIcons) {
        NamespacedKey key = key(enchantment);
        TooltipDefinition definition = this.registry.find(key)
            .orElseGet(() -> new TooltipDefinition(fallbackName(key), "common", List.of("暂无详细说明。"), List.of()));
        RarityDefinition rarity = this.registry.rarity(definition.rarity());

        String nameLine = replacePlaceholders(rarity.enchantFormat(), definition, rarity, key, level, showIcons);
        if (showIcons) {
            String icons = icons(definition.icons());
            if (!icons.isBlank() && !rarity.enchantFormat().contains("{icons}") && !rarity.enchantFormat().contains("{item_icons}")) {
                nameLine += " " + icons;
            }
        }
        lore.add(this.miniMessage.deserialize(nameLine, this.paletteResolver));

        for (String description : definition.description()) {
            String parsedDescription = replacePlaceholders(description, definition, rarity, key, level, false);
            String line = replacePlaceholders(rarity.descriptionFormat(), definition, rarity, key, level, false)
                .replace("{description}", parsedDescription);
            lore.add(this.miniMessage.deserialize(line, this.paletteResolver));
        }
    }

    private RarityDefinition highestRarity(Map<Enchantment, Integer> enchantments) {
        RarityDefinition highest = this.registry.rarity("common");
        for (Enchantment enchantment : enchantments.keySet()) {
            TooltipDefinition definition = this.registry.find(key(enchantment))
                .orElseGet(() -> new TooltipDefinition(fallbackName(key(enchantment)), "common", List.of(), List.of()));
            RarityDefinition rarity = this.registry.rarity(definition.rarity());
            if (rarity.weight() < highest.weight()) {
                highest = rarity;
            }
        }
        return highest;
    }

    private boolean shouldApplyTooltipStyle(ItemStack item) {
        return item.getType() == Material.ENCHANTED_BOOK
            && this.plugin.getConfig().getBoolean("tooltip.ce.tooltip-style-enabled", true);
    }

    private void appendSlotLore(List<Component> lore, ItemStack item) {
        if (!this.plugin.vethConfig().limits().enabled()
            || !this.plugin.vethConfig().limits().display().showSlotsInLore()) {
            return;
        }
        var summary = this.plugin.limitService().summary(item);
        if (summary.max() <= 0) {
            return;
        }
        String line = this.plugin.vethConfig().limits().display().format()
            .replace("{used}", Integer.toString(summary.used()))
            .replace("{max}", Integer.toString(summary.max()));
        lore.add(this.miniMessage.deserialize(line, this.paletteResolver));
    }

    private String replacePlaceholders(String input, TooltipDefinition definition, RarityDefinition rarity, NamespacedKey key, int level, boolean includeIcons) {
        String iconText = includeIcons ? icons(definition.icons()) : "";
        String result = input
            .replace("{name}", definition.name())
            .replace("{level}", Integer.toString(level))
            .replace("{rarity}", rarity.displayName())
            .replace("{rarity_id}", rarity.id())
            .replace("{key}", key.asString())
            .replace("{icons}", iconText)
            .replace("{item_icons}", iconText);

        result = result.replace("{sharpness_damage}", number(0.5D * level + 0.5D));
        result = result.replace("{bonus_damage_2_5}", number(2.5D * level));
        result = result.replace("{abrasion_chance}", Integer.toString(level * 3));
        result = result.replace("{abrasion_durability}", Integer.toString(level));
        result = result.replace("{wisdom_bonus}", Integer.toString(level * 15));
        result = result.replace("{freerunner_chance}", Integer.toString(20 + level * 20));
        result = result.replace("{block_breather_chance}", Integer.toString(level * 35));
        return result;
    }

    private static String icons(List<String> icons) {
        if (icons == null || icons.isEmpty()) {
            return "";
        }
        List<String> result = new ArrayList<>();
        for (String icon : icons) {
            if (icon == null || icon.isBlank()) {
                continue;
            }
            result.add(normalizeIcon(icon));
        }
        return String.join("", result);
    }

    private static String normalizeIcon(String icon) {
        String value = icon.trim();
        if (value.startsWith("<image:")) {
            return value;
        }
        return "<image:server:gui/menu/enchantments/items/" + value.toLowerCase(java.util.Locale.ROOT) + ">";
    }

    private NamespacedKey key(Enchantment enchantment) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        if (key != null) {
            return key;
        }
        return enchantment.getKey();
    }

    private void removeOldGeneratedLore(List<Component> lore, int oldGeneratedLines) {
        int remove = Math.min(Math.max(oldGeneratedLines, 0), lore.size());
        for (int index = 0; index < remove; index++) {
            lore.remove(0);
        }
    }

    private String fallbackName(NamespacedKey key) {
        String raw = key.getKey().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        boolean uppercase = true;
        for (char c : raw.toCharArray()) {
            if (uppercase && Character.isLetter(c)) {
                builder.append(Character.toUpperCase(c));
                uppercase = false;
            } else {
                builder.append(c);
            }
            if (c == ' ') {
                uppercase = true;
            }
        }
        return builder.toString();
    }

    private static String number(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return Double.toString(value);
    }
}
