package dev.vethcraft.vethenchant.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public record LimitsConfig(
    boolean enabled,
    String bypassPermission,
    LegacyMode legacyMode,
    Display display,
    Map<String, SlotRule> itemSlots,
    Map<String, Integer> bookSlots,
    SourceLimits sourceLimits
) {

    public static LimitsConfig defaults() {
        return new LimitsConfig(
            true,
            "vethenchant.limit.bypass",
            LegacyMode.ALLOW_EXISTING,
            new Display(true, "<muted>附魔槽位 </muted><primary>{used}</primary><muted>/</muted><primary>{max}</primary>"),
            Map.of("default", new SlotRule(6, Map.of())),
            Map.of("default", 6, "enchanting-table", 6, "loot", 6, "fishing", 6, "villager", 6, "anvil", 6),
            SourceLimits.defaults()
        );
    }

    public static LimitsConfig load(File file) {
        if (!file.exists()) {
            return defaults();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        LimitsConfig fallback = defaults();
        return new LimitsConfig(
            config.getBoolean("enabled", fallback.enabled()),
            config.getString("bypass-permission", fallback.bypassPermission()),
            LegacyMode.parse(config.getString("legacy-items.mode", fallback.legacyMode().id())),
            new Display(
                config.getBoolean("display.show-slots-in-lore", fallback.display().showSlotsInLore()),
                config.getString("display.format", fallback.display().format())
            ),
            loadItemSlots(config.getConfigurationSection("item-slots"), fallback.itemSlots()),
            loadBookSlots(config.getConfigurationSection("book-slots"), fallback.bookSlots()),
            SourceLimits.load(config.getConfigurationSection("source-limits"), fallback.sourceLimits())
        );
    }

    public SlotRule itemRule(String group) {
        SlotRule fallback = this.itemSlots.getOrDefault("default", new SlotRule(6, Map.of()));
        return this.itemSlots.getOrDefault(group, fallback);
    }

    public int bookMaxSlots(String source) {
        return this.bookSlots.getOrDefault(source, this.bookSlots.getOrDefault("default", 3));
    }

    private static Map<String, SlotRule> loadItemSlots(ConfigurationSection section, Map<String, SlotRule> fallback) {
        Map<String, SlotRule> result = new LinkedHashMap<>(fallback);
        if (section == null) {
            return Map.copyOf(result);
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection rule = section.getConfigurationSection(key);
            if (rule == null) {
                continue;
            }
            Map<String, Integer> groupLimits = new LinkedHashMap<>();
            ConfigurationSection groups = rule.getConfigurationSection("group-limits");
            if (groups != null) {
                for (String group : groups.getKeys(false)) {
                    groupLimits.put(group.toLowerCase(), Math.max(0, groups.getInt(group)));
                }
            }
            SlotRule currentFallback = result.getOrDefault(key.toLowerCase(), result.get("default"));
            result.put(key.toLowerCase(), new SlotRule(rule.getInt("max-slots", currentFallback.maxSlots()), Map.copyOf(groupLimits)));
        }
        return Map.copyOf(result);
    }

    private static Map<String, Integer> loadBookSlots(ConfigurationSection section, Map<String, Integer> fallback) {
        Map<String, Integer> result = new LinkedHashMap<>(fallback);
        if (section == null) {
            return Map.copyOf(result);
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection rule = section.getConfigurationSection(key);
            if (rule != null) {
                result.put(key.toLowerCase(), Math.max(0, rule.getInt("max-slots", result.getOrDefault(key.toLowerCase(), 3))));
            }
        }
        return Map.copyOf(result);
    }

    public enum LegacyMode {
        ALLOW_EXISTING("allow-existing"),
        STRICT("strict");

        private final String id;

        LegacyMode(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }

        public static LegacyMode parse(String raw) {
            if (raw == null) {
                return ALLOW_EXISTING;
            }
            return raw.equalsIgnoreCase("strict") ? STRICT : ALLOW_EXISTING;
        }
    }

    public record Display(boolean showSlotsInLore, String format) {
    }

    public record SlotRule(int maxSlots, Map<String, Integer> groupLimits) {
    }

    public record SourceLimits(
        EnchantingTable enchantingTable,
        LootSource loot,
        LootSource fishing,
        Villager villager,
        Anvil anvil
    ) {
        static SourceLimits defaults() {
            return new SourceLimits(
                new EnchantingTable(true, 3, -1, -1, "trim-lowest-priority"),
                new LootSource(true, new BookLimit(6, -1, -1, true), true, "trim-lowest-priority"),
                new LootSource(true, new BookLimit(6, -1, -1, true), true, "trim-lowest-priority"),
                new Villager(true, 6, false),
                new Anvil(false, "allow", true, 0)
            );
        }

        static SourceLimits load(ConfigurationSection section, SourceLimits fallback) {
            if (section == null) {
                return fallback;
            }
            return new SourceLimits(
                EnchantingTable.load(section.getConfigurationSection("enchanting-table"), fallback.enchantingTable()),
                LootSource.load(section.getConfigurationSection("loot"), fallback.loot()),
                LootSource.load(section.getConfigurationSection("fishing"), fallback.fishing()),
                Villager.load(section.getConfigurationSection("villager"), fallback.villager()),
                Anvil.load(section.getConfigurationSection("anvil"), fallback.anvil())
            );
        }
    }

    public record EnchantingTable(
        boolean enabled,
        int maxEnchantsAdded,
        int maxCustomEnchants,
        int maxRareOrAbove,
        String overflowPolicy
    ) {
        static EnchantingTable load(ConfigurationSection section, EnchantingTable fallback) {
            if (section == null) {
                return fallback;
            }
            return new EnchantingTable(
                section.getBoolean("enabled", fallback.enabled()),
                section.getInt("max-enchants-added", fallback.maxEnchantsAdded()),
                section.getInt("max-custom-enchants", fallback.maxCustomEnchants()),
                section.getInt("max-rare-or-above", fallback.maxRareOrAbove()),
                section.getString("overflow-policy", fallback.overflowPolicy())
            );
        }
    }

    public record LootSource(boolean enabled, BookLimit bookLimit, boolean useItemSlotLimit, String overflowPolicy) {
        static LootSource load(ConfigurationSection section, LootSource fallback) {
            if (section == null) {
                return fallback;
            }
            return new LootSource(
                section.getBoolean("enabled", fallback.enabled()),
                BookLimit.load(section.getConfigurationSection("enchanted-book"), fallback.bookLimit()),
                section.getBoolean("equipment.use-item-slot-limit", fallback.useItemSlotLimit()),
                section.getString("overflow-policy", fallback.overflowPolicy())
            );
        }
    }

    public record BookLimit(int maxSlots, int maxCustomEnchants, int maxRareOrAbove, boolean allowCustomEnchants) {
        static BookLimit load(ConfigurationSection section, BookLimit fallback) {
            if (section == null) {
                return fallback;
            }
            return new BookLimit(
                section.getInt("max-slots", fallback.maxSlots()),
                section.getInt("max-custom-enchants", fallback.maxCustomEnchants()),
                section.getInt("max-rare-or-above", fallback.maxRareOrAbove()),
                section.getBoolean("allow-custom-enchants", fallback.allowCustomEnchants())
            );
        }
    }

    public record Villager(boolean enabled, int maxBookSlots, boolean allowCustomEnchants) {
        static Villager load(ConfigurationSection section, Villager fallback) {
            if (section == null) {
                return fallback;
            }
            ConfigurationSection book = section.getConfigurationSection("enchanted-book");
            return new Villager(
                section.getBoolean("enabled", fallback.enabled()),
                book == null ? fallback.maxBookSlots() : book.getInt("max-slots", fallback.maxBookSlots()),
                book == null ? fallback.allowCustomEnchants() : book.getBoolean("allow-custom-enchants", fallback.allowCustomEnchants())
            );
        }
    }

    public record Anvil(boolean enabled, String overflowPolicy, boolean removePriorWorkPenalty, int maxRepairCost) {
        static Anvil load(ConfigurationSection section, Anvil fallback) {
            if (section == null) {
                return fallback;
            }
            return new Anvil(
                section.getBoolean("enabled", fallback.enabled()),
                section.getString("overflow-policy", fallback.overflowPolicy()),
                section.getBoolean("remove-prior-work-penalty", fallback.removePriorWorkPenalty()),
                Math.max(0, section.getInt("max-repair-cost", fallback.maxRepairCost()))
            );
        }
    }
}
