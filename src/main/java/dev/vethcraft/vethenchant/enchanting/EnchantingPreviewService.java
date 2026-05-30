package dev.vethcraft.vethenchant.enchanting;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.config.EnchantDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantingPreviewService {

    private final VethEnchantPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, List<PreviewLine>> offers = new ConcurrentHashMap<>();

    public EnchantingPreviewService(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(Player player, EnchantmentOffer[] offers) {
        if (player == null || offers == null || !enabled()) {
            return;
        }
        List<PreviewLine> lines = new ArrayList<>();
        for (int index = 0; index < offers.length; index++) {
            EnchantmentOffer offer = offers[index];
            if (offer == null || offer.getEnchantment() == null) {
                continue;
            }
            lines.add(line(index + 1, offer));
        }
        if (lines.isEmpty()) {
            clear(player);
        } else {
            this.offers.put(player.getUniqueId(), List.copyOf(lines));
        }
    }

    public void clear(Player player) {
        if (player != null) {
            this.offers.remove(player.getUniqueId());
        }
    }

    public void clearAll() {
        this.offers.clear();
    }

    public ItemStack decorate(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir() || !enabled()) {
            return item;
        }
        List<PreviewLine> lines = this.offers.get(player.getUniqueId());
        if (lines == null || lines.isEmpty()) {
            return item;
        }
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(parse(config("tooltip.enchanting-preview.title", "<primary>可选附魔预览</primary>")));
        for (PreviewLine line : lines) {
            lore.add(parse(config("tooltip.enchanting-preview.line", "<muted>{index}. </muted><primary>{name}</primary> <text>{level}</text> <muted>- 消耗 {cost} 级</muted>"), line));
            if (!line.description().isBlank()) {
                lore.add(parse(config("tooltip.enchanting-preview.description", "<muted>  {description}</muted>"), line));
            }
        }
        meta.lore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    private PreviewLine line(int index, EnchantmentOffer offer) {
        Enchantment enchantment = offer.getEnchantment();
        NamespacedKey key = key(enchantment);
        EnchantDefinition definition = this.plugin.vethConfig().enchants().find(key).orElse(null);
        String name = definition == null ? fallbackName(key) : definition.name();
        String description = definition == null || definition.description().isEmpty() ? "原版附魔" : String.join(" ", definition.description());
        return new PreviewLine(index, name, offer.getEnchantmentLevel(), offer.getCost(), description, key == null ? "" : key.asString());
    }

    private Component parse(String input) {
        return this.miniMessage.deserialize(input, this.plugin.paletteResolver());
    }

    private Component parse(String input, PreviewLine line) {
        return this.miniMessage.deserialize(
            input,
            this.plugin.paletteResolver(),
            Placeholder.parsed("index", Integer.toString(line.index())),
            Placeholder.parsed("name", line.name()),
            Placeholder.parsed("level", Integer.toString(line.level())),
            Placeholder.parsed("cost", Integer.toString(line.cost())),
            Placeholder.parsed("description", line.description()),
            Placeholder.parsed("key", line.key())
        );
    }

    private String config(String path, String fallback) {
        return this.plugin.getConfig().getString(path, fallback);
    }

    private boolean enabled() {
        return this.plugin.getConfig().getBoolean("tooltip.enchanting-preview-enabled", true);
    }

    private static NamespacedKey key(Enchantment enchantment) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        return key == null ? enchantment.getKey() : key;
    }

    private static String fallbackName(NamespacedKey key) {
        if (key == null) {
            return "未知附魔";
        }
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

    private record PreviewLine(int index, String name, int level, int cost, String description, String key) {
        private PreviewLine {
            name = name == null ? "未知附魔" : name;
            description = description == null ? "" : description;
            key = key == null ? "" : key.toLowerCase(Locale.ROOT);
        }
    }
}
